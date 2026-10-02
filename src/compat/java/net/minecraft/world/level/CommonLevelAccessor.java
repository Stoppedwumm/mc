package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public interface CommonLevelAccessor extends EntityGetter, LevelReader, LevelSimulatedRW {
    @Override
    default boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> test) { return test.test(getBlockState(pos)); }

    @Override
    default boolean isFluidAtPosition(BlockPos pos, Predicate<net.minecraft.world.level.material.FluidState> test) { return test.test(getFluidState(pos)); }
}
