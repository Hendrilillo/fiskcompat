package com.hendrilillo.fiskcompat.client.particle;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.client.HeroPackResources;
import com.hendrilillo.fiskcompat.item.ModParticles;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.io.InputStream;

/**
 * Las partículas propias de Fisk que usan los emisores de los heropacks. Se comportan como las de Fisk
 * (duración, tamaño, color y movimiento); las texturas de BIG_SMOKE y FLAME_BALL se sacan del jar de Fisk.
 */
@EventBusSubscriber(modid = FiskCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FiskParticles {
    private FiskParticles() {}

    @SubscribeEvent
    public static void providers(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FLIGHT_SMOKE.get(), s -> (t, l, x, y, z, mx, my, mz) -> new FlightSmoke(l, x, y, z, mx, my, mz, s, 1));
        event.registerSpriteSet(ModParticles.FLIGHT_SMOKE_SMALL.get(), s -> (t, l, x, y, z, mx, my, mz) -> new FlightSmoke(l, x, y, z, mx, my, mz, s, 1 / 1.5f));
        event.registerSpriteSet(ModParticles.BIG_SMOKE.get(), s -> (t, l, x, y, z, mx, my, mz) -> new BigSmoke(l, x, y, z, mx, my, mz, s));
        event.registerSpriteSet(ModParticles.FLAME_BALL.get(), s -> (t, l, x, y, z, mx, my, mz) -> new FlameBall(l, x, y, z, mx, my, mz, s));
    }

    // ------------------------------------------------------------------ texturas sacadas del jar de Fisk

    private static boolean generated;

    /** Recorta big_smoke.png (4x4) y las bolas de fuego de particles.png en sprites sueltos. */
    public static synchronized void generateTextures() {
        if (generated) return;
        generated = true;
        try {
            NativeImage big = read("textures/particle/big_smoke.png");
            if (big != null) {
                int t = big.getWidth() / 4;
                for (int i = 0; i < 12; i++) put("big_smoke_" + i, crop(big, (i % 4) * t, (i / 4) * t, t));
                big.close();
            }
            NativeImage sheet = read("textures/particle/particles.png");
            if (sheet != null) {
                int t = sheet.getWidth() / 16;
                put("flame_ball_0", crop(sheet, 2 * t, t, t));
                put("flame_ball_1", crop(sheet, 3 * t, t, t));
                sheet.close();
            }
        } catch (Exception e) {
            FiskCompat.LOGGER.warn("No se pudieron preparar las texturas de partículas de Fisk: {}", e.toString());
        }
    }

    public static synchronized void reset() {
        generated = false;
    }

    private static NativeImage read(String path) {
        if (HeroPackManager.findAsset("fiskheroes", path).isEmpty()) return null;
        try (InputStream in = HeroPackManager.openAsset("fiskheroes", path)) {
            return NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImage crop(NativeImage src, int x0, int y0, int size) {
        NativeImage out = new NativeImage(size, size, true);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) out.setPixelRGBA(x, y, src.getPixelRGBA(x0 + x, y0 + y));
        return out;
    }

    private static void put(String name, NativeImage img) throws Exception {
        HeroPackResources.putGeneratedBytes(FiskCompat.MODID, "textures/particle/fisk/" + name + ".png", img.asByteArray());
        img.close();
    }

    // ------------------------------------------------------------------ partículas

    /** SMOKE / SMOKE_SMALL: el humo de los propulsores (EntitySHFlightSmokeFX). */
    static final class FlightSmoke extends TextureSheetParticle {
        private final SpriteSet sprites;

        FlightSmoke(ClientLevel l, double x, double y, double z, double mx, double my, double mz, SpriteSet sprites, float size) {
            super(l, x, y, z);
            this.sprites = sprites;
            xd = mx;
            yd = my;
            zd = mz;
            lifetime = (int) (3.0 / (random.nextFloat() * 0.6 + 0.4));
            float c = random.nextFloat() * 0.2f + 0.2f;
            setColor(c, c, c);
            quadSize = 0.1f * (1.75f + random.nextFloat() * random.nextFloat() * 0.1f) * size;
            hasPhysics = true;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            yd += 0.004;
            move(xd, yd, zd);
            setSpriteFromAge(sprites);
            xd *= 0.9;
            yd *= 0.9;
            zd *= 0.9;
            if (onGround) {
                xd *= 0.7;
                zd *= 0.7;
            }
            rCol += (0.4f - rCol) / lifetime;
            gCol = bCol = rCol;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    /** BIG_SMOKE: humo grande y animado (EntitySHBigSmokeFX). */
    static final class BigSmoke extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float baseSize;

        BigSmoke(ClientLevel l, double x, double y, double z, double mx, double my, double mz, SpriteSet sprites) {
            super(l, x, y, z);
            this.sprites = sprites;
            xd = mx + (random.nextFloat() * 2 - 1) * 0.004;
            yd = my + (random.nextFloat() * 2 - 1) * 0.004;
            zd = mz + (random.nextFloat() * 2 - 1) * 0.004;
            baseSize = 0.1f * (2.3f + random.nextFloat());
            quadSize = baseSize;
            lifetime = (int) (5.0f / (random.nextFloat() * 0.8f + 0.2f)) + 4;
            hasPhysics = true;
            setSprite(sprites.get(0, 11));
        }

        @Override
        public Particle scale(float s) {
            super.scale(s);
            return this;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            yd += 0.004;
            move(xd, yd, zd);
            xd *= 0.96;
            yd *= 0.96;
            zd *= 0.96;
            float f = (float) age / lifetime;
            setSprite(sprites.get(Math.min(11, (int) (f * 12)), 11));
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera cam, float pt) {
            float f = (age + pt) / lifetime;
            alpha = Math.max(0, 1 - f);
            quadSize = baseSize * (1 - f * f * 0.5f);
            float c = Math.max(0, (1 - f * f * 1.2f) * 0.8f);
            setColor(c, c, c);
            super.render(buffer, cam, pt);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** FLAME_BALL: bola de fuego que se agranda y se apaga (EntitySHFlameBallFX). */
    static final class FlameBall extends TextureSheetParticle {
        private float baseSize;

        FlameBall(ClientLevel l, double x, double y, double z, double mx, double my, double mz, SpriteSet sprites) {
            super(l, x, y, z);
            xd = mx + (random.nextFloat() * 2 - 1) * 0.004;
            yd = my + (random.nextFloat() * 2 - 1) * 0.004;
            zd = mz + (random.nextFloat() * 2 - 1) * 0.004;
            baseSize = 0.1f * (1.3f + random.nextFloat() * 0.5f);
            quadSize = baseSize;
            lifetime = (int) (3.0f / (random.nextFloat() * 0.8f + 0.2f)) + 4;
            hasPhysics = true;
            setSprite(sprites.get(random.nextInt(2), 1));
        }

        @Override
        public Particle scale(float s) {
            baseSize *= s;
            return this;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            move(xd, yd, zd);
            xd *= 0.96;
            yd *= 0.96;
            zd *= 0.96;
            if (onGround) {
                xd *= 0.7;
                zd *= 0.7;
            }
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera cam, float pt) {
            float f = Math.min(1, (age + pt) / lifetime);
            alpha = 1 - f;
            quadSize = baseSize * (1 + f * f * 2);
            setColor(1, 1 - f * f * 0.6f, 1 - f * f);
            super.render(buffer, cam, pt);
        }

        @Override
        protected int getLightColor(float pt) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
