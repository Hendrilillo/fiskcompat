package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.api.DataManagerApi;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * fiskheroes:controlled_flight - vuelo estilo Iron Man.
 * Doble salto en el aire para volar; adelante vuela hacia donde miras, correr = turbo,
 * salto/agacharse para subir/bajar; al tocar el suelo se aterriza.
 */
public final class ControlledFlight {
    public static final String FLYING = "fiskheroes:flying";
    private static final Map<Player, Input> INPUT = new WeakHashMap<>();

    /** Entrada de movimiento del jugador local (la rellena el cliente). */
    public record Input(float forward, float strafe, boolean jump, boolean sneak, boolean jumpPressed) {}

    /** Ticks desde la última pulsación de salto, para detectar el doble salto. */
    private static final Map<Player, Integer> LAST_JUMP = new WeakHashMap<>();

    private ControlledFlight() {}

    public static void setInput(Player p, Input in) {
        INPUT.put(p, in);
    }

    /** Entrada de vuelo activa (la primera habilitada) para esta entidad este tick. */
    private static final Map<net.minecraft.world.entity.Entity, ModifierEntry> ACTIVE = new WeakHashMap<>();

    public static void beginTick(net.minecraft.world.entity.Entity e) {
        ACTIVE.remove(e);
    }

    static void tick(ModifierContext c, ModifierEntry m, boolean enabled) {
        if (enabled && !ACTIVE.containsKey(c.entity)) ACTIVE.put(c.entity, m);
    }

    /** Se llama después de todos los modificadores del tick. */
    public static void endTick(ModifierContext c) {
        ModifierEntry m = ACTIVE.get(c.entity);
        boolean flying = c.bool(FLYING);
        if (m == null) {
            if (flying) {
                c.set(FLYING, false);
                if (c.local) c.setLocal(FLYING, false);
            }
        } else if (c.local && c.entity instanceof Player p) {
            flying = controlLocal(c, m, p, flying);
        }
        flying = c.bool(FLYING);
        boolean boosting = flying && c.entity.isSprinting() && (m == null || m.bool("canBoost", true));
        DataManagerApi.incr(c.entity, "fiskheroes:flight_timer", 10, 10, flying, !flying);
        DataManagerApi.incr(c.entity, "fiskheroes:flight_boost_timer", 10, 10, boosting, !boosting);
        if (flying) {
            c.entity.fallDistance = 0;
            c.entity.setNoGravity(true);
        } else if (c.entity.isNoGravity() && c.num("fiskheroes:flight_timer") <= 0.1) {
            c.entity.setNoGravity(false);
        }
    }

    private static boolean controlLocal(ModifierContext c, ModifierEntry m, Player p, boolean flying) {
        Input in = INPUT.getOrDefault(p, new Input(0, 0, false, false, false));
        int since = LAST_JUMP.getOrDefault(p, 100);
        if (in.jumpPressed()) {
            if (since <= 7 && !p.onGround() && !p.getAbilities().flying) {
                flying = !flying;
                c.setLocal(FLYING, flying);
                since = 100;
            } else {
                since = 0;
            }
        } else {
            since++;
        }
        LAST_JUMP.put(p, since);

        if (flying && (p.onGround() && !in.jump() || p.isPassenger() || p.getAbilities().flying || p.isInWater() && !in.jump())) {
            flying = false;
            c.setLocal(FLYING, false);
        }
        if (!flying) return false;

        double speed = m.num("speed", 0.065);
        double boost = m.num("boostSpeed", speed * 2);
        boolean boosting = p.isSprinting() && m.bool("canBoost", true);
        double accel = boosting ? boost : speed;
        Vec3 v = p.getDeltaMovement();
        Vec3 look = p.getLookAngle();
        Vec3 add = Vec3.ZERO;
        if (in.forward() != 0) add = add.add(look.scale(accel * in.forward()));
        if (in.strafe() != 0) {
            Vec3 side = new Vec3(look.z, 0, -look.x).normalize();
            add = add.add(side.scale(speed * in.strafe()));
        }
        if (in.jump()) add = add.add(0, speed, 0);
        if (in.sneak()) add = add.add(0, -speed, 0);
        if (in.forward() == 0 && in.strafe() == 0 && !in.jump() && !in.sneak()) {
            v = v.scale(0.85); // flotar
        }
        p.setDeltaMovement(v.add(add));
        return true;
    }
}
