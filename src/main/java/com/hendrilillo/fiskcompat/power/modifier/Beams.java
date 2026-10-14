package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.api.DataManagerApi;
import com.hendrilillo.fiskcompat.power.BeamCast;
import com.hendrilillo.fiskcompat.power.DamageProfile;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Rayos continuos de Fisk: charged_beam (unibeam), energy_projection y heat_vision.
 * La lógica se ejecuta en los dos lados; el daño solo en el servidor.
 */
public final class Beams {
    public static final String CHARGING = "fiskheroes:beam_charging", CHARGE = "fiskheroes:beam_charge",
            SHOOTING = "fiskheroes:beam_shooting", SHOOTING_TIMER = "fiskheroes:beam_shooting_timer",
            ENERGY = "fiskheroes:energy_projection", ENERGY_TIMER = "fiskheroes:energy_projection_timer",
            HEAT = "fiskheroes:heat_vision", HEAT_TIMER = "fiskheroes:heat_vision_timer",
            HEAT_LENGTH = "fiskheroes:heat_vision_length";

    private Beams() {}

    static double scale(ModifierContext c) {
        double s = c.num("fiskheroes:scale");
        return s > 0 ? s : 1;
    }

    // ------------------------------------------------------------------ charged_beam

    static void chargedBeam(ModifierContext c, ModifierEntry m, boolean enabled) {
        boolean charging = c.bool(CHARGING);
        double charge = c.num(CHARGE);
        if (enabled) {
            if (charging && charge >= 1) {
                double s = c.num(SHOOTING);
                if (s < 1) {
                    double duration = Math.max(1, m.num("duration", 40));
                    c.set(SHOOTING, (float) Math.min(s + 1 / duration, 1));
                    shoot(c, m, (float) Math.min(s * duration / 7, 1), 6, true);
                } else {
                    c.set(CHARGING, false);
                    charging = false;
                }
            }
            DataManagerApi.incr(c.entity, CHARGE, m.num("chargeTime", 20), m.num("cooldownTime", 20), charging, !charging);
            if (c.local && !m.isToggle()) c.setLocal(CHARGING, charging && c.keyPressed("CHARGED_BEAM"));
        }
        // BEAM_SHOOTING vuelve a 0 cuando ni se carga ni queda carga
        if (!c.bool(CHARGING) && c.num(CHARGE) <= 0 && c.num(SHOOTING) != 0) c.set(SHOOTING, 0f);
        boolean firing = enabled && c.bool(CHARGING) && c.num(CHARGE) >= 1 && c.num(SHOOTING) < 1;
        DataManagerApi.incr(c.entity, SHOOTING_TIMER, 4, 4, firing, !firing);
    }

    // ------------------------------------------------------------------ energy_projection / heat_vision

    static void energyProjection(ModifierContext c, ModifierEntry m, boolean enabled) {
        continuous(c, m, enabled, ENERGY, ENERGY_TIMER, "ENERGY_PROJECTION", 4);
    }

    static void heatVision(ModifierContext c, ModifierEntry m, boolean enabled) {
        continuous(c, m, enabled, HEAT, HEAT_TIMER, "HEAT_VISION", 0);
    }

    private static void continuous(ModifierContext c, ModifierEntry m, boolean enabled, String var, String timer, String key, int resetEvery) {
        boolean on = c.bool(var);
        if (on && enabled) shoot(c, m, 1, resetEvery, false);
        if (c.local && enabled && !m.isToggle()) c.setLocal(var, c.keyPressed(key));
        boolean v = c.bool(var);
        DataManagerApi.incr(c.entity, timer, 4, 4, v, !v);
    }

    /** Lanza el rayo desde los ojos y hace daño (solo en el servidor). */
    static void shoot(ModifierContext c, ModifierEntry m, float factor, int resetEvery, boolean knock) {
        LivingEntity e = c.entity;
        double scale = scale(c);
        BeamCast.Hit hit = BeamCast.fromEyes(e, m.num("range", 16) * scale, m.num("radius", 0.1) * scale, 1);
        if (m.type.equals("fiskheroes:heat_vision")) c.set(HEAT_LENGTH, (float) hit.distance(e.getEyePosition()));
        if (e.level().isClientSide || hit.entity() == null) return;
        Entity target = hit.entity();
        if (resetEvery > 0 && e.tickCount % resetEvery == 0) target.invulnerableTime = 0;
        DamageProfile.of(m).apply(target, e, 1);
        if (knock && target instanceof LivingEntity le) {
            Vec3 look = e.getLookAngle();
            float k = (float) (0.2 * factor * Math.sqrt(scale));
            le.push(look.x * k, look.y * k * 0.5, look.z * k);
        }
    }
}
