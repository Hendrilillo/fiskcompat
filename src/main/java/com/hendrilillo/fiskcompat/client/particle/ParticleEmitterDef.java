package com.hendrilillo.fiskcompat.client.particle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.js.ScriptEngines;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Emisor de partículas de Fisk (models/particles/emitters/&lt;nombre&gt;.json): una lista de partículas con su
 * tipo, parte del cuerpo, cantidad, posición, movimiento y condición. Los valores pueden ser distintos en
 * primera ("fp") y tercera persona ("tp").
 */
public final class ParticleEmitterDef {
    public final String id;
    public final List<Emitter> emitters;

    private static final Map<String, Optional<ParticleEmitterDef>> CACHE = new HashMap<>();
    private static final Set<String> REPORTED = new HashSet<>();
    private static ScriptObjectMirror global;

    private ParticleEmitterDef(String id, List<Emitter> emitters) {
        this.id = id;
        this.emitters = emitters;
    }

    /** createResource("PARTICLE_EMITTER", id): se carga al usarse por primera vez. */
    public record Ref(String id) {
        public ParticleEmitterDef get() {
            return ParticleEmitterDef.get(id);
        }
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    public static synchronized ParticleEmitterDef get(String id) {
        Optional<ParticleEmitterDef> c = CACHE.get(id);
        if (c != null) return c.orElse(null);
        ParticleEmitterDef def = null;
        try {
            List<Emitter> list = load(id, 0);
            if (list != null && !list.isEmpty()) def = new ParticleEmitterDef(id, list);
        } catch (Exception e) {
            if (REPORTED.add(id)) FiskCompat.LOGGER.warn("Emisor de partículas {} no válido: {}", id, e.toString());
        }
        CACHE.put(id, Optional.ofNullable(def));
        return def;
    }

    private static List<Emitter> load(String id, int depth) throws Exception {
        if (depth > 16) throw new Exception("herencia circular");
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                .getResource(rl.withPath("models/particles/emitters/" + rl.getPath() + ".json"));
        if (res.isEmpty()) {
            if (REPORTED.add(id)) FiskCompat.LOGGER.warn("No existe el emisor de partículas {}", id);
            return null;
        }
        JsonObject json;
        try (Reader r = res.get().openAsReader()) {
            json = JsonParser.parseReader(r).getAsJsonObject();
        }
        List<Emitter> own = new ArrayList<>();
        if (json.has("particles") && json.get("particles").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("particles")) {
                if (!e.isJsonObject()) continue;
                try {
                    own.add(new Emitter(e.getAsJsonObject()));
                } catch (Exception ex) {
                    if (REPORTED.add(id + ex.getMessage())) FiskCompat.LOGGER.warn("Emisor {}: {}", id, ex.getMessage());
                }
            }
        }
        // Como Fisk: primero las del padre y después las propias
        if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
            List<Emitter> parent = load(json.get("parent").getAsString(), depth + 1);
            if (parent != null && !parent.isEmpty()) {
                List<Emitter> all = new ArrayList<>(parent);
                all.addAll(own);
                return all;
            }
        }
        return own;
    }

    // ------------------------------------------------------------------ valores por perspectiva

    /** Valor que puede ser distinto en primera ("fp") y tercera ("tp") persona. */
    public record PArg<T>(T tp, T fp) {
        public T get(boolean firstPerson) {
            return firstPerson ? fp : tp;
        }

        static <T> PArg<T> read(JsonElement e, T def, Function<JsonElement, T> reader) {
            if (e == null) return new PArg<>(def, def);
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("fp") && o.has("tp")) return new PArg<>(reader.apply(o.get("tp")), reader.apply(o.get("fp")));
                return new PArg<>(def, def);
            }
            T v = reader.apply(e);
            return new PArg<>(v, v);
        }
    }

    /** Condición: true/false o una expresión JavaScript con "entity" y "firstPerson". */
    public interface PBool {
        boolean test(JSEntity entity, boolean firstPerson);

        static PBool read(JsonElement e, boolean def) {
            if (e == null || !e.isJsonPrimitive()) return (en, fp) -> def;
            var p = e.getAsJsonPrimitive();
            if (p.isBoolean()) {
                boolean b = p.getAsBoolean();
                return (en, fp) -> b;
            }
            String s = p.getAsString();
            if (s.equals("true") || s.equals("false")) {
                boolean b = s.equals("true");
                return (en, fp) -> b;
            }
            ScriptObjectMirror fn = compile(s);
            if (fn == null) return (en, fp) -> false;
            return (en, fp) -> {
                try {
                    Object o = fn.call(null, en, fp);
                    return o instanceof Boolean b ? b : o instanceof Number n && n.doubleValue() != 0;
                } catch (Throwable t) {
                    if (REPORTED.add(s)) FiskCompat.LOGGER.warn("Error en la condición de partículas '{}': {}", s, t.toString());
                    return false;
                }
            };
        }
    }

    private static synchronized ScriptObjectMirror compile(String expr) {
        try {
            if (global == null) global = ScriptEngines.newGlobal();
            Object fn = ScriptEngines.eval("(function(entity, firstPerson) { return (\n" + expr + "\n); })", "particle_condition", global);
            if (fn instanceof ScriptObjectMirror m && m.isFunction()) return m;
        } catch (Throwable t) {
            FiskCompat.LOGGER.warn("No se pudo compilar la condición de partículas '{}': {}", expr, t.toString());
        }
        return null;
    }

    private static float[] vec3(JsonElement e) {
        float[] out = new float[3];
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < 3 && i < a.size(); i++) out[i] = a.get(i).getAsFloat();
        }
        return out;
    }

    /** Una partícula del emisor. */
    public static final class Emitter {
        public final String type;
        public final String anchor;
        public final double maxDistance;
        public final PArg<Integer> amount;
        public final PArg<float[]> offset, motion, randOffset, randMotion;
        public final PBool condition, mirror, lockFPYaw;

        Emitter(JsonObject o) throws Exception {
            type = o.has("type") ? o.get("type").getAsString() : null;
            if (type == null) throw new Exception("partícula sin tipo");
            anchor = o.has("anchor") && o.get("anchor").isJsonPrimitive() ? o.get("anchor").getAsString() : null;
            maxDistance = o.has("maxDistance") ? o.get("maxDistance").getAsDouble() : 64;
            amount = PArg.read(o.get("amount"), 1, JsonElement::getAsInt);
            offset = PArg.read(o.get("offset"), new float[3], ParticleEmitterDef::vec3);
            motion = PArg.read(o.get("motion"), new float[3], ParticleEmitterDef::vec3);
            randOffset = PArg.read(o.get("randOffset"), new float[3], ParticleEmitterDef::vec3);
            randMotion = PArg.read(o.get("randMotion"), new float[3], ParticleEmitterDef::vec3);
            condition = PBool.read(o.get("condition"), true);
            mirror = PBool.read(o.get("mirror"), false);
            lockFPYaw = PBool.read(o.get("lockFPYaw"), false);
        }
    }
}
