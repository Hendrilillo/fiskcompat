package com.hendrilillo.fiskcompat.data;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Valores actuales de las variables de datos de cada entidad.
 * (Fase 1: solo en memoria, sin sincronizar entre cliente y servidor.)
 */
public final class DataStore {
    private static final Map<Entity, Map<String, Object>> VALUES = new WeakHashMap<>();
    private static final Map<Entity, Map<String, Object>> PREV = new WeakHashMap<>();

    private DataStore() {}

    public static synchronized Object get(Entity entity, String name) {
        Map<String, Object> m = VALUES.get(entity);
        Object v = m == null ? null : m.get(name);
        return v != null ? v : DataVars.defaultValue(name);
    }

    public static synchronized Object getInterpolated(Entity entity, String name, float partialTicks) {
        Object now = get(entity, name);
        if (!(now instanceof Number n)) return now;
        Map<String, Object> p = PREV.get(entity);
        Object prev = p == null ? null : p.get(name);
        if (!(prev instanceof Number pn)) return n.doubleValue();
        return pn.doubleValue() + (n.doubleValue() - pn.doubleValue()) * partialTicks;
    }

    public static synchronized void set(Entity entity, String name, Object value) {
        VALUES.computeIfAbsent(entity, e -> new HashMap<>()).put(name, value);
    }

    /** Guarda los valores actuales como "anteriores" (para interpolar); se llama al principio de cada tick. */
    public static synchronized void snapshot(Entity entity) {
        Map<String, Object> m = VALUES.get(entity);
        if (m != null) PREV.put(entity, new HashMap<>(m));
    }
}
