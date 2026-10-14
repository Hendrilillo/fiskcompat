package com.hendrilillo.fiskcompat.data;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Valores de las variables de datos de Fisk ("fiskheroes:flying", "sind:dyn/icing"...) de cada entidad.
 * Cada lado (cliente / servidor) tiene sus propias entidades, así que no se mezclan.
 * El servidor apunta qué ha cambiado para enviarlo a los clientes.
 */
public final class DataStore {
    static final class Entry {
        final Map<String, Object> values = new HashMap<>();
        Map<String, Object> prev = new HashMap<>();
        final Set<String> dirty = new HashSet<>();
        /** Variables que el cliente dueño envió hace poco (tick de juego en que llegaron). */
        final Map<String, Long> ownerSynced = new HashMap<>();
    }

    private static final com.hendrilillo.fiskcompat.util.EntityMap<Entity, Entry> ENTRIES = new com.hendrilillo.fiskcompat.util.EntityMap<>();

    private DataStore() {}

    private static Entry entry(Entity e) {
        return ENTRIES.computeIfAbsent(e, k -> new Entry());
    }

    public static synchronized Object get(Entity entity, String name) {
        Entry en = ENTRIES.get(entity);
        Object v = en == null ? null : en.values.get(name);
        return v != null ? v : DataVars.defaultValue(name);
    }

    public static synchronized Object getInterpolated(Entity entity, String name, float partialTicks) {
        Object now = get(entity, name);
        if (!(now instanceof Number n) || now instanceof Byte || now instanceof Short || now instanceof Integer) return now;
        Entry en = ENTRIES.get(entity);
        Object prev = en == null ? null : en.prev.get(name);
        double p = prev instanceof Number pn ? pn.doubleValue() : n.doubleValue();
        return p + (n.doubleValue() - p) * partialTicks;
    }

    /** Cambia un valor (convertido al tipo declarado). Devuelve true si cambió. */
    public static synchronized boolean set(Entity entity, String name, Object value) {
        Object v = DataVars.coerce(name, value);
        if (v == null) return false;
        Entry en = entry(entity);
        Object old = en.values.get(name);
        if (old == null) old = DataVars.defaultValue(name);
        if (v.equals(old)) return false;
        en.values.put(name, v);
        en.dirty.add(name);
        return true;
    }

    /** Fija el valor anterior igual al nuevo (sin interpolación entre ambos). */
    public static synchronized void setNoInterp(Entity entity, String name, Object value) {
        set(entity, name, value);
        Entry en = entry(entity);
        Object v = en.values.get(name);
        if (v != null) en.prev.put(name, v);
    }

    /** Guarda los valores actuales como anteriores (para interpolar); al principio de cada tick. */
    public static synchronized void snapshot(Entity entity) {
        Entry en = ENTRIES.get(entity);
        if (en != null) en.prev = new HashMap<>(en.values);
    }

    /** snapshot() de todas las entidades de un nivel (cliente, al principio del tick). */
    public static synchronized void snapshotAll(net.minecraft.world.level.Level level) {
        ENTRIES.snapshot(level.isClientSide).forEach((e, en) -> { if (e.level() == level) en.prev = new HashMap<>(en.values); });
    }

    public static synchronized Map<String, Object> drainDirty(Entity entity) {
        Entry en = ENTRIES.get(entity);
        if (en == null || en.dirty.isEmpty()) return Map.of();
        Map<String, Object> out = new HashMap<>();
        for (String k : en.dirty) out.put(k, en.values.get(k));
        en.dirty.clear();
        return out;
    }

    public static synchronized Map<String, Object> all(Entity entity) {
        Entry en = ENTRIES.get(entity);
        return en == null ? Map.of() : new HashMap<>(en.values);
    }

    public static synchronized void markOwnerSynced(Entity entity, String name, long time) {
        entry(entity).ownerSynced.put(name, time);
    }

    public static synchronized boolean ownerSyncedRecently(Entity entity, String name, long time) {
        Entry en = ENTRIES.get(entity);
        Long t = en == null ? null : en.ownerSynced.get(name);
        return t != null && time - t < 20;
    }

    /** Restablece las variables marcadas con resetWithoutSuit (al quitarse el traje). */
    public static synchronized void resetWithoutSuit(Entity entity) {
        Entry en = ENTRIES.get(entity);
        if (en == null) return;
        for (String k : new HashSet<>(en.values.keySet())) {
            DataVars.Var v = DataVars.get(k);
            if (v != null && v.resetWithoutSuit()) set(entity, k, v.defaultValue());
        }
    }
}
