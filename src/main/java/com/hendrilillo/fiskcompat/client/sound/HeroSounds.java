package com.hendrilillo.fiskcompat.client.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.dev.AutoTest;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.util.EntityMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hace sonar los eventos de sonido de Fisk. Igual que en Fisk, casi todos se disparan en el cliente al
 * cambiar una variable de datos (empezar a volar, abrir la máscara, disparar el unirrayo...); el resto
 * (pasos, aterrizar, golpear, disparos...) llegan del servidor.
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class HeroSounds {
    private HeroSounds() {}

    // ------------------------------------------------------------------ disparar un trigger

    /** Trigger de un modificador ("fiskheroes:controlled_flight", "ENABLE") en todos los activos de ese tipo. */
    public static void fireModifier(Entity entity, String type, String trigger) {
        if (!(entity instanceof LivingEntity le)) return;
        ActiveHero hero = ActiveHero.of(le);
        if (hero == null) return;
        JSEntity js = JSEntity.of(le);
        for (ModifierEntry m : hero.modifiers) {
            if (m.type.equals(type) && hero.isModifierEnabled(js, m)) fireModifier(le, m, trigger);
        }
    }

    /** Trigger de un traje concreto (aunque la entidad aún no lo lleve puesto): salir/entrar en modo centinela. */
    public static void fireForSuit(Entity entity, String suitId, String type, String trigger) {
        if (!(entity instanceof LivingEntity le)) return;
        ActiveHero hero = ActiveHero.forSuit(suitId);
        if (hero == null) return;
        for (ModifierEntry m : hero.modifiers) {
            if (m.type.equals(type)) {
                fireModifier(le, m, trigger);
                return;
            }
        }
    }

    public static void fireModifier(LivingEntity entity, ModifierEntry m, String trigger) {
        JsonObject events = m.obj("soundEvents");
        if (events == null || !events.has(trigger)) return;
        play(entity, ids(events.get(trigger)), trigger.equals("BEAM_AMBIENT"));
    }

    /** Trigger del propio héroe (hero.addSoundEvent): MASK_OPEN, STEP, AIM_START... */
    public static void fireHero(Entity entity, String trigger) {
        if (!(entity instanceof LivingEntity le)) return;
        ActiveHero hero = ActiveHero.of(le);
        if (hero == null) return;
        List<String> ids = hero.def.soundEvents(trigger);
        if (ids != null && !ids.isEmpty()) play(le, ids, false);
    }

    private static List<String> ids(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e == null) return out;
        if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> { if (x.isJsonPrimitive()) out.add(x.getAsString()); });
        else if (e.isJsonPrimitive()) out.add(e.getAsString());
        return out;
    }

    private static void play(LivingEntity entity, List<String> ids, boolean beam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean self = entity == mc.player;
        for (String id : ids) {
            SoundEventDef def = SoundEventDef.get(id);
            if (def == null) continue;
            SoundPacks.Range range = SoundPacks.range(id);
            if (range.distance() <= 0 || !(self ? range.self() : range.others())) continue;
            if (!self && !Float.isInfinite(range.distance()) && entity.distanceTo(mc.player) > range.distance() * 1.5f) continue;
            try {
                if (!def.shouldStart(entity, null)) continue;
                HeroSoundInstance s = new HeroSoundInstance(entity, def, null, range.distance(), beam);
                if (def.delay() > 0) mc.getSoundManager().playDelayed(s, def.delay());
                else mc.getSoundManager().play(s);
                if (AutoTest.ENABLED) FiskCompat.LOGGER.info("[autotest] sonido {} ({}) en {}", id, def.sound(), entity.getName().getString());
            } catch (Throwable t) {
                FiskCompat.LOGGER.warn("No se pudo reproducir {}: {}", id, t.toString());
            }
        }
    }

    // ------------------------------------------------------------------ cambios de variables

    private enum Kind { TOGGLE, FLOAT_TOGGLE, BEAM_BOOL, BEAM_FLOAT, CHARGE, COOLDOWN, SUIT_OPEN, ROLL, DIVE, HERO_MASK, HERO_AIM }

    /** Una variable vigilada y a quién avisa (modificadores, o el héroe si la lista está vacía). */
    private record Rule(String var, Kind kind, List<ModifierEntry> targets) {}

    /** Variables de Fisk que activan/desactivan un modificador (ENABLE / DISABLE). */
    private static final Map<String, String> TOGGLE_VARS = Map.ofEntries(
            Map.entry("fiskheroes:controlled_flight", "fiskheroes:flying"),
            Map.entry("fiskheroes:shield", "fiskheroes:shield"),
            Map.entry("fiskheroes:blade", "fiskheroes:blade"),
            Map.entry("fiskheroes:super_speed", "fiskheroes:speeding"),
            Map.entry("fiskheroes:slow_motion", "fiskheroes:slow_motion"),
            Map.entry("fiskheroes:hover", "fiskheroes:hovering"),
            Map.entry("fiskheroes:charged_punch", "fiskheroes:punchmode"),
            Map.entry("fiskheroes:gliding", "fiskheroes:gliding"),
            Map.entry("fiskheroes:web_swinging", "fiskheroes:web_swinging"),
            Map.entry("fiskheroes:archery", "fiskheroes:horizontal_bow"),
            Map.entry("fiskheroes:gravity_manipulation", "fiskheroes:gravity_manip"),
            Map.entry("fiskheroes:intangibility", "fiskheroes:intangible"),
            Map.entry("fiskheroes:invisibility", "fiskheroes:invisible"),
            Map.entry("fiskheroes:shadowform", "fiskheroes:shadowform"));

    private static final Map<ActiveHero, List<Rule>> RULES = new WeakHashMap<>();

    private static List<Rule> rules(ActiveHero hero) {
        return RULES.computeIfAbsent(hero, h -> {
            Map<String, Rule> map = new java.util.LinkedHashMap<>();
            map.put("fiskheroes:mask_open", new Rule("fiskheroes:mask_open", Kind.HERO_MASK, new ArrayList<>()));
            map.put("fiskheroes:aiming", new Rule("fiskheroes:aiming", Kind.HERO_AIM, new ArrayList<>()));
            for (ModifierEntry m : h.modifiers) {
                if (m.obj("soundEvents") == null) continue;
                String tv = TOGGLE_VARS.get(m.type);
                if (tv != null) add(map, tv, Kind.TOGGLE, m);
                switch (m.type) {
                    case "fiskheroes:controlled_flight" -> {
                        add(map, "fiskheroes:flight_boost_timer", Kind.FLOAT_TOGGLE, m);
                        add(map, "fiskheroes:barrel_roll_timer", Kind.ROLL, m);
                        add(map, "fiskheroes:flight_diving", Kind.DIVE, m);
                    }
                    case "fiskheroes:shield" -> add(map, "fiskheroes:shield_blocking", Kind.TOGGLE, m);
                    case "fiskheroes:charged_beam" -> {
                        add(map, "fiskheroes:beam_charging", Kind.CHARGE, m);
                        add(map, "fiskheroes:beam_shooting", Kind.BEAM_FLOAT, m);
                    }
                    case "fiskheroes:heat_vision" -> add(map, "fiskheroes:heat_vision", Kind.BEAM_BOOL, m);
                    case "fiskheroes:energy_projection" -> add(map, "fiskheroes:energy_projection", Kind.BEAM_BOOL, m);
                    case "fiskheroes:propelled_flight" -> add(map, "fiskheroes:jetpacking_timer", Kind.FLOAT_TOGGLE, m);
                    case "fiskheroes:sentry_mode" -> add(map, "fiskheroes:suit_open_timer", Kind.SUIT_OPEN, m);
                    case "fiskheroes:transformation" -> {
                        JsonObject t = m.obj("transformation");
                        if (t != null && t.has("toggleData")) add(map, t.get("toggleData").getAsString(), Kind.TOGGLE, m);
                    }
                    case "fiskheroes:cooldown" -> {
                        JsonObject c = m.obj("cooldown");
                        if (c != null && c.has("cooldownData")) add(map, c.get("cooldownData").getAsString(), Kind.COOLDOWN, m);
                    }
                    default -> {}
                }
            }
            return new ArrayList<>(map.values());
        });
    }

    /** El escudo usa la misma clave para dos variables: se distinguen con un sufijo en la clave del mapa. */
    private static void add(Map<String, Rule> map, String var, Kind kind, ModifierEntry m) {
        String key = var + "#" + kind;
        map.computeIfAbsent(key, k -> new Rule(var, kind, new ArrayList<>())).targets().add(m);
    }

    private static final class State {
        ActiveHero hero;
        final Map<String, Object> values = new HashMap<>();
        final Map<String, Long> lastFired = new HashMap<>();
    }

    private static final EntityMap<Entity, State> STATES = new EntityMap<>();

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || !e.isAlive()) continue;
            ActiveHero hero = ActiveHero.of(le);
            State st = STATES.get(e);
            if (hero == null) {
                if (st != null) {
                    st.hero = null;
                    // seguir los valores para no perder lo que pase al volver a ponerse el traje
                    for (var en : st.values.entrySet()) en.setValue(DataStore.get(e, en.getKey().substring(0, en.getKey().indexOf('#'))));
                }
                else if (e instanceof net.minecraft.world.entity.player.Player) STATES.put(e, new State());
                continue;
            }
            boolean fresh = st == null;
            if (fresh) {
                st = new State();
                STATES.put(e, st);
            }
            boolean equipped = !fresh && st.hero == null;
            if (st.hero != hero) {
                if (st.hero != null) st.values.clear(); // otro traje distinto: empezar de cero
                st.hero = hero;
            }
            try {
                if (e != mc.player) com.hendrilillo.fiskcompat.hero.Movement.tick(e);
                if (equipped) fireHero(le, "EQUIP");
                check(le, hero, st, fresh, now);
            } catch (Throwable t) {
                if (AutoTest.ENABLED) FiskCompat.LOGGER.error("[autotest] sonidos", t);
            }
        }
    }

    private static void check(LivingEntity e, ActiveHero hero, State st, boolean fresh, long now) {
        for (Rule r : rules(hero)) {
            Object cur = DataStore.get(e, r.var());
            String key = r.var() + "#" + r.kind();
            Object prev = st.values.put(key, cur);
            if (fresh || prev == null || prev.equals(cur)) continue;
            suitOpen = DataStore.get(e, "fiskheroes:suit_open") instanceof Boolean b && b;
            List<String> triggers = triggers(r.kind(), prev, cur);
            for (String trigger : triggers) {
                // evita repeticiones si el servidor corrige el valor justo después
                Long last = st.lastFired.get(key + trigger);
                if (last != null && now - last < 3) continue;
                st.lastFired.put(key + trigger, now);
                if (r.targets().isEmpty()) {
                    fireHero(e, trigger);
                } else {
                    JSEntity js = JSEntity.of(e);
                    for (ModifierEntry m : r.targets()) {
                        if (hero.isModifierEnabled(js, m)) fireModifier(e, m, trigger);
                    }
                }
            }
        }
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        return 0;
    }

    private static boolean bool(Object o) {
        return num(o) != 0;
    }

    private static boolean suitOpen;

    private static List<String> triggers(Kind kind, Object prevO, Object curO) {
        double prev = num(prevO), cur = num(curO);
        List<String> out = new ArrayList<>(2);
        switch (kind) {
            case TOGGLE -> out.add(bool(curO) ? "ENABLE" : "DISABLE");
            case HERO_MASK -> out.add(bool(curO) ? "MASK_OPEN" : "MASK_CLOSE");
            case HERO_AIM -> out.add(bool(curO) ? "AIM_START" : "AIM_STOP");
            case CHARGE -> out.add(bool(curO) ? "CHARGE" : "SHOOT_STOP");
            case BEAM_BOOL -> {
                if (bool(curO)) {
                    out.add("SHOOT");
                    out.add("BEAM_AMBIENT");
                } else {
                    out.add("SHOOT_STOP");
                }
            }
            case BEAM_FLOAT -> {
                if (prev <= 0 && cur > 0) {
                    out.add("SHOOT");
                    out.add("BEAM_AMBIENT");
                }
            }
            case FLOAT_TOGGLE -> {
                if (prev == 0 && cur > 0) out.add("BOOST");
                else if (prev == 1 && cur < 1) out.add("BOOST_STOP");
            }
            case COOLDOWN -> {
                if (prev <= 0 && cur > 0) out.add("START");
                if (prev < 1 && cur >= 1) out.add("TIMEOUT");
                else if (prev > 0 && cur <= 0) out.add("RECOVER");
            }
            case SUIT_OPEN -> {
                // Se mira una vez por tick: con lag puede saltar de 0 a 5 de golpe, o llegar del servidor
                // ya cerrándose (al volver a entrar, 7 -> 0); fiskheroes:suit_open dice qué es
                if (prev == 0 && cur > 0 && cur <= 5) out.add(suitOpen ? "OPEN" : "CLOSE");
                else if (prev >= 5 && cur < 5) out.add("CLOSE");
            }
            case ROLL -> {
                if (prev == 0 && cur != 0) out.add("ROLL");
            }
            case DIVE -> {
                if (bool(curO)) out.add("DIVE");
            }
        }
        return out;
    }

    /** Recarga de recursos: olvidar eventos cargados. */
    public static void clearCaches() {
        SoundEventDef.clearCache();
        synchronized (RULES) {
            RULES.clear();
        }
    }
}
