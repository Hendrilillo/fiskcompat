package com.hendrilillo.fiskcompat.mixin;

import com.hendrilillo.fiskcompat.client.SkinLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No dibuja la capa del jugador si lleva puesta la pechera de un traje. */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void fiskcompat$hideCape(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                                     float a, float b, float c, float d, float e, float f, CallbackInfo ci) {
        if (SkinLayers.wearing(player, EquipmentSlot.CHEST)) ci.cancel();
    }
}
