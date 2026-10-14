package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.client.model.ModelResource;
import com.hendrilillo.fiskcompat.client.model.TabulaModel;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** fiskheroes:model - dibuja un modelo Tabula anclado a una parte del cuerpo. */
public class ModelEffect extends EffectObject {
    private ModelResource model;

    public ModelEffect(String type, HeroRendererScript renderer) {
        super(type, renderer);
    }

    @Override
    public Object getMember(String name) {
        if (name.equals("setModel")) return new JsFunction("setModel", a -> {
            model = a.length > 0 && a[0] instanceof ModelResource m ? m : null;
            return this;
        });
        if (name.equals("model")) return model;
        return super.getMember(name);
    }

    /** Cuántos modelos se han dibujado (para saber si el pack dibuja su propio brazo en primera persona). */
    public static int rendered;

    @Override
    protected void render(RenderContext ctx) {
        if (model == null || opacity <= 0.001) return;
        TabulaModel tbl = model.model();
        if (tbl == null) return;
        ResourceLocation normal = ClientHeroes.heroTexture(renderer.texturePath(model.texture.normal), ctx.jsEntity);
        ResourceLocation lights = ClientHeroes.heroTexture(renderer.texturePath(model.texture.lights), ctx.jsEntity);
        if (normal == null && lights == null) return;
        model.animate(tbl, ctx.jsEntity, ctx.entity, JSEntity.partialTicks);
        rendered++;

        ctx.poseStack.pushPose();
        applyTransform(ctx.poseStack, ctx.model, 1);
        float a = (float) Math.min(1, opacity);
        int color = ((int) (a * 255) << 24) | 0xFFFFFF;
        // las luces se suman (sin transparencia): la opacidad se aplica oscureciendo el color
        int c = (int) (a * 255);
        int lightColor = 0xFF000000 | c << 16 | c << 8 | c;
        for (ModelPart root : tbl.roots) {
            if (normal != null) {
                RenderType type = opacity < 1 ? RenderType.entityTranslucent(normal) : RenderType.entityCutoutNoCull(normal);
                root.render(ctx.poseStack, ctx.buffers.getBuffer(type), ctx.light, OverlayTexture.NO_OVERLAY, color);
            }
            if (lights != null) {
                root.render(ctx.poseStack, ctx.buffers.getBuffer(RenderType.eyes(lights)), ctx.light, OverlayTexture.NO_OVERLAY, lightColor);
            }
        }
        ctx.poseStack.popPose();
    }
}
