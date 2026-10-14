package com.hendrilillo.fiskcompat.entity;

import com.hendrilillo.fiskcompat.FiskCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, FiskCompat.MODID);

    /** El traje de Iron Man en modo centinela (EntityIronMan de Fisk). */
    public static final DeferredHolder<EntityType<?>, EntityType<SentryEntity>> SENTRY = ENTITIES.register("iron_man",
            () -> EntityType.Builder.<SentryEntity>of(SentryEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).build("iron_man"));

    private ModEntities() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SENTRY.get(), SentryEntity.createAttributes().build());
    }
}
