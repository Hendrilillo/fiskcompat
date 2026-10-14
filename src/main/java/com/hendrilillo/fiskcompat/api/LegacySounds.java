package com.hendrilillo.fiskcompat.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Nombres de sonidos de Minecraft 1.7.10 ("random.fizz") y su equivalente en 1.21. */
public final class LegacySounds {
    private static final Map<String, String> MAP = Map.ofEntries(
            Map.entry("random.orb", "entity.experience_orb.pickup"),
            Map.entry("random.fizz", "block.fire.extinguish"),
            Map.entry("random.explode", "entity.generic.explode"),
            Map.entry("random.anvil_use", "block.anvil.use"),
            Map.entry("random.anvil_land", "block.anvil.land"),
            Map.entry("random.anvil_break", "block.anvil.destroy"),
            Map.entry("random.click", "ui.button.click"),
            Map.entry("random.pop", "entity.item.pickup"),
            Map.entry("random.bow", "entity.arrow.shoot"),
            Map.entry("random.bowhit", "entity.arrow.hit"),
            Map.entry("random.break", "entity.item.break"),
            Map.entry("random.levelup", "entity.player.levelup"),
            Map.entry("random.door_open", "block.wooden_door.open"),
            Map.entry("random.door_close", "block.wooden_door.close"),
            Map.entry("random.wood_click", "block.wooden_button.click_on"),
            Map.entry("random.glass", "block.glass.break"),
            Map.entry("random.successful_hit", "entity.arrow.hit_player"),
            Map.entry("random.burp", "entity.player.burp"),
            Map.entry("random.eat", "entity.generic.eat"),
            Map.entry("random.drink", "entity.generic.drink"),
            Map.entry("random.chestopen", "block.chest.open"),
            Map.entry("random.chestclosed", "block.chest.close"),
            Map.entry("random.splash", "entity.generic.splash"),
            Map.entry("random.swim", "entity.generic.swim"),
            Map.entry("random.fuse", "entity.tnt.primed"),
            Map.entry("fire.fire", "block.fire.ambient"),
            Map.entry("fire.ignite", "item.flintandsteel.use"),
            Map.entry("note.pling", "block.note_block.pling"),
            Map.entry("note.harp", "block.note_block.harp"),
            Map.entry("note.bass", "block.note_block.bass"),
            Map.entry("tile.piston.out", "block.piston.extend"),
            Map.entry("tile.piston.in", "block.piston.contract"),
            Map.entry("mob.endermen.portal", "entity.enderman.teleport"),
            Map.entry("mob.wither.shoot", "entity.wither.shoot"),
            Map.entry("mob.wither.spawn", "entity.wither.spawn"),
            Map.entry("mob.ghast.fireball", "entity.ghast.shoot"),
            Map.entry("mob.irongolem.hit", "entity.iron_golem.hurt"),
            Map.entry("mob.irongolem.throw", "entity.iron_golem.attack"),
            Map.entry("mob.irongolem.walk", "entity.iron_golem.step"),
            Map.entry("mob.blaze.hit", "entity.blaze.hurt"),
            Map.entry("mob.zombie.metal", "entity.zombie.attack_iron_door"),
            Map.entry("mob.zombie.wood", "entity.zombie.attack_wooden_door"),
            Map.entry("mob.zombie.woodbreak", "entity.zombie.break_wooden_door"),
            Map.entry("mob.enderdragon.wings", "entity.ender_dragon.flap"),
            Map.entry("mob.enderdragon.growl", "entity.ender_dragon.growl"),
            Map.entry("mob.bat.takeoff", "entity.bat.takeoff"),
            Map.entry("mob.sheep.shear", "entity.sheep.shear"),
            Map.entry("game.player.hurt", "entity.player.hurt"),
            Map.entry("game.player.die", "entity.player.death"),
            Map.entry("ambient.weather.thunder", "entity.lightning_bolt.thunder"),
            Map.entry("ambient.weather.rain", "weather.rain"),
            Map.entry("dig.stone", "block.stone.break"),
            Map.entry("dig.wood", "block.wood.break"),
            Map.entry("dig.glass", "block.glass.break"),
            Map.entry("step.stone", "block.stone.step"),
            Map.entry("portal.travel", "block.portal.travel"),
            Map.entry("portal.trigger", "block.portal.trigger"));

    private LegacySounds() {}

    /** Nombre de sonido de un script de Fisk -&gt; ResourceLocation para 1.21 (null si no vale). */
    public static ResourceLocation map(String name) {
        if (name == null || name.isEmpty()) return null;
        String ns = "minecraft", path = name;
        int c = name.indexOf(':');
        if (c >= 0) {
            ns = name.substring(0, c);
            path = name.substring(c + 1);
        }
        if (ns.equals("minecraft") && MAP.containsKey(path)) path = MAP.get(path);
        return ResourceLocation.tryBuild(ns, path);
    }
}
