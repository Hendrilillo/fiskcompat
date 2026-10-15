package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Efecto "fiskheroes:shield": una placa plegable que se despliega desde una parte del cuerpo (escudos de
 * nanobots, cuchillas...). La forma es la de Fisk: una columna de segmentos con alas a los lados que se
 * abren con "unfold" y se curvan con setCurve.
 */
public class ShieldEffect extends EffectObject {
    public double unfold = 1;
    public boolean large, mirror;
    private float curveX, curveY;

    private static Model small, big;

    public ShieldEffect(String type, HeroRendererScript renderer) {
        super(type, renderer);
    }

    @Override
    public Object getMember(String name) {
        return switch (name) {
            case "unfold" -> unfold;
            case "large" -> large;
            case "mirror" -> mirror;
            case "setCurve" -> new JsFunction(name, a -> {
                curveX = (float) Math.toRadians(arg(a, 0, 0));
                curveY = (float) Math.toRadians(arg(a, 1, 0));
                return this;
            });
            default -> super.getMember(name);
        };
    }

    @Override
    public void setMember(String name, Object value) {
        switch (name) {
            case "unfold" -> unfold = num(value);
            case "large" -> large = Boolean.TRUE.equals(value) || value instanceof Number n && n.doubleValue() != 0;
            case "mirror" -> mirror = Boolean.TRUE.equals(value) || value instanceof Number n && n.doubleValue() != 0;
            default -> super.setMember(name, value);
        }
    }

    private Model model() {
        if (large) {
            if (big == null) big = new Model(3, 11, 6);
            return big;
        }
        if (small == null) small = new Model(2, 6, 2);
        return small;
    }

    @Override
    protected void render(RenderContext ctx) {
        if (unfold <= 0 || opacity <= 0) return;
        ResourceLocation normal = ClientHeroes.heroTexture(renderer.texturePath(texture.normal), ctx.jsEntity);
        ResourceLocation lights = ClientHeroes.heroTexture(renderer.texturePath(texture.lights), ctx.jsEntity);
        if (normal == null && lights == null) return;
        Model m = model();
        m.setAngles((float) unfold, curveX / (m.spine.length - 2), curveY / m.segments);
        String opposite = opposite(anchor.part);
        boolean hasMirror = opposite != null && !opposite.equals(anchor.part);
        // en primera persona solo el brazo derecho (o el izquierdo si es su reflejo)
        if (!ctx.firstPerson || "rightArm".equals(anchor.part)) draw(ctx, m, normal, lights, anchor.part, false);
        if (mirror && hasMirror && (!ctx.firstPerson || "leftArm".equals(opposite))) draw(ctx, m, normal, lights, opposite, true);
    }

    private static String opposite(String part) {
        if (part == null) return null;
        return switch (part) {
            case "rightArm" -> "leftArm";
            case "leftArm" -> "rightArm";
            case "rightLeg" -> "leftLeg";
            case "leftLeg" -> "rightLeg";
            default -> part;
        };
    }

