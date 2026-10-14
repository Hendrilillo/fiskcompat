package com.hendrilillo.fiskcompat.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** entity.world() de Fisk. */
public final class JSWorld {
    private final Level level;

    private JSWorld(Level level) {
        this.level = level;
    }

    public static JSWorld wrap(Level level) {
        return new JSWorld(level);
    }

    private static BlockPos pos(double x, double y, double z) {
        return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    /** Número de dimensión de la 1.7.10: 0 superficie, -1 Nether, 1 End. */
    public int getDimension() {
        var key = level.dimension();
        if (key == Level.OVERWORLD) return 0;
        if (key == Level.NETHER) return -1;
        if (key == Level.END) return 1;
        return Math.abs(key.location().toString().hashCode() % 10000) + 2;
    }

    public String name() {
        return level.dimension().location().toString();
    }

    public boolean isDaytime() { return level.isDay(); }
    public boolean isRaining() { return level.isRaining(); }
    public boolean isThundering() { return level.isThundering(); }

    public JSBlock blockAt(double x, double y, double z) { return new JSBlock(level, pos(x, y, z)); }
    public JSBlock blockAt(JSVector.Vec3 v) { return blockAt(v.x(), v.y(), v.z()); }

    public String getBlock(double x, double y, double z) { return LegacyBlocks.name(level.getBlockState(pos(x, y, z))); }
    public String getBlock(JSVector.Vec3 v) { return getBlock(v.x(), v.y(), v.z()); }

    public int getBlockMetadata(double x, double y, double z) { return LegacyBlocks.meta(level.getBlockState(pos(x, y, z))); }
    public int getBlockMetadata(JSVector.Vec3 v) { return getBlockMetadata(v.x(), v.y(), v.z()); }

    public JSEntity getEntityById(int id) {
        Entity e = level.getEntity(id);
        return e == null ? null : JSEntity.of(e);
    }

    public JSLocation getLocation(double x, double y, double z) { return new JSLocation(level, pos(x, y, z)); }
    public JSLocation getLocation(JSVector.Vec3 v) { return getLocation(v.x(), v.y(), v.z()); }

    public List<JSEntity> getEntitiesInRangeOf(JSVector.Vec3 v, double range) {
        Vec3 c = new Vec3(v.x(), v.y(), v.z());
        List<JSEntity> out = new ArrayList<>();
        for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(range), e -> e.position().distanceToSqr(c) <= range * range)) {
            out.add(JSEntity.of(e));
        }
        return out;
    }

    public boolean isUnobstructed(JSVector.Vec3 a, JSVector.Vec3 b) {
        Vec3 from = new Vec3(a.x(), a.y(), a.z()), to = new Vec3(b.x(), b.y(), b.z());
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    /** world.blockAt(...) */
    public static final class JSBlock {
        private final Level level;
        private final BlockPos pos;
        private final BlockState state;

        JSBlock(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
            this.state = level.getBlockState(pos);
        }

        public boolean matches(JSBlock o) { return o != null && o.state.getBlock() == state.getBlock(); }
        public boolean isEmpty() { return state.isAir(); }
        public String name() { return LegacyBlocks.name(state); }
        public int metadata() { return LegacyBlocks.meta(state); }
        public float hardness() { return state.getDestroySpeed(level, pos); }
        public boolean isSolid() { return state.isSolidRender(level, pos) || state.blocksMotion(); }
        public JSItem item() { return new JSItem(new net.minecraft.world.item.ItemStack(state.getBlock().asItem())); }
        public JSItem itemDropped() { return item(); }
        @Override public String toString() { return name(); }
        @Override public boolean equals(Object o) { return o instanceof JSBlock b && matches(b); }
        @Override public int hashCode() { return state.getBlock().hashCode(); }
    }

    /** world.getLocation(...) */
    public static final class JSLocation {
        private final Level level;
        private final BlockPos pos;

        JSLocation(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        public String biome() {
            return level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("minecraft:plains");
        }
        public boolean canSnow() { return level.getBiome(pos).value().coldEnoughToSnow(pos); }
        public boolean canRain() { return level.getBiome(pos).value().hasPrecipitation(); }
        public boolean isHighHumidity() { return level.getBiome(pos).value().getModifiedClimateSettings().downfall() > 0.85f; }
        public float getTemperature() { return level.getBiome(pos).value().getBaseTemperature(); }
        public float getRainfall() { return level.getBiome(pos).value().getModifiedClimateSettings().downfall(); }
        public String getStructure() { return null; }
    }
}
