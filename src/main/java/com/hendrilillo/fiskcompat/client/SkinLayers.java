package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.item.SuitItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Oculta las capas exteriores de la skin (gorro, chaqueta, mangas, pantalón) debajo de las piezas
 * de traje puestas, para que no atraviesen la armadura.
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class SkinLayers {
    private SkinLayers() {}

    public static boolean wearing(LivingEntity e, EquipmentSlot slot) {
        return SuitItem.heroId(e.getItemBySlot(slot)) != null;
    }

    @SubscribeEvent
    public static void pre(RenderPlayerEvent.Pre event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        LivingEntity e = event.getEntity();
        if (wearing(e, EquipmentSlot.HEAD)) model.hat.visible = false;
        if (wearing(e, EquipmentSlot.CHEST)) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (wearing(e, EquipmentSlot.LEGS) || wearing(e, EquipmentSlot.FEET)) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }
}
