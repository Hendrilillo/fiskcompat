package com.hendrilillo.fiskcompat.client.model;

import com.hendrilillo.fiskcompat.client.effect.EffectObject;
import com.hendrilillo.fiskcompat.fsk.FskScript;
import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * renderer.createResource("MODEL", "ns:ruta"): un modelo Tabula con textura y animaciones .fsk
 * (model.bindAnimation("ns:anim").setData((entity, data) =&gt; data.load(...))).
 */
public final class ModelResource extends AbstractJSObject {
    private static final Map<String, Optional<FskScript>> ANIMATIONS = new ConcurrentHashMap<>();

    public final String modelId;
    public final EffectObject.TextureRef texture = new EffectObject.TextureRef();
    final List<Binding> bindings = new ArrayList<>();
    private final Map<String, Object> props = new HashMap<>();

    public ModelResource(String modelId) {
        this.modelId = modelId;
    }

    public static void clearCache() {
        ANIMATIONS.clear();
    }

    public TabulaModel model() {
        return TabulaModel.get(modelId);
    }

    @Override
    public Object getMember(String name) {
        switch (name) {
            case "texture": return texture;
            case "bindAnimation": return new JsFunction("bindAnimation", a -> {
                Binding b = new Binding(String.valueOf(a[0]));
                bindings.add(b);
                return b;
            });
            default:
                Object v = props.get(name);
                if (v != null) return v;
                Unimplemented.report("model." + name);
                return new Dummy("model." + name);
        }
    }

    @Override
    public void setMember(String name, Object value) {
        props.put(name, value);
    }

