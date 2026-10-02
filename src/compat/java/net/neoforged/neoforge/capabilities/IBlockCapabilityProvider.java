package net.neoforged.neoforge.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IBlockCapabilityProvider<T, C> {
    T getCapability(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, C context);
}
