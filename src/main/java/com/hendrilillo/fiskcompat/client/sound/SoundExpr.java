package com.hendrilillo.fiskcompat.client.sound;

import com.google.gson.JsonElement;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.js.ScriptEngines;
import net.minecraft.world.entity.Entity;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Un valor de los eventos de sonido de Fisk: una constante, el nombre de una variable de datos
 * ("fiskheroes:flight_timer") o una expresión JavaScript con "entity" y "context".
 */
public interface SoundExpr {
    Object eval(Entity entity, Object context);

    Pattern VAR = Pattern.compile("^\\w+?:\\w+$|^\\w+?:dyn/\\w+$");
    Set<String> REPORTED = new HashSet<>();

    static SoundExpr constant(Object v) {
        return (e, c) -> v;
    }

    /** null si el JSON no es un valor válido. */
    static SoundExpr read(JsonElement json, boolean bool) {
        if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) return null;
        var p = json.getAsJsonPrimitive();
        if (p.isBoolean()) return constant(p.getAsBoolean());
        if (p.isNumber()) return bool ? constant(p.getAsDouble() != 0) : constant(p.getAsDouble());
        String s = p.getAsString();
        if (bool && (s.equals("true") || s.equals("false"))) return constant(s.equals("true"));
        return parse(s);
    }

    static SoundExpr parse(String s) {
        if (VAR.matcher(s).matches()) {
            return (e, c) -> e == null ? null : DataStore.get(e, s);
        }
        ScriptObjectMirror fn = compile(s);
        if (fn == null) return (e, c) -> null;
        return (e, c) -> {
            try {
                return fn.call(null, e == null ? null : JSEntity.of(e), c);
            } catch (Throwable t) {
                synchronized (REPORTED) {
                    if (REPORTED.add(s)) FiskCompat.LOGGER.warn("Error en una expresión de sonido '{}': {}", s, t.toString());
                }
                return null;
            }
        };
    }

    private static ScriptObjectMirror compile(String expr) {
        try {
            Object fn = ScriptEngines.eval("(function(entity, context) { return (\n" + expr + "\n); })", "sound_expr", Holder.global());
            if (fn instanceof ScriptObjectMirror m && m.isFunction()) return m;
        } catch (Throwable t) {
            // puede que no sea una expresión: evaluarlo como script
            try {
                String quoted = new com.google.gson.Gson().toJson(expr);
                Object fn = ScriptEngines.eval("(function(entity, context) { return eval(" + quoted + "); })", "sound_expr", Holder.global());
                if (fn instanceof ScriptObjectMirror m && m.isFunction()) return m;
            } catch (Throwable t2) {
                FiskCompat.LOGGER.warn("No se pudo compilar la expresión de sonido '{}': {}", expr, t2.toString());
            }
        }
        return null;
    }

    static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0;
        if (o instanceof String str) return Boolean.parseBoolean(str);
        return def;
    }

    static float asFloat(Object o, float def) {
        if (o instanceof Number n) return n.floatValue();
        return def;
    }

    final class Holder {
        private static ScriptObjectMirror global;

        static synchronized ScriptObjectMirror global() {
            if (global == null) global = ScriptEngines.newGlobal();
            return global;
        }
    }
}