    static FskScript animation(String id) {
        return ANIMATIONS.computeIfAbsent(id, k -> {
            int c = k.indexOf(':');
            ResourceLocation rl = ResourceLocation.tryBuild(c < 0 ? "fiskheroes" : k.substring(0, c),
                    "models/animations/" + (c < 0 ? k : k.substring(c + 1)) + ".fsk");
            if (rl == null) return Optional.empty();
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return Optional.empty();
            try (InputStream in = res.get().open()) {
                return Optional.of(FskScript.compile(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (Exception e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** Aplica todas las animaciones al modelo para esta entidad. */
    public void animate(TabulaModel model, Object jsEntity, Entity entity, float partialTicks) {
        model.reset();
        for (Binding b : bindings) b.apply(model, jsEntity, entity, partialTicks);
    }

    /** Una animación enlazada con bindAnimation. */
    public static final class Binding extends AbstractJSObject {
        final String animId;
        Object dataFunc;
        Object condition;
        double priority;
        private final double[] data = new double[32];
        private FskScript script;
        private TabulaModel mappedFor;
        private int[] kind;          // 0 = nada, 1 = data, 2 = builtin, 3 = canal de parte
        private int[] index;
        private List<ModelPart>[] targets;

        Binding(String animId) {
            this.animId = animId;
        }

        @Override
        public Object getMember(String name) {
            return switch (name) {
                case "setData" -> new JsFunction("setData", a -> { dataFunc = a[0]; return this; });
                case "setCondition" -> new JsFunction("setCondition", a -> { condition = a[0]; return this; });
                case "priority" -> priority;
                default -> new Dummy("animation." + name);
            };
        }

        @Override
        public void setMember(String name, Object value) {
            if (name.equals("priority") && value instanceof Number n) priority = n.doubleValue();
        }

        private static final String[] BUILTINS = {"motX", "motY", "motZ", "lookX", "lookY", "lookZ", "limbSwing",
                "limbSwingAmount", "ticks", "bodyYaw", "yaw", "pitch"};
        private static final String[] CHANNELS = {"rotX", "rotY", "rotZ", "posX", "posY", "posZ"};

        @SuppressWarnings("unchecked")
        private void map(TabulaModel model) {
            int n = script.size();
            kind = new int[n];
            index = new int[n];
            targets = new List[n];
            List<String> names = script.names();
            for (int i = 0; i < n; i++) {
                String name = names.get(i);
                if (name.equals("data")) { kind[i] = 1; index[i] = 0; continue; }
                if (name.startsWith("data_")) {
                    try { kind[i] = 1; index[i] = Integer.parseInt(name.substring(5)); continue; } catch (NumberFormatException ignored) { kind[i] = 0; }
                }
                boolean done = false;
                for (int k = 0; k < BUILTINS.length; k++) if (BUILTINS[k].equals(name)) { kind[i] = 2; index[i] = k; done = true; }
                if (done) continue;
                int u = name.lastIndexOf('_');
                if (u > 0) {
                    String part = name.substring(0, u), ch = name.substring(u + 1);
                    for (int k = 0; k < CHANNELS.length; k++) {
                        if (CHANNELS[k].equals(ch) && model.parts.containsKey(part)) {
                            kind[i] = 3; index[i] = k; targets[i] = model.parts.get(part);
                        }
                    }
                }
            }
            mappedFor = model;
        }

        private static double get(ModelPart p, int ch) {
            return switch (ch) { case 0 -> p.xRot; case 1 -> p.yRot; case 2 -> p.zRot; case 3 -> p.x; case 4 -> p.y; default -> p.z; };
        }

        private static void set(ModelPart p, int ch, float v) {
            switch (ch) { case 0 -> p.xRot = v; case 1 -> p.yRot = v; case 2 -> p.zRot = v; case 3 -> p.x = v; case 4 -> p.y = v; default -> p.z = v; }
        }

        void apply(TabulaModel model, Object jsEntity, Entity entity, float pt) {
            if (script == null) script = animation(animId);
            if (script == null) return;
            if (condition instanceof ScriptObjectMirror c && c.isFunction()) {
                Object ok = c.call(null, jsEntity);
                if (!(ok instanceof Boolean bo && bo)) return;
            }
            if (mappedFor != model) map(model);
            java.util.Arrays.fill(data, 0);
            if (dataFunc instanceof ScriptObjectMirror f && f.isFunction()) {
                f.call(null, jsEntity, new DataLoader(data));
            }
            double[] builtins = builtins(entity, pt);
            double[] vars = new double[script.size()];
            for (int i = 0; i < vars.length; i++) {
                switch (kind[i]) {
                    case 1 -> vars[i] = index[i] < data.length ? data[index[i]] : 0;
                    case 2 -> vars[i] = builtins[index[i]];
                    case 3 -> vars[i] = get(targets[i].get(0), index[i]);
                    default -> vars[i] = 0;
                }
            }
            script.run(vars, null);
            for (int i = 0; i < vars.length; i++) {
                if (kind[i] == 3) for (ModelPart p : targets[i]) set(p, index[i], (float) vars[i]);
            }
        }

        /** Como builtins0 pero con la guiñada relativa al cuerpo acotada y, durmiendo, mirando al frente. */
    private static double[] builtins(net.minecraft.world.entity.Entity e, float pt) {
        double[] b = builtins0(e instanceof LivingEntity l ? l : null, e, pt);
        if (e instanceof LivingEntity l && l.isSleeping()) {
            b[10] = b[9];
            b[11] = 0;
        } else {
            b[10] = b[9] + Mth.wrapDegrees(b[10] - b[9]);
        }
        return b;
    }

    private static double[] builtins0(LivingEntity unused, Entity e, float pt) {
            Vec3 look = e.getViewVector(pt);
            double limbSwing = 0, limbAmount = 0, bodyYaw = e.getViewYRot(pt);
            if (e instanceof LivingEntity l) {
                limbSwing = l.walkAnimation.position(pt);
                limbAmount = l.walkAnimation.speed(pt);
                bodyYaw = Mth.rotLerp(pt, l.yBodyRotO, l.yBodyRot);
            }
            return new double[]{e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo, look.x, look.y, look.z,
                    limbSwing, limbAmount, e.tickCount + pt, bodyYaw, e.getViewYRot(pt), e.getViewXRot(pt)};
        }
    }

    /** El objeto "data" que reciben las funciones setData: data.load(valor) o data.load(índice, valor). */
    public static final class DataLoader extends AbstractJSObject {
        private final double[] data;

        public DataLoader(double[] data) {
            this.data = data;
        }

        @Override
        public Object getMember(String name) {
            if (name.equals("load") || name.equals("set")) return new JsFunction("load", a -> {
                if (a.length >= 2) {
                    int i = (int) EffectObject.num(a[0]);
                    if (i >= 0 && i < data.length) data[i] = EffectObject.num(a[1]);
                } else if (a.length == 1) {
                    data[0] = a[0] instanceof Boolean bo ? (bo ? 1 : 0) : EffectObject.num(a[0]);
                }
                return null;
            });
            return new Dummy("data." + name);
        }
    }
}
