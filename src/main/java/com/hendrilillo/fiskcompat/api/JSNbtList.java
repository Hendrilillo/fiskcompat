package com.hendrilillo.fiskcompat.api;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Lista NBT vista desde los scripts (JSNBTList en Fisk). */
public final class JSNbtList {
    final ListTag list;

    public JSNbtList(ListTag list) { this.list = list == null ? new ListTag() : list; }

    public ListTag tag() { return list; }
    public String type() { return Tag.TAG_COMPOUND == list.getElementType() ? "COMPOUND" : String.valueOf(list.getElementType()); }
    public int tagCount() { return list.size(); }
    public JSNbt getCompoundTag(int i) { return new JSNbt(list.getCompound(i)); }
    public String getString(int i) { return list.getString(i); }
    public int[] getIntArray(int i) { return list.getIntArray(i); }
    public double getDouble(int i) { return list.getDouble(i); }
    public float getFloat(int i) { return list.getFloat(i); }
    @Override public String toString() { return list.toString(); }
}
