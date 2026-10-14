package com.hendrilillo.fiskcompat.pack;

import java.util.HashMap;
import java.util.Map;

/** Traducciones de los heropacks (archivos .lang de 1.7.10: "clave=valor"). */
public final class PackLang {
    private static Map<String, String> table;
    /** Idioma preferido (p. ej. "es_ES"); lo pone el cliente. */
    public static String preferred = "en_US";

    private PackLang() {}

    public static synchronized void clear() {
        table = null;
    }

    private static void load(Map<String, String> into, String locale) {
        for (HeroPack pack : HeroPackManager.packs()) {
            for (String ns : pack.namespaces()) {
                String rel = "assets/" + ns + "/lang/" + locale + ".lang";
                if (!pack.has(rel)) continue;
                try {
                    for (String line : pack.readString(rel).split("\\r?\\n")) {
                        int eq = line.indexOf('=');
                        if (eq <= 0 || line.startsWith("#")) continue;
                        into.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Traduce una clave; si no existe devuelve la clave tal cual. */
    public static synchronized String get(String key) {
        if (table == null) {
            table = new HashMap<>();
            load(table, "en_US");
            if (!preferred.equals("en_US")) load(table, preferred);
        }
        return table.getOrDefault(key, key);
    }
}
