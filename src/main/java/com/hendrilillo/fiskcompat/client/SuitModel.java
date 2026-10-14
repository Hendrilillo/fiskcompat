package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Modelo bípedo de 64x64 como el ModelBipedMultiLayer de Fisk: las partes base (como una skin)
 * ligeramente hinchadas y una segunda capa exterior (gorro, chaqueta, mangas y pantalones de la skin).
 */
public class SuitModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "suit"), "main");

    public final ModelPart headOverlay, bodyOverlay, rightArmOverlay, leftArmOverlay, rightLegOverlay, leftLegOverlay;

    public SuitModel(ModelPart root) {
        super(root);
        headOverlay = head.getChild("overlay");
        bodyOverlay = body.getChild("overlay");
        rightArmOverlay = rightArm.getChild("overlay");
        leftArmOverlay = leftArm.getChild("overlay");
        rightLegOverlay = rightLeg.getChild("overlay");
        leftLegOverlay = leftLeg.getChild("overlay");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation base = new CubeDeformation(0.05f);
        CubeDeformation baseArm = new CubeDeformation(0.025f);
        CubeDeformation outer = new CubeDeformation(0.55f);
        CubeDeformation outerArm = new CubeDeformation(0.525f);

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, base), PartPose.ZERO);
        head.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, outer), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, base), PartPose.ZERO);
        body.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, outer), PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4, baseArm), PartPose.offset(-5, 2, 0));
        rightArm.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(40, 32).addBox(-3, -2, -2, 4, 12, 4, outerArm), PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1, -2, -2, 4, 12, 4, baseArm), PartPose.offset(5, 2, 0));
        leftArm.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(48, 48).addBox(-1, -2, -2, 4, 12, 4, outerArm), PartPose.ZERO);

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, base), PartPose.offset(-1.9f, 12, 0));
        rightLeg.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(0, 32).addBox(-2, 0, -2, 4, 12, 4, outer), PartPose.ZERO);

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2, 0, -2, 4, 12, 4, base), PartPose.offset(1.9f, 12, 0));
        leftLeg.addOrReplaceChild("overlay", CubeListBuilder.create().texOffs(0, 48).addBox(-2, 0, -2, 4, 12, 4, outer), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Partes por defecto de cada pieza si el script no llama a showModel. */
    public static List<String> defaultParts(HeroRendererScript.Slot slot) {
        return switch (slot) {
            case HELMET -> List.of("head", "headwear");
            case CHESTPLATE -> List.of("body", "rightArm", "leftArm");
            case LEGGINGS -> List.of("body", "rightLeg", "leftLeg");
            case BOOTS -> List.of("rightLeg", "leftLeg");
        };
    }

    /** Muestra solo las partes indicadas (nombres de Fisk: head, headwear, body, rightArm, leftArm, rightLeg, leftLeg). */
    public void showParts(List<String> parts) {
        boolean headBase = parts.contains("head");
        boolean headWear = parts.contains("headwear");
        head.visible = headBase || headWear;
        head.skipDraw = !headBase;
        headOverlay.visible = headWear;
        hat.visible = false;
        set(body, bodyOverlay, parts.contains("body"));
        set(rightArm, rightArmOverlay, parts.contains("rightArm"));
        set(leftArm, leftArmOverlay, parts.contains("leftArm"));
        set(rightLeg, rightLegOverlay, parts.contains("rightLeg"));
        set(leftLeg, leftLegOverlay, parts.contains("leftLeg"));
    }

    private static void set(ModelPart part, ModelPart overlay, boolean visible) {
        part.visible = visible;
        part.skipDraw = false;
        overlay.visible = visible;
    }
}
