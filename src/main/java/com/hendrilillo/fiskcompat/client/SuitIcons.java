package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.item.ModItems;
import com.hendrilillo.fiskcompat.item.SuitItem;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.HashMap;
import java.util.Map;

/** Iconos de inventario de los trajes, sacados de setItemIcons(...) de cada renderer. */
public final class SuitIcons {
    /** "suitId#pieza" -> modelo del icono. */
    private static final Map<String, ModelResourceLocation> ICONS = new HashMap<>();
    private static final Map<ModelResourceLocation, BakedModel> BAKED = new HashMap<>();

    private SuitIcons() {}

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ICONS.clear();
        long t = System.currentTimeMillis();
        for (String suitId : ModItems.allSuitIds()) {
            HeroRendererScript r = ClientHeroes.renderer(suitId);
            if (r == null) continue;
            for (int i = 0; i < 4; i++) {
                String icon = r.itemIcon(i);
                if (icon == null) continue;
                int c = icon.indexOf(':');
                String ns = icon.substring(0, c), path = icon.substring(c + 1);
                if (HeroPackManager.findAsset(ns, "textures/items/heroes/" + path + ".png").isEmpty()) continue;
                ResourceLocation rl = ResourceLocation.tryBuild(FiskCompat.MODID, HeroPackResources.ICON_PREFIX + ns + "/" + path);
                if (rl == null) continue;
                ModelResourceLocation mrl = ModelResourceLocation.standalone(rl);
                ICONS.put(suitId + "#" + i, mrl);
                event.register(mrl);
            }
        }
        FiskCompat.LOGGER.info("FiskCompat: {} iconos de trajes ({} ms)", ICONS.size(), System.currentTimeMillis() - t);
    }

    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BAKED.clear();
        for (ModelResourceLocation mrl : ICONS.values()) {
            BakedModel m = models.get(mrl);
            if (m != null) BAKED.put(mrl, m);
        }
        for (SuitItem item : ModItems.suitItems()) {
            ModelResourceLocation key = ModelResourceLocation.inventory(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
            BakedModel original = models.get(key);
            if (original != null) models.put(key, new Wrapper(original));
        }
    }

    private static final class Wrapper extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides = new Overrides();

        Wrapper(BakedModel original) {
            super(original);
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    private static final class Overrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            String suitId = SuitItem.heroId(stack);
            if (suitId != null && stack.getItem() instanceof SuitItem item) {
                ModelResourceLocation mrl = ICONS.get(suitId + "#" + item.pieceIndex());
                BakedModel icon = mrl == null ? null : BAKED.get(mrl);
                if (icon != null) return icon;
            }
            return model;
        }
    }
}
