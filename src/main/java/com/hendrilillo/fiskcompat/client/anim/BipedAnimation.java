package com.hendrilillo.fiskcompat.client.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.client.effect.EffectObject;
import com.hendrilillo.fiskcompat.fsk.FskScript;
import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * renderer.createResource("ANIMATION", "ns:ruta"): animación del cuerpo del jugador en .fsk
 * (o .anim.json, que junta varios .fsk). Se añade con renderer.addCustomAnimation(nombre, anim).
 */
public final class BipedAnimation extends AbstractJSObject {
    private static final Map<String, Optional<FskScript>> CACHE = new ConcurrentHashMap<>();

    public final String path;
    Object dataFunc;
    Object condition;
    public double priority;

    public BipedAnimation(String path) {
        this.path = path;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    @Override
    public Object getMember(String name) {
        return switch (name) {
            case "setData" -> new JsFunction("setData", a -> { dataFunc = a[0]; return this; });
            case "setCondition" -> new JsFunction("setCondition", a -> { condition = a[0]; return this; });
            case "priority" -> priority;
            default -> new Dummy("animation." + name);
        };
    }

    @Override
    public void setMember(String name, Object value) {
        if (name.equals("priority")) priority = EffectObject.num(value);
    }

    public FskScript script() {
        return CACHE.computeIfAbsent(path, p -> Optional.ofNullable(load(p))).orElse(null);
    }

    private static String read(String id, String ext) {
        int c = id.indexOf(':');
        String ns = c < 0 ? "fiskheroes" : id.substring(0, c), p = c < 0 ? id : id.substring(c + 1);
        if (!p.endsWith(ext)) p = p + ext;
        ResourceLocation rl = ResourceLocation.tryBuild(ns, "models/animations/" + p);
        if (rl == null) return null;
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static FskScript load(String id) {
        if (id.endsWith(".anim.json")) {
            String json = read(id, "");
            if (json == null) return null;
            StringBuilder sb = new StringBuilder();
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            for (JsonElement in : o.getAsJsonArray("inputs")) {
                String src = read(in.getAsString(), ".fsk");
                if (src != null) sb.append(src).append('\n');
            }
            return FskScript.compile(sb.toString());
        }
        String src = read(id, ".fsk");
        return src == null ? null : FskScript.compile(src);
    }
}
