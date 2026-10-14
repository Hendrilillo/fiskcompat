package com.hendrilillo.fiskcompat.util;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Mapa débil por entidad que separa cliente y servidor.
 * <p>
 * Minecraft compara las entidades por su número de id, y en un mundo de un jugador el jugador del
 * cliente y el del servidor integrado tienen el mismo id: con un WeakHashMap normal compartirían
 * datos (y los temporizadores avanzarían el doble).
 */
public final class EntityMap<K extends Entity, V> {
    private final Map<K, V> client = new WeakHashMap<>(), server = new WeakHashMap<>();

    private Map<K, V> side(Entity e) {
        return e.level().isClientSide ? client : server;
    }

    public synchronized V get(K e) {
        return side(e).get(e);
    }

    public synchronized V getOrDefault(K e, V def) {
        return side(e).getOrDefault(e, def);
    }

    public synchronized V put(K e, V v) {
        return side(e).put(e, v);
    }

    public synchronized V remove(K e) {
        return side(e).remove(e);
    }

    public synchronized boolean containsKey(K e) {
        return side(e).containsKey(e);
    }

    public synchronized V computeIfAbsent(K e, Function<? super K, ? extends V> f) {
        return side(e).computeIfAbsent(e, f);
    }

    /** Todas las entradas de un lado (copia). */
    public synchronized Map<K, V> snapshot(boolean clientSide) {
        return new java.util.HashMap<>(clientSide ? client : server);
    }

    public synchronized void clear(boolean clientSide) {
        (clientSide ? client : server).clear();
    }
}
