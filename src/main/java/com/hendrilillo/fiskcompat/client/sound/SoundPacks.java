package com.hendrilillo.fiskcompat.client.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Sonidos de los heropacks. Como en Fisk, cada heropack dice en su heropack.json de qué repositorio de
 * GitHub se descargan ("sounds.download") y a qué distancia se oye cada evento ("sounds.range").
 * El zip descargado se guarda en .minecraft/fiskcompat/sounds y se sirve como si estuviera dentro del
 * heropack (assets/&lt;dominio&gt;/sounds.json y assets/&lt;dominio&gt;/sounds/...).
 */
public final class SoundPacks {
    /** Distancia y a quién se envía, como el "range" de Fisk. */
    public record Range(float distance, boolean self, boolean others) {
        public static final Range DEFAULT = new Range(16, true, true);

        static Range read(JsonElement e) {
            if (e == null || e.isJsonNull()) return new Range(Float.POSITIVE_INFINITY, true, true);
            if (e.isJsonPrimitive()) {
                try {
                    float f = e.getAsFloat();
                    return new Range(f < 0 ? Float.POSITIVE_INFINITY : f, true, true);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            if (!e.isJsonObject()) return null;
            JsonObject o = e.getAsJsonObject();
            float d = 16;
            if (o.has("distance")) d = o.get("distance").isJsonNull() ? -1 : o.get("distance").getAsFloat();
            boolean self = true, others = true;
            if (o.has("target")) {
                switch (o.get("target").getAsString()) {
                    case "ONLY_SELF" -> others = false;
                    case "OTHERS_IN_RANGE", "ALL_OTHERS" -> self = false;
                    default -> {}
                }
            }
            return new Range(d < 0 ? Float.POSITIVE_INFINITY : d, self, others);
        }
    }

    private record Repo(String domain, String repository, int version) {
        String url() {
            return "https://raw.githubusercontent.com/" + repository + "/master/sounds/" + version + ".zip";
        }

        String fileName() {
            return domain + "-" + version + ".zip";
        }
    }

    private static final Map<String, FileSystem> ZIPS = new ConcurrentHashMap<>();
    private static final Map<String, Range> RANGES = new ConcurrentHashMap<>();
    private static CompletableFuture<Void> ready = CompletableFuture.completedFuture(null);
    private static String signature;

    private SoundPacks() {}

    public static Path cacheDir() {
        return FMLPaths.GAMEDIR.get().resolve("fiskcompat").resolve("sounds");
    }

    /** Empieza (una vez por cada conjunto de heropacks) a descargar o abrir los zips de sonidos. */
    public static synchronized void ensureStarted() {
        List<Repo> repos = new ArrayList<>();
        Map<String, Range> ranges = new HashMap<>();
        for (HeroPack pack : HeroPackManager.packs()) {
            if (!pack.info().has("sounds") || !pack.info().get("sounds").isJsonObject()) continue;
            JsonObject s = pack.info().getAsJsonObject("sounds");
            if (s.has("download") && s.get("download").isJsonObject()) {
                JsonObject d = s.getAsJsonObject("download");
                try {
                    repos.add(new Repo(pack.domain(), d.get("repository").getAsString(), (int) d.get("version").getAsDouble()));
                } catch (Exception e) {
                    FiskCompat.LOGGER.warn("heropack.json de {}: 'sounds.download' no válido", pack.domain());
                }
            }
            if (s.has("range") && s.get("range").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : s.getAsJsonObject("range").entrySet()) {
                    Range r = Range.read(e.getValue());
                    if (r != null) ranges.put(pack.domain() + ":" + e.getKey(), r);
                }
            }
        }
        String sig = repos.toString();
        if (sig.equals(signature)) return;
        signature = sig;
        RANGES.clear();
        RANGES.putAll(ranges);
        closeAll();
        ready = CompletableFuture.runAsync(() -> {
            for (Repo r : repos) {
                try {
                    Path zip = fetch(r);
                    if (zip != null) ZIPS.put(r.domain(), FileSystems.newFileSystem(zip));
                } catch (Exception e) {
                    FiskCompat.LOGGER.error("No se pudieron cargar los sonidos de {} ({}): {}", r.domain(), r.url(), e.toString());
                }
            }
        });
    }

    private static void closeAll() {
        for (FileSystem fs : ZIPS.values()) {
            try {
                fs.close();
            } catch (IOException ignored) {
            }
        }
        ZIPS.clear();
    }

