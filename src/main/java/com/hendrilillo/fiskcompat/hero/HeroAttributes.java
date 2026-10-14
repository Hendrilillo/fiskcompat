package com.hendrilillo.fiskcompat.hero;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Atributos de Fisk (PUNCH_DAMAGE, BASE_SPEED...) traducidos a atributos de Minecraft. */
public final class HeroAttributes {
    private record Target(Holder<Attribute> attribute, double scale) {}

    private static final Map<String, List<Target>> MAPPING = new HashMap<>();

    static {
        MAPPING.put("PUNCH_DAMAGE", List.of(new Target(Attributes.ATTACK_DAMAGE, 1)));
        MAPPING.put("BASE_SPEED", List.of(new Target(Attributes.MOVEMENT_SPEED, 1)));
        MAPPING.put("JUMP_HEIGHT", List.of(new Target(Attributes.JUMP_STRENGTH, 0.1)));
        MAPPING.put("FALL_RESISTANCE", List.of(new Target(Attributes.SAFE_FALL_DISTANCE, 1)));
        MAPPING.put("KNOCKBACK", List.of(new Target(Attributes.ATTACK_KNOCKBACK, 1)));
        MAPPING.put("STEP_HEIGHT", List.of(new Target(Attributes.STEP_HEIGHT, 1)));
        MAPPING.put("REACH_DISTANCE", List.of(new Target(Attributes.ENTITY_INTERACTION_RANGE, 1), new Target(Attributes.BLOCK_INTERACTION_RANGE, 1)));
        MAPPING.put("DAMAGE_REDUCTION", List.of(new Target(Attributes.ARMOR, 1)));
    }

    private static final String PREFIX = "hero_";

    private HeroAttributes() {}

    private static ResourceLocation id(String attr, int i) {
        return ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, PREFIX + attr.toLowerCase() + "_" + i);
    }

    public static void apply(LivingEntity e, ActiveHero hero, JSEntity js) {
        List<HeroDefinition.Attribute> attrs = new ArrayList<>(hero.def.attributes());
        if (hero.def.attributeProfileFunc != null) {
            try {
                Object key = hero.def.attributeProfileFunc.apply(js);
                HeroDefinition.AttributeProfile profile = key == null ? null : hero.def.attributeProfiles.get(String.valueOf(key));
                if (profile != null) {
                    if (!profile.inheritDefaults) attrs.clear();
                    attrs.addAll(profile.attributes);
                }
            } catch (Throwable ignored) {
            }
        }
        Map<ResourceLocation, AttributeModifier> wanted = new HashMap<>();
        Map<ResourceLocation, Holder<Attribute>> where = new HashMap<>();
        Map<String, Integer> counter = new HashMap<>();
        for (HeroDefinition.Attribute a : attrs) {
            String name = a.name();
            boolean sprint = name.equals("SPRINT_SPEED");
            if (sprint) {
                if (!e.isSprinting()) continue;
                name = "BASE_SPEED";
            }
            List<Target> targets = MAPPING.get(name);
            if (targets == null) continue;
            for (Target t : targets) {
                int i = counter.merge(name, 1, Integer::sum);
                ResourceLocation id = id(name, i);
                AttributeModifier.Operation op = a.operation() == 0 ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                double amount = a.operation() == 0 ? a.value() * t.scale() : a.value();
                if (name.equals("BASE_SPEED") && op == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) amount = Math.max(-1, amount);
                wanted.put(id, new AttributeModifier(id, amount, op));
                where.put(id, t.attribute());
            }
        }
        // Quitar los que ya no tocan y poner/actualizar los demás
        for (Holder<Attribute> attr : allAttributes()) {
            AttributeInstance inst = e.getAttribute(attr);
            if (inst == null) continue;
            for (AttributeModifier mod : new ArrayList<>(inst.getModifiers())) {
                if (mod.id().getNamespace().equals(FiskCompat.MODID) && mod.id().getPath().startsWith(PREFIX)
                        && (!wanted.containsKey(mod.id()) || where.get(mod.id()) != attr)) {
                    inst.removeModifier(mod.id());
                }
            }
        }
        wanted.forEach((id, mod) -> {
            AttributeInstance inst = e.getAttribute(where.get(id));
            if (inst == null) return;
            AttributeModifier cur = inst.getModifier(id);
            if (cur == null || cur.amount() != mod.amount() || cur.operation() != mod.operation()) {
                inst.removeModifier(id);
                inst.addTransientModifier(mod);
            }
        });
    }

    public static void remove(LivingEntity e) {
        for (Holder<Attribute> attr : allAttributes()) {
            AttributeInstance inst = e.getAttribute(attr);
            if (inst == null) continue;
            for (AttributeModifier mod : new ArrayList<>(inst.getModifiers())) {
                if (mod.id().getNamespace().equals(FiskCompat.MODID) && mod.id().getPath().startsWith(PREFIX)) inst.removeModifier(mod.id());
            }
        }
    }

    private static List<Holder<Attribute>> allAttributes() {
        List<Holder<Attribute>> out = new ArrayList<>();
        for (List<Target> ts : MAPPING.values()) for (Target t : ts) if (!out.contains(t.attribute())) out.add(t.attribute());
        return out;
    }
}
