package com.hendrilillo.fiskcompat.pack;

import com.hendrilillo.fiskcompat.FiskCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Busca y mantiene los heropacks cargados. Se buscan en las carpetas "heropacks" y "fiskheroes"
 * dentro de la carpeta del juego. El jar original de Fisk's Superheroes también vale como heropack
 * (aporta los scripts y recursos base "fiskheroes:").
 */
public final class HeroPackManager {
    public static final String[] FOLDERS = {"heropacks", "fiskheroes"};
    private static final List<HeroPack> PACKS = new ArrayList<>();

    private HeroPackManager() {}

    public static synchronized void load(Path gameDir) {
        closeAll();
        try {
            Files.createDirectories(gameDir.resolve(FOLDERS[0]));
        } catch (IOException ignored) {
        }
        for (String folder : FOLDERS) {
            Path dir = gameDir.resolve(folder);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : s.sorted().toList()) {
                    try {
                        HeroPack pack = HeroPack.open(p);
                        if (pack != null) {
                            PACKS.add(pack);
                            FiskCompat.LOGGER.info("Heropack encontrado: {} [{}]", pack.displayName(), pack);
                        }
                    } catch (Exception e) {
                        FiskCompat.LOGGER.error("No se pudo abrir el heropack {}", p, e);
                    }
                }
            } catch (IOException e) {
                FiskCompat.LOGGER.error("No se pudo leer la carpeta {}", dir, e);
            }
        }
        // El pack base de Fisk va primero para que sus recursos actúen como base.
        PACKS.sort(Comparator.comparing(p -> !p.domain().equals("fiskheroes")));
        if (PACKS.stream().noneMatch(p -> p.domain().equals("fiskheroes"))) {
            FiskCompat.LOGGER.warn("No se ha encontrado el jar de Fisk's Superheroes en /heropacks. "
                    + "Muchos heropacks usan sus scripts base (fiskheroes:external/utils...) y fallarán sin él.");
        }
    }

    public static synchronized void closeAll() {
        for (HeroPack p : PACKS) {
            try {
                p.close();
            } catch (IOException ignored) {
            }
        }
        PACKS.clear();
    }

    public static List<HeroPack> packs() {
        return Collections.unmodifiableList(PACKS);
    }

    public static boolean isLoaded(String domain) {
        return PACKS.stream().anyMatch(p -> p.domain().equals(domain));
    }

    /** Busca un archivo "assets/&lt;ns&gt;/&lt;path&gt;" en los packs; el último pack cargado tiene prioridad. */
    public static Optional<HeroPack> findAsset(String namespace, String path) {
        String rel = "assets/" + namespace + "/" + path;
        for (int i = PACKS.size() - 1; i >= 0; i--) {
            if (PACKS.get(i).has(rel)) return Optional.of(PACKS.get(i));
        }
        return Optional.empty();
    }

    public static Optional<String> readAsset(String namespace, String path) {
        Optional<HeroPack> pack = findAsset(namespace, path);
        if (pack.isEmpty()) return Optional.empty();
        try {
            return Optional.of(pack.get().readString("assets/" + namespace + "/" + path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static InputStream openAsset(String namespace, String path) throws IOException {
        Optional<HeroPack> pack = findAsset(namespace, path);
        if (pack.isEmpty()) throw new IOException("No existe " + namespace + ":" + path);
        return pack.get().open("assets/" + namespace + "/" + path);
    }

    /** Todos los archivos bajo assets/&lt;ns&gt;/&lt;dir&gt; de todos los packs, como "ns:ruta/relativa/al/dir". */
    public static Set<String> listAssets(String dir) {
        Set<String> out = new LinkedHashSet<>();
        for (HeroPack pack : PACKS) {
            for (String ns : pack.namespaces()) {
                String prefix = "assets/" + ns + "/" + dir;
                for (String f : pack.listFiles(prefix)) {
                    out.add(ns + ":" + f.substring(prefix.length()));
                }
            }
        }
        return out;
    }
}
