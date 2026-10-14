package com.hendrilillo.fiskcompat.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Acceso de solo lectura a NBT para los scripts (JSNBT en Fisk). */
public final class JSNbt {
    final CompoundTag tag;
    /** Si no es null, se llama tras cada cambio para guardar el NBT en el ítem. */
    Runnable writeBack;

    public JSNbt(CompoundTag tag) { this.tag = tag == null ? new CompoundTag() : tag; }

    public JSNbt(CompoundTag tag, Runnable writeBack) {
        this(tag);
        this.writeBack = writeBack;
    }

    public void changed() {
        if (writeBack != null) writeBack.run();
    }

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
    public JSNbtList getTagList(String k) { return new JSNbtList(tag.getList(k, Tag.TAG_COMPOUND)); }
    public JSNbtList getStringList(String k) { return new JSNbtList(tag.getList(k, Tag.TAG_STRING)); }
    public JSNbtList getIntArrayList(String k) { return new JSNbtList(tag.getList(k, Tag.TAG_INT_ARRAY)); }
    public JSNbtList getDoubleList(String k) { return new JSNbtList(tag.getList(k, Tag.TAG_DOUBLE)); }
    public JSNbtList getFloatList(String k) { return new JSNbtList(tag.getList(k, Tag.TAG_FLOAT)); }
    @Override public String toString() { return tag.toString(); }
}
