package com.hendrilillo.fiskcompat.api;

import com.hendrilillo.fiskcompat.item.SuitItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Un ItemStack visto desde los scripts (JSItem en Fisk). */
public final class JSItem {
    private final ItemStack stack;

    public JSItem(ItemStack stack) { this.stack = stack == null ? ItemStack.EMPTY : stack; }

    public ItemStack stack() { return stack; }
    public boolean matches(JSItem o) { return o != null && ItemStack.isSameItemSameComponents(stack, o.stack); }
    public boolean isEmpty() { return stack.isEmpty(); }
    public String name() { return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public int stackSize() { return stack.getCount(); }
    public int maxStackSize() { return stack.getMaxStackSize(); }
    public int damage() { return stack.getDamageValue(); }
    public int maxDamage() { return stack.getMaxDamage(); }
    public String displayName() { return stack.getHoverName().getString(); }
    public boolean isWeapon() { return false; }
    public boolean isRifle() { return false; }
    public boolean isGun() { return false; }
    public boolean isLaserGun() { return false; }
    public boolean doesNeedTwoHands() { return false; }
    public boolean isRenamed() { return stack.has(DataComponents.CUSTOM_NAME); }
    public boolean isEnchanted() { return stack.isEnchanted(); }
    public int getEnchantmentLevel(int id) { return 0; }
    public boolean hasEnchantment(int id) { return false; }

    /** Id del héroe ("sind:mark2" o una variante "sind:mark3/gm"), o null si no es un traje. */
    public String suitType() { return SuitItem.heroId(stack); }

    public JSNbt nbt() {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copyTag();
        return new JSNbt(tag, () -> { if (!stack.isEmpty()) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); });
    }

    @Override public String toString() { return stack.toString(); }
}
