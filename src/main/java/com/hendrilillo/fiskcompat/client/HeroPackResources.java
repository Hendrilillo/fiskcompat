package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource pack virtual que expone a Minecraft los assets de todos los heropacks
 * (texturas, .mcmeta...) más algunos archivos generados por el mod (modelos de iconos, atlas).
 */
public final class HeroPackResources extends AbstractPackResources {
    public static final String ID = "fiskcompat_heropacks";
    /** Archivos generados: "assets/ns/ruta" -> contenido. */
    private static final Map<String, byte[]> GENERATED = new ConcurrentHashMap<>();

    public HeroPackResources(PackLocationInfo location) {
        super(location);
    }

    public static PackLocationInfo locationInfo() {
        return new PackLocationInfo(ID, Component.literal("FiskCompat: Heropacks"), PackSource.BUILT_IN, Optional.empty());
    }

    public static Pack.ResourcesSupplier supplier() {
        return new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo location) {
                return new HeroPackResources(location);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new HeroPackResources(location);
            }
        };
    }

    public static void putGenerated(String namespace, String path, String content) {
        GENERATED.put("assets/" + namespace + "/" + path, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void clearGenerated() {
        GENERATED.clear();
    }

    /** Prefijo de los modelos de iconos generados: fiskcompat:item/icon/&lt;ns&gt;/&lt;ruta&gt;. */
    public static final String ICON_PREFIX = "item/icon/";

    static {
        putGenerated("minecraft", "atlases/blocks.json",
                "{\"sources\":[{\"type\":\"directory\",\"source\":\"items/heroes\",\"prefix\":\"items/heroes/\"}]}");
    }

    /** Modelo de ítem para un icono de Fisk: fiskcompat:models/item/icon/sind/mark2/0.json -> textura sind:items/heroes/mark2/0. */
    private static byte[] iconModel(ResourceLocation location) {
        if (!location.getNamespace().equals(FiskCompat.MODID)) return null;
        String prefix = "models/" + ICON_PREFIX;
        String path = location.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
        String rest = path.substring(prefix.length(), path.length() - 5);
        int slash = rest.indexOf('/');
        if (slash < 0) return null;
        String tex = rest.substring(0, slash) + ":items/heroes/" + rest.substring(slash + 1);
        return ("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + tex + "\"}}").getBytes(StandardCharsets.UTF_8);
    }

    private static boolean allowedNamespace(String ns) {
        return !ns.equals("minecraft") && !ns.equals(FiskCompat.MODID) && !ns.equals("realms");
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals("pack.mcmeta")) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            String json = "{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Assets de los heropacks\"}}";
            return () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        String key = "assets/" + location.getNamespace() + "/" + location.getPath();
        byte[] gen = GENERATED.get(key);
        if (gen != null) return () -> new ByteArrayInputStream(gen);
        byte[] icon = iconModel(location);
        if (icon != null) return () -> new ByteArrayInputStream(icon);
        if (!allowedNamespace(location.getNamespace())) return null;
        Optional<HeroPack> pack = HeroPackManager.findAsset(location.getNamespace(), location.getPath());
        if (pack.isEmpty()) return null;
        HeroPack p = pack.get();
        return () -> p.open(key);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        String genPrefix = "assets/" + namespace + "/" + path;
        GENERATED.forEach((k, v) -> {
            if (k.startsWith(genPrefix)) {
                ResourceLocation rl = ResourceLocation.tryBuild(namespace, k.substring(("assets/" + namespace + "/").length()));
                if (rl != null) output.accept(rl, () -> new ByteArrayInputStream(v));
            }
        });
        if (!allowedNamespace(namespace)) return;
        for (HeroPack pack : HeroPackManager.packs()) {
            if (!pack.namespaces().contains(namespace)) continue;
            String base = "assets/" + namespace + "/";
            for (String file : pack.listFiles(base + path)) {
                ResourceLocation rl = ResourceLocation.tryBuild(namespace, file.substring(base.length()));
                if (rl != null) output.accept(rl, () -> pack.open(file));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> out = new LinkedHashSet<>();
        if (type != PackType.CLIENT_RESOURCES) return out;
        for (HeroPack pack : HeroPackManager.packs()) {
            for (String ns : pack.namespaces()) if (allowedNamespace(ns) && ResourceLocation.isValidNamespace(ns)) out.add(ns);
        }
        for (String k : GENERATED.keySet()) out.add(k.split("/")[1]);
        return out;
    }

    @Override
    public void close() {
    }
}
