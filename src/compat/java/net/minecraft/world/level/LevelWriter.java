package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public interface LevelWriter {
    boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft);

    default boolean setBlock(BlockPos pos, BlockState state, int flags) { return setBlock(pos, state, flags, 512); }

    boolean removeBlock(BlockPos pos, boolean moved);

    default boolean destroyBlock(BlockPos pos, boolean drop) { return destroyBlock(pos, drop, null); }

    default boolean destroyBlock(BlockPos pos, boolean drop, Entity breaker) { return destroyBlock(pos, drop, breaker, 512); }

    boolean destroyBlock(BlockPos pos, boolean drop, Entity breaker, int recursionLeft);

    default boolean addFreshEntity(Entity e) { return false; }
}
