package com.hendrilillo.fiskcompat.power;

import com.google.gson.JsonElement;

/** Un modificador visto desde los scripts (setModifierEnabled). */
public final class JSModifier {
    private final ModifierEntry entry;

    public JSModifier(ModifierEntry entry) { this.entry = entry; }

    public String name() { return entry.type; }
    public String id() { return entry.id; }
    public String power() { return entry.power; }

    public Object getProperty(String name) {
        JsonElement e = entry.json.get(name);
        if (e == null || !e.isJsonPrimitive()) return null;
        var p = e.getAsJsonPrimitive();
        return p.isBoolean() ? p.getAsBoolean() : p.isNumber() ? p.getAsDouble() : p.getAsString();
    }

    @Override public String toString() { return entry.toString(); }
}
