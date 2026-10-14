package com.hendrilillo.fiskcompat.client.anim;

import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.client.model.ModelResource;
import com.hendrilillo.fiskcompat.fsk.FskScript;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;
import java.util.List;

/**
 * Aplica las animaciones del cuerpo (addCustomAnimation) de un héroe: cambia la pose de las partes
 * del modelo y gira/mueve el cuerpo entero (salidas "rotate", "translate", "pivot").
 */
public final class BodyAnimator {
    private static final String[] PARTS = {"head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"};
    private static final String[] CHANNELS = {"rotX", "rotY", "rotZ", "posX", "posY", "posZ"};
    private static final String[] BUILTINS = {"motX", "motY", "motZ", "lookX", "lookY", "lookZ", "limbSwing",
            "limbSwingAmount", "ticks", "bodyYaw", "yaw", "pitch"};

    private BodyAnimator() {}

    private static ModelPart part(HumanoidModel<?> m, int i) {
        return switch (i) { case 0 -> m.head; case 1 -> m.body; case 2 -> m.rightArm; case 3 -> m.leftArm; case 4 -> m.rightLeg; default -> m.leftLeg; };
    }

    private static double get(ModelPart p, int ch) {
        return switch (ch) { case 0 -> p.xRot; case 1 -> p.yRot; case 2 -> p.zRot; case 3 -> p.x; case 4 -> p.y; default -> p.z; };
    }

    private static void set(ModelPart p, int ch, float v) {
        switch (ch) { case 0 -> p.xRot = v; case 1 -> p.yRot = v; case 2 -> p.zRot = v; case 3 -> p.x = v; case 4 -> p.y = v; default -> p.z = v; }
    }

    /** Se llama justo después de setupAnim del modelo de la entidad. */
    public static void apply(LivingEntity entity, HumanoidModel<?> model, PoseStack pose, float pt) {
        List<FskScript.Output> outputs = run(entity, model, pt, builtins(entity, pt));
        if (outputs == null) return;
        model.hat.copyFrom(model.head);
        if (model instanceof PlayerModel<?> pm) {
            pm.jacket.copyFrom(pm.body);
            pm.leftSleeve.copyFrom(pm.leftArm);
            pm.rightSleeve.copyFrom(pm.rightArm);
            pm.leftPants.copyFrom(pm.leftLeg);
            pm.rightPants.copyFrom(pm.rightLeg);
        }
        if (outputs.stream().allMatch(o -> o.channel().equals("firstPerson"))) return;

        // Las salidas están en el espacio del cuerpo antes del volteo de Minecraft (y hacia arriba, en bloques).
        float scale = 0.9375f;
        pose.translate(0, 1.501f, 0);
        pose.scale(1 / scale, 1 / scale, 1 / scale);
        pose.scale(-1, -1, 1);
        applyOutputs(outputs, pose, false);
        pose.scale(-1, -1, 1);
        pose.scale(scale, scale, scale);
        pose.translate(0, -1.501f, 0);
    }

    /**
     * Primera persona (brazo con la mano vacía): como Fisk, solo se aplican las salidas marcadas con
     * out "firstPerson" (true), sobre la pose del brazo que prepara Minecraft. Las partes no se mueven.
     */
    public static void applyFirstPerson(LivingEntity entity, PoseStack pose, float pt) {
        Vec3 look = entity.getViewVector(pt);
        double[] b = {entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo, look.x, look.y, look.z,
                0, 0, entity.tickCount + pt, 0, 0, 0};
        List<FskScript.Output> outputs = run(entity, null, pt, b);
        if (outputs != null) applyOutputs(outputs, pose, true);
    }

