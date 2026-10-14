package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Caché de renderers de héroes y resolución de texturas en el cliente. */
public final class ClientHeroes {
    private static final Map<String, Optional<HeroRendererScript>> RENDERERS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<ResourceLocation>> TEXTURES = new ConcurrentHashMap<>();

    private ClientHeroes() {}

    public static void clearTextures() {
        TEXTURES.clear();
        TxTextures.clear();
    }

    public static void clear() {
        RENDERERS.clear();
        TEXTURES.clear();
    }

    /** Renderer de un traje (o variante); si la variante no tiene script propio se usa el del héroe base. */
    public static HeroRendererScript renderer(String suitId) {
        if (suitId == null) return null;
        return RENDERERS.computeIfAbsent(suitId, id -> {
            String file = id.replace('/', '_');
            String toLoad = HeroRendererScript.RENDERERS.exists(file) ? id : id.contains("/") ? id.substring(0, id.indexOf('/')) : id;
            try {
                return Optional.of(HeroRendererScript.load(toLoad));
            } catch (Throwable e) {
                FiskCompat.LOGGER.error("Error cargando el renderer de {}: {}", id, e.getMessage());
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * Convierte una ruta de textura de Fisk ("sind:mark2/mark2_layer1") en la textura de Minecraft
     * ("sind:textures/heroes/mark2/mark2_layer1.png"). Devuelve null si no existe.
     */
    public static ResourceLocation heroTexture(String fiskPath, Object entity) {
        if (fiskPath == null) return null;
        if (fiskPath.endsWith(".tx.json")) return TxTextures.resolve(fiskPath, entity);
        return heroTexture(fiskPath);
    }

    public static ResourceLocation heroTexture(String fiskPath) {
        if (fiskPath == null) return null;
        return TEXTURES.computeIfAbsent(fiskPath, p -> {
            if (p.endsWith(".tx.json")) return Optional.empty(); // texturas dinámicas: pendiente
            int c = p.indexOf(':');
            String ns = c < 0 ? "fiskheroes" : p.substring(0, c);
            String path = c < 0 ? p : p.substring(c + 1);
            ResourceLocation rl = ResourceLocation.tryBuild(ns, "textures/heroes/" + path + ".png");
            if (rl == null) return Optional.empty();
            if (Minecraft.getInstance().getResourceManager().getResource(rl).isEmpty()) return Optional.empty();
            return Optional.of(rl);
        }).orElse(null);
    }
}
