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
    private static final Map<Player, ActiveHero> LAST = new WeakHashMap<>();

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

    private static void tick(Player p, boolean client, boolean local) {
        ActiveHero hero = ActiveHero.of(p);
        ActiveHero prev = LAST.get(p);
        if (hero == null) {
            if (prev != null) {
                LAST.remove(p);
                DataStore.resetWithoutSuit(p);
                DataStore.set(p, ControlledFlight.FLYING, false);
                if (local) Sync.setLocal(p, ControlledFlight.FLYING, false);
                p.setNoGravity(false);
                if (!client) HeroAttributes.remove(p);
            }
            return;
        }
        LAST.put(p, hero);
        JSEntity js = JSEntity.of(p);
        int keys = local ? localKeys.getAsInt() : KeyState.get(p);
        ModifierContext ctx = new ModifierContext(p, js, hero, local, keys);

        ControlledFlight.beginTick(p);
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
        for (int slot = 1; slot <= 5; slot++) {
            if ((pressed & KeyState.abilityBit(slot)) == 0) continue;
            for (String key : hero.keysInSlot(js, slot)) {
                for (ModifierEntry m : hero.modifiers) {
                    if (!key.equals(m.key()) || !m.isToggle() || !hero.isModifierEnabled(js, m)) continue;
                    String var = m.toggleVar();
                    if (var != null) Sync.setLocal(p, var, !(DataStore.get(p, var) instanceof Boolean b && b));
                }
            }
        }
    }

    /** Servidor: teclas recién pulsadas (funciones de tecla de los scripts). */
    public static void onKeysPressed(ServerPlayer p, int pressed) {
        ActiveHero hero = ActiveHero.of(p);
        if (hero == null) return;
        JSEntity js = JSEntity.of(p);
        for (int slot = 1; slot <= 5; slot++) {
            if ((pressed & KeyState.abilityBit(slot)) == 0) continue;
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
