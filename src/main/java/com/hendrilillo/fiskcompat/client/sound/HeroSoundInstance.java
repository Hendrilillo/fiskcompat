package com.hendrilillo.fiskcompat.client.sound;

import com.hendrilillo.fiskcompat.data.DataStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Un sonido de un evento de Fisk que sigue a una entidad y se actualiza cada tick. */
public final class HeroSoundInstance extends AbstractTickableSoundInstance {
    final Entity entity;
    final SoundEventDef def;
    final float range;
    final boolean beam;
    final Object context;
    int ticks;
    int fade;
    float volumeF = 1, pitchF = 1;
    private boolean pitchSet;
    private final JSSound js = new JSSound();

    public HeroSoundInstance(Entity entity, SoundEventDef def, Object context, float range, boolean beam) {
        super(SoundEvent.createVariableRangeEvent(ResourceLocation.parse(def.sound())), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.def = def;
        this.context = context;
        this.range = range;
        this.beam = beam;
        this.looping = def.loop();
        this.delay = 0;
        if (Float.isInfinite(range)) this.attenuation = Attenuation.NONE;
        // valores iniciales (Fisk los calcula en el primer tick)
        if (def.override() == null) {
            volumeF = def.volume(entity, context);
            pitchF = def.pitch(entity, context);
            pitchSet = true;
        }
        this.volume = Math.max(0, volumeF * fadeProgress());
        this.pitch = pitchF;
        updatePos();
    }

    @java.lang.Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents w = super.resolve(manager);
        // La distancia a la que se oye la marca el heropack ("range"), no el sounds.json
        if (sound != null && sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND
                && !Float.isInfinite(range)) {
            int dist = Math.max(1, Math.round(range * scaleFactor()));
            sound = new Sound(sound.getLocation(), sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(),
                    sound.shouldStream(), sound.shouldPreload(), dist);
        }
        return w;
    }

    private float scaleFactor() {
        if (!def.scale() || entity == null || entity == Minecraft.getInstance().player) return 1;
        Object s = DataStore.get(entity, "fiskheroes:scale");
        float scale = s instanceof Number n && n.floatValue() > 0 ? n.floatValue() : 1;
        return scale < 1 ? (float) Math.pow(scale, 2 / 3.0) : scale;
    }

    @java.lang.Override
    public boolean canStartSilent() {
        return true;
    }

    @java.lang.Override
    public boolean canPlaySound() {
        return entity == null || !entity.isSilent();
    }

    float fadeProgress() {
        if (fade > 0) return def.fadeOut() <= 0 ? 0 : fade / (float) def.fadeOut();
        if (ticks < def.fadeIn()) return Mth.clamp(ticks / (float) def.fadeIn(), 0, 1);
        return 1;
    }

    @java.lang.Override
    public void tick() {
        if (isStopped()) return;
        if (entity != null && entity.isRemoved()) {
            stop();
            return;
        }
        ticks++;
        boolean keep = (def.override() == null || entity == null || def.override().continuePlaying(entity, js))
                && fade == 0
                && (ticks <= 1 || entity == null || def.shouldContinue(entity, context));
        if (!keep) {
            if (def.fadeOut() == 0) {
                stop();
                return;
            }
            if (fade == 0) fade = def.fadeOut();
        }
        if (fade > 0 && --fade == 0) {
            stop();
            return;
        }
        if (def.override() == null && entity != null) {
            volumeF = def.volume(entity, context);
            if (looping || !pitchSet) {
                pitchF = def.pitch(entity, context);
                pitchSet = true;
            }
        }
        this.volume = Math.max(0, volumeF * fadeProgress());
        this.pitch = pitchF;
        updatePos();
    }

    private void updatePos() {
        if (entity == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (beam && entity != mc.player && mc.player != null) {
            Vec3 src = entity.getEyePosition();
            Object len = DataStore.get(entity, "fiskheroes:heat_vision_length");
            double l = len instanceof Number n ? n.doubleValue() : 0;
            Vec3 dst = src.add(entity.getLookAngle().scale(l));
            Vec3 p = nearest(src, dst, mc.player.getEyePosition());
            x = p.x;
            y = p.y;
            z = p.z;
            return;
        }
        x = entity.getX();
        y = entity.getY();
        z = entity.getZ();
    }

    private static Vec3 nearest(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        if (len < 1e-9) return a;
        double t = Mth.clamp(p.subtract(a).dot(ab) / len, 0, 1);
        return a.add(ab.scale(t));
    }

    /** Lo que ven las funciones "override" como "sound". */
    public final class JSSound {
        public String id() { return def.id; }
        public Object context() { return context; }
        public int ticksPlaying() { return ticks; }
        public double volume() { return volumeF; }
        public double pitch() { return pitchF; }
        public double range() { return range; }
        public double fadeProgress() { return HeroSoundInstance.this.fadeProgress(); }
        public void setVolume(double v) { volumeF = (float) v; }
        public void setPitch(double p) { pitchF = (float) p; }
        public void setRange(double r) {}
    }
}
