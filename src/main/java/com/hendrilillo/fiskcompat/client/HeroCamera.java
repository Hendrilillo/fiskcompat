package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.beam.PropertyBinding;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.Random;

/**
 * Propiedades de los renderers que afectan a la cámara del jugador local:
 * "fiskheroes:camera_shake" (temblor al ir con turbo) y "fiskheroes:night_vision" (visión nocturna del casco).
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class HeroCamera {
    /** Visión nocturna activa ahora (factor), o -1. */
    public static float nightVision = -1;
    public static float nightVisionFog = 1;

    private HeroCamera() {}

    private static HeroRendererScript renderer(LocalPlayer p) {
        ActiveHero hero = ActiveHero.of(p);
        return hero == null ? null : ClientHeroes.renderer(hero.suitId);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre event) {
        nightVision = -1;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        HeroRendererScript r = renderer(p);
        if (r == null) return;
        JSEntity js = JSEntity.of(p);
        JSEntity.partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Object o : r.properties()) {
            if (!(o instanceof PropertyBinding pb) || !pb.type.equals("fiskheroes:night_vision")) continue;
            if (!pb.test(js)) continue;
            if (pb.bool("firstPersonOnly", false) && !mc.options.getCameraType().isFirstPerson()) return;
            if (pb.bool("underwaterOnly", false) && !p.isEyeInFluid(FluidTags.WATER)) return;
            nightVision = (float) pb.num("factor", 1);
            nightVisionFog = (float) pb.num("fogStrength", 1);
            return;
        }
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || event.getCamera().getEntity() != p) return;
        HeroRendererScript r = renderer(p);
        if (r == null) return;
        JSEntity js = JSEntity.of(p);
        for (Object o : r.properties()) {
            if (!(o instanceof PropertyBinding pb) || !pb.type.equals("fiskheroes:camera_shake")) continue;
            if (!pb.test(js)) continue; // la condición suele calcular "factor"
            double factor = pb.num("factor", 0);
            if (factor <= 0) continue;
            double vel = p.getDeltaMovement().length();
            float f = (float) (factor * Math.pow(vel, pb.num("intensity", 0)));
            Random rand = new Random(p.tickCount);
            event.setRoll(event.getRoll() + (1 - rand.nextFloat() * 2) * f);
            event.setYaw(event.getYaw() + (1 - rand.nextFloat() * 2) * f);
            event.setPitch(event.getPitch() + (1 - rand.nextFloat() * 2) * f);
        }
    }
}
