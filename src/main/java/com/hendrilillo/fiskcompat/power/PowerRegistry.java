package com.hendrilillo.fiskcompat.power;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Poderes de los heropacks: assets/&lt;ns&gt;/data/powers/&lt;id&gt;.json */
public final class PowerRegistry {
    public record Power(String id, String name, List<ModifierEntry> modifiers, JsonObject json) {}

    private static final Map<String, Optional<Power>> CACHE = new HashMap<>();

    private PowerRegistry() {}

    public static synchronized void clear() {
        CACHE.clear();
    }

    public static synchronized Power get(String id) {
        return CACHE.computeIfAbsent(id, PowerRegistry::load).orElse(null);
    }

    private static Optional<Power> load(String id) {
        int c = id.indexOf(':');
        String ns = c < 0 ? "fiskheroes" : id.substring(0, c), path = c < 0 ? id : id.substring(c + 1);
        Optional<String> src = HeroPackManager.readAsset(ns, "data/powers/" + path + ".json");
        if (src.isEmpty()) return Optional.empty();
        try {
            JsonReader reader = new JsonReader(new StringReader(src.get()));
            reader.setLenient(true);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            List<ModifierEntry> mods = new ArrayList<>();
            if (json.has("modifiers") && json.get("modifiers").isJsonObject()) {
                JsonObject m = json.getAsJsonObject("modifiers");
                for (String key : m.keySet()) {
                    if (m.get(key).isJsonObject()) mods.add(new ModifierEntry(id, key, m.getAsJsonObject(key)));
                }
            }
            String name = json.has("name") ? json.get("name").getAsString() : id;
            return Optional.of(new Power(id, name, Collections.unmodifiableList(mods), json));
        } catch (Exception e) {
            FiskCompat.LOGGER.error("Error leyendo el poder {}: {}", id, e.toString());
            return Optional.empty();
        }
    }
}
