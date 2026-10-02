package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public interface LevelSimulatedReader {
    boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> test);

    boolean isFluidAtPosition(BlockPos pos, Predicate<net.minecraft.world.level.material.FluidState> test);
}
