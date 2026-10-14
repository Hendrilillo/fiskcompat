package com.hendrilillo.fiskcompat.mixin;

import com.hendrilillo.fiskcompat.power.ServerSoundTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Trigger STEP de los héroes: cada vez que la entidad hace el sonido de un paso. */
@Mixin(Entity.class)
public abstract class EntityStepMixin {
    @Inject(method = "walkingStepSound", at = @At("HEAD"))
    private void fiskcompat$step(BlockPos pos, BlockState state, CallbackInfo ci) {
        ServerSoundTriggers.onStep((Entity) (Object) this);
    }
}
