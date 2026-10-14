package com.hendrilillo.fiskcompat.client.beam;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** createResource("BEAM_RENDERER", "ns:nombre") → models/beams/nombre.json: cómo se ve un rayo. */
public final class BeamRendererDef extends AbstractJSObject {
    public record Pulse(float length, float size, float frequency) {}

    public record Bloom(float strength, float spread, float dropoff, float quality, boolean scaleToSize) {}

    /** type: LASER o LINE. coreColor -1 = sin núcleo. */
    public record Aspect(String type, float opacity, float scale, float spin, Pulse pulse, int coreColor, Bloom bloom) {}

    public final String id;
    public final List<Aspect> aspects;

    private static final Map<String, BeamRendererDef> CACHE = new ConcurrentHashMap<>();

    private BeamRendererDef(String id, List<Aspect> aspects) {
        this.id = id;
        this.aspects = aspects;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static BeamRendererDef get(String id) {
        return CACHE.computeIfAbsent(id, BeamRendererDef::load);
    }

    private static BeamRendererDef load(String id) {
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "fiskheroes";
        String path = id.substring(id.indexOf(':') + 1);
        List<Aspect> aspects = new ArrayList<>();
        try {
            String text = HeroPackManager.readAsset(ns, "models/beams/" + path + ".json").orElse(null);
            if (text == null) {
                FiskCompat.LOGGER.warn("[FiskCompat] No existe el rayo {}", id);
            } else {
                JsonObject root = JsonParser.parseString(text).getAsJsonObject();
                if (root.has("aspects")) for (JsonElement e : root.getAsJsonArray("aspects")) aspects.add(aspect(e.getAsJsonObject()));
            }
        } catch (Exception e) {
            FiskCompat.LOGGER.warn("[FiskCompat] Error leyendo el rayo {}: {}", id, e.toString());
        }
        if (aspects.isEmpty()) {
            aspects.add(new Aspect("LASER", 1, 1, 0, null, 0xFFFFFF, new Bloom(0.75f, 7.5f, 2, 1, false)));
        }
        return new BeamRendererDef(id, aspects);
    }

    private static float f(JsonObject o, String k, float def) {
        JsonElement e = o == null ? null : o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : def;
    }

    static int color(JsonElement e, int def) {
        if (e == null || !e.isJsonPrimitive()) return def;
        if (e.getAsJsonPrimitive().isNumber()) return e.getAsInt() & 0xFFFFFF;
        String s = e.getAsString().trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16);
            if (s.startsWith("#")) return Integer.parseInt(s.substring(1), 16);
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static Aspect aspect(JsonObject o) {
        String type = o.has("type") ? o.get("type").getAsString() : "LASER";
        Pulse pulse = null;
        if (o.has("pulse") && o.get("pulse").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("pulse");
            pulse = new Pulse(f(p, "length", 1), f(p, "size", 0), f(p, "frequency", 1));
        }
        int core = -1;
        if (o.has("core") && o.get("core").isJsonObject()) core = color(o.getAsJsonObject("core").get("color"), 0xFFFFFF);
        else if ("LINE".equals(type)) core = 0xFFFFFF;
        Bloom bloom = null;
        if (o.has("bloom") && o.get("bloom").isJsonObject()) {
            JsonObject b = o.getAsJsonObject("bloom");
            bloom = new Bloom(f(b, "strength", 1), f(b, "spread", 5), f(b, "dropoff", 2), f(b, "quality", 1),
                    b.has("scaleToSize") && b.get("scaleToSize").getAsBoolean());
        }
        return new Aspect(type, f(o, "opacity", 1), f(o, "scale", 1), f(o, "spin", 0), pulse, core, bloom);
    }

    @Override
    public Object getMember(String name) {
        return new Dummy("beam[" + id + "]." + name);
    }
}
