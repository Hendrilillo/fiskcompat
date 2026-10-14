package com.hendrilillo.fiskcompat.js;

import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carga scripts de heropack imitando el comportamiento de Fisk:
 * <ul>
 * <li>{@code extend("ns:ruta")}: hereda de otro script. El padre se evalúa primero en el mismo ámbito
 * y sus funciones quedan accesibles como {@code parent.x()}; las que redefine el hijo sustituyen a las
 * del padre (también cuando el padre las llama).</li>
 * <li>{@code implement("ns:ruta")}: crea una instancia independiente de otro script y la devuelve.</li>
 * </ul>
 * Las rutas se resuelven en {@code assets/<ns>/<dir><ruta>.js}.
 */
public final class ScriptLoader {
    private static final Pattern EXTEND = Pattern.compile("(?m)^\\s*extend\\(\\s*\"([^\"]+)\"\\s*\\)\\s*;?");
    private static final Pattern PARENT_REF = Pattern.compile("(?<![\\w$.])parent(?=\\s*\\.)");

    private final String dir;

    /** @param dir directorio dentro de assets/&lt;ns&gt;/, por ejemplo "data/heroes/" o "renderers/heroes/" */
    public ScriptLoader(String dir) {
        this.dir = dir;
    }

    public String dir() {
        return dir;
    }

    public boolean exists(String id) {
        String[] p = split(id);
        return HeroPackManager.findAsset(p[0], dir + p[1] + ".js").isPresent();
    }

    private static String[] split(String id) {
        int i = id.indexOf(':');
        return i < 0 ? new String[]{"fiskheroes", id} : new String[]{id.substring(0, i), id.substring(i + 1)};
    }

    private String source(String id) throws ScriptException {
        String[] p = split(id);
        return HeroPackManager.readAsset(p[0], dir + p[1] + ".js")
                .map(ArrowFunctionConverter::convert)
                .orElseThrow(() -> new ScriptException("No se encuentra el script " + id + " (" + dir + ")"));
    }

    /**
     * Evalúa un script (y toda su cadena de herencia) en un ámbito global nuevo.
     *
     * @param globals rellena las variables y funciones globales del ámbito (loadTextures, PackLoader...).
     *                Recibe también el id del script que se está evaluando.
     */
    public ScriptObjectMirror load(String id, BiConsumer<ScriptObjectMirror, ScriptLoader> globals) throws ScriptException {
        ScriptObjectMirror global = ScriptEngines.newGlobal();
        global.put("$SUIT_NAME", id);
        globals.accept(global, this);
        global.put("implement", new JsFunction("implement", args -> implement(String.valueOf(args[0]), globals)));
        global.put("extend", new JsFunction("extend", args -> null)); // ya resuelto al cargar
        evalWithParents(id, global, new ArrayDeque<>());
        return global;
    }

    /** Instancia independiente de un script externo, como hace implement() en Fisk. */
    public ScriptObjectMirror implement(String id, BiConsumer<ScriptObjectMirror, ScriptLoader> globals) throws ScriptException {
        return load(id, globals);
    }

    private void evalWithParents(String id, ScriptObjectMirror global, Deque<String> chain) throws ScriptException {
        if (chain.contains(id)) throw new ScriptException("Herencia circular: " + chain + " -> " + id);
        chain.push(id);
        int depth = chain.size();
        String src = source(id);

        Matcher m = EXTEND.matcher(src);
        if (m.find()) {
            String parentId = m.group(1);
            src = src.substring(0, m.start()) + src.substring(m.end());
            evalWithParents(parentId, global, chain);
            // Instantánea de las funciones del padre: es lo que el hijo ve como "parent".
            ScriptObjectMirror snapshot = (ScriptObjectMirror) global.eval("({})");
            for (String key : global.keySet()) {
                Object v = global.get(key);
                if (v instanceof ScriptObjectMirror f && f.isFunction()) snapshot.setMember(key, f);
            }
            String parentVar = "$parent" + depth;
            global.put(parentVar, snapshot);
            src = PARENT_REF.matcher(src).replaceAll(Matcher.quoteReplacement(parentVar));
        }
        ScriptEngines.eval(src, id + ".js", global);
        chain.pop();
    }
}
