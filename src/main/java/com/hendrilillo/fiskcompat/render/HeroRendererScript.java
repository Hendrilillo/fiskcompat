package com.hendrilillo.fiskcompat.render;

import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.js.ScriptLoader;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import com.hendrilillo.fiskcompat.hero.HeroRegistry;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * El objeto "renderer" de los scripts renderers/heroes/&lt;id&gt;.js.
 * Los métodos públicos imitan la API de Fisk (docs/api/JSHeroRenderer.txt y JSRendererClass.txt).
 * Por ahora solo se usan de verdad las texturas, luces, iconos y partes visibles;
 * efectos, propiedades y animaciones devuelven objetos comodín.
 */
public final class HeroRendererScript {
    public static final ScriptLoader RENDERERS = new ScriptLoader("renderers/heroes/");

    public enum Slot { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    private final String id;
    private ScriptObjectMirror script;

    final Map<String, String> textures = new LinkedHashMap<>();
    BiFunction<Object, Object, Object> textureFunc;
    BiFunction<Object, Object, Object> lightsFunc;
    final String[] itemIcons = new String[4];
    final Map<Slot, List<String>> shownParts = new EnumMap<>(Slot.class);
    final List<Slot> fixHatLayer = new ArrayList<>();
    final List<Object> effects = new ArrayList<>();

    private HeroRendererScript(String id) {
        this.id = id;
    }

    /** Carga y ejecuta el renderer de un héroe (o una variante "ns:heroe/alt"). */
    public static HeroRendererScript load(String heroId) throws Exception {
        String file = heroId.replace('/', '_');
        HeroRendererScript r = new HeroRendererScript(heroId);
        ScriptObjectMirror global = RENDERERS.load(file, (g, loader) -> {
            HeroRegistry.PackGlobals.bind(g, loader);
            g.put("loadTextures", new JsFunction("loadTextures", a -> {
                if (a.length > 0 && a[0] instanceof ScriptObjectMirror m) {
                    for (String k : m.keySet()) r.textures.put(k, String.valueOf(m.get(k)));
                }
                return null;
            }));
        });
        r.script = global;
        r.call("init", r);
        return r;
    }

    public Object call(String function, Object... args) {
        Object f = script.get(function);
        if (f instanceof ScriptObjectMirror m && m.isFunction()) return m.call(null, args);
        return null;
    }

    public boolean hasFunction(String function) {
        return script.get(function) instanceof ScriptObjectMirror m && m.isFunction();
    }

    // ----------------------------------------------------------------- API de los scripts

    public void setTexture(BiFunction<Object, Object, Object> f) { textureFunc = f; }
    public void setLights(BiFunction<Object, Object, Object> f) { lightsFunc = f; }

    public void setItemIcons(String... icons) {
        for (int i = 0; i < 4 && i < icons.length; i++) itemIcons[i] = icons[i];
    }

    public void setItemIcon(String slot, String icon) {
        itemIcons[Slot.valueOf(slot).ordinal()] = icon;
    }

    public void showModel(String slot, String... parts) {
        shownParts.put(Slot.valueOf(slot), List.of(parts));
    }

    public void fixHatLayer(String... slots) {
        for (String s : slots) fixHatLayer.add(Slot.valueOf(s));
    }

    /** Crea los objetos de efecto; el cliente la sustituye por la implementación real. */
    public interface EffectFactory {
        Object create(String type, HeroRendererScript renderer);
    }

    public static EffectFactory effectFactory = (type, renderer) -> {
        Unimplemented.report("renderer.createEffect(" + type + ")");
        return new Dummy("effect[" + type + "]");
    };

    public Object createEffect(String type) {
        Object effect = effectFactory.create(type, this);
        effects.add(effect);
        return effect;
    }

    /** Crea las propiedades enlazadas (rayos, partículas...); el cliente pone la implementación real. */
    public static EffectFactory propertyFactory = null;
    private final List<Object> properties = new ArrayList<>();

    public Object bindProperty(String type) {
        if (propertyFactory != null) {
            Object p = propertyFactory.create(type, this);
            if (p != null) {
                properties.add(p);
                return p;
            }
        }
        Unimplemented.report("renderer.bindProperty(" + type + ")");
        return new Dummy("property[" + type + "]");
    }

    public List<Object> properties() {
        return properties;
    }

    public static EffectFactory resourceFactory = null;

    public Object createResource(String type, String path) {
        if (resourceFactory != null) {
            Object r = resourceFactory.create(type + "|" + path, this);
            if (r != null) return r;
        }
        Unimplemented.report("renderer.createResource(" + type + ")");
        return new Dummy("resource[" + type + ":" + path + "]");
    }

    public void unbindResource(Object resource) {}

    final Map<String, Object> customAnimations = new LinkedHashMap<>();

    public void addAnimationEvent(String key, Object holder) { Unimplemented.report("renderer.addAnimationEvent"); }
    public void addCustomAnimation(String key, Object holder) { customAnimations.put(key, holder); }
    public void removeCustomAnimation(String key) { customAnimations.remove(key); }

    public Map<String, Object> customAnimations() { return customAnimations; }
    public void reprioritizeDefaultAnimation(String key, double priority) {}

    // ----------------------------------------------------------------- Lectura desde Java

    public String id() { return id; }

    /** Clave de textura a usar para una capa ("layer1", "layer2"...), llamando a setTexture del script. */
    public String textureKey(Object entity, Slot slot) {
        if (textureFunc != null) {
            Object v = textureFunc.apply(entity, slot.name());
            if (v != null) return String.valueOf(v);
        }
        return slot == Slot.LEGGINGS ? "layer2" : "layer1";
    }

    public String lightsKey(Object entity, Slot slot) {
        if (lightsFunc == null) return null;
        Object v = lightsFunc.apply(entity, slot.name());
        return v == null ? null : String.valueOf(v);
    }

    /** Ruta "ns:ruta" de una textura cargada con loadTextures, o null. */
    public String texturePath(String key) {
        return key == null ? null : textures.get(key);
    }

    public String itemIcon(int slot) {
        String icon = itemIcons[slot];
        if (icon == null) return null;
        String file = id.replace('/', '_');
        int c = file.indexOf(':');
        return file.substring(0, c + 1) + icon.replace("%s", file.substring(c + 1));
    }

    public List<String> shownParts(Slot slot) {
        return shownParts.get(slot);
    }

    public boolean fixesHatLayer(Slot slot) {
        return fixHatLayer.contains(slot);
    }
}
