package com.hendrilillo.fiskcompat.dev;

import com.hendrilillo.fiskcompat.FiskCompat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Parte de cliente de {@link AutoTest}: hace capturas y cierra el juego. */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class AutoTestClient {
    private static int ticks;

    private AutoTestClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!AutoTest.ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        if (ticks == 5) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (ticks == 110) mc.options.fov().set(30);
        if (ticks == 115) shot(mc, "fiskcompat_zoom.png");
        if (ticks == 118) mc.options.fov().set(70);
        if (ticks == 120) shot(mc, "fiskcompat_stands.png");
        if (ticks == 125) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (ticks == 160) shot(mc, "fiskcompat_player_front.png");
        if (ticks == 165) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 200) shot(mc, "fiskcompat_player_back.png");
        if (ticks == 205) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.getInventory().selected = 0;
            mc.player.getInventory().setItem(0, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());
            mc.player.getInventory().setItem(1, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy());
            mc.player.getInventory().setItem(2, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy());
            mc.player.getInventory().setItem(3, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy());
        }
        if (ticks == 240) shot(mc, "fiskcompat_hotbar.png");
        if (ticks == 245) mc.player.getInventory().selected = 6;
        if (ticks == 270) shot(mc, "fiskcompat_arm.png");
        if (ticks == 290) mc.stop();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> FiskCompat.LOGGER.info("[autotest] {}", msg.getString()));
    }
}
