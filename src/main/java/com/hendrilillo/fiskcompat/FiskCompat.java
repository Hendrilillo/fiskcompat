package com.hendrilillo.fiskcompat;

import com.hendrilillo.fiskcompat.data.DataVars;
import com.hendrilillo.fiskcompat.hero.HeroRegistry;
import com.hendrilillo.fiskcompat.item.ModItems;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * FiskCompat: carga heropacks de Fisk's Superheroes (1.7.10) en NeoForge 1.21.1.
 * Los heropacks (y el jar original de Fisk, para los recursos base) van en la carpeta "heropacks".
 */
@Mod(FiskCompat.MODID)
public class FiskCompat {
    public static final String MODID = "fiskcompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FiskCompat(IEventBus modBus) {
        ModItems.register(modBus);
        com.hendrilillo.fiskcompat.net.Net.register(modBus);
        reloadHeroPacks();
    }

    public static void reloadHeroPacks() {
        HeroPackManager.load(FMLPaths.GAMEDIR.get());
        DataVars.load();
        com.hendrilillo.fiskcompat.power.PowerRegistry.clear();
        com.hendrilillo.fiskcompat.hero.ActiveHero.clearCache();
        HeroRegistry.loadAll();
    }
}
