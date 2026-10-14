package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.effect.RenderContext;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Capa que dibuja los trajes de los heropacks sobre cualquier entidad humanoide. */
public class SuitLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Set<String> REPORTED = new HashSet<>();

    private final SuitModel<T> model;

    public SuitLayer(RenderLayerParent<T, M> parent, SuitModel<T> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        JSEntity.partialTicks = partialTick;
        JSEntity js = null;
        boolean captured = false;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = entity.getItemBySlot(SLOTS[i]);
            String suitId = SuitItem.heroId(stack);
            if (suitId == null) continue;
            HeroRendererScript r = ClientHeroes.renderer(suitId);
            if (r == null) continue;
            if (js == null) js = JSEntity.of(entity);
            HeroRendererScript.Slot slot = HeroRendererScript.Slot.values()[i];
            try {
                getParentModel().copyPropertiesTo(model);
                if (!captured) {
                    captured = true;
                    com.hendrilillo.fiskcompat.client.beam.BeamRender.capture(entity, js, r, model, poseStack, partialTick);
                    if (com.hendrilillo.fiskcompat.client.beam.BeamRender.inWorld()) {
                        com.hendrilillo.fiskcompat.client.particle.ParticleEmitters.capture(entity, getParentModel(), poseStack, partialTick);
                    }
                }
                List<String> parts = r.shownParts(slot);
                model.showParts(parts != null ? parts : SuitModel.defaultParts(slot));

                ResourceLocation tex = ClientHeroes.heroTexture(r.texturePath(r.textureKey(js, slot)), js);
                if (tex != null) {
                    model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(tex)), packedLight, OverlayTexture.NO_OVERLAY);
                }
                ResourceLocation lights = ClientHeroes.heroTexture(r.texturePath(r.lightsKey(js, slot)), js);
                if (lights != null) {
                    model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(lights)), packedLight, OverlayTexture.NO_OVERLAY);
                }
                if (r.hasFunction("render")) {
                    RenderContext.current = new RenderContext(poseStack, buffer, packedLight, model, entity, js, slot, r,
                            parts != null ? parts : SuitModel.defaultParts(slot));
                    try {
                        r.call("render", js, slot.name(), false);
                    } finally {
                        RenderContext.current = null;
                    }
                }
            } catch (Throwable e) {
                if (REPORTED.add(suitId + ":" + slot)) {
                    FiskCompat.LOGGER.error("Error dibujando {} ({}): {}", suitId, slot, e.toString());
                }
            }
        }
    }
}