    private void draw(RenderContext ctx, Model m, ResourceLocation normal, ResourceLocation lights, String part, boolean mirrored) {
        PoseStack pose = ctx.poseStack;
        pose.pushPose();
        ModelPart p = anchor.ignore ? null : anchorPart(ctx.model, part);
        if (p != null) p.translateAndRotate(pose);
        // Igual que Fisk (AbstractHeroEffectPositioned): desplazamiento con X invertida y giros X, Y, Z
        float sx = mirrored ? 1 : -1, sr = mirrored ? -1 : 1;
        pose.translate(sx * offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
        if (rotation[0] != 0) pose.mulPose(Axis.XP.rotationDegrees(rotation[0]));
        if (rotation[1] != 0) pose.mulPose(Axis.YP.rotationDegrees(sr * rotation[1]));
        if (rotation[2] != 0) pose.mulPose(Axis.ZP.rotationDegrees(sr * rotation[2]));
        if (mirrored) pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(scale[0], scale[1], scale[2]);
        float a = (float) Math.min(1, opacity);
        int color = ((int) (a * 255) << 24) | 0xFFFFFF;
        if (normal != null) {
            RenderType type = a < 1 ? RenderType.entityTranslucentCull(normal) : RenderType.entityCutout(normal);
            m.render(pose, ctx.buffers.getBuffer(type), ctx.light, color);
        }
        if (lights != null) {
            int c = (int) (a * 255);
            m.render(pose, ctx.buffers.getBuffer(RenderType.eyes(lights)), ctx.light, 0xFF000000 | c << 16 | c << 8 | c);
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ modelo (ModelRetractableShield de Fisk)

    /** Una pieza: punto de giro, ángulos, cuadriláteros (x, y, z, u, v) e hijas. */
    private static final class Shape {
        float rpX, rpY, rpZ, rotX, rotY, rotZ;
        final List<float[]> quads = new ArrayList<>(); // cada float[] = 4 vértices * 5
        final List<Shape> children = new ArrayList<>();

        void render(PoseStack pose, VertexConsumer vc, int light, int color, float tw, float th) {
            pose.pushPose();
            pose.translate(rpX / 16f, rpY / 16f, rpZ / 16f);
            if (rotZ != 0) pose.mulPose(Axis.ZP.rotation(rotZ));
            if (rotY != 0) pose.mulPose(Axis.YP.rotation(rotY));
            if (rotX != 0) pose.mulPose(Axis.XP.rotation(rotX));
            Matrix4f mat = pose.last().pose();
            Matrix3f nm = pose.last().normal();
            for (float[] q : quads) {
                // normal de la cara
                Vector3f p0 = new Vector3f(q[0], q[1], q[2]), p1 = new Vector3f(q[5], q[6], q[7]), p2 = new Vector3f(q[10], q[11], q[12]);
                Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p1)).normalize();
                if (!Float.isFinite(n.x)) n.set(1, 0, 0);
                nm.transform(n);
                for (int i = 0; i < 4; i++) {
                    int o = i * 5;
                    Vector3f v = mat.transformPosition(q[o] / 16f, q[o + 1] / 16f, q[o + 2] / 16f, new Vector3f());
                    vc.addVertex(v.x, v.y, v.z, color, q[o + 3] / tw, q[o + 4] / th, OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
                }
            }
            for (Shape c : children) c.render(pose, vc, light, color, tw, th);
            pose.popPose();
        }
    }

    private static final class Model {
        final int segments, center, width;
        final Shape[] spine;
        final Shape[][][] sides;
        float tw = 32, th = 16;

        Model(int sideSegments, int spineLength, int spineCenter) {
            segments = sideSegments;
            center = spineCenter - 1;
            width = segments * 4 + 2;
            spine = new Shape[spineLength];
            sides = new Shape[spineLength][2][segments];
            while (tw < width * 2) tw *= 2;
            while (th < spineLength * 2) th *= 2;
            for (int i = 0; i < spine.length; i++) {
                int y = i < center ? -2 : 0;
                spine[i] = shape(-1, y, 2, 2, segments * 2, i * 2);
                if (i > center) spine[i - 1].children.add(spine[i]);
                else if (i > 0) spine[i].children.add(spine[i - 1]);
                for (int side = 0; side < 2; side++) {
                    Shape prev = spine[i];
                    int j = side * 2 - 1;
                    for (int s = 0; s < segments; s++) {
                        Shape sh = shape(-side * 2, y, 2, 2, segments * 2 + j * (s + 1) * 2, i * 2);
                        prev.children.add(sh);
                        prev = sh;
                        sides[i][side][s] = sh;
                    }
                }
            }
        }

        private Shape shape(int x, int y, int w, int h, int u, int v) {
            Shape s = new Shape();
            s.quads.add(new float[]{
                    0, y, x + w, u, v,
                    0, y + h, x + w, u, v + h,
                    0, y + h, x, u + w, v + h,
                    0, y, x, u + w, v});
            int u2 = u + width;
            s.quads.add(new float[]{
                    0, y + h, x, u2 + w, v + h,
                    0, y + h, x + w, u2, v + h,
                    0, y, x + w, u2, v,
                    0, y, x, u2 + w, v});
            return s;
        }

        void setAngles(float mult, float curveX, float curveZ) {
            int max = Math.max(spine.length - center, center);
            for (int i = 0; i < spine.length; i++) {
                float f = Mth.clamp(i < center ? mult * max - center + i + 1 : mult * max - i + center, 0, 1);
                float f1 = 1 - f;
                if (i != center) {
                    int j = i < center ? -1 : 1;
                    spine[i].rpY = 2 * j * f + (i == center - 1 ? 2 : 0);
                    spine[i].rotX = 0;
                    if (i != center - 1) spine[i].rotZ = -curveX * j * f;
                }
                for (int side = 0; side < 2; side++) {
                    int j = side * 2 - 1;
                    for (int s = 0; s < segments; s++) {
                        Shape sh = sides[i][side][s];
                        sh.rpZ = (s == 0 ? j : 0) - 2 * j * f;
                        sh.rpX = 0.1f * f1;
                        sh.rotY = -curveZ * j * f;
                    }
                }
            }
        }

        void render(PoseStack pose, VertexConsumer vc, int light, int color) {
            spine[center].render(pose, vc, light, color, tw, th);
        }
    }
}
