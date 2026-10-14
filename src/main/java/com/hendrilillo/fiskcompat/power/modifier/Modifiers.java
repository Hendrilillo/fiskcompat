package com.hendrilillo.fiskcompat.power.modifier;

import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.api.DataManagerApi;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import com.hendrilillo.fiskcompat.power.ModifierEntry;

import java.util.HashMap;
import java.util.Map;

/** Implementaciones de los modificadores de Fisk que se ejecutan cada tick. */
public final class Modifiers {
    public interface Modifier {
        void tick(ModifierContext c, ModifierEntry m, boolean enabled);
    }

    private static final Map<String, Modifier> TICKING = new HashMap<>();
    /** Modificadores que no necesitan tick (se aplican en eventos de daño, respiración...). */
    private static final java.util.Set<String> PASSIVE = java.util.Set.of(
            "fiskheroes:fire_immunity", "fiskheroes:projectile_immunity", "fiskheroes:damage_immunity",
            "fiskheroes:water_breathing", "fiskheroes:fire_resistance", "fiskheroes:damage_resistance",
            "fiskheroes:metal_skin", "fiskheroes:potion_immunity");

    static {
        TICKING.put("fiskheroes:transformation", Modifiers::transformation);
        TICKING.put("fiskheroes:cooldown", Modifiers::cooldown);
        TICKING.put("fiskheroes:shield", Modifiers::shield);
        TICKING.put("fiskheroes:controlled_flight", ControlledFlight::tick);
    }

    private Modifiers() {}

    public static void tick(ModifierContext c, ModifierEntry m, boolean enabled) {
        Modifier mod = TICKING.get(m.type);
        if (mod != null) mod.tick(c, m, enabled);
        else if (!PASSIVE.contains(m.type)) Unimplemented.report("modifier " + m.type);
    }

    public static boolean isPassive(String type) {
        return PASSIVE.contains(type);
    }

    // ------------------------------------------------------------------ transformation

    static void transformation(ModifierContext c, ModifierEntry m, boolean enabled) {
        JsonObject t = m.obj("transformation");
        if (t == null || !t.has("toggleData") || !t.has("timerData")) return;
        String toggle = t.get("toggleData").getAsString(), timer = t.get("timerData").getAsString();
        double time = t.has("time") ? Math.max(1, t.get("time").getAsDouble()) : 20;
        boolean on = c.bool(toggle);
        DataManagerApi.incr(c.entity, timer, time, time, on, !on);
        if (c.local && enabled && !m.isToggle() && m.key() != null) {
            c.setLocal(toggle, c.keyPressed(m.key()));
        }
    }

    // ------------------------------------------------------------------ cooldown

    static void cooldown(ModifierContext c, ModifierEntry m, boolean enabled) {
        if (!enabled) return;
        JsonObject cd = m.obj("cooldown");
        if (cd == null || !cd.has("cooldownData") || !cd.has("toggleData")) return;
        String cooldown = cd.get("cooldownData").getAsString(), toggle = cd.get("toggleData").getAsString();
        double duration = cd.has("duration") ? cd.get("duration").getAsDouble() : 0;
        double recovery = cd.has("recovery") ? cd.get("recovery").getAsDouble() : 1;
        if (duration <= 0) {
            c.set(cooldown, 0f);
            return;
        }
        if (c.local && !m.isToggle() && m.key() != null) c.setLocal(toggle, c.keyPressed(m.key()));
        boolean on = c.bool(toggle);
        double v = c.num(cooldown);
        if (on && v < 1) {
            v += 1 / duration;
            if (v >= 1) c.set(toggle, false);
        } else if (!on && v > 0) {
            v -= 1 / duration * recovery;
        }
        c.set(cooldown, (float) Math.max(0, Math.min(1, v)));
    }

    // ------------------------------------------------------------------ shield

    static void shield(ModifierContext c, ModifierEntry m, boolean enabled) {
        if (c.local) {
            boolean block = enabled && c.keyPressed("SHIELD") && c.num("fiskheroes:shield_cooldown") <= 0;
            c.setLocal("fiskheroes:shield_blocking", block);
        }
        if (!enabled && c.bool("fiskheroes:shield_blocking")) c.set("fiskheroes:shield_blocking", false);
        DataManagerApi.incr(c.entity, "fiskheroes:shield_blocking_timer", 5, 5, c.bool("fiskheroes:shield_blocking"), !c.bool("fiskheroes:shield_blocking"));
    }
}
