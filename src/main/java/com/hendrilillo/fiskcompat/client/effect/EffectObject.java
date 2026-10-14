package com.hendrilillo.fiskcompat.client.effect;

import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.client.SuitModel;
import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base de los efectos de render de Fisk (renderer.createEffect(...)).
 * Es un objeto JS dinámico: lo que no está implementado se acepta sin romper el script.
 */
public class EffectObject extends AbstractJSObject {
    protected final String type;
    protected final HeroRendererScript renderer;
    private final Map<String, Object> props = new HashMap<>();

    public final TextureRef texture = new TextureRef();
    public final AnchorRef anchor = new AnchorRef();
    public double opacity = 1;
    public double progress = 0;
    public final float[] offset = {0, 0, 0};
    public final float[] rotation = {0, 0, 0};
    public final float[] scale = {1, 1, 1};

    public EffectObject(String type, HeroRendererScript renderer) {
        this.type = type;
        this.renderer = renderer;
    }

    static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    static double arg(Object[] a, int i, double def) {
        return i < a.length && a[i] instanceof Number n ? n.doubleValue() : def;
    }

    @Override
    public Object getMember(String name) {
        switch (name) {
            case "texture": return texture;
            case "anchor": return anchor;
            case "opacity": return opacity;
            case "progress": return progress;
            case "render": return new JsFunction("render", a -> { safeRender(); return null; });
            case "setOffset": return new JsFunction("setOffset", a -> { set(offset, a, 0); return this; });
            case "setRotation": return new JsFunction("setRotation", a -> { set(rotation, a, 0); return this; });
            case "setScale": return new JsFunction("setScale", a -> {
                if (a.length == 1) { float s = (float) arg(a, 0, 1); scale[0] = scale[1] = scale[2] = s; } else set(scale, a, 1);
                return this;
            });
            default:
                Object v = props.get(name);
                if (v != null) return v;
                Unimplemented.report("effect[" + type + "]." + name);
                Dummy d = new Dummy("effect[" + type + "]." + name);
                props.put(name, d);
                return d;
        }
    }

    private static void set(float[] target, Object[] a, double def) {
        for (int i = 0; i < 3; i++) target[i] = (float) arg(a, i, def);
    }

    @Override
    public void setMember(String name, Object value) {
        switch (name) {
            case "opacity" -> opacity = num(value);
            case "progress" -> progress = num(value);
            default -> props.put(name, value);
        }
    }

    @Override
    public boolean hasMember(String name) {
        return true;
    }

    @Override
    public Set<String> keySet() {
        return props.keySet();
    }

    private void safeRender() {
        RenderContext ctx = RenderContext.current;
        if (ctx == null) return;
        render(ctx);
    }

    /** Dibuja el efecto. Por defecto nada (efecto aún sin implementar). */
    protected void render(RenderContext ctx) {
        Unimplemented.report("effect[" + type + "].render()");
    }

    // ------------------------------------------------------------------ utilidades para subclases

    protected static ModelPart anchorPart(SuitModel<?> model, String anchor) {
        if (anchor == null) return null;
        return switch (anchor) {
            case "head", "headwear" -> model.head;
            case "body" -> model.body;
            case "rightArm" -> model.rightArm;
            case "leftArm" -> model.leftArm;
            case "rightLeg" -> model.rightLeg;
            case "leftLeg" -> model.leftLeg;
            default -> null;
        };
    }

    /** Aplica ancla + desplazamiento + rotación (escalada por "amount") + escala, como hace Fisk. */
    protected void applyTransform(PoseStack pose, SuitModel<?> model, float amount) {
        ModelPart part = anchor.ignore ? null : anchorPart(model, anchor.part);
        if (part != null) part.translateAndRotate(pose);
        pose.translate(offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
        if (rotation[2] != 0) pose.mulPose(Axis.ZP.rotationDegrees(rotation[2] * amount));
        if (rotation[1] != 0) pose.mulPose(Axis.YP.rotationDegrees(rotation[1] * amount));
        if (rotation[0] != 0) pose.mulPose(Axis.XP.rotationDegrees(rotation[0] * amount));
        pose.scale(scale[0], scale[1], scale[2]);
    }

    protected ResourceLocation tex(RenderContext ctx, String key) {
        return ClientHeroes.heroTexture(renderer.texturePath(key), ctx.jsEntity);
    }

    /** effect.texture.set(normal, lights) */
    public static final class TextureRef extends AbstractJSObject {
        public String normal;
        public String lights;

        @Override
        public Object getMember(String name) {
            if (name.equals("set")) return new JsFunction("set", a -> {
                normal = a.length > 0 && a[0] != null ? String.valueOf(a[0]) : null;
                lights = a.length > 1 && a[1] != null ? String.valueOf(a[1]) : null;
                return null;
            });
            if (name.equals("normal")) return normal;
            if (name.equals("lights")) return lights;
            return new Dummy("texture." + name);
        }
    }

    /** effect.anchor.set(parte) / effect.anchor.ignoreAnchor(bool) */
    public static final class AnchorRef extends AbstractJSObject {
        public String part;
        public boolean ignore;

        @Override
        public Object getMember(String name) {
            if (name.equals("set")) return new JsFunction("set", a -> { part = a.length > 0 ? String.valueOf(a[0]) : null; return null; });
            if (name.equals("ignoreAnchor")) return new JsFunction("ignoreAnchor", a -> { ignore = a.length == 0 || Boolean.TRUE.equals(a[0]); return null; });
            return new Dummy("anchor." + name);
        }
    }
}
