package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.BeamCast;
import com.hendrilillo.fiskcompat.power.DamageProfile;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.util.EntityMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * fiskheroes:lightning_cast: clic derecho con la mano vacía (sin agacharse ni apuntar). Rayo instantáneo
 * hasta "range" que daña a lo que toca y, con "chainRadius", a lo que haya alrededor (repartiendo el daño).
 * En Sind son los cohetes del brazo.
 */
public final class LightningCast {
    public static final String TYPE = "fiskheroes:lightning_cast";
    private static final EntityMap<LivingEntity, Long> LAST = new EntityMap<>();

    private LightningCast() {}

    public static int cooldown(ModifierEntry m) {
        return (int) m.num("cooldownTime", 20);
    }

    /** ¿Puede lanzarlo ahora (sin contar el tiempo de espera)? Vale en los dos lados. */
    public static ModifierEntry usable(LivingEntity p) {
        if (!p.getMainHandItem().isEmpty() || p.isShiftKeyDown()) return null;
        if (DataStore.get(p, Repulsor.AIMING) instanceof Boolean b && b) return null;
        ActiveHero hero = ActiveHero.of(p);
        return hero == null ? null : hero.enabledModifier(JSEntity.of(p), TYPE);
    }

    public static void cast(ServerPlayer p) {
        ModifierEntry m = usable(p);
        if (m == null) return;
        long now = p.level().getGameTime();
        Long last = LAST.get(p);
        if (last != null && now - last < Math.max(0, cooldown(m) - 2)) return;
        LAST.put(p, now);

        double range = m.num("range", 16);
        BeamCast.Hit hit = BeamCast.fromEyes(p, range, 0, 1);
        DamageProfile dmg = DamageProfile.of(m);
        Entity target = hit.entity();
        if (target != null) dmg.apply(target, p, 1);

        double radius = m.num("chainRadius", 0);
        double[] chain = new double[0];
        if (radius > 0) {
            Vec3 c = hit.end();
            List<Entity> others = p.level().getEntities(p, new AABB(c, c).inflate(radius),
                    e -> e != target && e instanceof LivingEntity && e.isAlive() && e.position().distanceTo(c) <= radius + 1);
            chain = new double[others.size() * 3];
            float share = 1f / Math.max(others.size(), 1);
            for (int i = 0; i < others.size(); i++) {
                Entity e = others.get(i);
                e.invulnerableTime = 0;
                dmg.apply(e, p, share);
                Vec3 ce = e.getBoundingBox().getCenter();
                chain[i * 3] = ce.x;
                chain[i * 3 + 1] = ce.y;
                chain[i * 3 + 2] = ce.z;
            }
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        Vec3 end = hit.end();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new Net.LightningCast(p.getId(), end.x, end.y, end.z, chain));
    }
}
