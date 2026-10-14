package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.power.modifier.ControlledFlight;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Inmunidades, escudo, caídas y respiración bajo el agua de los héroes. */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class HeroEvents {
    private HeroEvents() {}

    private static boolean has(ActiveHero h, JSEntity js, String type) {
        for (ModifierEntry m : h.modifiers) if (m.type.equals(type) && h.isModifierEnabled(js, m)) return true;
        return false;
    }

    /** Tipo de daño de Fisk ("FIRE", "CACTUS"...) que corresponde a una fuente de daño de Minecraft. */
    static boolean matches(String fiskType, DamageSource s) {
        return switch (fiskType) {
            case "FIRE" -> s.is(DamageTypeTags.IS_FIRE);
            case "CACTUS" -> s.is(DamageTypes.CACTUS) || s.is(DamageTypes.SWEET_BERRY_BUSH);
            case "EXPLOSION" -> s.is(DamageTypeTags.IS_EXPLOSION);
            case "MAGIC" -> s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC);
            case "COLD" -> s.is(DamageTypeTags.IS_FREEZING);
            case "ELECTRICITY" -> s.is(DamageTypeTags.IS_LIGHTNING);
            case "THORNS" -> s.is(DamageTypes.THORNS);
            case "BULLET", "SHARP" -> s.is(DamageTypeTags.IS_PROJECTILE);
            case "BLUNT" -> s.is(DamageTypes.FALLING_BLOCK) || s.is(DamageTypes.FALLING_ANVIL);
            default -> false;
        };
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        ActiveHero h = ActiveHero.of(e);
        if (h == null) return;
        JSEntity js = JSEntity.of(e);
        DamageSource s = event.getSource();
        for (ModifierEntry m : h.modifiers) {
            if (!h.isModifierEnabled(js, m)) continue;
            switch (m.type) {
                case "fiskheroes:fire_immunity" -> { if (s.is(DamageTypeTags.IS_FIRE)) { event.setCanceled(true); e.clearFire(); return; } }
                case "fiskheroes:projectile_immunity" -> { if (s.is(DamageTypeTags.IS_PROJECTILE)) { event.setCanceled(true); return; } }
                case "fiskheroes:damage_immunity" -> { if (matches(m.str("damageType", ""), s)) { event.setCanceled(true); return; } }
                case "fiskheroes:fire_resistance" -> { if (s.is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 0.5f); }
                case "fiskheroes:damage_resistance" -> {
                    if (matches(m.str("damageType", ""), s)) event.setAmount((float) (event.getAmount() * (1 - m.num("factor", 0.5))));
                }
                default -> { }
            }
        }
        // Escudo: bloquea lo que llega de frente
        if (DataStore.get(e, "fiskheroes:shield_blocking") instanceof Boolean b && b && blocksFrom(e, s)) {
            event.setCanceled(true);
            Entity attacker = s.getDirectEntity();
            if (attacker instanceof LivingEntity l) {
                Vec3 push = l.position().subtract(e.position()).normalize().scale(0.5);
                l.push(push.x, 0.1, push.z);
            }
        }
    }

    private static boolean blocksFrom(LivingEntity e, DamageSource s) {
        Vec3 from = s.getSourcePosition();
        if (from == null || s.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        Vec3 look = e.getViewVector(1);
        Vec3 dir = from.subtract(e.position()).normalize();
        return look.x * dir.x + look.z * dir.z > 0.3;
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        if (ActiveHero.of(e) == null) return;
        if (DataStore.get(e, ControlledFlight.FLYING) instanceof Boolean b && b) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void breathe(LivingBreatheEvent event) {
        LivingEntity e = event.getEntity();
        ActiveHero h = ActiveHero.of(e);
        if (h == null) return;
        JSEntity js = JSEntity.of(e);
        if (has(h, js, "fiskheroes:water_breathing")) {
            event.setCanBreathe(true);
        }
    }
}
