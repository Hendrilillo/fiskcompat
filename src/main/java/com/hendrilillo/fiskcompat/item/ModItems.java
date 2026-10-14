package com.hendrilillo.fiskcompat.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.hero.HeroDefinition;
import com.hendrilillo.fiskcompat.hero.HeroRegistry;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FiskCompat.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, FiskCompat.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FiskCompat.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FiskCompat.MODID);

    /** Héroe del traje: "dominio:heroe" o una variante "dominio:heroe/alt". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> HERO =
            COMPONENTS.registerComponentType("hero", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUIT_MATERIAL = MATERIALS.register("suit", () -> {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "suit"))), 2.0f, 0.0f);
    });

    public static final DeferredItem<SuitItem> SUIT_HELMET = suit("suit_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<SuitItem> SUIT_CHESTPLATE = suit("suit_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<SuitItem> SUIT_LEGGINGS = suit("suit_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<SuitItem> SUIT_BOOTS = suit("suit_boots", ArmorItem.Type.BOOTS);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("heroes", () -> CreativeModeTab.builder()
            .title(Component.literal("FiskCompat - Heropacks"))
            .icon(() -> new ItemStack(SUIT_CHESTPLATE.get()))
            .displayItems((params, output) -> {
                for (String heroId : allSuitIds()) {
                    for (SuitItem item : suitItems()) {
                        ItemStack s = item.create(heroId);
                        if (!s.isEmpty()) output.accept(s);
                    }
                }
            })
            .build());

    private ModItems() {}

    private static DeferredItem<SuitItem> suit(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new SuitItem(SUIT_MATERIAL, type, new Item.Properties().stacksTo(1)));
    }

    public static SuitItem[] suitItems() {
        return new SuitItem[]{SUIT_HELMET.get(), SUIT_CHESTPLATE.get(), SUIT_LEGGINGS.get(), SUIT_BOOTS.get()};
    }

    /** Todos los héroes visibles y sus variantes ("alts" del heropack.json). */
    public static List<String> allSuitIds() {
        List<String> out = new java.util.ArrayList<>();
        for (HeroDefinition hero : HeroRegistry.all()) {
            if (hero.hidden()) continue;
            out.add(hero.id());
            for (String alt : alts(hero.id()).keySet()) out.add(hero.id() + "/" + alt);
        }
        return out;
    }

    /** Variantes de un héroe declaradas en heropack.json -> nombre. */
    public static Map<String, String> alts(String heroId) {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        for (HeroPack pack : HeroPackManager.packs()) {
            if (!pack.info().has("alts")) continue;
            JsonObject alts = pack.info().getAsJsonObject("alts");
            if (!alts.has(heroId)) continue;
            JsonObject o = alts.getAsJsonObject(heroId);
            for (String key : o.keySet()) {
                if (key.startsWith("$")) continue;
                JsonElement e = o.get(key);
                String name = e.isJsonPrimitive() ? e.getAsString()
                        : e.getAsJsonObject().has("name") ? e.getAsJsonObject().get("name").getAsString() : key;
                out.put(key, name);
            }
        }
        return out;
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        MATERIALS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    static Holder<ArmorMaterial> material() {
        return SUIT_MATERIAL;
    }
}
