package com.hendrilillo.fiskcompat.entity;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.hero.HeroTicker;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.power.modifier.ControlledFlight;
import com.hendrilillo.fiskcompat.power.modifier.Repulsor;
import com.hendrilillo.fiskcompat.power.modifier.SentryMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * El traje de Iron Man en modo centinela (como EntityIronMan de Fisk): lleva puestas las piezas del
 * traje, sigue a su dueño, ataca a quien le ataca (con repulsores si el traje los tiene) y se abre para
 * que el dueño vuelva a entrar. Clic derecho del dueño: abrir/cerrar; agachado: volar/aterrizar.
 */
public class SentryEntity extends PathfinderMob implements RangedAttackMob, OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(SentryEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public int exitTimer;
    private int aggroTimer;

    public SentryEntity(EntityType<? extends SentryEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        for (EquipmentSlot s : ARMOR) setDropChance(s, 2.0f); // el traje se recupera si lo destruyen
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.275)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 40, 30f) {
            @Override
            public boolean canUse() {
                return !isSuitOpen() && hasRepulsor() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isSuitOpen() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !isSuitOpen() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new FollowOwnerGoal(8, 2));
        targetSelector.addGoal(1, new OwnerTargetGoal(true));
        targetSelector.addGoal(2, new OwnerTargetGoal(false));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, e -> e instanceof Enemy));
    }

    // ------------------------------------------------------------------ dueño

    public void setOwner(UUID uuid) {
        entityData.set(OWNER, Optional.ofNullable(uuid));
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return entityData.get(OWNER).orElse(null);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        LivingEntity owner = getOwner();
        if (other == owner) return true;
        if (owner != null && owner.isAlliedTo(other)) return true;
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target != getOwner() && !isAlliedTo(target) && super.canAttack(target);
    }

    // ------------------------------------------------------------------ estado del traje

    public boolean isSuitOpen() {
        return DataStore.get(this, SentryMode.OPEN) instanceof Boolean b && b;
    }

    public boolean isFlying() {
        return DataStore.get(this, ControlledFlight.FLYING) instanceof Boolean b && b;
    }

    private int suitTimer() {
        return DataStore.get(this, SentryMode.TIMER) instanceof Number n ? n.intValue() : 0;
    }

    private boolean hasRepulsor() {
        ActiveHero hero = ActiveHero.of(this);
        return hero != null && hero.enabledModifier(JSEntity.of(this), Repulsor.TYPE) != null;
    }

    @Override
    public Component getName() {
        if (hasCustomName()) return super.getName();
        ActiveHero hero = ActiveHero.of(this);
        return hero != null ? Component.literal(hero.def.plainName()) : super.getName();
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (exitTimer > 0 && --exitTimer == 0) DataStore.set(this, SentryMode.OPEN, false);
        LivingEntity owner = getOwner();
        boolean open = isSuitOpen();
        if (open) {
            if (exitTimer == 0 && owner instanceof Player player && player.isAlive() && contains(getBoundingBox().inflate(0.25), player.getBoundingBox())) {
                enter(player);
                return;
            }
            if (owner != null) {
                setYRot(owner.yBodyRot);
                yBodyRot = owner.yBodyRot;
                yHeadRot = owner.yBodyRot;
            }
            getNavigation().stop();
        }
        if (getTarget() != null) aggroTimer = 30;
        else if (aggroTimer > 0) aggroTimer--;
        DataStore.set(this, "fiskheroes:aiming", aggroTimer > 0 && !open);
        if (isFlying() && onGround()) DataStore.set(this, ControlledFlight.FLYING, false);
        if (isFlying()) steerFlight(owner, open);
        HeroTicker.tickEntity(this);
    }

    private static boolean contains(AABB a, AABB b) {
        return a.minX <= b.minX && a.minY <= b.minY && a.minZ <= b.minZ && a.maxX >= b.maxX && a.maxY >= b.maxY && a.maxZ >= b.maxZ;
    }

    /** Vuelo: se mantiene en el aire y se acerca a su objetivo o a su dueño. */
    private void steerFlight(@Nullable LivingEntity owner, boolean open) {
        Vec3 v = getDeltaMovement();
        Vec3 want = null;
        LivingEntity target = getTarget();
        if (!open && target != null && target.isAlive()) {
            Vec3 t = target.position().add(0, target.getBbHeight() + 1.5, 0);
            Vec3 d = position().subtract(t);
            double dist = d.length();
            want = dist > 1e-3 ? t.add(d.scale(Math.min(dist, 8) / dist)) : t;
        } else if (owner != null) {
            Vec3 o = owner.position().add(0, open ? 0 : 1.5, 0);
            if (open || position().distanceTo(o) > 3) want = o;
        }
        if (want != null) {
            Vec3 d = want.subtract(position());
            double len = d.length();
            if (len > 0.5) {
                Vec3 add = d.scale(Math.min(0.06, len * 0.02) / len);
                v = v.add(add);
                if (!open) lookAtFlight(d);
            }
        }
        v = v.scale(0.9);
        if (open && owner != null) {
            // Fisk: abierto y volando, va bajando hasta la altura del dueño
            double dist = distanceTo(owner);
            v = new Vec3(v.x, v.y * (1 - Mth.clamp(2 - dist * 0.75, 0, 1)), v.z);
        }
        setDeltaMovement(v);
    }

    private void lookAtFlight(Vec3 d) {
        if (getTarget() != null) return;
        float yaw = (float) (Mth.atan2(d.z, d.x) * (180 / Math.PI)) - 90;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 10));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlying() && isControlledByLocalInstance()) {
            moveRelative(getSpeed() * 0.4f, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    /** El dueño vuelve a entrar en el traje. */
    private void enter(Player player) {
        String suitId = SuitItem.heroId(getItemBySlot(EquipmentSlot.CHEST));
        for (EquipmentSlot s : ARMOR) {
            ItemStack mine = getItemBySlot(s);
            if (mine.isEmpty()) continue;
            ItemStack theirs = player.getItemBySlot(s);
            if (!theirs.isEmpty() && !player.getInventory().add(theirs)) player.drop(theirs, false);
            player.setItemSlot(s, mine);
            setItemSlot(s, ItemStack.EMPTY);
        }
        Net.soundTrigger(player, suitId, SentryMode.TYPE, "ENTER");
        DataStore.set(player, SentryMode.OPEN, false);
        DataStore.set(player, SentryMode.TIMER, (byte) 7);
        DataStore.set(player, "fiskheroes:mask_open", false);
        DataStore.setNoInterp(player, "fiskheroes:mask_open_timer", 0);
        DataStore.setNoInterp(player, "fiskheroes:mask_open_timer2", 0f);
        discard();
    }

    // ------------------------------------------------------------------ interacción

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player != getOwner()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            boolean flying = isFlying();
            if (!flying && onGround()) setDeltaMovement(getDeltaMovement().add(0, 0.7, 0));
            DataStore.set(this, ControlledFlight.FLYING, !flying);
        } else {
            int t = suitTimer();
            if (t == 0 || t == 5) DataStore.set(this, SentryMode.OPEN, !isSuitOpen());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        double dx = target.getX() - getX();
        double dy = target.getY() + target.getBbHeight() * 0.75 - (getY() + getEyeHeight() - 0.1);
        double dz = target.getZ() - getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h >= 1e-7) {
            setYRot((float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90);
            setXRot((float) -(Mth.atan2(dy, h) * (180 / Math.PI)));
            yHeadRot = getYRot();
        }
        ActiveHero hero = ActiveHero.of(this);
        ModifierEntry m = hero == null ? null : hero.enabledModifier(JSEntity.of(this), Repulsor.TYPE);
        if (m != null) Repulsor.fire(this, m); // el cliente hace sonar SHOOT al recibir el disparo
    }

    // ------------------------------------------------------------------ daño y colisiones

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.STARVE) || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.IN_WALL)) return false;
        if (source.getEntity() == getOwner() && source.getEntity() != null) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return tickCount > 10;
    }

    @Override
    protected void doPush(Entity other) {
        if (tickCount > 10 && (!isSuitOpen() || other != getOwner())) super.doPush(other);
    }

    @Override
    public void push(Entity other) {
        if (tickCount > 10 && (!isSuitOpen() || other != getOwner())) super.push(other);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("SuitOpen", isSuitOpen());
        tag.putBoolean("Flying", isFlying());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"));
        boolean open = tag.getBoolean("SuitOpen");
        DataStore.set(this, SentryMode.OPEN, open);
        DataStore.setNoInterp(this, SentryMode.TIMER, (byte) (open ? 5 : 0));
        DataStore.set(this, ControlledFlight.FLYING, tag.getBoolean("Flying"));
    }

    // ------------------------------------------------------------------ IA

    /** Sigue al dueño (si no está abierto); se teletransporta cerca si se queda muy lejos. */
    private final class FollowOwnerGoal extends Goal {
        private final float min, max;
        private LivingEntity owner;
        private int timer;

        FollowOwnerGoal(float min, float max) {
            this.min = min;
            this.max = max;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            if (o == null || o.isSpectator() || isSuitOpen() || distanceToSqr(o) < min * min) return false;
            owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !isSuitOpen() && owner != null && owner.isAlive() && distanceToSqr(owner) > max * max
                    && (isFlying() || !getNavigation().isDone());
        }

        @Override
        public void start() {
            timer = 0;
        }

        @Override
        public void stop() {
            owner = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(owner, 10, getMaxHeadXRot());
            if (--timer > 0) return;
            timer = 10;
            if (isFlying()) return; // en vuelo se mueve en steerFlight
            if (!getNavigation().moveTo(owner, 1.0) && !isLeashed() && distanceToSqr(owner) >= 144) teleportNear(owner);
        }

        private void teleportNear(LivingEntity o) {
            BlockPos base = o.blockPosition();
            for (int i = 0; i < 10; i++) {
                int dx = random.nextIntBetweenInclusive(-3, 3), dz = random.nextIntBetweenInclusive(-3, 3);
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                BlockPos p = base.offset(dx, 0, dz);
                if (level().getBlockState(p.below()).isSolidRender(level(), p.below())
                        && level().noCollision(SentryEntity.this, getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(position())))) {
                    moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, getYRot(), getXRot());
                    getNavigation().stop();
                    return;
                }
            }
        }
    }

    /** Ataca a quien ataca al dueño (hurtBy=true) o a quien el dueño ataca. */
    private final class OwnerTargetGoal extends TargetGoal {
        private final boolean hurtBy;
        private LivingEntity found;
        private int timestamp;

        OwnerTargetGoal(boolean hurtBy) {
            super(SentryEntity.this, false);
            this.hurtBy = hurtBy;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            if (o == null) return false;
            found = hurtBy ? o.getLastHurtByMob() : o.getLastHurtMob();
            int ts = hurtBy ? o.getLastHurtByMobTimestamp() : o.getLastHurtMobTimestamp();
            return found != null && ts != timestamp && canAttack(found, TargetingConditions.DEFAULT) && found != SentryEntity.this;
        }

        @Override
        public void start() {
            mob.setTarget(found);
            LivingEntity o = getOwner();
            if (o != null) timestamp = hurtBy ? o.getLastHurtByMobTimestamp() : o.getLastHurtMobTimestamp();
            super.start();
        }
    }
}
