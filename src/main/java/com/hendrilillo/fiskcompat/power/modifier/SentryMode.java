package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.data.Sync;
import com.hendrilillo.fiskcompat.entity.ModEntities;
import com.hendrilillo.fiskcompat.entity.SentryEntity;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.util.EntityMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * fiskheroes:sentry_mode: con la tecla SENTRY_MODE el traje se abre (fiskheroes:suit_open) y, cuando
 * termina de abrirse, el jugador sale y el traje se queda como un centinela que le sigue y le protege.
 */
public final class SentryMode {
    public static final String TYPE = "fiskheroes:sentry_mode", KEY = "SENTRY_MODE",
            OPEN = "fiskheroes:suit_open", TIMER = "fiskheroes:suit_open_timer";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final EntityMap<LivingEntity, Long> PUSHED = new EntityMap<>();

    private SentryMode() {}

    /** Cliente (jugador local): se pulsa la tecla SENTRY_MODE. */
    public static void keyPressed(LivingEntity p) {
        boolean open = DataStore.get(p, OPEN) instanceof Boolean b && b;
        int timer = DataStore.get(p, TIMER) instanceof Number n ? n.intValue() : 0;
        if (DataStore.get(p, "fiskheroes:mask_open") instanceof Boolean b && b) {
            Sync.setLocal(p, "fiskheroes:mask_open", false);
        }
        if (!open && timer == 0) set(p, true);
        else if (open && timer == 5) set(p, false);
    }

    private static void set(LivingEntity p, boolean open) {
        Sync.setLocal(p, OPEN, open);
    }

    static void tick(ModifierContext c, ModifierEntry m, boolean enabled) {
        LivingEntity e = c.entity;
        int timer = (int) c.num(TIMER);
        boolean open = c.bool(OPEN);
        if (c.num("fiskheroes:mask_open_timer") <= 0) {
            if (timer < 5 && open) c.set(TIMER, (byte) (timer + 1));
            else if (timer > 0 && !open) c.set(TIMER, (byte) Math.max(0, timer - 1));
        } else if (open && c.bool("fiskheroes:mask_open")) {
            c.set("fiskheroes:mask_open", false);
            c.setLocal("fiskheroes:mask_open", false);
        }
        if (timer != 5 || !open || !enabled || e instanceof SentryEntity) return;
        if (e instanceof Player p && !p.mayBuild()) return;
        if (!e.level().isClientSide) eject(e);
        // el jugador sale hacia atrás
        long now = e.level().getGameTime();
        Long last = PUSHED.get(e);
        if (last == null || now - last > 20) {
            PUSHED.put(e, now);
            float yaw = e.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 back = new Vec3(Mth.sin(yaw) * 0.25, 0, -Mth.cos(yaw) * 0.25);
            if (c.local || !(e instanceof Player)) e.setDeltaMovement(back);
        }
    }

    private static void eject(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        SentryEntity s = ModEntities.SENTRY.get().create(level);
        if (s == null) return;
        String suitId = SuitItem.heroId(e.getItemBySlot(EquipmentSlot.CHEST));
        s.moveTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot());
        s.yBodyRot = s.yBodyRotO = e.yBodyRot;
        s.yHeadRot = s.yHeadRotO = e.yHeadRot;
        s.setDeltaMovement(e.getDeltaMovement());
        s.exitTimer = 4;
        for (EquipmentSlot slot : ARMOR) {
            s.setItemSlot(slot, e.getItemBySlot(slot).copy());
            e.setItemSlot(slot, ItemStack.EMPTY);
        }
        s.setOwner(e.getUUID());
        boolean flying = DataStore.get(e, ControlledFlight.FLYING) instanceof Boolean b && b;
        DataStore.set(s, ControlledFlight.FLYING, flying || !e.onGround());
        DataStore.set(s, OPEN, true);
        DataStore.setNoInterp(s, TIMER, (byte) 5);
        level.addFreshEntity(s);
        Net.soundTrigger(e, suitId, TYPE, "EXIT");
    }
}
