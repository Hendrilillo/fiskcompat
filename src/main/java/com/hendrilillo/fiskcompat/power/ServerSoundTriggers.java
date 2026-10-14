package com.hendrilillo.fiskcompat.power;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.net.Net;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.List;

/** Triggers de sonido que en Fisk salen del servidor: STEP, LAND y PUNCH. */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class ServerSoundTriggers {
    private ServerSoundTriggers() {}

    private static void heroTrigger(Entity e, String trigger) {
        if (e.level().isClientSide || !(e instanceof LivingEntity le)) return;
        ActiveHero hero = ActiveHero.of(le);
        if (hero == null) return;
        List<String> ids = hero.def.soundEvents(trigger);
        if (ids != null && !ids.isEmpty()) Net.soundTrigger(e, null, trigger);
    }

    public static void onStep(Entity e) {
        if (e.isInWater() || e.isInLava()) return;
        heroTrigger(e, "STEP");
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getDistance() > 0) heroTrigger(event.getEntity(), "LAND");
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().isEmpty()) heroTrigger(event.getEntity(), "PUNCH");
    }
}
