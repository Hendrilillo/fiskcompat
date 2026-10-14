package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** fiskheroes:overlay - vuelve a dibujar las partes de la pieza con otra textura (y opacidad). */
public class OverlayEffect extends EffectObject {
    public OverlayEffect(String type, HeroRendererScript renderer) {
        super(type, renderer);
    }

    @Override
    protected void render(RenderContext ctx) {
        if (opacity <= 0.001) return;
        float alpha = (float) Math.min(1, opacity);
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        ResourceLocation normal = tex(ctx, texture.normal);
        if (normal != null) {
            RenderType type = alpha < 1 ? RenderType.entityTranslucent(normal) : RenderType.armorCutoutNoCull(normal);
            ctx.model.renderToBuffer(ctx.poseStack, ctx.buffers.getBuffer(type), ctx.light, OverlayTexture.NO_OVERLAY, color);
        }
        ResourceLocation lights = tex(ctx, texture.lights);
        if (lights != null) {
            ctx.model.renderToBuffer(ctx.poseStack, ctx.buffers.getBuffer(RenderType.eyes(lights)), ctx.light, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
