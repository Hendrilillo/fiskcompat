package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/** Dibuja el brazo del traje en primera persona. */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class FirstPersonArm {
    private static SuitModel<AbstractClientPlayer> model;

    private FirstPersonArm() {}

    @SubscribeEvent
    public static void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        String suitId = SuitItem.heroId(player.getItemBySlot(EquipmentSlot.CHEST));
        if (suitId == null) return;
        HeroRendererScript r = ClientHeroes.renderer(suitId);
        if (r == null) return;
        if (model == null) {
            model = new SuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(SuitModel.LAYER));
        }
        try {
            // Animaciones en primera persona (p. ej. al apuntar): mueven el brazo antes de dibujarlo
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            com.hendrilillo.fiskcompat.client.anim.BodyAnimator.applyFirstPerson(player, event.getPoseStack(), pt);
        } catch (Throwable ignored) {
        }
        try {
            JSEntity js = JSEntity.of(player);
            HeroRendererScript.Slot slot = HeroRendererScript.Slot.CHESTPLATE;
            model.attackTime = 0;
            model.crouching = false;
            model.swimAmount = 0;
            model.setupAnim(player, 0, 0, 0, 0, 0);
            model.showParts(java.util.List.of("rightArm", "leftArm"));
            ModelPart arm = event.getArm() == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            arm.xRot = 0;
            ResourceLocation tex = ClientHeroes.heroTexture(r.texturePath(r.textureKey(js, slot)), js);
            if (tex != null) {
                arm.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.armorCutoutNoCull(tex)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            }
            ResourceLocation lights = ClientHeroes.heroTexture(r.texturePath(r.lightsKey(js, slot)), js);
            if (lights != null) {
                arm.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.eyes(lights)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            }
        } catch (Throwable e) {
            // no romper el render de la mano por un fallo del script
        }
    }
}
