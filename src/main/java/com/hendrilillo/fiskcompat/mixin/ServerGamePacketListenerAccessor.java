package com.hendrilillo.fiskcompat.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para que el servidor no eche por "volar" a quien vuela con un traje. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
    @Accessor("aboveGroundTickCount")
    void fiskcompat$setAboveGroundTickCount(int value);
}
