package com.hendrilillo.fiskcompat.hero;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Teclas que tiene pulsadas cada jugador (bits). En el servidor llegan por red;
 * en el cliente las rellena ClientKeys para el jugador local.
 */
public final class KeyState {
    /** Bits 0-4: habilidades 1-5. */
    public static final int USE = 1 << 5, ATTACK = 1 << 6, JUMP = 1 << 7, MASK = 1 << 8;

    private static final Map<Entity, Integer> HELD = new WeakHashMap<>();

    private KeyState() {}

    public static synchronized void set(Entity e, int mask) {
        HELD.put(e, mask);
    }

    public static synchronized int get(Entity e) {
        Integer v = HELD.get(e);
        return v == null ? 0 : v;
    }

    public static int abilityBit(int slot) {
        return slot >= 1 && slot <= 5 ? 1 << (slot - 1) : 0;
    }
}
