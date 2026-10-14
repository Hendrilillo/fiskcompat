package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.entity.SentryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** El centinela no tiene cuerpo: solo se ve el traje (lo dibuja SuitLayer). */
public class SentryRenderer extends HumanoidMobRenderer<SentryEntity, HumanoidModel<SentryEntity>> {
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "textures/entity/empty.png");

    public SentryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SentryEntity entity) {
        return EMPTY;
    }
}
