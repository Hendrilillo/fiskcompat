package com.hendrilillo.fiskcompat.client;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.data.Sync;
import com.hendrilillo.fiskcompat.hero.HeroTicker;
import com.hendrilillo.fiskcompat.hero.KeyState;
import com.hendrilillo.fiskcompat.net.Net;
import com.hendrilillo.fiskcompat.power.modifier.ControlledFlight;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/** Teclas de habilidad (1-5) y de máscara, y envío al servidor de lo que hace el jugador local. */
public final class ClientKeys {
    public static final String CATEGORY = "key.categories.fiskcompat";
    public static final KeyMapping[] ABILITIES = {
            new KeyMapping("key.fiskcompat.ability1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.fiskcompat.ability2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.fiskcompat.ability3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY),
            new KeyMapping("key.fiskcompat.ability4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY),
            new KeyMapping("key.fiskcompat.ability5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY),
    };
    public static final KeyMapping MASK = new KeyMapping("key.fiskcompat.mask", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);

    private static int held, lastSent = -1;
    private static boolean prevJump;

    private ClientKeys() {}

    public static void init() {
        HeroTicker.isLocalPlayer = p -> p == Minecraft.getInstance().player;
        HeroTicker.localKeys = () -> held;
    }

    @EventBusSubscriber(modid = FiskCompat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            for (KeyMapping k : ABILITIES) event.register(k);
            event.register(MASK);
            init();
        }
    }

    @EventBusSubscriber(modid = FiskCompat.MODID, value = Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent
        public static void pre(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            DataStore.snapshotAll(mc.level);
            LocalPlayer p = mc.player;
            if (p == null) return;

            int now = 0, pressed = 0;
            boolean gui = mc.screen != null;
            for (int i = 0; i < 5; i++) {
                if (!gui && ABILITIES[i].isDown()) now |= 1 << i;
                while (ABILITIES[i].consumeClick()) if (!gui) pressed |= 1 << i;
            }
            if (!gui && mc.options.keyUse.isDown()) now |= KeyState.USE;
            if (!gui && mc.options.keyAttack.isDown()) now |= KeyState.ATTACK;
            boolean jump = !gui && mc.options.keyJump.isDown();
            if (jump) now |= KeyState.JUMP;
            if (!gui && MASK.isDown()) now |= KeyState.MASK;
            while (MASK.consumeClick()) if (!gui) pressed |= KeyState.MASK;
            held = now;

            ControlledFlight.setInput(p, new ControlledFlight.Input(p.input.forwardImpulse, p.input.leftImpulse,
                    jump, p.input.shiftKeyDown, jump && !prevJump));
            prevJump = jump;

            if (pressed != 0) HeroTicker.onLocalKeysPressed(p, pressed);
            if (now != lastSent || pressed != 0) {
                PacketDistributor.sendToServer(new Net.Keys(now, pressed));
                lastSent = now;
            }
        }

        @SubscribeEvent
        public static void post(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().getConnection() == null) return;
            Map<String, Object> changes = Sync.drain();
            if (!changes.isEmpty()) PacketDistributor.sendToServer(new Net.ClientData(changes));
        }
    }
}