    /** Ejecuta las animaciones del héroe (por prioridad). Con modelo, mueve sus partes. Null si no hay. */
    private static List<FskScript.Output> run(LivingEntity entity, HumanoidModel<?> model, float pt, double[] builtins) {
        if (!(entity instanceof Player) || ActiveHero.of(entity) == null) return null;
        ActiveHero hero = ActiveHero.of(entity);
        HeroRendererScript r = ClientHeroes.renderer(hero.suitId);
        if (r == null || r.customAnimations().isEmpty()) return null;
        JSEntity.partialTicks = pt;
        JSEntity js = JSEntity.of(entity);

        List<BipedAnimation> anims = new ArrayList<>();
        for (Object o : r.customAnimations().values()) if (o instanceof BipedAnimation b) anims.add(b);
        anims.sort((a, b) -> Double.compare(a.priority, b.priority));

        List<FskScript.Output> outputs = new ArrayList<>();
        for (BipedAnimation a : anims) {
            FskScript s = a.script();
            if (s == null) continue;
            try {
                if (a.condition instanceof ScriptObjectMirror c && c.isFunction()) {
                    Object ok = c.call(null, js);
                    if (!(ok instanceof Boolean b && b)) continue;
                }
                double[] data = new double[32];
                if (a.dataFunc instanceof ScriptObjectMirror f && f.isFunction()) f.call(null, js, new ModelResource.DataLoader(data));
                double[] vars = new double[s.size()];
                List<String> names = s.names();
                int[] map = new int[vars.length];
                for (int i = 0; i < vars.length; i++) {
                    String n = names.get(i);
                    map[i] = -1;
                    if (n.equals("data")) { vars[i] = data[0]; continue; }
                    if (n.startsWith("data_")) { try { vars[i] = data[Integer.parseInt(n.substring(5))]; } catch (Exception ignored) { } continue; }
                    boolean done = false;
                    for (int k = 0; k < BUILTINS.length && !done; k++) if (BUILTINS[k].equals(n)) { vars[i] = builtins[k]; done = true; }
                    if (done) continue;
                    int u = n.lastIndexOf('_');
                    if (u > 0) {
                        String pn = n.substring(0, u), ch = n.substring(u + 1);
                        for (int p = 0; p < PARTS.length; p++) {
                            if (!PARTS[p].equals(pn)) continue;
                            for (int c = 0; c < CHANNELS.length; c++) {
                                if (CHANNELS[c].equals(ch)) {
                                    map[i] = p * 6 + c;
                                    vars[i] = model != null ? get(part(model, p), c) : 0;
                                }
                            }
                        }
                    }
                }
                outputs.add(new FskScript.Output("firstPerson", new double[]{0})); // cada animación empieza en tercera persona
                s.run(vars, outputs);
                if (model != null) {
                    for (int i = 0; i < vars.length; i++) {
                        if (map[i] >= 0) set(part(model, map[i] / 6), map[i] % 6, (float) vars[i]);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return outputs;
    }

    /** Aplica las salidas de la vista indicada (como Fisk: el pivote va en fracciones de 0.6 x 1.8 x 0.6). */
    private static void applyOutputs(List<FskScript.Output> outputs, PoseStack pose, boolean firstPersonView) {
        double[] pivot = null;
        boolean firstPerson = false;
        for (FskScript.Output o : outputs) {
            double[] a = o.args();
            if (o.channel().equals("firstPerson")) {
                firstPerson = a.length > 0 && a[0] == 1;
                continue;
            }
            if (firstPerson != firstPersonView) continue;
            switch (o.channel()) {
                case "pivot" -> pivot = new double[]{a.length > 0 ? a[0] : 0, a.length > 1 ? a[1] : 0, a.length > 2 ? a[2] : 0};
                case "translate" -> { if (a.length >= 3) pose.translate(a[0], a[1], a[2]); }
                case "rotate" -> {
                    if (a.length >= 4 && a[0] != 0 && (a[1] != 0 || a[2] != 0 || a[3] != 0)) {
                        if (pivot != null) pose.translate(pivot[0] * 0.6, pivot[1] * 1.8, pivot[2] * 0.6);
                        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(a[0]), (float) a[1], (float) a[2], (float) a[3]));
                        if (pivot != null) pose.translate(-pivot[0] * 0.6, -pivot[1] * 1.8, -pivot[2] * 0.6);
                    }
                }
                default -> { }
            }
        }
    }

    /** Como builtins0 pero con la guiñada relativa al cuerpo acotada y, durmiendo, mirando al frente. */
    private static double[] builtins(net.minecraft.world.entity.Entity e, float pt) {
        double[] b = builtins0(null, (LivingEntity) e, pt);
        if (e instanceof LivingEntity l && l.isSleeping()) {
            b[10] = b[9];
            b[11] = 0;
        } else {
            b[10] = b[9] + Mth.wrapDegrees(b[10] - b[9]);
        }
        return b;
    }

    private static double[] builtins0(LivingEntity unused, LivingEntity e, float pt) {
        Vec3 look = e.getViewVector(pt);
        return new double[]{e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo, look.x, look.y, look.z,
                e.walkAnimation.position(pt), e.walkAnimation.speed(pt), e.tickCount + pt,
                Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot), e.getViewYRot(pt), e.getViewXRot(pt)};
    }
}
