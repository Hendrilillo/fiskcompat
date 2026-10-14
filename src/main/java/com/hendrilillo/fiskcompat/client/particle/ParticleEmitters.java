package com.hendrilillo.fiskcompat.client.particle;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.client.ClientHeroes;
import com.hendrilillo.fiskcompat.client.beam.PropertyBinding;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.dev.AutoTest;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.item.ModParticles;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import com.hendrilillo.fiskcompat.util.EntityMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/**
 * La propiedad "fiskheroes:particles" de los renderers: cada tick, cada emisor activo lanza sus partículas
 * desde la parte del cuerpo indicada (el humo de las botas de Iron Man al volar, por ejemplo).
 */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class ParticleEmitters {
    private static final String[] PARTS = {"head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"};

    /** Matrices de cada parte del cuerpo en el último fotograma (relativas a la posición de la entidad). */
    private record Frames(Matrix4f[] parts, long tick) {}

    private static final EntityMap<Entity, Frames> FRAMES = new EntityMap<>();

    private ParticleEmitters() {}

    /** Lo llama la capa del traje al dibujar la entidad en el mundo. */
    public static void capture(LivingEntity e, HumanoidModel<?> model, PoseStack pose, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        Matrix4f[] m = new Matrix4f[PARTS.length];
        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        for (int i = 0; i < parts.length; i++) {
            pose.pushPose();
            parts[i].translateAndRotate(pose);
            m[i] = new Matrix4f(pose.last().pose()).translateLocal((float) (cam.x - ex), (float) (cam.y - ey), (float) (cam.z - ez));
            pose.popPose();
        }
        FRAMES.put(e, new Frames(m, mc.level.getGameTime()));
    }

    /** Para las pruebas: dónde quedaría un punto de una parte, relativo a la entidad. */
    public static Vector3f debugPoint(Entity e, String anchor, float x, float y, float z) {
        Frames f = FRAMES.get(e);
        int i = partIndex(anchor);
        return f == null || i < 0 ? null : f.parts()[i].transformPosition(x, y, z, new Vector3f());
    }

    private static int partIndex(String anchor) {
        if (anchor == null) return -1;
        for (int i = 0; i < PARTS.length; i++) if (PARTS[i].equals(anchor)) return i;
        return -1;
    }

    private static String opposite(String anchor) {
        if (anchor == null) return null;
        return switch (anchor) {
            case "rightArm" -> "leftArm";
            case "leftArm" -> "rightArm";
            case "rightLeg" -> "leftLeg";
            case "leftLeg" -> "rightLeg";
            default -> anchor;
        };
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity e) || !e.isAlive()) continue;
            ActiveHero hero = ActiveHero.of(e);
            if (hero == null) continue;
            HeroRendererScript r = ClientHeroes.renderer(hero.suitId);
            if (r == null) continue;
            JSEntity js = null;
            for (Object o : r.properties()) {
                if (!(o instanceof PropertyBinding pb) || !pb.type.equals("fiskheroes:particles") || !(pb.particles instanceof ParticleEmitterDef.Ref ref)) continue;
                ParticleEmitterDef def = ref.get();
                if (def == null) continue;
                if (js == null) js = JSEntity.of(e);
                JSEntity.partialTicks = 1;
                if (!pb.test(js)) continue;
                try {
                    emit(mc, e, js, def, now);
                } catch (Throwable t) {
                    if (AutoTest.ENABLED) FiskCompat.LOGGER.error("[autotest] partículas", t);
                }
            }
        }
    }

    private static void emit(Minecraft mc, LivingEntity e, JSEntity js, ParticleEmitterDef def, long now) {
        boolean self = e == mc.player;
        boolean fp = self && mc.options.getCameraType().isFirstPerson();
        float scale = DataStore.get(e, "fiskheroes:scale") instanceof Number n && n.floatValue() > 0 ? n.floatValue() : 1;
        if (fp && scale < 1) return;
        Frames frames = null;
        if (!fp) {
            frames = FRAMES.get(e);
            if (frames == null || now - frames.tick() > 20) return; // no se ha dibujado hace poco
        }
        for (ParticleEmitterDef.Emitter em : def.emitters) {
            if (!em.condition.test(js, fp)) continue;
            int amount = em.amount.get(fp);
            int sides = em.mirror.test(js, fp) ? 2 : 1;
            float[] off = em.offset.get(fp), mot = em.motion.get(fp), rOff = em.randOffset.get(fp), rMot = em.randMotion.get(fp);
            float yaw = e.yBodyRot;
            String anchor = em.anchor;
            Kind kind = kind(em.type);
            if (kind == null) continue;
            float unit = anchor != null ? 1 / 16f : scale;
            double ox = off[0] * unit, oy = off[1] * unit, oz = off[2] * unit;
            double mx = mot[0], my = mot[1], mz = mot[2];
            if (fp) {
                oy = -oy;
                oz = -oz;
                if (em.lockFPYaw.test(js, fp)) yaw = e.getYRot();
            }
            for (int side = 0; side < sides; side++) {
                float scalem = 1;
                if (!kind.rgb) {
                    mx = -mx;
                    scalem = scale;
                }
                ox = -ox;
                Vec3 p;
                if (fp) {
                    // primera persona: desde los ojos, girado con el cuerpo (en Fisk se mide en bloques/16 * escala)
                    double k = anchor != null ? scale : 1;
                    p = new Vec3(e.getX(), e.getEyeY(), e.getZ()).add(new Vec3(ox * k, oy * k, oz * k).yRot((float) Math.toRadians(-yaw)));
                } else {
                    p = thirdPerson(e, frames, anchor, ox, oy, oz, yaw, scale);
                }
                Vec3 m1 = kind.rgb ? new Vec3(mx, my, mz) : new Vec3(mx, my, mz).yRot((float) Math.toRadians(-yaw));
                for (int i = 0; i < amount; i++) {
                    spawn(mc, kind, em, p.x + rnd() * rOff[0] * scale, p.y + rnd() * rOff[1] * scale, p.z + rnd() * rOff[2] * scale,
                            (m1.x + rnd() * rMot[0]) * scalem, (m1.y + rnd() * rMot[1]) * scalem, (m1.z + rnd() * rMot[2]) * scalem, scale);
                }
                anchor = opposite(anchor);
            }
        }
    }

    private static double rnd() {
        return Math.random() * 2 - 1;
    }

    private static Vec3 thirdPerson(LivingEntity e, Frames frames, String anchor, double ox, double oy, double oz, float yaw, float scale) {
        int i = partIndex(anchor);
        if (i >= 0) {
            // espacio de la parte del modelo (como el de Fisk: y hacia abajo)
            Vector3f v = frames.parts()[i].transformPosition((float) ox, (float) oy, (float) oz, new Vector3f());
            return e.position().add(v.x, v.y, v.z);
        }
        // sin ancla: respecto al centro del modelo, a 1.41 bloques del suelo (escalado), girado con el cuerpo
        Vec3 v = new Vec3(ox, -oy, -oz).yRot((float) Math.toRadians(-yaw));
        return e.position().add(0, 1.41 * e.getBbHeight() / 1.8, 0).add(v);
    }

    // ------------------------------------------------------------------ tipos

    private record Kind(ParticleOptions options, boolean rgb, String legacy) {}

    private static final Map<String, Kind> KINDS = new java.util.HashMap<>();

    private static Kind kind(String type) {
        return KINDS.computeIfAbsent(type, ParticleEmitters::resolve);
    }

    private static Kind resolve(String type) {
        if (type.startsWith("minecraft:")) {
            String n = type.substring(10);
            if (n.equals("reddust")) return new Kind(null, true, n);
            if (n.matches("^mobSpell(?:|Ambient)(_(\\d*\\.\\d+|\\d+))?$")) return new Kind(null, true, n);
            ParticleOptions o = switch (n) {
                case "largesmoke" -> ParticleTypes.LARGE_SMOKE;
                case "smoke" -> ParticleTypes.SMOKE;
                case "bubble" -> ParticleTypes.BUBBLE;
                case "largeexplode" -> ParticleTypes.EXPLOSION;
                case "hugeexplosion" -> ParticleTypes.EXPLOSION_EMITTER;
                case "explode" -> ParticleTypes.POOF;
                case "crit" -> ParticleTypes.CRIT;
                case "magicCrit" -> ParticleTypes.ENCHANTED_HIT;
                case "flame" -> ParticleTypes.FLAME;
                case "lava" -> ParticleTypes.LAVA;
                case "cloud" -> ParticleTypes.CLOUD;
                case "fireworksSpark" -> ParticleTypes.FIREWORK;
                case "spell" -> ParticleTypes.EFFECT;
                case "instantSpell" -> ParticleTypes.INSTANT_EFFECT;
                case "witchMagic" -> ParticleTypes.WITCH;
                case "note" -> ParticleTypes.NOTE;
                case "portal" -> ParticleTypes.PORTAL;
                case "enchantmenttable" -> ParticleTypes.ENCHANT;
                case "splash" -> ParticleTypes.SPLASH;
                case "dripWater" -> ParticleTypes.DRIPPING_WATER;
                case "dripLava" -> ParticleTypes.DRIPPING_LAVA;
                case "snowballpoof" -> ParticleTypes.ITEM_SNOWBALL;
                case "slime" -> ParticleTypes.ITEM_SLIME;
                case "heart" -> ParticleTypes.HEART;
                case "happyVillager" -> ParticleTypes.HAPPY_VILLAGER;
                case "angryVillager" -> ParticleTypes.ANGRY_VILLAGER;
                case "suspended", "depthsuspend" -> ParticleTypes.UNDERWATER;
                case "townaura" -> ParticleTypes.MYCELIUM;
                case "snowshovel" -> ParticleTypes.SNOWFLAKE;
                case "wake" -> ParticleTypes.FISHING;
                default -> null;
            };
            if (o == null) {
                Unimplemented.report("partícula " + type);
                return null;
            }
            return new Kind(o, false, null);
        }
        ParticleOptions o = switch (type) {
            case "SMOKE" -> ModParticles.FLIGHT_SMOKE.get();
            case "SMOKE_SMALL" -> ModParticles.FLIGHT_SMOKE_SMALL.get();
            case "BIG_SMOKE" -> ModParticles.BIG_SMOKE.get();
            case "FLAME_BALL" -> ModParticles.FLAME_BALL.get();
            default -> null;
        };
        if (o == null) {
            Unimplemented.report("partícula " + type);
            return null;
        }
        return new Kind(o, false, null);
    }

    private static void spawn(Minecraft mc, Kind kind, ParticleEmitterDef.Emitter em, double x, double y, double z,
                              double mx, double my, double mz, float scale) {
        // ajustes de partículas y distancia, como Fisk
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        if (status == ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0) return;
        Entity cam = mc.getCameraEntity();
        if (cam != null) {
            double max = Math.min(mc.options.getEffectiveRenderDistance() * 16, em.maxDistance);
            if (cam.distanceToSqr(x, y, z) > max * max) return;
        }
        ParticleOptions o = kind.options;
        if (kind.legacy != null) {
            // reddust / mobSpell: el "movimiento" es el color
            float r = (float) mx, g = (float) my, b = (float) mz;
            if (kind.legacy.equals("reddust")) {
                if (r == 0) r = 1;
                o = new DustParticleOptions(new Vector3f(r, g, b), 1);
            } else {
                o = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, r, g, b);
            }
            mx = my = mz = 0;
        }
        Particle p = mc.particleEngine.createParticle(o, x, y, z, mx, my, mz);
        if (p != null && scale != 1) p.scale(scale);
    }

    private static final class Mth {
        static double lerp(float t, double a, double b) {
            return a + (b - a) * t;
        }
    }
}
