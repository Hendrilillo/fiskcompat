package com.hendrilillo.fiskcompat.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tipos y valores por defecto de las variables de datos que leen los scripts con entity.getData(...).
 * Incluye las de Fisk (fiskheroes:flying, fiskheroes:metal_heat...) y las "dyn/" que declara cada
 * heropack en su heropack.json.
 */
public final class DataVars {
    public enum Type { BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING, OBJECT }

    public record Var(String name, Type type, Object defaultValue, boolean interpolated, boolean resetWithoutSuit) {}

    private static final Map<String, Var> VARS = new HashMap<>();

    private DataVars() {}

    public static synchronized void load() {
        VARS.clear();
        try (InputStream in = DataVars.class.getResourceAsStream("/data/fiskcompat/fiskheroes_vars.json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : json.keySet()) {
                JsonObject o = json.getAsJsonObject(key);
                Type type = Type.valueOf(o.get("type").getAsString());
                Object def = convert(type, o.get("default"));
                VARS.put("fiskheroes:" + key, new Var("fiskheroes:" + key, type, def, o.get("interp").getAsBoolean(), false));
            }
        } catch (Exception e) {
            FiskCompat.LOGGER.error("No se pudo leer la tabla de variables de Fisk", e);
        }
        for (HeroPack pack : HeroPackManager.packs()) {
            if (!pack.info().has("dataVars")) continue;
            JsonObject vars = pack.info().getAsJsonObject("dataVars");
            for (String key : vars.keySet()) {
                JsonElement e = vars.get(key);
                String typeName;
                boolean reset = false;
                if (e.isJsonObject()) {
                    typeName = e.getAsJsonObject().get("type").getAsString();
                    reset = e.getAsJsonObject().has("resetWithoutSuit") && e.getAsJsonObject().get("resetWithoutSuit").getAsBoolean();
                } else {
                    typeName = e.getAsString();
                }
                boolean interp = typeName.endsWith("_INTERP");
                Type type;
                try {
                    type = Type.valueOf(typeName.replace("_INTERP", ""));
                } catch (IllegalArgumentException ex) {
                    type = Type.FLOAT;
                }
                String name = pack.domain() + ":dyn/" + key;
                VARS.put(name, new Var(name, type, convert(type, null), interp, reset));
            }
        }
    }

    private static Object convert(Type type, JsonElement e) {
        boolean n = e == null || e.isJsonNull();
        return switch (type) {
            case BOOLEAN -> !n && e.getAsBoolean();
            case BYTE -> n ? (byte) 0 : e.getAsByte();
            case SHORT -> n ? (short) 0 : e.getAsShort();
            case INTEGER -> n ? 0 : e.getAsInt();
            case LONG -> n ? 0L : e.getAsLong();
            case FLOAT -> n ? 0f : e.getAsFloat();
            case DOUBLE -> n ? 0d : e.getAsDouble();
            case STRING -> n ? "" : e.getAsString();
            case OBJECT -> null;
        };
    }

    public static Var get(String name) {
        return VARS.get(name);
    }

    /** Valor por defecto: el declarado, o 0 si la variable es desconocida (0 se comporta bien como número y como booleano en JS). */
    public static Object defaultValue(String name) {
        Var v = VARS.get(name);
        if (v == null) return 0;
        return v.defaultValue == null ? 0 : v.defaultValue;
    }
}
