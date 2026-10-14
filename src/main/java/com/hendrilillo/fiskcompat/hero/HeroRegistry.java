package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.js.ScriptLoader;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ejecuta los scripts data/heroes/*.js de todos los heropacks y guarda los héroes resultantes. */
public final class HeroRegistry {
    public static final ScriptLoader DATA = new ScriptLoader("data/heroes/");
    private static final Map<String, HeroDefinition> HEROES = new LinkedHashMap<>();
    private static final Map<String, String> ERRORS = new LinkedHashMap<>();

    private HeroRegistry() {}

    public static synchronized void loadAll() {
        HEROES.clear();
        ERRORS.clear();
        long t = System.currentTimeMillis();
        for (String file : HeroPackManager.listAssets(DATA.dir())) {
            if (!file.endsWith(".js")) continue;
            String id = file.substring(0, file.length() - 3);
            if (id.contains(":external/")) continue;
            load(id);
        }
        FiskCompat.LOGGER.info("FiskCompat: {} héroes cargados, {} con errores ({} ms)",
                HEROES.size(), ERRORS.size(), System.currentTimeMillis() - t);
    }

    private static void load(String id) {
        try {
            ScriptObjectMirror global = DATA.load(id, PackGlobals::bind);
            HeroDefinition hero = new HeroDefinition(id);
            Object init = global.get("init");
            if (!(init instanceof ScriptObjectMirror f) || !f.isFunction()) {
                throw new IllegalStateException("el script no tiene función init(hero)");
            }
            f.call(null, hero);
            hero.setScript(global);
            HEROES.put(id, hero);
        } catch (Throwable e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            ERRORS.put(id, msg);
            FiskCompat.LOGGER.error("Error cargando el héroe {}: {}", id, msg);
        }
    }

    public static HeroDefinition get(String id) {
        return HEROES.get(id);
    }

    public static Collection<HeroDefinition> all() {
        return Collections.unmodifiableCollection(HEROES.values());
    }

    public static Map<String, String> errors() {
        return Collections.unmodifiableMap(ERRORS);
    }

    /** Globales comunes a todos los scripts (equivalente al objeto PackLoader de Fisk). */
    public static final class PackGlobals {
        private PackGlobals() {}

        public static void bind(ScriptObjectMirror global, ScriptLoader loader) {
            global.put("PackLoader", new PackLoaderApi());
            global.put("print", new JsFunction("print", a -> {
                FiskCompat.LOGGER.info("[script] {}", a.length > 0 ? a[0] : "");
                return null;
            }));
        }
    }
}
