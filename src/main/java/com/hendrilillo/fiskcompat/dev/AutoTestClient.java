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
        if (ticks == 275) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskcompat:dummy", 0);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", true);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 6, mc.player.getZ());
        }
        if (ticks == 300) shot(mc, "fiskcompat_flying_mask.png");
        if (ticks == 301) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        }
        if (ticks == 322) shot(mc, "fiskcompat_flying_boost.png");
        if (ticks == 323) { mc.player.setYRot(mc.player.getYRot() + 90); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 330) shot(mc, "fiskcompat_flying_side.png");
        if (ticks == 331) { mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false); }
        if (ticks == 345) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", false);
        if (ticks == 346) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", false);
        if (ticks == 370) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.fov().set(30);
            boolean n = com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/nanites") instanceof Boolean b && b;
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "sind:dyn/nanites", !n);
        }
        if (ticks == 374) shot(mc, "fiskcompat_nano_mid.png");
        if (ticks == 395) shot(mc, "fiskcompat_nano_end.png");
        if (ticks == 396) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
        if (ticks == 425) shot(mc, "fiskcompat_mask_zoom.png");
        if (ticks == 426) mc.player.setYRot(mc.player.getYRot() + 60);
        if (ticks == 428) mc.player.setYBodyRot(mc.player.getYRot());
        if (ticks == 429) shot(mc, "fiskcompat_mask_side.png");
        if (ticks == 432) {
            FiskCompat.LOGGER.info("[autotest] flying={} y={} timer={}", com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:flying"),
                    mc.player.getY(), com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:flight_timer"));
            mc.stop();
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> FiskCompat.LOGGER.info("[autotest] {}", msg.getString()));
    }
}
