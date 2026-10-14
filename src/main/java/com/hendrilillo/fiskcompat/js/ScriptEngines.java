package com.hendrilillo.fiskcompat.js;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/** Motor JavaScript compartido, configurado como el de Fisk (ES5, sin acceso a clases Java). */
public final class ScriptEngines {
    private static ScriptEngine engine;

    private ScriptEngines() {}

    public static synchronized ScriptEngine get() {
        if (engine == null) {
            engine = new NashornScriptEngineFactory().getScriptEngine(
                    new String[]{"--no-java", "--optimistic-types=false"}, ScriptEngines.class.getClassLoader());
        }
        return engine;
    }

    /** Crea un ámbito global nuevo y vacío. */
    public static ScriptObjectMirror newGlobal() {
        return (ScriptObjectMirror) get().createBindings();
    }

    public static Object eval(String source, String fileName, ScriptObjectMirror global) throws ScriptException {
        ScriptEngine e = get();
        javax.script.ScriptContext ctx = new javax.script.SimpleScriptContext();
        ctx.setBindings(global, javax.script.ScriptContext.ENGINE_SCOPE);
        ctx.setAttribute(ScriptEngine.FILENAME, fileName, javax.script.ScriptContext.ENGINE_SCOPE);
        return e.eval(source, ctx);
    }
}
