package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Oculta las capas exteriores de la skin (gorro, chaqueta, mangas, pantalón) bajo las partes
 * que dibuja el traje. Se mira qué partes enseña cada pieza puesta (showModel), así funciona también
 * con trajes de una sola pieza como los nanotrajes.
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class SkinLayers {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private SkinLayers() {}

    public static boolean wearing(LivingEntity e, EquipmentSlot slot) {
        return SuitItem.heroId(e.getItemBySlot(slot)) != null;
    }

    /** Partes del cuerpo (nombres de Fisk) que tapa el traje que lleva la entidad. */
    public static Set<String> coveredParts(LivingEntity e) {
        Set<String> out = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String suit = SuitItem.heroId(e.getItemBySlot(SLOTS[i]));
            if (suit == null) continue;
            HeroRendererScript r = ClientHeroes.renderer(suit);
            HeroRendererScript.Slot slot = HeroRendererScript.Slot.values()[i];
            List<String> parts = r != null ? r.shownParts(slot) : null;
            out.addAll(parts != null ? parts : SuitModel.defaultParts(slot));
        }
        return out;
    }

    @SubscribeEvent
    public static void pre(RenderPlayerEvent.Pre event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        Set<String> parts = coveredParts(event.getEntity());
        if (parts.isEmpty()) return;
        if (parts.contains("head") || parts.contains("headwear")) model.hat.visible = false;
        if (parts.contains("body")) model.jacket.visible = false;
        if (parts.contains("rightArm")) model.rightSleeve.visible = false;
        if (parts.contains("leftArm")) model.leftSleeve.visible = false;
        if (parts.contains("rightLeg")) model.rightPants.visible = false;
        if (parts.contains("leftLeg")) model.leftPants.visible = false;
    }
}
