package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.anim.BodyAnimator;
import com.hendrilillo.fiskcompat.client.effect.ModelEffect;
import com.hendrilillo.fiskcompat.client.effect.RenderContext;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.util.List;

/**
 * El brazo del traje en primera persona (con la mano vacía), como en Fisk: se aplican las animaciones
 * de primera persona, se dibuja el brazo y luego los efectos del renderer con isFirstPersonArm = true.
 * Si la animación levanta también el brazo izquierdo (apuntar con las dos manos...), se dibuja el
 * izquierdo en espejo.
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class FirstPersonArm {
    private static SuitModel<AbstractClientPlayer> model;
    /** Del brazo derecho de este fotograma, para dibujar después el izquierdo. */
    private static BodyAnimator.FirstPerson pendingLeft;
    /** El pack ha dibujado su propio brazo delante de la cámara este fotograma. */
    public static boolean packArmDrawn;

    private FirstPersonArm() {}

    private static SuitModel<AbstractClientPlayer> model() {
        if (model == null) model = new SuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(SuitModel.LAYER));
        return model;
    }

    private static HeroRendererScript renderer(AbstractClientPlayer player) {
        String suitId = SuitItem.heroId(player.getItemBySlot(EquipmentSlot.CHEST));
        return suitId == null ? null : ClientHeroes.renderer(suitId);
    }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent event) {
        pendingLeft = null;
        packArmDrawn = false;
        AbstractClientPlayer player = event.getPlayer();
        HeroRendererScript r = renderer(player);
        if (r == null) return;
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        JSEntity.partialTicks = pt;
        SuitModel<AbstractClientPlayer> m = model();
        BodyAnimator.FirstPerson fp = null;
        try {
            fp = BodyAnimator.firstPerson(player, m, pt);
            if (fp != null) {
                if (right) fp.applyRight(event.getPoseStack());
                else fp.applyLeft(event.getPoseStack());
            }
        } catch (Throwable e) {
            if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED) FiskCompat.LOGGER.error("[autotest] fp anim", e);
        }
        drawSuitArm(player, r, m, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), right, true);
        if (right && fp != null && fp.bothArms() && player.getOffhandItem().isEmpty()) {
            pendingLeft = fp;
        }
    }

    /** Brazo izquierdo en espejo cuando la animación usa las dos manos. */
    /** Pose de la mano principal antes de colocar el brazo (para los efectos delante de la cámara). */
    private static final org.joml.Matrix4f CAMERA_POSE = new org.joml.Matrix4f();
    private static final org.joml.Matrix3f CAMERA_NORMAL = new org.joml.Matrix3f();

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            CAMERA_POSE.set(event.getPoseStack().last().pose());
            CAMERA_NORMAL.set(event.getPoseStack().last().normal());
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND) return;
        BodyAnimator.FirstPerson fp = pendingLeft;
        pendingLeft = null;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (fp == null || packArmDrawn || player == null || !event.getItemStack().isEmpty() || player.isInvisible()) return;
        HeroRendererScript r = renderer(player);
        if (r == null) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Las mismas transformaciones que usa Minecraft para el brazo en primera persona, del lado izquierdo
        float f = -1;
        pose.translate(f * 0.64000005F, -0.6F, -0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);
        fp.applyLeft(pose);
        if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer pr) {
            pr.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        drawSuitArm(player, r, model(), pose, event.getMultiBufferSource(), event.getPackedLight(), false, false);
        pose.popPose();
        event.setCanceled(true);
    }

    private static void drawSuitArm(AbstractClientPlayer player, HeroRendererScript r, SuitModel<AbstractClientPlayer> m, PoseStack pose,
                                    MultiBufferSource buffers, int light, boolean right, boolean effects) {
        try {
            JSEntity js = JSEntity.of(player);
            HeroRendererScript.Slot slot = HeroRendererScript.Slot.CHESTPLATE;
            m.attackTime = 0;
            m.crouching = false;
            m.swimAmount = 0;
            m.setupAnim(player, 0, 0, 0, 0, 0);
            List<String> parts = List.of(right ? "rightArm" : "leftArm");
            m.showParts(parts);
            ModelPart arm = right ? m.rightArm : m.leftArm;
            arm.xRot = 0;
            ResourceLocation tex = ClientHeroes.heroTexture(r.texturePath(r.textureKey(js, slot)), js);
            if (tex != null) {
                arm.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(tex)), light, OverlayTexture.NO_OVERLAY);
            }
            ResourceLocation lights = ClientHeroes.heroTexture(r.texturePath(r.lightsKey(js, slot)), js);
            if (lights != null) {
                arm.render(pose, buffers.getBuffer(RenderType.eyes(lights)), light, OverlayTexture.NO_OVERLAY);
            }
            if (effects) {
                // Como Fisk: render(entity, capa, true) para cada pieza del traje que se lleva puesta
                // (el HUD del casco, por ejemplo, se dibuja en la capa HELMET)
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (int i = 0; i < 4; i++) {
                    String id = SuitItem.heroId(player.getItemBySlot(slots[i]));
                    HeroRendererScript pr = id == null ? null : ClientHeroes.renderer(id);
                    if (pr == null || !pr.hasFunction("render")) continue;
                    HeroRendererScript.Slot ps = HeroRendererScript.Slot.values()[i];
                    RenderContext.current = new RenderContext(pose, buffers, light, m, player, js, ps, pr, parts);
                    RenderContext.current.firstPerson = true;
                    RenderContext.current.cameraPose = CAMERA_POSE;
                    RenderContext.current.cameraNormal = CAMERA_NORMAL;
                    try {
                        pr.call("render", js, ps.name(), true);
                    } catch (Throwable e) {
                        if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED) FiskCompat.LOGGER.error("[autotest] fp render " + ps, e);
                    } finally {
                        RenderContext.current = null;
                    }
                }
            }
        } catch (Throwable e) {
            // no romper el render de la mano por un fallo del script
            if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED) FiskCompat.LOGGER.error("[autotest] fp arm", e);
        }
    }
}
