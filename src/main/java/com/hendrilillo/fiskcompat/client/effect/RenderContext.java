package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.SuitModel;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Estado del render en curso, que usan los efectos cuando el script llama a effect.render(). */
public final class RenderContext {
    public static RenderContext current;

    public final PoseStack poseStack;
    public final MultiBufferSource buffers;
    public final int light;
    public final SuitModel<?> model;
    public final LivingEntity entity;
    public final JSEntity jsEntity;
    public final HeroRendererScript.Slot slot;
    public final HeroRendererScript renderer;
    public final List<String> parts;
    /** Dibujando el brazo en primera persona. */
    public boolean firstPerson;
    /**
     * En primera persona: la pose de la mano antes de colocar el brazo (espacio de la cámara). Fisk dibuja
     * ahí, al final, los efectos con el ancla ignorada (el HUD del casco de Iron Man, por ejemplo).
     */
    public org.joml.Matrix4f cameraPose;
    public org.joml.Matrix3f cameraNormal;

    /** Copia con otra pila de transformaciones. */
    public RenderContext withPose(PoseStack pose) {
        RenderContext c = new RenderContext(pose, buffers, light, model, entity, jsEntity, slot, renderer, parts);
        c.firstPerson = firstPerson;
        return c;
    }

    public RenderContext(PoseStack poseStack, MultiBufferSource buffers, int light, SuitModel<?> model, LivingEntity entity,
                         JSEntity jsEntity, HeroRendererScript.Slot slot, HeroRendererScript renderer, List<String> parts) {
        this.poseStack = poseStack;
        this.buffers = buffers;
        this.light = light;
        this.model = model;
        this.entity = entity;
        this.jsEntity = jsEntity;
        this.slot = slot;
        this.renderer = renderer;
        this.parts = parts;
    }
}
