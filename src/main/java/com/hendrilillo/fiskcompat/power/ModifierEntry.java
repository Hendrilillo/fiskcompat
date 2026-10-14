package com.hendrilillo.fiskcompat.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Un modificador dentro de un poder: "fiskheroes:controlled_flight|flight_normal": { ... } */
public final class ModifierEntry {
    public final String power;
    public final String type;
    public final String id;
    public final JsonObject json;

    public ModifierEntry(String power, String key, JsonObject json) {
        this.power = power;
        int bar = key.indexOf('|');
        this.type = bar < 0 ? key : key.substring(0, bar);
        this.id = bar < 0 ? key.substring(key.indexOf(':') + 1) : key.substring(bar + 1);
        this.json = json;
    }

    public String key() {
        return json.has("key") ? json.get("key").getAsString() : null;
    }

    /** Modificadores cuyo isToggle vale true por defecto en Fisk (el resto, false). */
    private static final java.util.Set<String> TOGGLE_BY_DEFAULT = java.util.Set.of(
            "fiskheroes:blade", "fiskheroes:charged_punch", "fiskheroes:hover", "fiskheroes:intangibility",
            "fiskheroes:invisibility", "fiskheroes:shadowform", "fiskheroes:shield", "fiskheroes:slow_motion",
            "fiskheroes:transformation", "fiskheroes:web_swinging");

    public boolean isToggle() {
        JsonElement e = json.get("isToggle");
        if (e != null && e.isJsonPrimitive()) return e.getAsBoolean();
        return TOGGLE_BY_DEFAULT.contains(type);
    }

    public JsonObject obj(String name) {
        JsonElement e = json.get(name);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    public double num(String name, double def) {
        JsonElement e = json.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsDouble() : def;
    }

    public boolean bool(String name, boolean def) {
        JsonElement e = json.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    public String str(String name, String def) {
        JsonElement e = json.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    /** Variable booleana que activa/desactiva el modificador (transformation / cooldown). */
    public String toggleVar() {
        for (String k : new String[]{"transformation", "cooldown"}) {
            JsonObject o = obj(k);
            if (o != null && o.has("toggleData")) return o.get("toggleData").getAsString();
        }
        return json.has("toggleData") ? json.get("toggleData").getAsString() : null;
    }

    @Override
    public String toString() {
        return type + "|" + id;
    }
}
