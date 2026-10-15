package com.hendrilillo.fiskcompat.dev;

import com.hendrilillo.fiskcompat.FiskCompat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Parte de cliente de {@link AutoTest}: hace capturas y cierra el juego. */
@EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
public final class AutoTestClient {
    private static int ticks;

    private AutoTestClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!AutoTest.ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        if (AutoTest.MODE.equals("fp")) {
            firstPersonTest(mc);
            return;
        }
        if (AutoTest.MODE.equals("attacks")) {
            attacks(mc);
            return;
        }
        if (AutoTest.MODE.equals("sentry")) {
            sentry(mc);
            return;
        }
        if (AutoTest.MODE.equals("hud")) {
            hud(mc);
            return;
        }
        if (AutoTest.MODE.equals("nano")) {
            nano(mc);
            return;
        }
        if (ticks == 5) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (ticks == 110) mc.options.fov().set(30);
        if (ticks == 115) shot(mc, "fiskcompat_zoom.png");
        if (ticks == 118) mc.options.fov().set(70);
        if (ticks == 120) shot(mc, "fiskcompat_stands.png");
        if (ticks == 125) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (ticks == 160) shot(mc, "fiskcompat_player_front.png");
        if (ticks == 165) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 200) shot(mc, "fiskcompat_player_back.png");
        if (ticks == 205) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.getInventory().selected = 0;
            mc.player.getInventory().setItem(0, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());
            mc.player.getInventory().setItem(1, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy());
            mc.player.getInventory().setItem(2, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy());
            mc.player.getInventory().setItem(3, mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy());
        }
        if (ticks == 240) shot(mc, "fiskcompat_hotbar.png");
        if (ticks == 245) mc.player.getInventory().selected = 6;
        if (ticks == 270) shot(mc, "fiskcompat_arm.png");
        if (ticks == 275) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskcompat:dummy", 0);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", true);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 6, mc.player.getZ());
        }
        if (ticks == 300) shot(mc, "fiskcompat_flying_mask.png");
        if (ticks == 301) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        }
        if (ticks == 322) shot(mc, "fiskcompat_flying_boost.png");
        if (ticks == 323) { mc.player.setYRot(mc.player.getYRot() + 90); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 330) shot(mc, "fiskcompat_flying_side.png");
        if (ticks == 331) { mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false); }
        if (ticks == 345) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", false);
        if (ticks == 346) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", false);
        if (ticks == 370) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.fov().set(30);
            boolean n = com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/nanites") instanceof Boolean b && b;
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "sind:dyn/nanites", !n);
        }
        if (ticks == 374) shot(mc, "fiskcompat_nano_mid.png");
        if (ticks == 395) shot(mc, "fiskcompat_nano_end.png");
        if (ticks == 396) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
        if (ticks == 425) shot(mc, "fiskcompat_mask_zoom.png");
        if (ticks == 426) mc.player.setYRot(mc.player.getYRot() + 60);
        if (ticks == 428) mc.player.setYBodyRot(mc.player.getYRot());
        if (ticks == 429) shot(mc, "fiskcompat_mask_side.png");
        if (ticks == 432) {
            FiskCompat.LOGGER.info("[autotest] flying={} y={} timer={}", com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:flying"),
                    mc.player.getY(), com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:flight_timer"));
            mc.stop();
        }
    }

    private static void logNano(Minecraft mc, String what) {
        var p = mc.player;
        java.util.function.Function<String, Object> d = v -> com.hendrilillo.fiskcompat.data.DataStore.get(p, v);
        FiskCompat.LOGGER.info("[autotest] {} nanites2={} charging={} charge={} shooting={} shoot_t={} shield={} shield_t={} slot={} swapper={} keys2={} keys3={} keys4={}",
                what, d.apply("sind:dyn/nanites2"), d.apply("fiskheroes:beam_charging"), d.apply("fiskheroes:beam_charge"), d.apply("fiskheroes:beam_shooting"),
                d.apply("fiskheroes:beam_shooting_timer"), d.apply("fiskheroes:shield"), d.apply("fiskheroes:shield_timer"), d.apply("sind:dyn/slot"),
                com.hendrilillo.fiskcompat.api.JSEntity.of(p).getWornChestplate().nbt(),
                hero(mc).keysInSlot(com.hendrilillo.fiskcompat.api.JSEntity.of(p), 2), hero(mc).keysInSlot(com.hendrilillo.fiskcompat.api.JSEntity.of(p), 3),
                hero(mc).keysInSlot(com.hendrilillo.fiskcompat.api.JSEntity.of(p), 4));
    }

    private static com.hendrilillo.fiskcompat.hero.ActiveHero hero(Minecraft mc) {
        return com.hendrilillo.fiskcompat.hero.ActiveHero.of(mc.player);
    }

    /** Golpe al suelo (B) y terremoto (G) con el ariete puesto, mirando de frente y desde arriba. */
    private static void smashTest(Minecraft mc, net.minecraft.client.KeyMapping[] keys, int t) {
        if (t == 0) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.options.fov().set(90); }
        if (t == 5) { net.minecraft.client.KeyMapping.click(keys[2].getKey()); keys[2].setDown(true); }
        if (t == 15 || t == 30) { logNano(mc, "smash"); shot(mc, "fiskcompat_nano_smash" + t + ".png"); }
        if (t == 40) keys[2].setDown(false);
        if (t == 60) shot(mc, "fiskcompat_nano_smash60.png");
        if (t == 80) { net.minecraft.client.KeyMapping.click(keys[4].getKey()); keys[4].setDown(true); }
        if (t == 90 || t == 105) { logNano(mc, "quake"); shot(mc, "fiskcompat_nano_quake" + t + ".png"); }
        if (t == 115) keys[4].setDown(false);
        if (t == 140) shot(mc, "fiskcompat_nano_quake140.png");
        if (t == 150) { mc.options.keyAttack.setDown(true); }
        if (t == 153) { mc.options.keyAttack.setDown(false); shot(mc, "fiskcompat_nano_punch.png"); }
        if (t == 200) mc.options.fov().set(70);
    }

    /** Trajes de nanobots (Mark 50/85): cañones, escudo y armas. */
    private static void nano(Minecraft mc) {
        var keys = com.hendrilillo.fiskcompat.client.ClientKeys.ABILITIES;
        if (ticks == 5) { mc.options.hideGui = false; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
        if (ticks == 10) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "sind:dyn/nanites", true);
        if (ticks == 60) logNano(mc, "desplegado");
        if (ticks == 62) { net.minecraft.client.KeyMapping.click(keys[1].getKey()); keys[1].setDown(true); }
        if (ticks == 80 || ticks == 100) logNano(mc, "cargando");
        if (ticks == 95) shot(mc, "fiskcompat_nano_beam_back.png");
        if (ticks == 96) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 102) shot(mc, "fiskcompat_nano_beam_front.png");
        if (ticks == 130) { keys[1].setDown(false); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 190) { logNano(mc, "antes escudo"); net.minecraft.client.KeyMapping.click(keys[2].getKey()); keys[2].setDown(true); }
        if (ticks == 193) keys[2].setDown(false);
        if (ticks == 215) { logNano(mc, "escudo"); shot(mc, "fiskcompat_nano_shield.png"); }
        if (ticks == 216) { mc.options.keyUse.setDown(true); }
        if (ticks == 225 && mc.getSingleplayerServer() != null) {
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var p = server.getPlayerList().getPlayers().get(0);
                var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());
                var front = p.position().add(p.getLookAngle().multiply(2, 0, 2));
                z.moveTo(front.x, p.getY(), front.z, 0, 0);
                z.setNoAi(true);
                p.serverLevel().addFreshEntity(z);
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                float before = p.getHealth();
                p.invulnerableTime = 0;
                boolean hit = p.hurt(p.damageSources().mobAttack(z), 6);
                p.invulnerableTime = 0;
                boolean hit2 = p.hurt(p.damageSources().mobAttack(z), 6);
                FiskCompat.LOGGER.info("[autotest] segundo golpe {} vida {}", hit2, p.getHealth());
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                FiskCompat.LOGGER.info("[autotest] golpe con escudo: dañado={} vida {} -> {} daño_escudo={} bloqueando={}", hit, before, p.getHealth(),
                        com.hendrilillo.fiskcompat.data.DataStore.get(p, "fiskheroes:shield_damage"), com.hendrilillo.fiskcompat.data.DataStore.get(p, "fiskheroes:shield_blocking"));
                z.discard();
            });
        }
        if (ticks == 228) { mc.player.setYRot(mc.player.getYRot() - 60); mc.player.setYBodyRot(mc.player.getYRot()); }
        if (ticks == 230) { logNano(mc, "escudo bloqueando"); shot(mc, "fiskcompat_nano_shield_block.png"); }
        if (ticks == 231) { mc.player.setYRot(mc.player.getYRot() + 60); mc.options.keyUse.setDown(false); }
        if (ticks == 235) { net.minecraft.client.KeyMapping.click(keys[2].getKey()); keys[2].setDown(true); }
        if (ticks == 238) keys[2].setDown(false);
        if (ticks == 250) { logNano(mc, "antes swap"); net.minecraft.client.KeyMapping.click(keys[4].getKey()); keys[4].setDown(true); }
        if (ticks == 253) keys[4].setDown(false);
        if (ticks == 270) { logNano(mc, "swap"); net.minecraft.client.KeyMapping.click(keys[3].getKey()); keys[3].setDown(true); }
        if (ticks == 273) keys[3].setDown(false);
        if (ticks == 295) { logNano(mc, "blunt"); shot(mc, "fiskcompat_nano_blunt.png"); }
        if (ticks >= 296 && ticks < 520) { smashTest(mc, keys, ticks - 296); return; }
        if (ticks == 520) { net.minecraft.client.KeyMapping.click(keys[3].getKey()); keys[3].setDown(true); }
        if (ticks == 523) keys[3].setDown(false);
        if (ticks == 544) { logNano(mc, "mazo"); shot(mc, "fiskcompat_nano_mallet.png"); }
        if (ticks == 550) mc.stop();
    }

    /** HUD del casco (JARVIS) y partículas. */
    private static void hud(Minecraft mc) {
        if (ticks == 5) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.getInventory().selected = 8;
            mc.player.setXRot(0);
        }
        if (ticks == 10) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "sind:dyn/jarvis", true);
        if (ticks == 60) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 100) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 109) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 90) shot(mc, "fiskcompat_hud.png");
        if (ticks == 91) {
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", true);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 5, mc.player.getZ());
        }
        if (ticks == 110) shot(mc, "fiskcompat_hud_flying.png");
        if (ticks == 50 || ticks == 108 || ticks == 130) FiskCompat.LOGGER.info("[autotest] pierna dcha={} izda={} cabeza={} yaw={} cam={}",
                com.hendrilillo.fiskcompat.client.particle.ParticleEmitters.debugPoint(mc.player, "rightLeg", 0, 0.75f, 0),
                com.hendrilillo.fiskcompat.client.particle.ParticleEmitters.debugPoint(mc.player, "leftLeg", 0, 0.75f, 0),
                com.hendrilillo.fiskcompat.client.particle.ParticleEmitters.debugPoint(mc.player, "head", 0, 0, 0), mc.player.yBodyRot, mc.options.getCameraType());
        if (ticks == 111) { mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); }
        if (ticks == 125) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
        if (ticks == 135) shot(mc, "fiskcompat_particles_back.png");
        if (ticks == 136) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 146) shot(mc, "fiskcompat_particles_front.png");
        if (ticks == 147) { mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false); mc.options.setCameraType(CameraType.FIRST_PERSON); }
        if (ticks == 160) shot(mc, "fiskcompat_hud_fp_particles.png");
        if (ticks == 162) {
            var server = mc.getSingleplayerServer();
            if (server != null) server.execute(() -> server.overworld().setDayTime(18000));
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", false);
        }
        if (ticks == 180) shot(mc, "fiskcompat_night_closed.png");
        if (ticks == 181) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
        if (ticks == 205) shot(mc, "fiskcompat_night_open.png");
        if (ticks == 210) mc.stop();
    }

    /** Sonidos y modo centinela. */
    private static void sentry(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (ticks == 5) { mc.options.hideGui = true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 30) {
            var sm = mc.getSoundManager();
            for (String n : new String[]{"sind:unibeam", "sind:beep", "fiskheroes:suit.ironman.beam.loop", "fiskheroes:suit.ironman.boost.loop"}) {
                var ev = sm.getSoundEvent(net.minecraft.resources.ResourceLocation.parse(n));
                FiskCompat.LOGGER.info("[autotest] sonido registrado {} -> {}", n, ev == null ? "NO" : ev.getSound(net.minecraft.util.RandomSource.create()).getLocation());
            }
            FiskCompat.LOGGER.info("[autotest] zips de sonidos: {}", com.hendrilillo.fiskcompat.client.sound.SoundPacks.namespaces());
            int ok = 0, bad = 0;
            var res = mc.getResourceManager().listResources("events/sounds", rl -> rl.getPath().endsWith(".json"));
            for (var rl : res.keySet()) {
                String id = rl.getNamespace() + ":" + rl.getPath().substring("events/sounds/".length(), rl.getPath().length() - 5);
                var def = com.hendrilillo.fiskcompat.client.sound.SoundEventDef.get(id);
                if (def == null) { if (!id.endsWith("sound_base")) bad++; continue; }
                def.shouldStart(mc.player, null); def.shouldContinue(mc.player, null); def.volume(mc.player, null); def.pitch(mc.player, null);
                if (mc.getSoundManager().getSoundEvent(net.minecraft.resources.ResourceLocation.parse(def.sound())) == null) {
                    FiskCompat.LOGGER.info("[autotest] evento {} usa un sonido que no existe: {}", id, def.sound());
                    bad++;
                } else ok++;
            }
            FiskCompat.LOGGER.info("[autotest] eventos de sonido: {} bien, {} mal", ok, bad);
        }
        if (ticks == 40) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", true);
        if (ticks == 55) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:mask_open", false);
        if (ticks == 75) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); com.hendrilillo.fiskcompat.power.modifier.SentryMode.keyPressed(mc.player); }
        if (ticks == 79) shot(mc, "fiskcompat_sentry_opening.png");
        if ((ticks == 78 || ticks == 82 || ticks == 95) && server != null) server.execute(() -> {
            var p = server.getPlayerList().getPlayers().get(0);
            var h = com.hendrilillo.fiskcompat.hero.ActiveHero.of(p);
            FiskCompat.LOGGER.info("[autotest] servidor: open={} timer={} mask={} hero={} sentryMod={}", com.hendrilillo.fiskcompat.data.DataStore.get(p, "fiskheroes:suit_open"),
                    com.hendrilillo.fiskcompat.data.DataStore.get(p, "fiskheroes:suit_open_timer"), com.hendrilillo.fiskcompat.data.DataStore.get(p, "fiskheroes:mask_open_timer"),
                    h == null ? null : h.suitId, h == null ? null : h.enabledModifier(com.hendrilillo.fiskcompat.api.JSEntity.of(p), "fiskheroes:sentry_mode"));
        });
        if (ticks == 110) {
            var sentries = mc.level.getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, mc.player.getBoundingBox().inflate(20));
            FiskCompat.LOGGER.info("[autotest] centinelas={} pecho_jugador={} open={} timer={}", sentries.size(),
                    mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                    sentries.isEmpty() ? "-" : com.hendrilillo.fiskcompat.data.DataStore.get(sentries.get(0), "fiskheroes:suit_open"),
                    sentries.isEmpty() ? "-" : com.hendrilillo.fiskcompat.data.DataStore.get(sentries.get(0), "fiskheroes:suit_open_timer"));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setXRot(15);
            mc.player.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 2.5);
        }
        if (ticks == 114) shot(mc, "fiskcompat_sentry_open.png");
        if (ticks == 115 && server != null) server.execute(() -> {
            for (var s : server.overworld().getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, new net.minecraft.world.phys.AABB(-50, -100, -50, 50, 400, 50))) {
                com.hendrilillo.fiskcompat.data.DataStore.set(s, "fiskheroes:suit_open", false);
            }
        });
        if (ticks == 116 && server != null) server.execute(() -> {
            for (var se : server.overworld().getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, new net.minecraft.world.phys.AABB(-50, -100, -50, 50, 400, 50))) {
                var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(server.overworld());
                if (z == null) continue;
                z.moveTo(se.getX() + 7, se.getY(), se.getZ(), 0, 0);
                z.setNoAi(true);
                z.setPersistenceRequired();
                server.overworld().addFreshEntity(z);
            }
        });
        if (ticks == 135) shot(mc, "fiskcompat_sentry_closed.png");
        if (ticks == 158 && server != null) server.execute(() -> {
            for (var se : server.overworld().getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, new net.minecraft.world.phys.AABB(-50, -100, -50, 50, 400, 50))) {
                var zs = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, se.getBoundingBox().inflate(20));
                FiskCompat.LOGGER.info("[autotest] centinela objetivo={} apuntando={} zombi={}", se.getTarget(), com.hendrilillo.fiskcompat.data.DataStore.get(se, "fiskheroes:aiming"),
                        zs.stream().map(z -> z.getHealth() + (z.isAlive() ? "" : " muerto")).toList());
            }
        });
        if (ticks == 136) FiskCompat.LOGGER.info("[autotest] centinela cerrado: {}", mc.level.getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class,
                mc.player.getBoundingBox().inflate(20)).stream().map(e -> com.hendrilillo.fiskcompat.data.DataStore.get(e, "fiskheroes:suit_open_timer") + "/" + e.getHealth()).toList());
        if (ticks == 140 && server != null) server.execute(() -> {
            for (var s : server.overworld().getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, new net.minecraft.world.phys.AABB(-50, -100, -50, 50, 400, 50))) {
                com.hendrilillo.fiskcompat.data.DataStore.set(s, "fiskheroes:suit_open", true);
            }
        });
        if (ticks == 145) {
            for (var se : mc.level.getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, mc.player.getBoundingBox().inflate(20))) {
                mc.player.setPos(se.getX(), se.getY(), se.getZ() + 3);
                mc.player.setYRot(180);
                mc.player.setXRot(15);
            }
        }
        if (ticks == 155) shot(mc, "fiskcompat_sentry_front_open.png");
        if (ticks == 160 && server != null) server.execute(() -> {
            var p = server.getPlayerList().getPlayers().get(0);
            for (var s : server.overworld().getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, new net.minecraft.world.phys.AABB(-50, -100, -50, 50, 400, 50))) {
                p.teleportTo(s.getX(), s.getY(), s.getZ());
            }
        });
        if (ticks == 185) {
            var sentries = mc.level.getEntitiesOfClass(com.hendrilillo.fiskcompat.entity.SentryEntity.class, mc.player.getBoundingBox().inflate(20));
            FiskCompat.LOGGER.info("[autotest] tras entrar: centinelas={} pecho_jugador={} timer={}", sentries.size(),
                    mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                    com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:suit_open_timer"));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (ticks == 190) shot(mc, "fiskcompat_sentry_back_in.png");
        if (ticks == 200) { mc.options.keyUp.setDown(true); FiskCompat.LOGGER.info("[autotest] andar desde {} suelo={}", mc.player.position(), mc.player.onGround()); }
        if (ticks == 214) FiskCompat.LOGGER.info("[autotest] andar hasta {}", mc.player.position());
        if (ticks == 215) mc.options.keyUp.setDown(false);
        if (ticks == 220) {
            com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", true);
            mc.player.setPos(mc.player.getX(), mc.player.getY() + 4, mc.player.getZ());
        }
        if (ticks == 240) com.hendrilillo.fiskcompat.data.Sync.setLocal(mc.player, "fiskheroes:flying", false);
        if (ticks == 270) mc.stop();
    }

    private static void set(net.minecraft.world.entity.Entity e, String var, Object v) {
        com.hendrilillo.fiskcompat.data.DataStore.set(e, var, v);
        com.hendrilillo.fiskcompat.data.DataStore.snapshot(e);
    }

    private static void log(Minecraft mc, String what) {
        var golems = mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, mc.player.getBoundingBox().inflate(40));
        FiskCompat.LOGGER.info("[autotest] {} aiming={} aim_t={} charging={} charge={} shooting={} heat={} golem={}", what,
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:aiming"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:aiming_timer"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_charging"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_charge"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:beam_shooting_timer"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "fiskheroes:heat_vision_timer"),
                golems.isEmpty() ? "-" : golems.get(0).getHealth());
    }

    private static void logNbt(Minecraft mc, String what) {
        var chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        var data = chest.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        FiskCompat.LOGGER.info("[autotest] {} hand={} armgun={} armtimer={} nbt={}", what, mc.player.getMainHandItem(),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/armgun_bool"),
                com.hendrilillo.fiskcompat.data.DataStore.get(mc.player, "sind:dyn/armrockets_timer"), data);
    }

    /** Primera persona: apuntar y rayos con las dos manos; tercera persona de frente (brazos visibles). */
    private static void firstPersonTest(Minecraft mc) {
        var keys = com.hendrilillo.fiskcompat.client.ClientKeys.ABILITIES;
        if (ticks == 5) { mc.options.hideGui = false; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 40) {
            for (var st : mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, mc.player.getBoundingBox().inflate(20))) {
                set(st, "sind:dyn/armgun_timer", 1f);
                set(st, "sind:dyn/armrockets_timer", 1f);
                set(st, "sind:dyn/armgun_bool", true);
            }
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(30);
        }
        if (ticks == 50) shot(mc, "fiskcompat_fp_stands.png");
        if (ticks == 51) { mc.options.fov().set(70); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 60) shot(mc, "fiskcompat_fp_front.png");
        if (ticks == 61) { mc.options.setCameraType(CameraType.FIRST_PERSON); keys[0].setDown(true); }
        if (ticks == 80) shot(mc, "fiskcompat_fp_aim.png");
        if (ticks == 81) keys[0].setDown(false);
        if (ticks == 90) keys[4].setDown(true);
        if (ticks == 110) shot(mc, "fiskcompat_fp_key5.png");
        if (ticks == 111) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 125) shot(mc, "fiskcompat_fp_key5_front.png");
        if (ticks == 126) { keys[4].setDown(false); mc.options.setCameraType(CameraType.FIRST_PERSON); }
        if (ticks == 140) mc.stop();
    }

    /** Prueba de ataques: rayos en un soporte de armadura (vista lateral) y disparos del jugador. */
    private static void attacks(Minecraft mc) {
        var keys = com.hendrilillo.fiskcompat.client.ClientKeys.ABILITIES;
        var stands = mc.level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, mc.player.getBoundingBox().inflate(20));
        var stand = stands.isEmpty() ? null : stands.get(0);
        if (ticks == 5) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        // Soporte: unibeam, láser del brazo y rayos de energía forzando sus variables
        if (ticks == 60 && stand != null) { set(stand, "fiskheroes:beam_charging", true); set(stand, "fiskheroes:beam_charge", 1f); set(stand, "fiskheroes:beam_shooting_timer", 1f); }
        if (ticks == 64) shot(mc, "fiskcompat_stand_unibeam.png");
        if (ticks == 65 && stand != null) { set(stand, "fiskheroes:beam_shooting_timer", 0f); set(stand, "fiskheroes:heat_vision", true); set(stand, "fiskheroes:heat_vision_timer", 1f); set(stand, "fiskheroes:aiming_timer", 1f); }
        if (ticks == 69) shot(mc, "fiskcompat_stand_laser.png");
        if (ticks == 70 && stand != null) { set(stand, "fiskheroes:heat_vision_timer", 0f); set(stand, "fiskheroes:aiming_timer", 0f); set(stand, "fiskheroes:energy_projection_timer", 1f); }
        if (ticks == 74) shot(mc, "fiskcompat_stand_energy.png");
        if (ticks == 75 && stand != null) set(stand, "fiskheroes:energy_projection_timer", 0f);
        // Jugador: apuntar y repulsor
        if (ticks == 80) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); keys[0].setDown(true); }
        if (ticks == 95) { log(mc, "aim"); mc.options.keyUse.setDown(true); }
        if (ticks == 102) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 100 || ticks == 104 || ticks == 108) { FiskCompat.LOGGER.info("[autotest] shot tick {}", mc.level.getGameTime()); shot(mc, "fiskcompat_repulsor_back" + ticks + ".png"); }
        if (ticks == 97) mc.options.keyUse.setDown(false);
        if (ticks == 120) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 125) shot(mc, "fiskcompat_aim_back.png");
        if (ticks == 130) { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.keyUse.setDown(true); }
        if (ticks == 134 || ticks == 138) shot(mc, "fiskcompat_repulsor_fp" + ticks + ".png");
        if (ticks == 131) mc.options.keyUse.setDown(false);
        if (ticks == 140) { keys[0].setDown(false); log(mc, "after repulsor"); }
        // Unibeam
        if (ticks == 150) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            net.minecraft.client.KeyMapping.click(keys[1].getKey());
            keys[1].setDown(true);
        }
        if (ticks == 170) log(mc, "charging");
        if (ticks == 215) { log(mc, "unibeam"); shot(mc, "fiskcompat_unibeam_back.png"); }
        if (ticks == 216) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 222) shot(mc, "fiskcompat_unibeam_fp.png");
        if (ticks == 260) { keys[1].setDown(false); log(mc, "after unibeam"); }
        // Láser del brazo (HEAT_VISION en la tecla 5)
        if (ticks == 330) { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); keys[4].setDown(true); }
        if (ticks == 345) { log(mc, "laser"); shot(mc, "fiskcompat_laser_back.png"); }
        if (ticks == 346) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (ticks == 350) shot(mc, "fiskcompat_laser_fp.png");
        if (ticks == 380) { keys[4].setDown(false); log(mc, "end laser"); }
        // Primera persona apuntando, con la mano visible
        if (ticks == 385) { mc.options.hideGui = false; mc.options.setCameraType(CameraType.FIRST_PERSON); keys[0].setDown(true); }
        if (ticks == 400) shot(mc, "fiskcompat_aim_fp.png");
        if (ticks == 401) keys[0].setDown(false);
        if (ticks == 410) shot(mc, "fiskcompat_noaim_fp.png");
        // Cohete del brazo: guardar TNT en el traje, recargar, sacar el brazo y disparar
        if (ticks == 415) {
            mc.player.getInventory().selected = 0;
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                var sp = server.getPlayerList().getPlayer(uuid);
                sp.getInventory().selected = 0;
                sp.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TNT, 8));
            });
        }
        if (ticks == 418) net.minecraft.client.KeyMapping.click(com.hendrilillo.fiskcompat.client.ClientKeys.EQUIP.getKey());
        if (ticks == 425) { logNbt(mc, "stored"); net.minecraft.client.KeyMapping.click(keys[3].getKey()); }
        if (ticks == 435) { logNbt(mc, "reloaded"); keys[3].setDown(true); }
        if (ticks == 470) { logNbt(mc, "deployed"); mc.options.keyUse.setDown(true); }
        if (ticks == 472) mc.options.keyUse.setDown(false);
        if (ticks == 474) shot(mc, "fiskcompat_armrocket_fp.png");
        if (ticks == 480) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
        if (ticks == 500) { mc.options.keyUse.setDown(true); }
        if (ticks == 502) mc.options.keyUse.setDown(false);
        if (ticks == 504) shot(mc, "fiskcompat_armrocket_front.png");
        if (ticks == 506) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (ticks == 512) shot(mc, "fiskcompat_armrocket_back.png");
        if (ticks == 520) { keys[3].setDown(false); logNbt(mc, "after rockets"); log(mc, "end"); }
        if (ticks == 530) mc.stop();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> FiskCompat.LOGGER.info("[autotest] {}", msg.getString()));
    }
}
