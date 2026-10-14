package com.hendrilillo.fiskcompat.data;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Cambios que el jugador local hace por su cuenta (volar, abrir la máscara, mantener una tecla...).
 * Se aplican al momento en el cliente y se envían al servidor al final del tick.
 */
public final class Sync {
    private static final Map<String, Object> PENDING = new HashMap<>();

    private Sync() {}

    /** Cambia una variable del jugador local y la marca para enviarla al servidor. */
    public static synchronized void setLocal(Entity player, String name, Object value) {
        if (DataStore.set(player, name, value)) {
            PENDING.put(name, DataStore.get(player, name));
        }
        DataStore.markOwnerSynced(player, name, player.level().getGameTime());
    }

    public static synchronized Map<String, Object> drain() {
        if (PENDING.isEmpty()) return Map.of();
        Map<String, Object> out = new HashMap<>(PENDING);
        PENDING.clear();
        return out;
    }
}
