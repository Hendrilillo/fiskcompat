package com.hendrilillo.fiskcompat.hero;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.DataManagerApi;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.data.Sync;
import com.hendrilillo.fiskcompat.mixin.ServerGamePacketListenerAccessor;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.power.modifier.ControlledFlight;
import com.hendrilillo.fiskcompat.power.modifier.ModifierContext;
import com.hendrilillo.fiskcompat.power.modifier.Modifiers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Cada tick, para cada jugador con un traje completo: ejecuta los modificadores de sus poderes
 * y el tickHandler del script del héroe. En el servidor además aplica atributos y sincroniza datos.
 */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class HeroTicker {
    private static final com.hendrilillo.fiskcompat.util.EntityMap<LivingEntity, ActiveHero> LAST = new com.hendrilillo.fiskcompat.util.EntityMap<>();

    /** Lo pone el cliente: ¿es este jugador el jugador local? */
    public static Predicate<Player> isLocalPlayer = p -> false;
    /** Lo pone el cliente: teclas del jugador local. */
    public static java.util.function.IntSupplier localKeys = () -> 0;

    private HeroTicker() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        boolean client = p.level().isClientSide;
        boolean local = client && isLocalPlayer.test(p);
        if (client && !local) return;
        tick(p, client, local);
        if (!client) Net.broadcastChanges(p);
    }

    /** Tick del traje de una entidad que no es un jugador (el centinela de Iron Man); solo en el servidor. */
    public static void tickEntity(LivingEntity e) {
        if (e.level().isClientSide) return;
        tick(e, false, false);
        Net.broadcastChanges(e);
    }

    private static void tick(LivingEntity p, boolean client, boolean local) {
        ActiveHero hero = ActiveHero.of(p);
        ActiveHero prev = LAST.get(p);
        if (hero == null) {
            if (prev != null) {
                LAST.remove(p);
                DataStore.resetWithoutSuit(p);
                DataStore.set(p, ControlledFlight.FLYING, false);
                if (local) Sync.setLocal(p, ControlledFlight.FLYING, false);
                // Fisk reinicia también el estado del traje abierto y la máscara
                for (String v : new String[]{"fiskheroes:suit_open", "fiskheroes:mask_open"}) {
                    DataStore.set(p, v, false);
                    if (local) Sync.setLocal(p, v, false);
                }
                DataStore.setNoInterp(p, "fiskheroes:suit_open_timer", (byte) 0);
                p.setNoGravity(false);
                if (!client) HeroAttributes.remove(p);
            }
            return;
        }
        LAST.put(p, hero);
        Movement.tick(p);
        JSEntity js = JSEntity.of(p);
        int keys = local ? localKeys.getAsInt() : KeyState.get(p);
        ModifierContext ctx = new ModifierContext(p, js, hero, local, keys);

        ControlledFlight.beginTick(p);
        tickAiming(ctx);
        for (ModifierEntry m : hero.modifiers) {
            boolean enabled = hero.isModifierEnabled(js, m);
            Modifiers.tick(ctx, m, enabled);
        }
        ControlledFlight.endTick(ctx);
        tickMask(ctx);
        hero.tick(js, DataManagerApi.INSTANCE);

        if (!client) {
            HeroAttributes.apply(p, hero, js);
            if (p instanceof ServerPlayer sp && DataStore.get(p, ControlledFlight.FLYING) instanceof Boolean b && b && sp.connection != null) {
                ((ServerGamePacketListenerAccessor) sp.connection).fiskcompat$setAboveGroundTickCount(0);
            }
        }
    }

    // ------------------------------------------------------------------ apuntar

    private static void tickAiming(ModifierContext c) {
        if (c.local) {
            boolean aim = c.hero.funcBoolean(c.js, "canAim", false) && c.keyPressed("AIM");
            Sync.setLocal(c.entity, "fiskheroes:aiming", aim);
        }
        boolean aiming = c.bool("fiskheroes:aiming");
        DataManagerApi.incr(c.entity, "fiskheroes:aiming_timer", 4, 4, aiming, !aiming);
        boolean aimed = c.num("fiskheroes:aiming_timer") >= 1;
        DataManagerApi.incr(c.entity, "fiskheroes:aimed_timer", 10, 10, aimed, !aimed);
    }

    // ------------------------------------------------------------------ máscara

    private static void tickMask(ModifierContext c) {
        boolean open = c.bool("fiskheroes:mask_open");
        int ticks = maskToggleTicks(c.hero.suitId);
        DataManagerApi.incr(c.entity, "fiskheroes:mask_open_timer2", ticks, ticks, open, !open);
        double t = c.num("fiskheroes:mask_open_timer");
        if (open && t < ticks) c.set("fiskheroes:mask_open_timer", t + 1);
        else if (!open && t > 0) c.set("fiskheroes:mask_open_timer", t - 1);
    }

    private static final Map<String, Integer> MASK_TICKS = new java.util.concurrent.ConcurrentHashMap<>();

    /** maskToggleTicks del bloque "alts" del heropack.json (variante o $DEFAULT); 10 si no hay. */
    public static int maskToggleTicks(String suitId) {
        return MASK_TICKS.computeIfAbsent(suitId, id -> {
            String base = id.contains("/") ? id.substring(0, id.indexOf('/')) : id;
            String alt = id.contains("/") ? id.substring(id.indexOf('/') + 1) : null;
            for (HeroPack pack : HeroPackManager.packs()) {
                if (!pack.info().has("alts")) continue;
                JsonObject alts = pack.info().getAsJsonObject("alts");
                if (!alts.has(base)) continue;
                JsonObject h = alts.getAsJsonObject(base);
                for (String k : alt != null ? new String[]{alt, "$DEFAULT"} : new String[]{"$DEFAULT"}) {
                    JsonElement e = h.get(k);
                    if (e != null && e.isJsonObject() && e.getAsJsonObject().has("maskToggleTicks")) {
                        return e.getAsJsonObject().get("maskToggleTicks").getAsInt();
                    }
                }
            }
            return 10;
        });
    }

    // ------------------------------------------------------------------ teclas

    /** Cliente: teclas recién pulsadas por el jugador local (bits de KeyState). */
    public static void onLocalKeysPressed(Player p, int pressed) {
        ActiveHero hero = ActiveHero.of(p);
        if (hero == null) return;
        JSEntity js = JSEntity.of(p);
        if ((pressed & KeyState.MASK) != 0 && hero.hasProperty(js, "MASK_TOGGLE")) {
            Sync.setLocal(p, "fiskheroes:mask_open", !(DataStore.get(p, "fiskheroes:mask_open") instanceof Boolean b && b));
        }
        boolean sentryDone = false;
        for (int slot = -1; slot <= 5; slot++) {
            int bit = KeyState.slotBit(slot);
            if (bit == 0 || (pressed & bit) == 0) continue;
            for (String key : hero.keysInSlot(js, slot)) {
                for (ModifierEntry m : hero.modifiers) {
                    if (!hero.isModifierEnabled(js, m)) continue;
                    if (m.type.equals(com.hendrilillo.fiskcompat.power.modifier.SentryMode.TYPE) && key.equals(com.hendrilillo.fiskcompat.power.modifier.SentryMode.KEY)) {
                        if (!sentryDone) com.hendrilillo.fiskcompat.power.modifier.SentryMode.keyPressed(p);
                        sentryDone = true;
                        continue;
                    }
                    if (m.type.equals("fiskheroes:charged_beam") && key.equals("CHARGED_BEAM")) {
                        boolean charging = DataStore.get(p, "fiskheroes:beam_charging") instanceof Boolean b && b;
                        double charge = DataStore.get(p, "fiskheroes:beam_charge") instanceof Number n ? n.doubleValue() : 0;
                        if (!charging && charge <= 0) Sync.setLocal(p, "fiskheroes:beam_charging", true);
                        else if (charging && m.isToggle()) Sync.setLocal(p, "fiskheroes:beam_charging", false);
                        continue;
                    }
                    String var = toggledBy(m, key);
                    if (var != null) Sync.setLocal(p, var, !(DataStore.get(p, var) instanceof Boolean b && b));
                }
            }
        }
    }

    /** Fisk: teclas que, si el modificador es interruptor, alternan una variable al pulsarse. */
    private static final Map<String, String[]> KEY_TOGGLES = Map.of(
            "fiskheroes:shield", new String[]{"SHIELD", "fiskheroes:shield"},
            "fiskheroes:blade", new String[]{"BLADE", "fiskheroes:blade"},
            "fiskheroes:invisibility", new String[]{"INVISIBILITY", "fiskheroes:invisible"},
            "fiskheroes:charged_punch", new String[]{"CHARGED_PUNCH", "fiskheroes:charged_punch"},
            "fiskheroes:slow_motion", new String[]{"SLOW_MOTION", "fiskheroes:slow_motion"},
            "fiskheroes:web_swinging", new String[]{"WEB_SWINGING", "fiskheroes:web_swinging"},
            "fiskheroes:energy_projection", new String[]{"ENERGY_PROJECTION", "fiskheroes:energy_projection"},
            "fiskheroes:heat_vision", new String[]{"HEAT_VISION", "fiskheroes:heat_vision"});

    /** Variable que alterna esta tecla en este modificador, o null. */
    private static String toggledBy(ModifierEntry m, String key) {
        if (!m.isToggle()) return null;
        if (m.type.equals("fiskheroes:transformation")) return key.equals(m.key()) ? m.toggleVar() : null;
        String[] t = KEY_TOGGLES.get(m.type);
        return t != null && t[0].equals(key) ? t[1] : null;
    }

    /** Servidor: teclas recién pulsadas (funciones de tecla de los scripts). */
    public static void onKeysPressed(ServerPlayer p, int pressed) {
        ActiveHero hero = ActiveHero.of(p);
        if (hero == null) return;
        JSEntity js = JSEntity.of(p);
        for (int slot = -1; slot <= 5; slot++) {
            if (KeyState.slotBit(slot) == 0 || (pressed & KeyState.slotBit(slot)) == 0) continue;
            for (String key : hero.keysInSlot(js, slot)) {
                HeroDefinition.KeyBind kb = hero.def.keyBinds().get(key);
                if (kb == null || kb.func() == null) continue;
                try {
                    kb.func().apply(js, DataManagerApi.INSTANCE);
                } catch (Throwable t) {
                    FiskCompat.LOGGER.error("Error en la tecla {} de {}: {}", key, hero.def.id(), t.getMessage());
                }
            }
        }
    }

    // ------------------------------------------------------------------ sincronización inicial

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer sp) Net.sendAll(event.getTarget(), sp);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Net.sendAll(sp, sp);
    }
}
