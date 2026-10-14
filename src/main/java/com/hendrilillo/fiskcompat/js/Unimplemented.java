package com.hendrilillo.fiskcompat.js;

import com.hendrilillo.fiskcompat.FiskCompat;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Lleva la cuenta de las partes de la API de Fisk que los packs usan y aún no existen. */
public final class Unimplemented {
    private static final Map<String, Integer> SEEN = new ConcurrentHashMap<>();

    private Unimplemented() {}

    public static void report(String what) {
        if (SEEN.merge(what, 1, Integer::sum) == 1) {
            FiskCompat.LOGGER.debug("[FiskCompat] Sin implementar todavía: {}", what);
        }
    }

    public static Map<String, Integer> snapshot() {
        return new TreeMap<>(SEEN);
    }

    public static void clear() {
        SEEN.clear();
    }
}
