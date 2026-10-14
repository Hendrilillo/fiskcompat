package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * fiskheroes:opening_mask - una máscara (cabeza) que gira sobre una bisagra según "progress"
 * (0 = cerrada, 1 = abierta del todo), como el casco de Iron Man.
 */
public class OpeningMaskEffect extends EffectObject {
    public OpeningMaskEffect(String type, HeroRendererScript renderer) {
        super(type, renderer);
    }

    @Override
    protected void render(RenderContext ctx) {
        if (opacity <= 0.001) return;
        ResourceLocation normal = tex(ctx, texture.normal);
        ResourceLocation lights = tex(ctx, texture.lights);
        if (normal == null && lights == null) return;
        ModelPart head = ctx.model.head;
        PartPose saved = head.storePose();
        boolean visible = head.visible, skip = head.skipDraw, overlayVisible = ctx.model.headOverlay.visible;

        ctx.poseStack.pushPose();
        ModelPart anchorPart = anchor.ignore ? null : anchorPart(ctx.model, anchor.part == null ? "head" : anchor.part);
        if (anchorPart != null) anchorPart.translateAndRotate(ctx.poseStack);
        float p = (float) Math.max(0, Math.min(1, progress));
        ctx.poseStack.translate(offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
        if (rotation[0] != 0) ctx.poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rotation[0] * p));
        if (rotation[1] != 0) ctx.poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation[1] * p));
        if (rotation[2] != 0) ctx.poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rotation[2] * p));
        ctx.poseStack.translate(-offset[0] / 16f, -offset[1] / 16f, -offset[2] / 16f);

        head.loadPose(PartPose.ZERO);
        head.visible = true;
        head.skipDraw = false;
        ctx.model.headOverlay.visible = true;
        int color = ((int) (Math.min(1, opacity) * 255) << 24) | 0xFFFFFF;
        if (normal != null) {
            RenderType type = opacity < 1 ? RenderType.entityTranslucent(normal) : RenderType.armorCutoutNoCull(normal);
            head.render(ctx.poseStack, ctx.buffers.getBuffer(type), ctx.light, OverlayTexture.NO_OVERLAY, color);
        }
        if (lights != null) {
            head.render(ctx.poseStack, ctx.buffers.getBuffer(RenderType.eyes(lights)), ctx.light, OverlayTexture.NO_OVERLAY, color);
        }
        ctx.poseStack.popPose();

        head.loadPose(saved);
        head.visible = visible;
        head.skipDraw = skip;
        ctx.model.headOverlay.visible = overlayVisible;
    }
}
