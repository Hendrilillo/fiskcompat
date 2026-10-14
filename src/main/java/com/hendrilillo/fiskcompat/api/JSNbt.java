package com.hendrilillo.fiskcompat.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Acceso de solo lectura a NBT para los scripts (JSNBT en Fisk). */
public final class JSNbt {
    final CompoundTag tag;

    public JSNbt(CompoundTag tag) { this.tag = tag == null ? new CompoundTag() : tag; }

    public CompoundTag tag() { return tag; }
    public boolean isEmpty() { return tag.isEmpty(); }
    public boolean hasKey(String k) { return tag.contains(k); }
    public byte getByte(String k) { return tag.getByte(k); }
    public short getShort(String k) { return tag.getShort(k); }
    public int getInteger(String k) { return tag.getInt(k); }
    public long getLong(String k) { return tag.getLong(k); }
    public float getFloat(String k) { return tag.getFloat(k); }
    public double getDouble(String k) { return tag.getDouble(k); }
    public String getString(String k) { return tag.getString(k); }
    public byte[] getByteArray(String k) { return tag.getByteArray(k); }
    public int[] getIntArray(String k) { return tag.getIntArray(k); }
    public boolean getBoolean(String k) { return tag.getBoolean(k); }
    public JSNbt getCompoundTag(String k) { return new JSNbt(tag.getCompound(k)); }
    public Object getTagList(String k) { return tag.getList(k, Tag.TAG_COMPOUND); }
    public Object getStringList(String k) { return tag.getList(k, Tag.TAG_STRING); }
    @Override public String toString() { return tag.toString(); }
}
