package com.hendrilillo.fiskcompat.api;

import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Una entidad vista desde los scripts. Imita JSEntity / JSEntityLiving / JSPlayer de Fisk
 * (ver docs/api). Las partes que todavía no tienen equivalente devuelven valores neutros.
 */
public class JSEntity {
    /** Ticks parciales del frame actual, para getInterpolatedData en el render. */
    public static float partialTicks = 1f;

    protected final Entity entity;

    public JSEntity(Entity entity) {
        this.entity = entity;
    }

    public static JSEntity of(Entity e) {
        return new JSEntity(e);
    }

    public Entity mc() { return entity; }

    private LivingEntity living() { return entity instanceof LivingEntity l ? l : null; }

    public boolean exists() { return entity != null; }

    public String type() {
        if (entity instanceof Player) return "PLAYER";
        if (entity instanceof ArmorStand) return "DISPLAY";
        if (entity instanceof LivingEntity) return "LIVING";
        return "ENTITY";
    }

    /** Fisk: "PLAYER", "DISPLAY", "LIVING", "ANIMAL", "TAMEABLE"... */
    public boolean is(String t) {
        return switch (t) {
            case "PLAYER" -> entity instanceof Player;
            case "DISPLAY" -> entity instanceof ArmorStand;
            case "LIVING" -> entity instanceof LivingEntity;
            default -> false;
        };
    }

    public JSEntity as(String t) { return this; }
    public Object world() { Unimplemented.report("entity.world()"); return new Dummy("world"); }

    public JSItem getEquipmentInSlot(int slot) {
        LivingEntity l = living();
        if (l == null) return new JSItem(ItemStack.EMPTY);
        return new JSItem(switch (slot) {
            case 0 -> l.getMainHandItem();
            case 1 -> l.getItemBySlot(EquipmentSlot.FEET);
            case 2 -> l.getItemBySlot(EquipmentSlot.LEGS);
            case 3 -> l.getItemBySlot(EquipmentSlot.CHEST);
            case 4 -> l.getItemBySlot(EquipmentSlot.HEAD);
            default -> ItemStack.EMPTY;
        });
    }

    public final JSItem getWornHelmet() { return getEquipmentInSlot(4); }
    public final JSItem getWornChestplate() { return getEquipmentInSlot(3); }
    public final JSItem getWornLeggings() { return getEquipmentInSlot(2); }
    public final JSItem getWornBoots() { return getEquipmentInSlot(1); }
    public JSItem getHeldItem() { return getEquipmentInSlot(0); }

    public boolean hasPotionEffect(int id) { return false; }
    public boolean hasStatusEffect(String id) { return false; }
    public double getHealth() { return living() != null ? living().getHealth() : 0; }
    public double getMaxHealth() { return living() != null ? living().getMaxHealth() : 0; }
    public double getAbsorptionAmount() { return living() != null ? living().getAbsorptionAmount() : 0; }
    public int getAir() { return entity.getAirSupply(); }
    public boolean isChild() { return living() != null && living().isBaby(); }
    public boolean isSneaking() { return entity.isShiftKeyDown(); }
    public boolean isSprinting() { return entity.isSprinting(); }
    public boolean isBurning() { return entity.isOnFire(); }
    public boolean isOnGround() { return entity.onGround(); }
    public boolean isAlive() { return entity.isAlive(); }
    public boolean isInvulnerable() { return entity.isInvulnerable(); }
    public boolean isInWater() { return entity.isInWater(); }
    public boolean isWet() { return entity.isInWaterRainOrBubble(); }
    public boolean isPunching() { return living() != null && living().swinging; }

    public boolean isWearingFullSuit() {
        LivingEntity l = living();
        if (l == null) return false;
        String hero = SuitItem.heroId(l.getItemBySlot(EquipmentSlot.CHEST));
        return hero != null;
    }

    public boolean isLivingEntity() { return entity instanceof LivingEntity; }
    public boolean isDisplayStand() { return entity instanceof ArmorStand; }
    public boolean isBookPlayer() { return false; }
    public boolean isPlayer() { return entity instanceof Player; }
    public JSEntity asPlayer() { return this; }
    public boolean isArthropod() { return false; }
    public boolean isUndead() { return living() != null && living().isInvertedHealAndHarm(); }
    public boolean canSee(JSEntity other) { return living() != null && other != null && living().hasLineOfSight(other.entity); }
    public String getName() { return entity.getName().getString(); }
    public String getUUID() { return entity.getStringUUID(); }
    public String getEntityName() { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    public int ticksExisted() { return entity.tickCount; }
    public double posX() { return entity.getX(); }
    public double posY() { return entity.getY(); }
    public double posZ() { return entity.getZ(); }
    public double eyeHeight() { return entity.getEyeHeight(); }
    public JSVector.Vec3 pos() { return vec(entity.position()); }
    public JSVector.Vec3 eyePos() { return vec(entity.getEyePosition()); }
    public double motionX() { return entity.getDeltaMovement().x; }
    public double motionY() { return entity.getDeltaMovement().y; }
    public double motionZ() { return entity.getDeltaMovement().z; }
    public JSVector.Vec3 motion() { return vec(entity.getDeltaMovement()); }
    public JSVector.Vec3 motionInterpolated() { return motion(); }
    public double rotPitch() { return entity.getXRot(); }
    public double rotYaw() { return entity.getYRot(); }
    public double rotBodyYaw() { return living() != null ? living().yBodyRot : entity.getYRot(); }
    public double rotBodyYawInterpolated() { return living() != null ? net.minecraft.util.Mth.rotLerp(partialTicks, living().yBodyRotO, living().yBodyRot) : rotYaw(); }
    public JSVector.Vec2 rotation() { return new JSVector.Vec2(entity.getYRot(), entity.getXRot()); }
    public JSVector.Vec2 rotationInterpolated() { return new JSVector.Vec2(entity.getViewYRot(partialTicks), entity.getViewXRot(partialTicks)); }
    public JSVector.Vec3 getLookVector() { return vec(entity.getLookAngle()); }
    public double getPunchTimer() { return living() != null ? living().attackAnim : 0; }
    public double getPunchTimerInterpolated() { return living() != null ? living().getAttackAnim(partialTicks) : 0; }

    public Object getData(String name) { return DataStore.get(entity, name); }
    public Object getInterpolatedData(String name) { return DataStore.getInterpolated(entity, name, partialTicks); }

    public Object getDataOrDefault(String name, Object def) {
        Object v = DataStore.get(entity, name);
        return v == null ? def : v;
    }

    public Object getInterpolatedDataOrDefault(String name, Object def) {
        Object v = DataStore.getInterpolated(entity, name, partialTicks);
        return v == null ? def : v;
    }

    /** Fisk: valor que va de 0 a 1 en bucle con un periodo de "ticks". */
    public double loop(double ticks) {
        double t = entity.tickCount + partialTicks;
        return (t % ticks) / ticks;
    }

    public void playSound(String sound, double volume, double pitch) { Unimplemented.report("entity.playSound"); }
    public Object team() { return new Dummy("team"); }
    public void hurt(Object hero, String type, String damageType, float amount) { Unimplemented.report("entity.hurt"); }
    public void hurtByAttacker(Object hero, String type, String damageType, float amount, JSEntity attacker) { Unimplemented.report("entity.hurtByAttacker"); }

    private static JSVector.Vec3 vec(Vec3 v) { return new JSVector.Vec3(v.x, v.y, v.z); }

    @Override
    public boolean equals(Object o) {
        return o instanceof JSEntity e && e.entity == entity;
    }

    @Override
    public int hashCode() { return System.identityHashCode(entity); }
}
