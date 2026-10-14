package com.hendrilillo.fiskcompat.item;

import com.hendrilillo.fiskcompat.hero.HeroDefinition;
import com.hendrilillo.fiskcompat.hero.HeroRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Pieza de traje genérica. El héroe concreto se guarda en el componente "fiskcompat:hero",
 * así un mismo ítem sirve para todos los héroes de todos los heropacks.
 */
public class SuitItem extends ArmorItem {
    public SuitItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /** Índice de pieza al estilo Fisk: 0 casco, 1 pecho, 2 piernas, 3 botas. */
    public int pieceIndex() {
        return switch (getType()) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            default -> 3;
        };
    }

    public static String heroId(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SuitItem)) return null;
        return stack.get(ModItems.HERO.get());
    }

    /** Id del héroe base (sin la variante). */
    public static String baseHeroId(String suitId) {
        if (suitId == null) return null;
        int c = suitId.indexOf(':');
        int s = suitId.indexOf('/', c + 1);
        return s < 0 ? suitId : suitId.substring(0, s);
    }

    public static HeroDefinition hero(ItemStack stack) {
        String id = baseHeroId(heroId(stack));
        return id == null ? null : HeroRegistry.get(id);
    }

    /** Crea esta pieza para un héroe, o EMPTY si el héroe no tiene esta pieza. */
    public ItemStack create(String suitId) {
        HeroDefinition hero = HeroRegistry.get(baseHeroId(suitId));
        if (hero == null || hero.piece(pieceIndex()) == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(this);
        stack.set(ModItems.HERO.get(), suitId);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String suitId = heroId(stack);
        HeroDefinition hero = hero(stack);
        if (hero == null) return super.getName(stack);
        String name = hero.name();
        String alt = suitId.equals(hero.id()) ? null : suitId.substring(hero.id().length() + 1);
        if (alt != null) {
            String altName = ModItems.alts(hero.id()).get(alt);
            if (altName != null) name = name + " §r(" + com.hendrilillo.fiskcompat.pack.PackLang.get(altName) + "§r)";
        }
        return Component.literal(name + "§r " + hero.pieceDisplayName(pieceIndex()));
    }

    /** La armadura vanilla no se dibuja (ver FiskCompatClient); esto solo evita texturas inexistentes. */
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_1.png");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = heroId(stack);
        if (id != null) tooltip.add(Component.literal("§8" + id));
        HeroDefinition hero = hero(stack);
        if (hero == null && id != null) tooltip.add(Component.literal("§cHéroe no cargado"));
    }
}
