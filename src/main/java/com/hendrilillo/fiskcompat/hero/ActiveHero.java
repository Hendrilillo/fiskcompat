package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.power.JSModifier;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.power.PowerRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** El héroe que lleva puesto una entidad (traje completo) y sus modificadores. */
public final class ActiveHero {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Set<String> REPORTED = new HashSet<>();

    public final HeroDefinition def;
    public final String suitId;
    public final List<ModifierEntry> modifiers;

    private ActiveHero(HeroDefinition def, String suitId) {
        this.def = def;
        this.suitId = suitId;
        List<ModifierEntry> mods = new ArrayList<>();
        for (String p : def.powers()) {
            PowerRegistry.Power power = PowerRegistry.get(p);
            if (power != null) mods.addAll(power.modifiers());
        }
        this.modifiers = mods;
    }

    private static final java.util.Map<String, ActiveHero> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    /** Devuelve el héroe activo si la entidad lleva todas las piezas del mismo traje. */
    public static ActiveHero of(LivingEntity e) {
        String suitId = SuitItem.heroId(e.getItemBySlot(EquipmentSlot.CHEST));
        if (suitId == null) {
            // Héroes sin pechera (raro): mirar el casco
            suitId = SuitItem.heroId(e.getItemBySlot(EquipmentSlot.HEAD));
            if (suitId == null) return null;
        }
        String base = SuitItem.baseHeroId(suitId);
        HeroDefinition def = HeroRegistry.get(base);
        if (def == null) return null;
        for (int i = 0; i < 4; i++) {
            if (def.piece(i) == null) continue;
            String worn = SuitItem.heroId(e.getItemBySlot(SLOTS[i]));
            if (worn == null || !base.equals(SuitItem.baseHeroId(worn))) return null;
        }
        return CACHE.computeIfAbsent(suitId, id -> new ActiveHero(def, id));
    }

    // ------------------------------------------------------------------ llamadas a funciones del script

    private static void report(String what, Throwable t) {
        if (REPORTED.add(what)) FiskCompat.LOGGER.error("[FiskCompat] Error en {}: {}", what, t.getMessage());
    }

    private static boolean truthy(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0;
        return o != null;
    }

    public boolean isModifierEnabled(JSEntity e, ModifierEntry m) {
        if (def.modifierEnabledFunc == null) return true;
        try {
            return truthy(def.modifierEnabledFunc.apply(e, new JSModifier(m)));
        } catch (Throwable t) {
            report(def.id() + " isModifierEnabled", t);
            return true;
        }
    }

    public boolean isKeyBindEnabled(JSEntity e, String key) {
        if (def.keyBindEnabledFunc == null) return true;
        try {
            return truthy(def.keyBindEnabledFunc.apply(e, key));
        } catch (Throwable t) {
            report(def.id() + " isKeyBindEnabled", t);
            return true;
        }
    }

    public boolean hasProperty(JSEntity e, String property) {
        if (def.hasPropertyFunc == null) return false;
        try {
            return truthy(def.hasPropertyFunc.apply(e, property));
        } catch (Throwable t) {
            report(def.id() + " hasProperty", t);
            return false;
        }
    }

    public void tick(JSEntity e, Object manager) {
        if (def.tickHandler == null) return;
        try {
            def.tickHandler.apply(e, manager);
        } catch (Throwable t) {
            report(def.id() + " tickHandler", t);
        }
    }

    /** ¿Está pulsada la tecla del héroe con este nombre ("AIM", "SHIELD"...)? */
    public boolean isKeyPressed(JSEntity e, LivingEntity mc, String key, int held) {
        if (key == null) return false;
        HeroDefinition.KeyBind kb = def.keyBinds().get(key);
        if (kb == null) return false;
        int bit = KeyState.slotBit(kb.slot());
        return bit != 0 && (held & bit) != 0 && isKeyBindEnabled(e, key);
    }

    /** ¿Hay alguna tecla activa en la ranura -1 (clic izquierdo)? Entonces el clic no golpea. */
    public boolean usesAttackKey(JSEntity e) {
        return !keysInSlot(e, -1).isEmpty();
    }

    /** Primer modificador de ese tipo que esté activo ahora, o null. */
    public ModifierEntry enabledModifier(JSEntity e, String type) {
        for (ModifierEntry m : modifiers) {
            if (m.type.equals(type) && isModifierEnabled(e, m)) return m;
        }
        return null;
    }

    public boolean hasModifier(String type) {
        for (ModifierEntry m : modifiers) if (m.type.equals(type)) return true;
        return false;
    }

    /** Valor de una función de hero.supplyFunction(nombre, ...) como booleano. */
    public boolean funcBoolean(JSEntity e, String name, boolean def) {
        Object f = this.def.functions.get(name);
        if (f == null) return def;
        try {
            if (f instanceof org.openjdk.nashorn.api.scripting.JSObject o && o.isFunction()) return truthy(o.call(null, e));
            return truthy(f);
        } catch (Throwable t) {
            report(this.def.id() + " " + name, t);
            return def;
        }
    }

    /** Nombres de teclas activas en una ranura (1-5). */
    public List<String> keysInSlot(JSEntity e, int slot) {
        List<String> out = new ArrayList<>();
        for (HeroDefinition.KeyBind kb : def.keyBinds().values()) {
            if (kb.slot() == slot && isKeyBindEnabled(e, kb.key())) out.add(kb.key());
        }
        return out;
    }
}
