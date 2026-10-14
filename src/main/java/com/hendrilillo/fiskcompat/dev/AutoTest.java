package com.hendrilillo.fiskcompat.dev;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.item.ModItems;
import com.hendrilillo.fiskcompat.item.SuitItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Solo para desarrollo (se activa con -Dfiskcompat.autotest=true): prepara una escena con trajes
 * para poder hacer capturas automáticas del render. No hace nada en partidas normales.
 */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class AutoTest {
    public static final boolean ENABLED = Boolean.getBoolean("fiskcompat.autotest");
    /** "attacks": prueba de rayos y repulsores en vez de la de trajes. */
    public static final String MODE = System.getProperty("fiskcompat.autotest.mode", "suits");

    private AutoTest() {}

    public static String[] suits() {
        return System.getProperty("fiskcompat.autotest.suits", "sind:mark2,sind:mark3,sind:mark5,sind:mark7,sind:mark42,sind:mark43,sind:mark46,sind:mark50").split(",");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        double y = player.getY();
        player.teleportTo(level, 0.5, y, -4.5, 0, 0);
        String[] suits = suits();
        equip(player, suits[0]);
        if (MODE.equals("attacks")) {
            for (var old : level.getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(64),
                    e -> e instanceof ArmorStand || e instanceof net.minecraft.world.entity.animal.IronGolem)) old.discard();
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand != null) {
                stand.moveTo(4.0, y, 1.5, 90, 0);
                stand.setYHeadRot(90);
                equip(stand, suits[0]);
                level.addFreshEntity(stand);
            }
            var golem = EntityType.IRON_GOLEM.create(level);
            if (golem != null) {
                golem.moveTo(0.5, y, 12.5, 180, 0);
                golem.setNoAi(true);
                level.addFreshEntity(golem);
            }
            return;
        }
        for (int i = 0; i < suits.length; i++) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) continue;
            double x = (i - (suits.length - 1) / 2.0) * 1.4 + 0.5;
            stand.moveTo(x, y, 0.5, 180, 0);
            equip(stand, suits[i]);
            level.addFreshEntity(stand);
        }
    }

    private static void equip(net.minecraft.world.entity.LivingEntity e, String suit) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        SuitItem[] items = ModItems.suitItems();
        for (int i = 0; i < 4; i++) {
            ItemStack s = items[i].create(suit.trim());
            e.setItemSlot(slots[i], s);
        }
    }
}
