package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSVector;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;

import java.util.List;

/** Objeto global "PackLoader" que ven los scripts. */
public final class PackLoaderApi {
    public boolean isPackLoaded(String domain) { return HeroPackManager.isLoaded(domain); }

    public List<String> getLoadedPacks() { return HeroPackManager.packs().stream().map(HeroPack::domain).toList(); }

    public void print(Object o) { FiskCompat.LOGGER.info("[script] {}", o); }

    public void printChat(Object o) { FiskCompat.LOGGER.info("[script chat] {}", o); }

    public String getSide() { return Side.isClient() ? "CLIENT" : "SERVER"; }

    public JSVector.Vec3 asVec3(double x, double y, double z) { return new JSVector.Vec3(x, y, z); }

    public JSVector.Vec2 asVec2(double x, double y) { return new JSVector.Vec2(x, y); }

    public int getNumericalItemId(String item) { return 0; }

    /** Lado lógico actual; se actualiza desde el código de Minecraft. */
    public static final class Side {
        private static final ThreadLocal<Boolean> CLIENT = ThreadLocal.withInitial(() -> false);

        public static boolean isClient() { return CLIENT.get(); }

        public static void set(boolean client) { CLIENT.set(client); }
    }
}
