package com.hendrilillo.fiskcompat.api;

import com.hendrilillo.fiskcompat.data.DataStore;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;

/** El objeto "manager" que reciben los tickHandler y funciones de teclas (JSDataManager en Fisk). */
public final class DataManagerApi {
    public static final DataManagerApi INSTANCE = new DataManagerApi();

    private DataManagerApi() {}

    private static Entity mc(JSEntity e) {
        return e == null ? null : e.mc();
    }

    public void setData(JSEntity e, String name, Object value) {
        if (mc(e) != null) DataStore.set(mc(e), name, value);
    }

    public void setDataWithNotify(JSEntity e, String name, Object value) {
        setData(e, name, value);
    }

    public void setInterpolatedData(JSEntity e, String name, Object value) {
        if (mc(e) != null) DataStore.setNoInterp(mc(e), name, value);
    }

    /** Sube (hasta 1) en "up" ticks si inc, baja (hasta 0) en "down" ticks si dec. */
    public static void incr(Entity e, String name, double up, double down, boolean inc, boolean dec) {
        // Cuentas en float, igual que Fisk (los scripts comparan "== 1")
        Object cur = DataStore.get(e, name);
        float v = cur instanceof Number n ? n.floatValue() : 0;
        if (inc && v < 1) v += 1f / (float) up;
        else if (dec && v > 0) v -= 1f / (float) down;
        DataStore.set(e, name, Math.max(0f, Math.min(1f, v)));
    }

    public void incrementData(JSEntity e, String name, double up, double down, boolean inc, boolean dec) {
        if (mc(e) != null) incr(mc(e), name, up, down, inc, dec);
    }

    public void incrementData(JSEntity e, String name, double duration, boolean inc, boolean dec) {
        incrementData(e, name, duration, duration, inc, dec);
    }

    public void incrementData(JSEntity e, String name, double up, double down, boolean flag) {
        incrementData(e, name, up, down, flag, !flag);
    }

    public void incrementData(JSEntity e, String name, double duration, boolean flag) {
        incrementData(e, name, duration, duration, flag, !flag);
    }

    // ------------------------------------------------------------------ NBT

    public void setByte(JSNbt n, String k, Number v) { n.tag.putByte(k, v.byteValue()); n.changed(); }
    public void setShort(JSNbt n, String k, Number v) { n.tag.putShort(k, v.shortValue()); n.changed(); }
    public void setInteger(JSNbt n, String k, Number v) { n.tag.putInt(k, v.intValue()); n.changed(); }
    public void setLong(JSNbt n, String k, Number v) { n.tag.putLong(k, v.longValue()); n.changed(); }
    public void setFloat(JSNbt n, String k, Number v) { n.tag.putFloat(k, v.floatValue()); n.changed(); }
    public void setDouble(JSNbt n, String k, Number v) { n.tag.putDouble(k, v.doubleValue()); n.changed(); }
    public void setString(JSNbt n, String k, String v) { n.tag.putString(k, v); n.changed(); }
    public void setBoolean(JSNbt n, String k, boolean v) { n.tag.putBoolean(k, v); n.changed(); }
    public void setCompoundTag(JSNbt n, String k, JSNbt v) { n.tag.put(k, v.tag.copy()); n.changed(); }
    public void setTagList(JSNbt n, String k, Object list) { ListTag l = list instanceof JSNbtList jl ? jl.list : list instanceof ListTag lt ? lt : null; if (l != null) { n.tag.put(k, l.copy()); n.changed(); } }
    public void setByteArray(JSNbt n, String k, Object v) { if (v instanceof byte[] b) { n.tag.put(k, new ByteArrayTag(b)); n.changed(); } }
    public void setIntArray(JSNbt n, String k, Object v) { if (v instanceof int[] b) { n.tag.put(k, new IntArrayTag(b)); n.changed(); } }
    public void removeTag(JSNbt n, String k) { n.tag.remove(k); n.changed(); }
    public void removeTag(JSNbtList l, int i) { if (i >= 0 && i < l.list.size()) { l.list.remove(i); l.changed(); } }
    public JSNbt newCompoundTag() { return new JSNbt(new CompoundTag()); }
    public JSNbt newCompoundTag(String snbt) {
        try { return new JSNbt(net.minecraft.nbt.TagParser.parseTag(snbt)); } catch (Exception e) { return new JSNbt(new CompoundTag()); }
    }
    public JSNbtList newTagList() { return new JSNbtList(new ListTag()); }
    public JSNbtList newTagList(String snbt) { return new JSNbtList(new ListTag()); }
    public void appendTag(JSNbtList l, JSNbt t) { l.list.add(t.tag.copy()); l.changed(); }
    public void appendString(JSNbtList l, String s) { l.list.add(StringTag.valueOf(s)); l.changed(); }
    public void appendFloat(JSNbtList l, Number v) { l.list.add(FloatTag.valueOf(v.floatValue())); l.changed(); }
    public void appendDouble(JSNbtList l, Number v) { l.list.add(DoubleTag.valueOf(v.doubleValue())); l.changed(); }
    public void appendIntArray(JSNbtList l, Object v) { if (v instanceof int[] a) l.list.add(new IntArrayTag(a)); }
}
