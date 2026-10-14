package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.api.JSEntity;
import com.hendrilillo.fiskcompat.hero.ActiveHero;
import com.hendrilillo.fiskcompat.hero.HeroDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Lista de habilidades del traje y sus teclas, en la esquina de la pantalla (como en Fisk). */
@EventBusSubscriber(modid = FiskCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyHud {
    private KeyHud() {}

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "keys"), KeyHud::render);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ActiveHero hero = ActiveHero.of(mc.player);
        if (hero == null) return;
        JSEntity js = JSEntity.of(mc.player);
        int y = 4;
        for (int slot = 1; slot <= 5; slot++) {
            for (String key : hero.keysInSlot(js, slot)) {
                HeroDefinition.KeyBind kb = hero.def.keyBinds().get(key);
                String name = kb.name().startsWith("key.") ? com.hendrilillo.fiskcompat.pack.PackLang.get(kb.name()) : kb.name();
                String bind = ClientKeys.ABILITIES[slot - 1].getTranslatedKeyMessage().getString();
                g.drawString(mc.font, "§7[" + bind + "] §f" + name, 4, y, 0xFFFFFF, true);
                y += 10;
            }
        }
    }
}
