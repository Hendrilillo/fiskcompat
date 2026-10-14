package com.hendrilillo.fiskcompat.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Un heropack de Fisk (carpeta, .zip o .jar) que contiene un heropack.json en la raíz
 * y sus recursos bajo assets/&lt;dominio&gt;/.
 */
public final class HeroPack implements Closeable {
    private final Path source;
    private final FileSystem fileSystem;
    private final Path root;
    private final JsonObject info;
    private final String domain;

    private HeroPack(Path source, FileSystem fileSystem, Path root, JsonObject info) {
        this.source = source;
        this.fileSystem = fileSystem;
        this.root = root;
        this.info = info;
        this.domain = info.has("domain") ? info.get("domain").getAsString() : "unknown";
    }

    /** Abre un heropack o devuelve null si la ruta no es uno. */
    public static HeroPack open(Path path) throws IOException {
        FileSystem fs = null;
        Path root;
        if (Files.isDirectory(path)) {
            root = path;
        } else {
            String name = path.getFileName().toString().toLowerCase();
            if (!name.endsWith(".zip") && !name.endsWith(".jar")) return null;
            fs = FileSystems.newFileSystem(path);
            root = fs.getPath("/");
        }
        Path infoFile = root.resolve("heropack.json");
        if (!Files.isRegularFile(infoFile)) {
            if (fs != null) fs.close();
            return null;
        }
        JsonObject info = JsonParser.parseString(Files.readString(infoFile, StandardCharsets.UTF_8)).getAsJsonObject();
        return new HeroPack(path, fs, root, info);
    }

    public String domain() { return domain; }
    public JsonObject info() { return info; }
    public Path source() { return source; }

    public String displayName() {
        String n = info.has("name") ? info.get("name").getAsString() : domain;
        return n.replaceAll("§.", "");
    }

    private Path resolve(String rel) {
        return root.resolve(rel.startsWith("/") ? rel.substring(1) : rel);
    }

    public boolean has(String rel) {
        return Files.isRegularFile(resolve(rel));
    }

    public InputStream open(String rel) throws IOException {
        return Files.newInputStream(resolve(rel));
    }

    public String readString(String rel) throws IOException {
        return Files.readString(resolve(rel), StandardCharsets.UTF_8);
    }

    /** Lista recursivamente los archivos bajo un directorio relativo (rutas relativas a la raíz, con '/'). */
    public List<String> listFiles(String relDir) {
        Path dir = resolve(relDir);
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(p -> out.add(root.relativize(p).toString().replace('\\', '/')));
        } catch (IOException ignored) {
        }
        return out;
    }

    /** Namespaces presentes bajo assets/. */
    public Set<String> namespaces() {
        Set<String> out = new LinkedHashSet<>();
        Path assets = resolve("assets");
        if (Files.isDirectory(assets)) {
            try (Stream<Path> s = Files.list(assets)) {
                s.filter(Files::isDirectory).forEach(p -> {
                    String n = p.getFileName().toString();
                    if (n.endsWith("/")) n = n.substring(0, n.length() - 1);
                    out.add(n);
                });
            } catch (IOException ignored) {
            }
        }
        return out;
    }

    @Override
    public void close() throws IOException {
        if (fileSystem != null) fileSystem.close();
    }

    @Override
    public String toString() {
        return domain + " (" + source.getFileName() + ")";
    }
}
