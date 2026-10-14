package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.util.EntityMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** fiskheroes:moving: si la entidad se ha movido desde el tick anterior (lo calcula cada lado). */
public final class Movement {
    private static final EntityMap<Entity, Vec3> LAST = new EntityMap<>();

    private Movement() {}

    public static void tick(Entity e) {
        Vec3 pos = e.getBoundingBox().getCenter();
        Vec3 prev = LAST.put(e, pos);
        if (prev != null) DataStore.set(e, "fiskheroes:moving", pos.distanceToSqr(prev) > 1.0E-6);
    }
}
