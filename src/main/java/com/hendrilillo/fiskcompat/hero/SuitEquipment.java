package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.api.JSItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import org.openjdk.nashorn.api.scripting.JSObject;

import java.util.Map;

/**
 * Objetos guardados en el traje (hero.addPrimaryEquipment): con el objeto en la mano, la tecla de
 * equipo lo mete en el traje; con la mano vacía, saca el último. Se guardan como en Fisk, en la lista
 * "Equipment" del NBT de la pechera: {Index, Item:{id, Count, Damage}} (los scripts leen eso).
 */
public final class SuitEquipment {
    /** Números de objeto de la 1.7.10 que usan los heropacks; el resto recibe uno fijo propio. */
    private static final Map<String, Integer> LEGACY = Map.ofEntries(
            Map.entry("minecraft:tnt", 46), Map.entry("minecraft:gunpowder", 289), Map.entry("minecraft:fire_charge", 385),
            Map.entry("minecraft:arrow", 262), Map.entry("minecraft:redstone", 331), Map.entry("minecraft:iron_ingot", 265),
            Map.entry("minecraft:gold_ingot", 266), Map.entry("minecraft:diamond", 264), Map.entry("minecraft:coal", 263),
            Map.entry("minecraft:snowball", 332), Map.entry("minecraft:ender_pearl", 368), Map.entry("minecraft:blaze_rod", 369),
            Map.entry("minecraft:sugar", 353), Map.entry("minecraft:feather", 288), Map.entry("minecraft:glowstone_dust", 348));

    private SuitEquipment() {}

    private static String clean(String item) {
        String s = item;
        int i = s.indexOf('{');
        if (i >= 0) s = s.substring(0, i);
        i = s.indexOf('@');
        if (i >= 0) s = s.substring(0, i);
        return s.contains(":") ? s : "minecraft:" + s;
    }

    public static int legacyId(String item) {
        String name = clean(item);
        Integer id = LEGACY.get(name);
        return id != null ? id : 4096 + Math.floorMod(name.hashCode(), 28000);
    }

    private static int capacity(HeroDefinition def) {
        return Math.max(1, def.equipment.size());
    }

    private static boolean valid(HeroDefinition def, ItemStack stack) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (Object[] e : def.equipment) {
            if (e.length == 0 || !(e[0] instanceof String s) || !clean(s).equals(name)) continue;
            if (e.length > 2 && e[2] instanceof JSObject f && f.isFunction()) {
                try {
                    Object ok = f.call(null, new JSItem(stack));
                    if (!(ok instanceof Boolean b && b)) continue;
                } catch (Throwable t) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    /** Servidor: el jugador ha pulsado la tecla de equipo. */
    public static void toggle(ServerPlayer p) {
        ActiveHero hero = ActiveHero.of(p);
        if (hero == null || hero.def.equipment.isEmpty()) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        CustomData data = chest.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        ListTag list = tag.getList("Equipment", Tag.TAG_COMPOUND);
        ItemStack held = p.getMainHandItem();
        if (!held.isEmpty()) {
            if (!valid(hero.def, held)) return;
            int id = legacyId(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            int left = held.getCount();
            // Primero se junta con lo que ya haya del mismo tipo
            for (int i = 0; i < list.size() && left > 0; i++) {
                CompoundTag item = list.getCompound(i).getCompound("Item");
                if (item.getShort("id") != id) continue;
                int add = Math.min(left, held.getMaxStackSize() - item.getByte("Count"));
                if (add <= 0) continue;
                item.putByte("Count", (byte) (item.getByte("Count") + add));
                left -= add;
            }
            if (left > 0 && list.size() < capacity(hero.def)) {
                CompoundTag item = new CompoundTag();
                item.putShort("id", (short) id);
                item.putByte("Count", (byte) left);
                item.putShort("Damage", (short) 0);
                item.putString("fiskcompat:name", BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
                CompoundTag entry = new CompoundTag();
                entry.putByte("Index", (byte) list.size());
                entry.put("Item", item);
                list.add(entry);
                left = 0;
            }
            if (left == held.getCount()) return;
            held.setCount(left);
            p.setItemInHand(InteractionHand.MAIN_HAND, left > 0 ? held : ItemStack.EMPTY);
        } else {
            if (list.isEmpty()) return;
            CompoundTag item = list.getCompound(list.size() - 1).getCompound("Item");
            list.remove(list.size() - 1);
            String name = item.getString("fiskcompat:name");
            var it = name.isEmpty() ? null : BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(name)).orElse(null);
            if (it != null && item.getByte("Count") > 0) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(it, item.getByte("Count")));
        }
        tag.put("Equipment", list);
        chest.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
