package com.hendrilillo.fiskcompat.power;

import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.net.Net;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Escudos de los trajes (fiskheroes:shield): mientras se bloquea, paran los golpes que vienen de delante
 * (dentro de "coverage"). El escudo tiene vida ("shield.health"); si se rompe deja pasar el golpe y tarda
 * "shield.cooldown" ticks en volver. Se recupera "shield.regeneration" por segundo tras "recoveryDelay".
 */
@EventBusSubscriber(modid = FiskCompat.MODID)
public final class ShieldBlock {
    private static final String DAMAGE = "fiskheroes:shield_damage", COOLDOWN = "fiskheroes:shield_cooldown",
            SINCE = "fiskheroes:ticks_since_shield_damaged", TYPE = "fiskheroes:shield";

    private ShieldBlock() {}

    private static double prop(ModifierEntry m, String obj, String key, double def) {
        JsonObject o = m.obj(obj);
        return o != null && o.has(key) ? o.get(key).getAsDouble() : def;
    }

    private static int num(Entity e, String var) {
        return DataStore.get(e, var) instanceof Number n ? n.intValue() : 0;
    }

    /** Servidor, cada tick: espera tras romperse y regeneración. */
    public static void tick(LivingEntity e, ModifierEntry m) {
        int cooldown = num(e, COOLDOWN);
        if (cooldown > 0) {
            DataStore.set(e, COOLDOWN, (short) (cooldown - 1));
            return;
        }
        int since = num(e, SINCE);
        if (since < 10000) DataStore.set(e, SINCE, since + 1);
        float dmg = DataStore.get(e, DAMAGE) instanceof Number n ? n.floatValue() : 0;
        if (dmg > 0 && since > prop(m, "shield", "recoveryDelay", 20)) {
            DataStore.set(e, DAMAGE, (float) Math.max(dmg - prop(m, "shield", "regeneration", 10) / 20, 0));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !(DataStore.get(e, "fiskheroes:shield_blocking") instanceof Boolean b && b)) return;
        ActiveHero hero = ActiveHero.of(e);
        if (hero == null || num(e, COOLDOWN) > 0) return;
        ModifierEntry m = hero.enabledModifier(JSEntity.of(e), TYPE);
        if (m == null) return;
        DamageSource source = event.getSource();
        Entity src = source.getDirectEntity();
        if (src == null || source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (!covers(e, src, m)) return;
        if (e.invulnerableTime == 0) {
            float amount = event.getAmount();
            float total = (DataStore.get(e, DAMAGE) instanceof Number n ? n.floatValue() : 0) + amount;
            DataStore.set(e, SINCE, 0);
            DataStore.set(e, DAMAGE, total);
            boolean destroyed = total >= prop(m, "shield", "health", 100);
            if (destroyed) DataStore.set(e, COOLDOWN, (short) prop(m, "shield", "cooldown", 100));
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity la && (source.is(DamageTypeTags.IS_EXPLOSION) || source.getDirectEntity() == attacker)) {
                double kb = m.num("knockback", 0);
                if (destroyed) kb = Math.max(kb * 1.5, 0.3);
                if (kb != 0) la.knockback(Math.min(amount, 20) * kb, e.getX() - la.getX(), e.getZ() - la.getZ());
            }
            if (destroyed) {
                Net.soundTrigger(e, TYPE, "DISARM");
                return; // el golpe que lo rompe pasa
            }
            e.invulnerableTime = 7;
            if (!source.is(DamageTypeTags.IS_EXPLOSION)) Net.soundTrigger(e, TYPE, "DEFLECT");
        }
        event.setCanceled(true);
    }

    private static boolean covers(LivingEntity e, Entity src, ModifierEntry m) {
        double yawR = prop(m, "coverage", "yawRadius", 90), pitchR = prop(m, "coverage", "pitchRadius", 90);
        double yawO = prop(m, "coverage", "yawOffset", 0), pitchO = prop(m, "coverage", "pitchOffset", 0);
        Vec3 d = new Vec3(src.getX() - e.getX(), src.getY() + src.getBbHeight() / 2 - e.getEyeY(), src.getZ() - e.getZ());
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(d.y, h) * 180 / Math.PI);
        float dy = Math.abs(Mth.wrapDegrees(yaw - e.getYRot() + (float) yawO));
        float dp = Math.abs(Mth.wrapDegrees(pitch - e.getXRot() + (float) pitchO));
        return dy < yawR && dp < pitchR;
    }
}
