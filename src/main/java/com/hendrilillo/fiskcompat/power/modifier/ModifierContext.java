package com.hendrilillo.fiskcompat.power.modifier;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.data.Sync;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import net.minecraft.world.entity.LivingEntity;

/** Todo lo que necesita un modificador en un tick. */
public final class ModifierContext {
    public final LivingEntity entity;
    public final JSEntity js;
    public final ActiveHero hero;
    /** true si este lado controla a la entidad con teclas (cliente del propio jugador). */
    public final boolean local;
    public final int keys;

    public ModifierContext(LivingEntity entity, JSEntity js, ActiveHero hero, boolean local, int keys) {
        this.entity = entity;
        this.js = js;
        this.hero = hero;
        this.local = local;
        this.keys = keys;
    }

    public Object get(String var) {
        return DataStore.get(entity, var);
    }

    public boolean bool(String var) {
        Object v = get(var);
        return v instanceof Boolean b ? b : v instanceof Number n && n.doubleValue() != 0;
    }

    public double num(String var) {
        Object v = get(var);
        return v instanceof Number n ? n.doubleValue() : v instanceof Boolean b && b ? 1 : 0;
    }

    public void set(String var, Object value) {
        DataStore.set(entity, var, value);
    }

    /** Cambio hecho por el jugador local: se envía al servidor. */
    public void setLocal(String var, Object value) {
        if (local) Sync.setLocal(entity, var, value);
    }

    public boolean keyPressed(String key) {
        return hero.isKeyPressed(js, entity, key, keys);
    }
}