    /** Devuelve el zip de la caché o lo descarga. */
    private static Path fetch(Repo r) throws IOException {
        Path dir = cacheDir();
        Files.createDirectories(dir);
        Path file = dir.resolve(r.fileName());
        if (Files.isRegularFile(file) && Files.size(file) > 0) return file;
        FiskCompat.LOGGER.info("Descargando los sonidos de {} desde {}", r.domain(), r.url());
        HttpURLConnection c = (HttpURLConnection) URI.create(r.url()).toURL().openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "FiskCompat");
        int code = c.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        Path tmp = dir.resolve(r.fileName() + ".part");
        try (InputStream in = c.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        // borrar versiones antiguas del mismo dominio
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> {
                String n = p.getFileName().toString();
                return n.startsWith(r.domain() + "-") && n.endsWith(".zip") && !n.equals(r.fileName());
            }).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
        FiskCompat.LOGGER.info("Sonidos de {} descargados ({} KB)", r.domain(), Files.size(file) / 1024);
        return file;
    }

    private static void await() {
        try {
            ready.get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            FiskCompat.LOGGER.warn("Los sonidos de los heropacks aún no están listos: {}", e.toString());
        }
    }

    /** ¿Es una ruta que pueden aportar los zips de sonidos? */
    public static boolean isSoundPath(String path) {
        return path.equals("sounds.json") || path.startsWith("sounds/");
    }

    /** Abre assets/&lt;ns&gt;/&lt;path&gt; del zip de sonidos, o null. */
    public static InputStream open(String namespace, String path) throws IOException {
        ensureStarted();
        await();
        FileSystem fs = ZIPS.get(namespace);
        if (fs == null) return null;
        Path p = fs.getPath("/" + path);
        if (!Files.isRegularFile(p)) return null;
        if (path.equals("sounds.json")) return new java.io.ByteArrayInputStream(convertSoundsJson(namespace, Files.readString(p)));
        return Files.newInputStream(p);
    }

    /**
     * En 1.7.10 las rutas de sounds.json sin dominio son del dominio del propio archivo; en 1.21 serían
     * de "minecraft". Se les añade el dominio.
     */
    static byte[] convertSoundsJson(String namespace, String text) {
        try {
            JsonObject root = com.google.gson.JsonParser.parseString(text).getAsJsonObject();
            for (Map.Entry<String, JsonElement> ev : root.entrySet()) {
                if (!ev.getValue().isJsonObject()) continue;
                JsonObject o = ev.getValue().getAsJsonObject();
                o.remove("category");
                if (!o.has("sounds") || !o.get("sounds").isJsonArray()) continue;
                com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                for (JsonElement snd : o.getAsJsonArray("sounds")) {
                    if (snd.isJsonPrimitive()) {
                        String n = snd.getAsString();
                        out.add(n.contains(":") ? n : namespace + ":" + n);
                    } else if (snd.isJsonObject()) {
                        JsonObject so = snd.getAsJsonObject();
                        boolean event = so.has("type") && "event".equals(so.get("type").getAsString());
                        if (!event && so.has("name") && !so.get("name").getAsString().contains(":")) {
                            so.addProperty("name", namespace + ":" + so.get("name").getAsString());
                        }
                        out.add(so);
                    }
                }
                o.add("sounds", out);
            }
            return root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            FiskCompat.LOGGER.error("sounds.json de {} no válido: {}", namespace, e.toString());
            return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public static boolean has(String namespace, String path) {
        ensureStarted();
        await();
        FileSystem fs = ZIPS.get(namespace);
        return fs != null && Files.isRegularFile(fs.getPath("/" + path));
    }

    /** Archivos bajo assets/&lt;ns&gt;/&lt;dir&gt; del zip (rutas relativas a assets/&lt;ns&gt;/). */
    public static List<String> list(String namespace, String dir) {
        ensureStarted();
        await();
        List<String> out = new ArrayList<>();
        FileSystem fs = ZIPS.get(namespace);
        if (fs == null) return out;
        Path base = fs.getPath("/" + dir);
        if (!Files.isDirectory(base)) return out;
        try (Stream<Path> w = Files.walk(base)) {
            w.filter(Files::isRegularFile).forEach(p -> out.add(p.toString().substring(1)));
        } catch (IOException ignored) {
        }
        return out;
    }

    public static Set<String> namespaces() {
        return ZIPS.keySet();
    }

    /** Alcance de un evento de sonido ("sind:unibeam"). */
    public static Range range(String eventId) {
        return RANGES.getOrDefault(eventId, Range.DEFAULT);
    }
}
