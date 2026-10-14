package com.hendrilillo.fiskcompat.hero;

import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Definición de un héroe tal y como la construye el script data/heroes/&lt;id&gt;.js.
 * Los métodos públicos imitan la API "hero" de Fisk (ver docs/api/JSHero.txt).
 */
public final class HeroDefinition {
    public static final String[] SLOT_NAMES = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
    private static final String[] DEFAULT_PIECES = {
            "item.superhero_armor.piece.helmet", "item.superhero_armor.piece.chestplate",
            "item.superhero_armor.piece.leggings", "item.superhero_armor.piece.boots"};

    public record KeyBind(String key, String name, int slot, BiFunction<Object, Object, Object> func) {}

    public record Attribute(String name, double value, int operation) {}

    private final String id;
    private ScriptObjectMirror script;

    String name;
    String version;
    String[] aliases = new String[0];
    int tier = 1;
    boolean cosmic;
    boolean hidden;
    final String[] armor = new String[4];
    final List<String> powers = new ArrayList<>();
    final List<Object[]> equipment = new ArrayList<>();
    final List<Attribute> attributes = new ArrayList<>();
    final Map<String, AttributeProfile> attributeProfiles = new LinkedHashMap<>();
    final Map<String, Object> damageProfiles = new LinkedHashMap<>();
    final Map<String, KeyBind> keyBinds = new LinkedHashMap<>();
    final Map<String, Object> functions = new LinkedHashMap<>();
    final Map<String, List<String>> soundEvents = new LinkedHashMap<>();
    final Map<String, Object> soundOverrides = new LinkedHashMap<>();

    Function<Object, Object> attributeProfileFunc;
    Function<Object, Object> damageProfileFunc;
    Function<Object, Object> defaultScaleFunc;
    Function<Object, Object> tierOverrideFunc;
    BiFunction<Object, Object, Object> hasPropertyFunc;
    BiFunction<Object, Object, Object> hasPermissionFunc;
    BiFunction<Object, Object, Object> modifierEnabledFunc;
    BiFunction<Object, Object, Object> keyBindEnabledFunc;
    BiFunction<Object, Object, Object> ruleValueModifierFunc;
    BiFunction<Object, Object, Object> onToggleMaskFunc;
    BiFunction<Object, Object, Object> tickHandler;

    public HeroDefinition(String id) {
        this.id = id;
    }

    // ----------------------------------------------------------------- API de los scripts

    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setAliases(String... aliases) { this.aliases = aliases; }
    public void setTier(int tier) { this.tier = tier; }
    public void setCosmic() { this.cosmic = true; }
    public void hide() { this.hidden = true; }
    public void setHelmet(String name) { armor[0] = name; }
    public void setChestplate(String name) { armor[1] = name; }
    public void setLeggings(String name) { armor[2] = name; }
    public void setBoots(String name) { armor[3] = name; }

    public void addEquipment(String item) { equipment.add(new Object[]{item}); }
    public void addEquipment(String item, int amount) { equipment.add(new Object[]{item, amount}); }
    public void addEquipment(String item, String nbt) { equipment.add(new Object[]{item, nbt}); }
    public void addEquipment(String item, int amount, String nbt) { equipment.add(new Object[]{item, amount, nbt}); }
    public void addPrimaryEquipment(String item, boolean b) { equipment.add(new Object[]{item, b}); }
    public void addPrimaryEquipment(String item, boolean b, Function<Object, Object> pred) { equipment.add(new Object[]{item, b, pred}); }
    public void setPrimaryEquipment(String item) { equipment.add(new Object[]{item}); }

    public void addPowers(String... ids) { powers.addAll(Arrays.asList(ids)); }

    public void addAttribute(String attr, double value, int operation) {
        attributes.add(new Attribute(attr, value, operation));
    }

    public void addKeyBind(String key, String name, int slot) {
        keyBinds.put(key, new KeyBind(key, name, slot, null));
    }

    public void addKeyBindFunc(String key, BiFunction<Object, Object, Object> func, String name, int slot) {
        keyBinds.put(key, new KeyBind(key, name, slot, func));
    }

    public void addAttributeProfile(String key, Function<Object, Object> builder) {
        AttributeProfile profile = new AttributeProfile();
        builder.apply(profile);
        attributeProfiles.put(key, profile);
    }

