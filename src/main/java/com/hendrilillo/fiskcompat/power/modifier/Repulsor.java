package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.BeamCast;
import com.hendrilillo.fiskcompat.power.DamageProfile;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Disparo de repulsor (fiskheroes:repulsor_blast): clic derecho mientras se apunta.
 * Es un proyectil muy rápido que avanza "speed" bloques por tick hasta "range".
 */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class Repulsor {
    public static final String TYPE = "fiskheroes:repulsor_blast", AIMING = "fiskheroes:aiming";

    private static final class Ray {
        final LivingEntity shooter;
        Vec3 pos;
        final Vec3 dir;
        final double speed, radius;
        double remaining;
        final DamageProfile damage;

        Ray(LivingEntity shooter, Vec3 pos, Vec3 dir, double speed, double range, double radius, DamageProfile damage) {
            this.shooter = shooter;
            this.pos = pos;
            this.dir = dir;
            this.speed = speed;
            this.remaining = range;
            this.radius = radius;
            this.damage = damage;
        }
    }

    private static final List<Ray> RAYS = new ArrayList<>();
    private static final com.hendrilillo.fiskcompat.util.EntityMap<LivingEntity, Long> LAST_SHOT = new com.hendrilillo.fiskcompat.util.EntityMap<>();

    private Repulsor() {}

    public static int cooldown(ModifierEntry m) {
        return (int) m.num("cooldownTime", 20);
    }

    /** Servidor: el jugador pide disparar. */
    public static void shoot(ServerPlayer p) {
        ActiveHero hero = ActiveHero.of(p);
        if (hero == null || !(DataStore.get(p, AIMING) instanceof Boolean b && b)) return;
        ModifierEntry m = hero.enabledModifier(JSEntity.of(p), TYPE);
        if (m == null) return;
        long now = p.level().getGameTime();
        Long last = LAST_SHOT.get(p);
        if (last != null && now - last < Math.max(0, cooldown(m) - 2)) return;
        LAST_SHOT.put(p, now);

        double scale = DataStore.get(p, "fiskheroes:scale") instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : 1;
        double range = m.num("range", 32) * scale, speed = Math.max(0.5, m.num("speed", 10) * scale);
        double radius = m.num("radius", 0.1) * scale, spread = m.num("spread", 0);
        Vec3 dir = p.getLookAngle();
        if (spread > 0) {
            var rnd = p.getRandom();
            dir = dir.xRot((float) Math.toRadians((rnd.nextFloat() - 0.5) * spread))
                    .yRot((float) Math.toRadians((rnd.nextFloat() - 0.5) * spread)).normalize();
        }
        Vec3 eye = p.getEyePosition();
        RAYS.add(new Ray(p, eye, dir, speed, range, radius, DamageProfile.of(m)));
        Net.RepulsorShot msg = new Net.RepulsorShot(p.getId(), eye.x, eye.y, eye.z, (float) dir.x, (float) dir.y, (float) dir.z,
                (float) speed, (float) range, (float) radius);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, msg);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (Iterator<Ray> it = RAYS.iterator(); it.hasNext(); ) {
            Ray r = it.next();
            if (r.shooter.isRemoved()) {
                it.remove();
                continue;
            }
            double step = Math.min(r.speed, r.remaining);
            BeamCast.Hit hit = BeamCast.cast(r.shooter.level(), r.shooter, r.pos, r.dir, step, r.radius);
            if (hit.entity() != null) {
                r.damage.apply(hit.entity(), r.shooter, 1);
                it.remove();
            } else if (hit.block() != null) {
                it.remove();
            } else {
                r.pos = hit.end();
                r.remaining -= step;
                if (r.remaining <= 1e-3) it.remove();
            }
        }
    }
}
