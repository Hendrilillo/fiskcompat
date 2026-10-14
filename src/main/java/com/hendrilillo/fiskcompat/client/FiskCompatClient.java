package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@EventBusSubscriber(modid = FiskCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FiskCompatClient {
    static {
        com.hendrilillo.fiskcompat.render.HeroRendererScript.effectFactory = (type, renderer) -> switch (type) {
            case "fiskheroes:overlay" -> new com.hendrilillo.fiskcompat.client.effect.OverlayEffect(type, renderer);
            case "fiskheroes:opening_mask" -> new com.hendrilillo.fiskcompat.client.effect.OpeningMaskEffect(type, renderer);
            case "fiskheroes:model" -> new com.hendrilillo.fiskcompat.client.effect.ModelEffect(type, renderer);
            default -> new com.hendrilillo.fiskcompat.client.effect.EffectObject(type, renderer);
        };
        com.hendrilillo.fiskcompat.render.HeroRendererScript.resourceFactory = (spec, renderer) -> {
            int bar = spec.indexOf('|');
            String type = spec.substring(0, bar), path = spec.substring(bar + 1);
            return switch (type) {
                case "MODEL" -> new com.hendrilillo.fiskcompat.client.model.ModelResource(path);
                case "ANIMATION" -> new com.hendrilillo.fiskcompat.client.anim.BipedAnimation(path);
                default -> null;
            };
        };
    }

    private FiskCompatClient() {}

    /** Añade el resource pack virtual con los assets de los heropacks (siempre activo). */
    @SubscribeEvent
    public static void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(HeroPackResources.locationInfo(), HeroPackResources.supplier(),
                    PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack != null) consumer.accept(pack);
        });
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SuitModel.LAYER, SuitModel::create);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            EntityRenderer<?> r = event.getSkin(skin);
            if (r instanceof LivingEntityRenderer lr) addTo(lr, event);
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> r = event.getRenderer(type);
            if (r instanceof LivingEntityRenderer lr && lr.getModel() instanceof HumanoidModel) addTo(lr, event);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addTo(LivingEntityRenderer renderer, EntityRenderersEvent.AddLayers event) {
        if (!(renderer.getModel() instanceof HumanoidModel)) return;
        renderer.addLayer(new SuitLayer(renderer, new SuitModel<>(event.getEntityModels().bakeLayer(SuitModel.LAYER))));
    }

    /** La capa de armadura vanilla no dibuja nada para los trajes: lo hace SuitLayer. */
    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        Model empty = new Model(RenderType::entityCutoutNoCull) {
            @Override
            public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            }
        };
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                return empty;
            }

        }, ModItems.SUIT_HELMET.get(), ModItems.SUIT_CHESTPLATE.get(), ModItems.SUIT_LEGGINGS.get(), ModItems.SUIT_BOOTS.get());
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        SuitIcons.registerModels(event);
    }

    @SubscribeEvent
    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        SuitIcons.wrapModels(event);
    }

    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            ClientHeroes.clearTextures();
            com.hendrilillo.fiskcompat.client.model.TabulaModel.clearCache();
            com.hendrilillo.fiskcompat.client.model.ModelResource.clearCache();
            com.hendrilillo.fiskcompat.client.anim.BipedAnimation.clearCache();
        });
    }
}
