package com.hendrilillo.fiskcompat.net;

import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.data.DataStore;
import com.hendrilillo.fiskcompat.hero.KeyState;
import com.hendrilillo.fiskcompat.hero.HeroTicker;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Paquetes de red del mod: sincronización de variables y estado de las teclas. */
public final class Net {
    private Net() {}

    // ------------------------------------------------------------------ valores

    static final StreamCodec<ByteBuf, Object> VALUE = new StreamCodec<>() {
        @Override
        public Object decode(ByteBuf buf) {
            return switch (buf.readByte()) {
                case 0 -> buf.readBoolean();
                case 1 -> buf.readByte();
                case 2 -> buf.readShort();
                case 3 -> buf.readInt();
                case 4 -> buf.readLong();
                case 5 -> buf.readFloat();
                case 6 -> buf.readDouble();
                default -> ByteBufCodecs.STRING_UTF8.decode(buf);
            };
        }

        @Override
        public void encode(ByteBuf buf, Object v) {
            if (v instanceof Boolean b) { buf.writeByte(0); buf.writeBoolean(b); }
            else if (v instanceof Byte b) { buf.writeByte(1); buf.writeByte(b); }
            else if (v instanceof Short s) { buf.writeByte(2); buf.writeShort(s); }
            else if (v instanceof Integer i) { buf.writeByte(3); buf.writeInt(i); }
            else if (v instanceof Long l) { buf.writeByte(4); buf.writeLong(l); }
            else if (v instanceof Float f) { buf.writeByte(5); buf.writeFloat(f); }
            else if (v instanceof Double d) { buf.writeByte(6); buf.writeDouble(d); }
            else { buf.writeByte(7); ByteBufCodecs.STRING_UTF8.encode(buf, String.valueOf(v)); }
        }
    };

    static final StreamCodec<ByteBuf, Map<String, Object>> VALUES = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, VALUE);

    static boolean sendable(Object v) {
        return v instanceof Boolean || v instanceof Number || v instanceof String;
    }

    static Map<String, Object> filter(Map<String, Object> in) {
        Map<String, Object> out = new HashMap<>();
        in.forEach((k, v) -> { if (sendable(v)) out.put(k, v); });
        return out;
    }

    // ------------------------------------------------------------------ paquetes

    /** Servidor -> cliente: valores de variables de una entidad. */
    public record DataSync(int entityId, Map<String, Object> values) implements CustomPacketPayload {
        public static final Type<DataSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "data_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DataSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DataSync::entityId, VALUES, DataSync::values, DataSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Cliente -> servidor: variables que el jugador cambia por sus propias acciones (volar, máscara...). */
    public record ClientData(Map<String, Object> values) implements CustomPacketPayload {
        public static final Type<ClientData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "client_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientData> CODEC = StreamCodec.composite(VALUES, ClientData::values, ClientData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Cliente -> servidor: teclas mantenidas y teclas recién pulsadas. */
    public record Keys(int held, int pressed) implements CustomPacketPayload {
        public static final Type<Keys> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID, "keys"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Keys> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Keys::held, ByteBufCodecs.VAR_INT, Keys::pressed, Keys::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(Net::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(DataSync.TYPE, DataSync.CODEC, Net::handleDataSync);
        r.playToServer(ClientData.TYPE, ClientData.CODEC, Net::handleClientData);
        r.playToServer(Keys.TYPE, Keys.CODEC, Net::handleKeys);
    }

    private static void handleDataSync(DataSync msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSide.apply(msg));
    }

    private static void handleClientData(ClientData msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            long time = p.level().getGameTime();
            msg.values().forEach((k, v) -> {
                DataStore.set(p, k, v);
                DataStore.markOwnerSynced(p, k, time);
            });
        });
    }

    private static void handleKeys(Keys msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            KeyState.set(p, msg.held());
            if (msg.pressed() != 0) HeroTicker.onKeysPressed(p, msg.pressed());
        });
    }

    // ------------------------------------------------------------------ envío

    /** Envía al resto de jugadores (y al propio jugador, menos lo que él mismo controla) lo que ha cambiado. */
    public static void broadcastChanges(Entity entity) {
        Map<String, Object> changed = filter(DataStore.drainDirty(entity));
        if (changed.isEmpty()) return;
        PacketDistributor.sendToPlayersTrackingEntity(entity, new DataSync(entity.getId(), changed));
        if (entity instanceof ServerPlayer self) {
            long time = self.level().getGameTime();
            Map<String, Object> own = new HashMap<>();
            changed.forEach((k, v) -> { if (!DataStore.ownerSyncedRecently(self, k, time)) own.put(k, v); });
            if (!own.isEmpty()) PacketDistributor.sendToPlayer(self, new DataSync(entity.getId(), own));
        }
    }

    public static void sendAll(Entity entity, ServerPlayer to) {
        Map<String, Object> all = filter(DataStore.all(entity));
        if (!all.isEmpty()) PacketDistributor.sendToPlayer(to, new DataSync(entity.getId(), all));
    }

    /** Solo cliente. */
    static final class ClientSide {
        static void apply(DataSync msg) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity e = level.getEntity(msg.entityId());
            if (e == null) return;
            boolean self = e == Minecraft.getInstance().player;
            long time = level.getGameTime();
            msg.values().forEach((k, v) -> {
                if (self && DataStore.ownerSyncedRecently(e, k, time)) return;
                DataStore.set(e, k, v);
            });
            DataStore.drainDirty(e);
        }
    }
}