    public void setAttributeProfile(Function<Object, Object> func) { attributeProfileFunc = func; }
    public void addDamageProfile(String key, Object profile) { damageProfiles.put(key, profile); }
    public void setDamageProfile(Function<Object, Object> func) { damageProfileFunc = func; }
    public void setHasProperty(BiFunction<Object, Object, Object> f) { hasPropertyFunc = f; }
    public void setHasPermission(BiFunction<Object, Object, Object> f) { hasPermissionFunc = f; }
    public void setModifierEnabled(BiFunction<Object, Object, Object> f) { modifierEnabledFunc = f; }
    public void setKeyBindEnabled(BiFunction<Object, Object, Object> f) { keyBindEnabledFunc = f; }
    public void setOnToggleMask(BiFunction<Object, Object, Object> f) { onToggleMaskFunc = f; }
    public void setRuleValueModifier(BiFunction<Object, Object, Object> f) { ruleValueModifierFunc = f; }
    public void setTierOverride(Function<Object, Object> f) { tierOverrideFunc = f; }
    public void setTickHandler(BiFunction<Object, Object, Object> f) { tickHandler = f; }

    public void setDefaultScale(Object scale) {
        if (scale instanceof ScriptObjectMirror m && m.isFunction()) {
            defaultScaleFunc = e -> m.call(null, e);
        } else {
            double d = scale instanceof Number n ? n.doubleValue() : 1.0;
            defaultScaleFunc = e -> d;
        }
    }

    public void supplyFunction(String name, Object value) { functions.put(name, value); }

    public void addSoundEvent(String trigger, Object sounds) {
        List<String> list = soundEvents.computeIfAbsent(trigger, k -> new ArrayList<>());
        if (sounds instanceof ScriptObjectMirror m && m.isArray()) {
            for (Object o : m.values()) list.add(String.valueOf(o));
        } else if (sounds instanceof String[] arr) {
            list.addAll(Arrays.asList(arr));
        } else {
            list.add(String.valueOf(sounds));
        }
    }

    public void addSoundOverrides(String key, Object value) { soundOverrides.put(key, value); }

    // ----------------------------------------------------------------- Lectura desde Java

    void setScript(ScriptObjectMirror script) { this.script = script; }
    public ScriptObjectMirror script() { return script; }
    public String id() { return id; }
    public String name() { return name != null ? name : id; }
    public String plainName() { return name().replaceAll("§.", ""); }
    public int tier() { return tier; }
    public boolean hidden() { return hidden; }
    public List<String> powers() { return powers; }
    public List<Attribute> attributes() { return attributes; }
    public Map<String, KeyBind> keyBinds() { return keyBinds; }
    public BiFunction<Object, Object, Object> tickHandler() { return tickHandler; }

    /** Piezas de armadura que tiene el héroe (null = no tiene esa pieza). */
    public String piece(int slot) {
        boolean any = false;
        for (String s : armor) any |= s != null;
        if (!any) return DEFAULT_PIECES[slot];
        return armor[slot];
    }

    /** Nombre legible de una pieza ("item.superhero_armor.piece.helmet" -> "Helmet"). */
    public String pieceDisplayName(int slot) {
        String p = piece(slot);
        if (p == null) return "";
        String translated = com.hendrilillo.fiskcompat.pack.PackLang.get(p + ".name");
        if (!translated.equals(p + ".name")) return translated;
        if (p.startsWith("item.superhero_armor.piece.")) {
            String raw = p.substring("item.superhero_armor.piece.".length()).replace('_', ' ');
            StringBuilder sb = new StringBuilder();
            for (String w : raw.split(" ")) {
                if (w.isEmpty()) continue;
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
            return sb.toString().trim();
        }
        return p;
    }

    /** Perfil de atributos alternativo creado con addAttributeProfile. */
    public static final class AttributeProfile {
        public final List<Attribute> attributes = new ArrayList<>();
        public double weight = -1;

        public void addAttribute(String attr, double value, int operation) {
            attributes.add(new Attribute(attr, value, operation));
        }

        public boolean inheritDefaults;
        public boolean revokeAugments;

        public void inheritDefaults() { inheritDefaults = true; }

        public void revokeAugments() { revokeAugments = true; }

        public void setWeight(double w) { weight = w; }

    }
}
