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
        if (AutoTest.MODE.equals("attacks")) {
            attacks(mc);
            return;
        }
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

    private static void set(net.minecraft.world.entity.Entity e, String var, Object v) {
        com.hendrilillo.fiskcompat.data.DataStore.set(e, var, v);
        com.hendrilillo.fiskcompat.data.DataStore.snapshot(e);
    }

    private static void log(Minecraft mc, String what) {
        var golems = mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, mc.player.getBoundingBox().inflate(40));
        FiskCompat.LOGGER.info("[autotest] {} aiming={} aim_t={} charging={} charge={} shooting={} heat={} golem={}", what,
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:aiming"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:aiming_timer"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_charging"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_charge"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_shooting_timer"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:heat_vision_timer"),
                golems.isEmpty() ? "-" : golems.get(0).getHealth());
    }

    private static void logNbt(Minecraft mc, String what) {
        var chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        var data = chest.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        FiskCompat.LOGGER.info("[autotest] {} hand={} armgun={} armtimer={} nbt={}", what, mc.player.getMainHandItem(),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/armgun_bool"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/armrockets_timer"), data);
    }

    /** Prueba de ataques: rayos en un soporte de armadura (vista lateral) y disparos del jugador. */
    private static void attacks(Minecraft mc) {
        var keys = com.hendrilillo.fiskcompat.client.ClientKeys.ABILITIES;
        var stands = mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, mc.player.getBoundingBox().inflate(20));
        var stand = stands.isEmpty() ? null : stands.get(0);
        if (ticks == 5) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        // Soporte: unibeam, láser del brazo y rayos de energía forzando sus variables
        if (ticks == 60 && stand != null) { set(stand, "fiskheroes:beam_charging", true); set(stand, "fiskheroes:beam_charge", 1f); set(stand, "fiskheroes:beam_shooting_timer", 1f); }
        if (ticks == 64) shot(mc, "fiskcompat_stand_unibeam.png");
        if (ticks == 65 && stand != null) { set(stand, "fiskheroes:beam_shooting_timer", 0f); set(stand, "fiskheroes:heat_vision", true); set(stand, "fiskheroes:heat_vision_timer", 1f); set(stand, "fiskheroes:aiming_timer", 1f); }
        if (ticks == 69) shot(mc, "fiskcompat_stand_laser.png");
        if (ticks == 70 && stand != null) { set(stand, "fiskheroes:heat_vision_timer", 0f); set(stand, "fiskheroes:aiming_timer", 0f); set(stand, "fiskheroes:energy_projection_timer", 1f); }
        if (ticks == 74) shot(mc, "fiskcompat_stand_energy.png");
        if (ticks == 75 && stand != null) set(stand, "fiskheroes:energy_projection_timer", 0f);
        // Jugador: apuntar y repulsor
        if (ticks == 80) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); keys[0].setDown(true); }
        if (ticks == 95) { log(mc, "aim"); mc.options.keyUse.setDown(true); }
        if (ticks == 102) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 100 || ticks == 104 || ticks == 108) { FiskCompat.LOGGER.info("[autotest] shot tick {}", mc.level.getGameTime()); shot(mc, "fiskcompat_repulsor_back" + ticks + ".png"); }
        if (ticks == 97) mc.options.keyUse.setDown(false);
        if (ticks == 120) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 125) shot(mc, "fiskcompat_aim_back.png");
        if (ticks == 130) { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.keyUse.setDown(true); }
        if (ticks == 134 || ticks == 138) shot(mc, "fiskcompat_repulsor_fp" + ticks + ".png");
        if (ticks == 131) mc.options.keyUse.setDown(false);
        if (ticks == 140) { keys[0].setDown(false); log(mc, "after repulsor"); }
        // Unibeam
        if (ticks == 150) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            net.minecraft.client.KeyMapping.click(keys[1].getKey());
            keys[1].setDown(true);
        }
        if (ticks == 170) log(mc, "charging");
        if (ticks == 215) { log(mc, "unibeam"); shot(mc, "fiskcompat_unibeam_back.png"); }
        if (ticks == 216) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 222) shot(mc, "fiskcompat_unibeam_fp.png");
        if (ticks == 260) { keys[1].setDown(false); log(mc, "after unibeam"); }
        // Láser del brazo (HEAT_VISION en la tecla 5)
        if (ticks == 330) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); keys[4].setDown(true); }
        if (ticks == 345) { log(mc, "laser"); shot(mc, "fiskcompat_laser_back.png"); }
        if (ticks == 346) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 350) shot(mc, "fiskcompat_laser_fp.png");
        if (ticks == 380) { keys[4].setDown(false); log(mc, "end laser"); }
        // Primera persona apuntando, con la mano visible
        if (ticks == 385) { mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); keys[0].setDown(true); }
        if (ticks == 400) shot(mc, "fiskcompat_aim_fp.png");
        if (ticks == 401) keys[0].setDown(false);
        if (ticks == 410) shot(mc, "fiskcompat_noaim_fp.png");
        // Cohete del brazo: guardar TNT en el traje, recargar, sacar el brazo y disparar
        if (ticks == 415) {
            mc.player.getInventory().selected = 0;
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                var sp = server.getPlayerList().getPlayer(uuid);
                sp.getInventory().selected = 0;
                sp.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TNT, 8));
            });
        }
        if (ticks == 418) net.minecraft.client.KeyMapping.click(com.hendrilillo.fiskcompat.client.ClientKeys.EQUIP.getKey());
        if (ticks == 425) { logNbt(mc, "stored"); net.minecraft.client.KeyMapping.click(keys[3].getKey()); }
        if (ticks == 435) { logNbt(mc, "reloaded"); keys[3].setDown(true); }
        if (ticks == 470) { logNbt(mc, "deployed"); mc.options.keyUse.setDown(true); }
        if (ticks == 472) mc.options.keyUse.setDown(false);
        if (ticks == 474) shot(mc, "fiskcompat_armrocket_fp.png");
        if (ticks == 480) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 500) { mc.options.keyUse.setDown(true); }
        if (ticks == 502) mc.options.keyUse.setDown(false);
        if (ticks == 504) shot(mc, "fiskcompat_armrocket_front.png");
        if (ticks == 520) { keys[3].setDown(false); logNbt(mc, "after rockets"); log(mc, "end"); }
        if (ticks == 530) mc.stop();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> FiskCompat.LOGGER.info("[autotest] {}", msg.getString()));
    }
}
