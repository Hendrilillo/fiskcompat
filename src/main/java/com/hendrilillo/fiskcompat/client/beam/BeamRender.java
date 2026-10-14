package com.hendrilillo.fiskcompat.client.beam;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.client.SuitModel;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.BeamCast;
import com.hendrilillo.fiskcompat.power.ModifierEntry;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dibuja los rayos de Fisk (unibeam, rayos de energía, láseres) y los disparos de repulsor.
 * <p>
 * Mientras se dibuja cada traje (SuitLayer) se apunta dónde está cada punto de salida del rayo
 * (ancla + desplazamiento, como en Fisk). Después de dibujar todas las entidades se trazan los rayos
 * desde esos puntos hasta donde mira la entidad. En primera persona se usan los puntos "firstPerson".
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class BeamRender {
    public static final String CHARGED_BEAM = "fiskheroes:charged_beam", ENERGY_PROJECTION = "fiskheroes:energy_projection",
            HEAT_VISION = "fiskheroes:heat_vision", REPULSOR = "fiskheroes:repulsor_blast", LIGHTNING = "fiskheroes:lightning_cast";

    /** Punto de salida de un rayo, relativo a la cámara. */
    record Start(PropertyBinding prop, BeamConstellation.Entry entry, Vector3f pos) {}

    private static final Map<LivingEntity, List<Start>> CAPTURED = new WeakHashMap<>();
    /** Última posición (en el mundo) de la mano del repulsor de cada entidad. */
    private static final Map<LivingEntity, Vec3> REPULSOR_HAND = new WeakHashMap<>();
    private static boolean inWorld;

    private BeamRender() {}

    static {
        Net.onRepulsorShot = BeamRender::onRepulsorShot;
        Net.onLightningCast = BeamRender::onLightning;
    }

    public static void init() {
        // Fuerza la carga de la clase (y el registro del manejador de disparos).
    }

    // ------------------------------------------------------------------ tipo de render

    static final class Types extends RenderType {
        private Types() {
            super("fiskcompat_dummy", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
        }

        /** Aditivo, sin escribir profundidad y visible por las dos caras: como el "bloom" de Fisk. */
        static final RenderType BEAM = create("fiskcompat_beam", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                        .setWriteMaskState(COLOR_WRITE)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    // ------------------------------------------------------------------ captura de puntos

    private static String timerVar(String type) {
        return switch (type) {
            case CHARGED_BEAM -> "fiskheroes:beam_shooting_timer";
            case ENERGY_PROJECTION -> "fiskheroes:energy_projection_timer";
            case HEAT_VISION -> "fiskheroes:heat_vision_timer";
            case REPULSOR -> "fiskheroes:aiming_timer";
            case LIGHTNING -> "";
            default -> null;
        };
    }

    static float timer(Entity e, String type, float pt) {
        if (type.equals(LIGHTNING)) return flashAlpha(e, pt);
        String var = timerVar(type);
        if (var == null) return 0;
        Object v = DataStore.getInterpolated(e, var, pt);
        return v instanceof Number n ? n.floatValue() : 0;
    }

    static ModelPart part(SuitModel<?> model, String anchor) {
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

    /** Llamado desde SuitLayer, con la pose del modelo de la entidad ya colocada. */
    /** ¿Se está dibujando el mundo (y no un inventario)? */
    public static boolean inWorld() {
        return inWorld;
    }

    public static void capture(LivingEntity e, JSEntity js, HeroRendererScript r, SuitModel<?> model, PoseStack pose, float pt) {
        if (!inWorld || CAPTURED.containsKey(e)) return;
        List<Start> list = new ArrayList<>();
        for (Object o : r.properties()) {
            if (!(o instanceof PropertyBinding pb) || pb.constellation == null || timerVar(pb.type) == null) continue;
            if (timer(e, pb.type, pt) <= 0 || !pb.test(js)) continue;
            for (BeamConstellation.Entry entry : pb.constellation.entries) {
                pose.pushPose();
                ModelPart p = part(model, entry.anchor() != null ? entry.anchor() : pb.anchor);
                if (p != null) p.translateAndRotate(pose);
                if (entry.offset() != null) pose.translate(entry.offset()[0] / 16f, entry.offset()[1] / 16f, entry.offset()[2] / 16f);
                Vector3f pos = pose.last().pose().transformPosition(0, 0, 0, new Vector3f());
                pose.popPose();
                list.add(new Start(pb, entry, pos));
            }
        }
        CAPTURED.put(e, list);
    }

    /** Puntos de salida en primera persona (relativos a la cámara). */
    private static List<Start> firstPerson(LivingEntity e, JSEntity js, HeroRendererScript r, Camera cam, float pt) {
        List<Start> list = new ArrayList<>();
        for (Object o : r.properties()) {
            if (!(o instanceof PropertyBinding pb) || pb.constellation == null || timerVar(pb.type) == null) continue;
            if (timer(e, pb.type, pt) <= 0 || !pb.test(js)) continue;
            for (BeamConstellation.Entry entry : pb.constellation.entries) {
                float[] fp = entry.firstPerson();
                if (fp == null) continue;
                // Espacio de Fisk: x hacia la izquierda del jugador, y hacia abajo, z hacia atrás
                Vector3f v = new Vector3f(-fp[0] / 16f, -fp[1] / 16f, fp[2] / 16f);
                cam.rotation().transform(v);
                list.add(new Start(pb, entry, v));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ dibujo

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            CAPTURED.clear();
            inWorld = true;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        inWorld = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        double ticks = mc.level.getGameTime() + pt;

        Map<LivingEntity, List<Start>> all = new IdentityHashMap<>(CAPTURED);
        LivingEntity self = mc.player;
        if (self != null && !all.containsKey(self) && mc.options.getCameraType().isFirstPerson() && cam.getEntity() == self) {
            ActiveHero hero = ActiveHero.of(self);
            HeroRendererScript r = hero != null ? ClientHeroes.renderer(hero.suitId) : null;
            if (r != null) all.put(self, firstPerson(self, JSEntity.of(self), r, cam, pt));
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(Types.BEAM);
        boolean drew = false;
        for (Map.Entry<LivingEntity, List<Start>> en : all.entrySet()) {
            LivingEntity e = en.getKey();
            ActiveHero hero = ActiveHero.of(e);
            if (hero == null) continue;
            JSEntity js = JSEntity.of(e);
            Map<String, Vector3f> ends = new java.util.HashMap<>();
            for (Start s : en.getValue()) {
                String type = s.prop.type;
                if (type.equals(REPULSOR)) {
                    REPULSOR_HAND.put(e, new Vec3(s.pos.x + camPos.x, s.pos.y + camPos.y, s.pos.z + camPos.z));
                    continue;
                }
                if (type.equals(LIGHTNING)) {
                    drew |= drawFlashes(vc, e, s, camPos, pt, ticks);
                    continue;
                }
                if (!hero.hasModifier(type)) continue;
                float t = timer(e, type, pt);
                Vector3f end = ends.computeIfAbsent(type, k -> {
                    ModifierEntry m = hero.enabledModifier(js, k);
                    double scale = DataStore.get(e, "fiskheroes:scale") instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : 1;
                    double range = (m != null ? m.num("range", 16) : 16) * scale;
                    double radius = (m != null ? m.num("radius", 0.1) : 0.1) * scale;
                    Vec3 hit = BeamCast.fromEyes(e, range, radius, pt).end();
                    return new Vector3f((float) (hit.x - camPos.x), (float) (hit.y - camPos.y), (float) (hit.z - camPos.z));
                });
                float[] size = s.entry.size() != null ? s.entry.size() : new float[]{1, 1};
                drawBeam(vc, s.pos, end, size[0] / 16f * t, size[1] / 16f * t, s.prop.beam, s.prop.color.rgb, 1, ticks);
                drew = true;
            }
        }
        drew |= drawShots(vc, camPos, pt, ticks);
        if (drew) buffers.endBatch(Types.BEAM);
    }

    /**
     * Un rayo de a hasta b (relativos a la cámara) de ancho w x h bloques, con núcleo y resplandor
     * según el JSON del rayo. Se dibuja como cintas que miran a la cámara, con el brillo cayendo
     * suavemente hacia los bordes.
     */
    static void drawBeam(VertexConsumer vc, Vector3f a, Vector3f b, float w, float h, BeamRendererDef def, int color, float alpha, double ticks) {
        if (w <= 0 && h <= 0) return;
        Vector3f axis = new Vector3f(b).sub(a);
        if (axis.lengthSquared() < 1e-6f) return;
        axis.normalize();
        if (def == null) def = BeamRendererDef.get("fiskheroes:energy_projection");
        float half = (w + h) / 4;
        for (BeamRendererDef.Aspect asp : def.aspects) {
            float sc = asp.scale();
            if (asp.pulse() != null) {
                sc *= 1 + asp.pulse().size() * (float) Math.sin(ticks * asp.pulse().frequency() * Math.PI);
            }
            float hw = Math.max(half * sc, 0.004f);
            float op = asp.opacity() * alpha;
            if ("LINE".equals(asp.type())) {
                ribbon(vc, a, b, axis, 0.01f, color, op, 0);
                continue;
            }
            BeamRendererDef.Bloom bloom = asp.bloom();
            if (bloom != null && bloom.strength() > 0) {
                float extra = bloom.scaleToSize() ? bloom.spread() * hw * 0.6f : bloom.spread() / 16f * 0.25f;
                ribbon(vc, a, b, axis, hw + extra, color, Math.min(1, bloom.strength() * op), Math.max(0.5f, bloom.dropoff()));
            }
            if (asp.coreColor() >= 0) ribbon(vc, a, b, axis, hw, asp.coreColor(), op, 0.15f);
        }
    }

    private static final int STEPS = 6;

    /** Cinta de ancho 2*hw orientada hacia la cámara; alfa = op * (1-|t|)^dropoff a lo ancho. */
    private static void ribbon(VertexConsumer vc, Vector3f a, Vector3f b, Vector3f axis, float hw, int rgb, float op, float dropoff) {
        if (op <= 0.003f) return;
        Vector3f sa = side(axis, a), sb = side(axis, b);
        int cr = rgb >> 16 & 255, cg = rgb >> 8 & 255, cb = rgb & 255;
        for (int i = -STEPS; i < STEPS; i++) {
            float t0 = i / (float) STEPS, t1 = (i + 1) / (float) STEPS;
            int a0 = (int) (255 * op * Math.pow(1 - Math.abs(t0), dropoff));
            int a1 = (int) (255 * op * Math.pow(1 - Math.abs(t1), dropoff));
            if (a0 <= 0 && a1 <= 0) continue;
            vertex(vc, a, sa, t0 * hw, cr, cg, cb, a0);
            vertex(vc, a, sa, t1 * hw, cr, cg, cb, a1);
            vertex(vc, b, sb, t1 * hw, cr, cg, cb, a1);
            vertex(vc, b, sb, t0 * hw, cr, cg, cb, a0);
        }
    }

    private static void vertex(VertexConsumer vc, Vector3f p, Vector3f side, float d, int r, int g, int b, int a) {
        vc.addVertex(p.x + side.x * d, p.y + side.y * d, p.z + side.z * d).setColor(r, g, b, Math.min(255, a));
    }

    /** Dirección perpendicular al rayo y a la línea de visión (la cámara está en el origen). */
    private static Vector3f side(Vector3f axis, Vector3f p) {
        Vector3f s = new Vector3f(axis).cross(-p.x, -p.y, -p.z);
        if (s.lengthSquared() < 1e-8f) s = new Vector3f(axis).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-8f) s.set(1, 0, 0);
        return s.normalize();
    }

    // ------------------------------------------------------------------ lightning_cast (cohetes del brazo...)

    private static final int FLASH_TICKS = 6;

    private static final class Flash {
        final LivingEntity shooter;
        final Vec3 end;
        final List<Vec3> chain;
        int age, slow;

        Flash(LivingEntity shooter, Vec3 end, List<Vec3> chain) {
            this.shooter = shooter;
            this.end = end;
            this.chain = chain;
        }
    }

    private static final List<Flash> FLASHES = new ArrayList<>();

    static float flashAlpha(Entity e, float pt) {
        float best = 0;
        for (Flash f : FLASHES) if (f.shooter == e) best = Math.max(best, 1 - (f.age + pt) / FLASH_TICKS);
        return best;
    }

    private static void onLightning(Net.LightningCast m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(m.shooter()) instanceof LivingEntity le)) return;
        List<Vec3> chain = new ArrayList<>();
        for (int i = 0; i + 2 < m.chain().length; i += 3) chain.add(new Vec3(m.chain()[i], m.chain()[i + 1], m.chain()[i + 2]));
        FLASHES.add(new Flash(le, new Vec3(m.x(), m.y(), m.z()), chain));
        if (le == mc.player) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private static boolean drawFlashes(VertexConsumer vc, LivingEntity e, Start s, Vec3 camPos, float pt, double ticks) {
        boolean drew = false;
        float[] size = s.entry.size() != null ? s.entry.size() : new float[]{1, 1};
        for (Flash f : FLASHES) {
            if (f.shooter != e) continue;
            float alpha = Math.max(0, 1 - (f.age + pt) / FLASH_TICKS);
            if (alpha <= 0) continue;
            Vector3f end = rel(f.end, camPos);
            drawBeam(vc, s.pos, end, size[0] / 16f, size[1] / 16f, s.prop.beam, s.prop.color.rgb, alpha, ticks);
            for (Vec3 c : f.chain) drawBeam(vc, end, rel(c, camPos), size[0] / 16f, size[1] / 16f, s.prop.beam, s.prop.color.rgb, alpha, ticks);
            drew = true;
        }
        return drew;
    }

    private static Vector3f rel(Vec3 v, Vec3 camPos) {
        return new Vector3f((float) (v.x - camPos.x), (float) (v.y - camPos.y), (float) (v.z - camPos.z));
    }

    // ------------------------------------------------------------------ disparos de repulsor

    private static final class Shot {
        final Vec3 origin, dir, hand, end;
        final double speed, dist;
        final float w, h;
        final int color;
        final BeamRendererDef def;
        int age, slow;

        Shot(Vec3 origin, Vec3 dir, Vec3 hand, Vec3 end, double speed, double dist, float w, float h, int color, BeamRendererDef def) {
            this.origin = origin;
            this.dir = dir;
            this.hand = hand;
            this.end = end;
            this.speed = speed;
            this.dist = dist;
            this.w = w;
            this.h = h;
            this.color = color;
            this.def = def;
        }
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final int TRAIL = 3;

    private static void onRepulsorShot(Net.RepulsorShot m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity shooter = mc.level.getEntity(m.shooter());
        Vec3 origin = new Vec3(m.x(), m.y(), m.z());
        Vec3 dir = new Vec3(m.dx(), m.dy(), m.dz());
        BeamCast.Hit hit = BeamCast.cast(mc.level, shooter, origin, dir, m.range(), m.radius());
        double dist = hit.distance(origin);

        int color = 0xFFC462;
        BeamRendererDef def = null;
        float w = 1.5f / 16, h = 1.5f / 16;
        Vec3 hand = null;
        float[] fp = {-4.5f, 3.75f, -7f};
        if (shooter instanceof LivingEntity le) {
            ActiveHero hero = ActiveHero.of(le);
            HeroRendererScript r = hero != null ? ClientHeroes.renderer(hero.suitId) : null;
            if (r != null) {
                JSEntity js = JSEntity.of(le);
                for (Object o : r.properties()) {
                    if (o instanceof PropertyBinding pb && pb.type.equals(REPULSOR) && pb.test(js)) {
                        color = pb.color.rgb;
                        def = pb.beam;
                        if (pb.constellation != null && !pb.constellation.entries.isEmpty()) {
                            BeamConstellation.Entry en = pb.constellation.entries.get(0);
                            if (en.size() != null) {
                                w = en.size()[0] / 16f;
                                h = en.size()[1] / 16f;
                            }
                            if (en.firstPerson() != null) fp = en.firstPerson();
                        }
                        break;
                    }
                }
            }
            if (le == mc.player && mc.options.getCameraType().isFirstPerson()) {
                Camera cam = mc.gameRenderer.getMainCamera();
                Vector3f v = new Vector3f(-fp[0] / 16f, -fp[1] / 16f, fp[2] / 16f);
                cam.rotation().transform(v);
                hand = cam.getPosition().add(v.x, v.y, v.z);
            } else {
                hand = REPULSOR_HAND.get(le);
                if (hand == null || hand.distanceTo(origin) > 4) {
                    // Sin punto definido: la mano derecha extendida hacia delante
                    double yaw = Math.toRadians(le.getYRot());
                    Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                    hand = origin.add(right.scale(0.35)).add(0, -0.3, 0).add(dir.scale(0.6));
                }
            }
        }
        if (def == null) def = BeamRendererDef.get("fiskheroes:repulsor_blast");
        SHOTS.add(new Shot(origin, dir, hand != null ? hand : origin, hit.end(), m.speed(), dist, w, h, color, def));
        if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED) {
            FiskCompat.LOGGER.info("[autotest] repulsor shot t={} dist={} hand={} color={}", mc.level.getGameTime(), dist, hand, Integer.toHexString(color));
        }
    }

    private static boolean drawShots(VertexConsumer vc, Vec3 camPos, float pt, double ticks) {
        boolean drew = false;
        for (Shot s : SHOTS) {
            double t = s.age + pt;
            double head = Math.min(s.dist, t * s.speed), tail = Math.max(0, (t - TRAIL) * s.speed);
            if (tail >= s.dist || s.dist <= 0) continue;
            Vec3 a = s.hand.lerp(s.end, tail / s.dist), b = s.hand.lerp(s.end, head / s.dist);
            Vector3f va = new Vector3f((float) (a.x - camPos.x), (float) (a.y - camPos.y), (float) (a.z - camPos.z));
            Vector3f vb = new Vector3f((float) (b.x - camPos.x), (float) (b.y - camPos.y), (float) (b.z - camPos.z));
            drawBeam(vc, va, vb, s.w, s.h, s.def, s.color, 1, ticks);
            drew = true;

        }
        return drew;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        for (Iterator<Shot> it = SHOTS.iterator(); it.hasNext(); ) {
            Shot s = it.next();
            // En la prueba automática (pocos FPS) los disparos van a cámara lenta para poder capturarlos
            if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED && ++s.slow % 8 != 0) continue;
            s.age++;
            if ((s.age - TRAIL) * s.speed >= s.dist || s.age > 200) it.remove();
        }
        for (Iterator<Flash> it = FLASHES.iterator(); it.hasNext(); ) {
            Flash f = it.next();
            if (com.hendrilillo.fiskcompat.dev.AutoTest.ENABLED && ++f.slow % 8 != 0) continue;
            if (++f.age >= FLASH_TICKS || f.shooter.isRemoved()) it.remove();
        }
        if (Minecraft.getInstance().level == null) {
            SHOTS.clear();
            FLASHES.clear();
        }
    }
}
