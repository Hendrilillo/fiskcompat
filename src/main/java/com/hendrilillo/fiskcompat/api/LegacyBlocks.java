package com.hendrilillo.fiskcompat.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Nombre y "metadata" de un bloque como en la 1.7.10 (los scripts de Fisk comparan con esos nombres). */
public final class LegacyBlocks {
    private static final Map<String, String> NAME = new HashMap<>();
    private static final Map<String, Integer> META = new HashMap<>();
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static void put(String modern, String legacy, int meta) {
        NAME.put(modern, legacy);
        META.put(modern, meta);
    }

    static {
        put("grass_block", "grass", 0);
        put("dirt", "dirt", 0);
        put("coarse_dirt", "dirt", 1);
        put("podzol", "dirt", 2);
        String[] stone = {"stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"};
        for (int i = 0; i < stone.length; i++) put(stone[i], "stone", i);
        String[] woods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < woods.length; i++) {
            put(woods[i] + "_planks", "planks", i);
            put(woods[i] + "_log", i < 4 ? "log" : "log2", i % 4);
            put(woods[i] + "_leaves", i < 4 ? "leaves" : "leaves2", i % 4);
        }
        for (int i = 0; i < 16; i++) {
            put(COLORS[i] + "_wool", "wool", i);
            put(COLORS[i] + "_terracotta", "stained_hardened_clay", i);
            put(COLORS[i] + "_stained_glass", "stained_glass", i);
            put(COLORS[i] + "_carpet", "carpet", i);
        }
        put("terracotta", "hardened_clay", 0);
        put("stone_bricks", "stonebrick", 0);
        put("mossy_stone_bricks", "stonebrick", 1);
        put("cracked_stone_bricks", "stonebrick", 2);
        put("chiseled_stone_bricks", "stonebrick", 3);
        put("sandstone", "sandstone", 0);
        put("chiseled_sandstone", "sandstone", 1);
        put("cut_sandstone", "sandstone", 2);
        put("sand", "sand", 0);
        put("red_sand", "sand", 1);
        put("bricks", "brick_block", 0);
        put("melon", "melon_block", 0);
        put("jack_o_lantern", "lit_pumpkin", 0);
        put("carved_pumpkin", "pumpkin", 0);
        put("nether_quartz_ore", "quartz_ore", 0);
        put("nether_bricks", "nether_brick", 0);
        put("snow_block", "snow", 0);
        put("snow", "snow_layer", 0);
        put("quartz_block", "quartz_block", 0);
        put("chiseled_quartz_block", "quartz_block", 1);
        put("quartz_pillar", "quartz_block", 2);
        put("short_grass", "tallgrass", 1);
        put("fern", "tallgrass", 2);
        put("cobweb", "web", 0);
        put("spawner", "mob_spawner", 0);
        put("dandelion", "yellow_flower", 0);
        put("poppy", "red_flower", 0);
        put("lily_pad", "waterlily", 0);
        put("end_stone_bricks", "end_stone", 0);
        put("redstone_lamp", "redstone_lamp", 0);
        put("wet_sponge", "sponge", 1);
        put("magma_block", "netherrack", 0);
    }

    private LegacyBlocks() {}

    public static String name(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("minecraft")) return id.toString();
        return "minecraft:" + NAME.getOrDefault(id.getPath(), id.getPath());
    }

    public static int meta(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("minecraft") ? META.getOrDefault(id.getPath(), 0) : 0;
    }
}
