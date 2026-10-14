package com.hendrilillo.fiskcompat.client.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.js.ScriptEngines;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Un evento de sonido de Fisk (assets/&lt;ns&gt;/events/sounds/&lt;ruta&gt;.json): qué sonido suena, cuándo
 * empieza y sigue sonando, volumen, tono, bucle, retraso y fundidos. Los valores que no tiene se
 * heredan del "parent".
 */
public final class SoundEventDef {
    public final String id;
    String parent;
    String sound;
    SoundExpr start, cont, volume, pitch;
    Boolean loop, scale;
    Integer delay, fadeIn, fadeOut;
    String overridePath;
    Override override;

    private static final Map<String, Optional<SoundEventDef>> CACHE = new HashMap<>();
    private static final Map<String, Override> FUNCS = new HashMap<>();
    private static final Set<String> REPORTED = new HashSet<>();

    private SoundEventDef(String id) {
        this.id = id;
    }

    public static synchronized void clearCache() {
        CACHE.clear();
        FUNCS.clear();
    }

    /** Evento por su id ("sind:beep", "fiskheroes:iron_man_flight_loop"), o null si no existe o no vale. */
    public static synchronized SoundEventDef get(String id) {
        Optional<SoundEventDef> c = CACHE.get(id);
        if (c != null) return c.orElse(null);
        SoundEventDef def = null;
        try {
            def = load(id, 0);
            if (def != null) def.init();
        } catch (Exception e) {
            if (REPORTED.add(id)) FiskCompat.LOGGER.warn("Evento de sonido {} no válido: {}", id, e.getMessage());
            def = null;
        }
        CACHE.put(id, Optional.ofNullable(def));
        return def;
    }

    private static JsonObject readJson(ResourceLocation rl) throws Exception {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) return null;
        try (Reader r = res.get().openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static SoundEventDef load(String id, int depth) throws Exception {
        if (depth > 16) throw new Exception("herencia circular");
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        JsonObject json = readJson(rl.withPath("events/sounds/" + rl.getPath() + ".json"));
        if (json == null) {
            if (REPORTED.add(id)) FiskCompat.LOGGER.warn("No existe el evento de sonido {}", id);
            return null;
        }
        SoundEventDef d = new SoundEventDef(id);
        d.read(json);
        if (d.parent != null) {
            SoundEventDef p = load(d.parent, depth + 1);
            if (p != null) d.inherit(p);
        }
        return d;
    }

    private void read(JsonObject j) {
        if (j.has("parent") && j.get("parent").isJsonPrimitive()) parent = j.get("parent").getAsString();
        if (j.has("sound") && j.get("sound").isJsonPrimitive()) sound = j.get("sound").getAsString();
        if (j.has("override") && j.get("override").isJsonPrimitive()) overridePath = j.get("override").getAsString();
        if (j.has("condition")) {
            JsonElement c = j.get("condition");
            if (c.isJsonObject()) {
                // Con "start"/"continue", lo que falte vale true
                start = c.getAsJsonObject().has("start") ? SoundExpr.read(c.getAsJsonObject().get("start"), true) : null;
                cont = c.getAsJsonObject().has("continue") ? SoundExpr.read(c.getAsJsonObject().get("continue"), true) : null;
                if (start == null) start = SoundExpr.constant(true);
                if (cont == null) cont = SoundExpr.constant(true);
            } else {
                start = cont = SoundExpr.read(c, true);
            }
        }
        if (j.has("volume")) volume = SoundExpr.read(j.get("volume"), false);
        if (j.has("pitch")) pitch = SoundExpr.read(j.get("pitch"), false);
        loop = bool(j, "loop");
        scale = bool(j, "scale");
        delay = integer(j, "delay");
        fadeIn = integer(j, "fadeIn");
        fadeOut = integer(j, "fadeOut");
    }

    private static Boolean bool(JsonObject j, String k) {
        JsonElement e = j.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : null;
    }

    private static Integer integer(JsonObject j, String k) {
        JsonElement e = j.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? (int) e.getAsDouble() : null;
    }

    private void inherit(SoundEventDef p) {
        if (start == null) start = p.start;
        if (cont == null) cont = p.cont;
        if (volume == null) volume = p.volume;
        if (pitch == null) pitch = p.pitch;
        if (loop == null) loop = p.loop;
        if (scale == null) scale = p.scale;
        if (delay == null) delay = p.delay;
        if (fadeIn == null) fadeIn = p.fadeIn;
        if (fadeOut == null) fadeOut = p.fadeOut;
        if (overridePath == null) overridePath = p.overridePath;
    }

    private void init() throws Exception {
        if (sound == null || sound.isEmpty()) throw new Exception("no tiene sonido");
        if (loop == null) loop = false;
        if (scale == null) scale = true;
        if (delay == null) delay = 0;
        if (fadeIn == null) fadeIn = 0;
        if (fadeOut == null) fadeOut = 0;
        if (overridePath != null) override = loadOverride(overridePath);
    }

    // ------------------------------------------------------------------ lectura

    public boolean shouldStart(Entity e, Object ctx) {
        return start == null || SoundExpr.asBool(start.eval(e, ctx), true);
    }

    public boolean shouldContinue(Entity e, Object ctx) {
        return cont == null || SoundExpr.asBool(cont.eval(e, ctx), true);
    }

    public float volume(Entity e, Object ctx) {
        return volume == null ? 1 : SoundExpr.asFloat(volume.eval(e, ctx), 1);
    }

    public float pitch(Entity e, Object ctx) {
        return pitch == null ? 1 : SoundExpr.asFloat(pitch.eval(e, ctx), 1);
    }

    public String sound() { return sound; }
    public boolean loop() { return loop; }
    public boolean scale() { return scale; }
    public int delay() { return delay; }
    public int fadeIn() { return fadeIn; }
    public int fadeOut() { return fadeOut; }
    public Override override() { return override; }

    // ------------------------------------------------------------------ funciones "override"

    /** events/sounds/&lt;ruta&gt;.js con una función continuePlaying(entity, sound). */
    public static final class Override {
        private final String id;
        private final ScriptObjectMirror fn;

        Override(String id, ScriptObjectMirror fn) {
            this.id = id;
            this.fn = fn;
        }

        public boolean continuePlaying(Entity e, Object jsSound) {
            try {
                return SoundExpr.asBool(fn.call(null, JSEntity.of(e), jsSound), false);
            } catch (Throwable t) {
                if (REPORTED.add("func " + id)) FiskCompat.LOGGER.warn("Error en {}: {}", id, t.toString());
                return false;
            }
        }
    }

    private static Override loadOverride(String path) throws Exception {
        if (FUNCS.containsKey(path)) return FUNCS.get(path);
        ResourceLocation rl = ResourceLocation.tryParse(path);
        Override o = null;
        if (rl != null) {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                    .getResource(rl.withPath("events/sounds/" + rl.getPath() + ".js"));
            if (res.isPresent()) {
                String src;
                try (var in = res.get().open()) {
                    src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                ScriptObjectMirror global = ScriptEngines.newGlobal();
                ScriptEngines.eval(src, path, global);
                if (global.get("continuePlaying") instanceof ScriptObjectMirror m && m.isFunction()) o = new Override(path, m);
            }
        }
        if (o == null) throw new Exception("no existe la función " + path);
        FUNCS.put(path, o);
        return o;
    }
}
