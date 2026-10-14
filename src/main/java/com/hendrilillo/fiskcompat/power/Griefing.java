package com.hendrilillo.fiskcompat.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lo que hacen los rayos de Fisk al bloque que tocan (Griefing.melt): el hielo se derrite y, con
 * destrozos permitidos (regla mobGriefing), el láser funde el bloque en lo que daría un horno
 * (arena → cristal, piedra labrada → piedra...).
 */
public final class Griefing {
    public static final int ICE = 4, SMELT = 8;

    private Griefing() {}

    /** heat_vision usa 15 (todo) y energy_projection 7 (sin fundir); sin destrozos, ninguno de los dos. */
    public static void melt(Level level, BlockPos pos, int flagsIfAllowed) {
        if (level.isClientSide) return;
        boolean allowed = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        int flags = allowed ? flagsIfAllowed : 3;
        BlockState state = level.getBlockState(pos);
        if ((flags & ICE) != 0 && state.is(Blocks.ICE)) {
            if (level.dimensionType().ultraWarm()) level.removeBlock(pos, false);
            else level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            return;
        }
        if ((flags & SMELT) != 0 && !state.isAir()) {
            ItemStack in = new ItemStack(state.getBlock().asItem());
            if (in.isEmpty()) return;
            level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), level).ifPresent(r -> {
                ItemStack out = r.value().getResultItem(level.registryAccess());
                if (out.getItem() instanceof BlockItem bi && bi.getBlock() != state.getBlock()) {
                    level.setBlock(pos, bi.getBlock().defaultBlockState(), 3);
                }
            });
        }
    }
}
