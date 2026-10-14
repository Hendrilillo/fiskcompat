package com.hendrilillo.fiskcompat.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * "damageProfile" de los poderes de Fisk: daño base, tipos (ENERGY, FIRE...) y propiedades
 * (IGNITE, ADD_KNOCKBACK, REDUCE_KNOCKBACK, HIT_COOLDOWN...).
 */
public final class DamageProfile {
    public final float damage;
    private final JsonObject types, props;

    private DamageProfile(float damage, JsonObject types, JsonObject props) {
        this.damage = damage;
        this.types = types;
        this.props = props;
    }

    public static DamageProfile of(ModifierEntry m) {
        JsonObject o = m.obj("damageProfile");
        if (o == null) return new DamageProfile(0, new JsonObject(), new JsonObject());
        float dmg = o.has("damage") ? o.get("damage").getAsFloat() : 0;
        JsonObject types = o.has("types") && o.get("types").isJsonObject() ? o.getAsJsonObject("types") : new JsonObject();
        JsonObject props = o.has("properties") && o.get("properties").isJsonObject() ? o.getAsJsonObject("properties") : new JsonObject();
        return new DamageProfile(dmg, types, props);
    }

    public double prop(String name, double def) {
        JsonElement e = props.get(name);
        if (e == null || !e.isJsonPrimitive()) return def;
        if (e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean() ? 1 : 0;
        return e.getAsDouble();
    }

    public double type(String name) {
        JsonElement e = types.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsDouble() : 0;
    }

    /**
     * Aplica el daño a la entidad. {@code scale} multiplica el daño (p. ej. la carga del unibeam).
     * Devuelve true si la entidad recibió daño.
     */
    public boolean apply(Entity target, LivingEntity attacker, float scale) {
        if (damage * scale <= 0 || target == attacker) return false;
        DamageSource src = attacker instanceof Player p ? attacker.damageSources().playerAttack(p)
                : attacker.damageSources().mobAttack(attacker);
        Vec3 before = target.getDeltaMovement();
        int cooldown = (int) prop("HIT_COOLDOWN", -1);
        boolean hurt = target.hurt(src, damage * scale);
        if (!hurt) return false;
        if (cooldown >= 0 && target instanceof LivingEntity le) le.invulnerableTime = cooldown;
        double ignite = prop("IGNITE", 0);
        if (ignite > 0) target.igniteForSeconds((float) ignite);
        else if (type("FIRE") > 0 && prop("COOK_ENTITY", 0) > 0 && !target.fireImmune()) target.igniteForSeconds(1);
        double reduce = prop("REDUCE_KNOCKBACK", 0);
        if (reduce > 0) {
            Vec3 now = target.getDeltaMovement();
            target.setDeltaMovement(before.add(now.subtract(before).scale(Math.max(0, 1 - reduce))));
        }
        double add = prop("ADD_KNOCKBACK", 0);
        if (add > 0 && target instanceof LivingEntity le) {
            Vec3 dir = target.position().subtract(attacker.position());
            le.knockback(add * 0.5, -dir.x, -dir.z);
        }
        return true;
    }
}
