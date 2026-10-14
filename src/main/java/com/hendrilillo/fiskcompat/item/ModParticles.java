package com.hendrilillo.fiskcompat.item;

import com.hendrilillo.fiskcompat.FiskCompat;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Partículas propias de Fisk que usan los emisores de los heropacks (SMOKE, SMOKE_SMALL, BIG_SMOKE, FLAME_BALL). */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, FiskCompat.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLIGHT_SMOKE = PARTICLES.register("flight_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLIGHT_SMOKE_SMALL = PARTICLES.register("flight_smoke_small", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SMOKE = PARTICLES.register("big_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAME_BALL = PARTICLES.register("flame_ball", () -> new SimpleParticleType(false));

    private ModParticles() {}

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
