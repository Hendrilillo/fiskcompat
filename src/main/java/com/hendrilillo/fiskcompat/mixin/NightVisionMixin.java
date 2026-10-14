package com.hendrilillo.fiskcompat.mixin;

import com.hendrilillo.fiskcompat.client.HeroCamera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Visión nocturna de los cascos ("fiskheroes:night_vision"): la luz del mundo como con el efecto. */
@Mixin(LightTexture.class)
public abstract class NightVisionMixin {
    @Redirect(method = "updateLightTexture", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean fiskcompat$hasEffect(LocalPlayer player, Holder<MobEffect> effect) {
        if (effect == MobEffects.NIGHT_VISION && HeroCamera.nightVision >= 0) return true;
        return player.hasEffect(effect);
    }

    @Redirect(method = "updateLightTexture", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float fiskcompat$scale(LivingEntity entity, float partialTicks) {
        if (HeroCamera.nightVision >= 0 && !entity.hasEffect(MobEffects.NIGHT_VISION)) return HeroCamera.nightVision;
        return GameRenderer.getNightVisionScale(entity, partialTicks);
    }
}
