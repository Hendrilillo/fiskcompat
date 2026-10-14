package com.hendrilillo.fiskcompat.power;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Rayo recto desde un punto: se para en el primer bloque o en la primera entidad que toca. */
public final class BeamCast {
    public record Hit(Vec3 end, Entity entity, BlockHitResult block) {
        public double distance(Vec3 from) {
            return end.distanceTo(from);
        }
    }

    private BeamCast() {}

    public static Hit cast(Level level, Entity shooter, Vec3 from, Vec3 dir, double range, double radius) {
        Vec3 to = from.add(dir.scale(range));
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                shooter == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(shooter)));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();

        Entity best = null;
        double bestDist = from.distanceToSqr(end);
        Vec3 bestPoint = end;
        double r = Math.max(0, radius);
        AABB area = new AABB(from, end).inflate(r + 1);
        for (Entity e : level.getEntities(shooter, area, e -> e.isPickable() && !e.isSpectator() && (shooter == null || e != shooter.getVehicle()))) {
            AABB box = e.getBoundingBox().inflate(r + e.getPickRadius());
            Optional<Vec3> hit = box.contains(from) ? Optional.of(from) : box.clip(from, end);
            if (hit.isEmpty()) continue;
            double d = from.distanceToSqr(hit.get());
            if (d < bestDist) {
                bestDist = d;
                best = e;
                bestPoint = hit.get();
            }
        }
        return best != null ? new Hit(bestPoint, best, null)
                : new Hit(end, null, block.getType() == HitResult.Type.MISS ? null : block);
    }

    /** Rayo desde los ojos de la entidad en la dirección en la que mira. */
    public static Hit fromEyes(LivingEntity shooter, double range, double radius, float partialTicks) {
        return cast(shooter.level(), shooter, shooter.getEyePosition(partialTicks), shooter.getViewVector(partialTicks), range, radius);
    }
}
