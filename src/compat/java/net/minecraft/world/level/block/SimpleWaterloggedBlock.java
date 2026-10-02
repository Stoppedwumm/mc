package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

/** A block that can stand in water (reamc-compat). */
public interface SimpleWaterloggedBlock extends BucketPickup, LiquidBlockContainer {
    @Override
    default boolean canPlaceLiquid(Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { return fluid == Fluids.WATER; }

    @Override
    default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (state.getValue(BlockStateProperties.WATERLOGGED) || fluid.getType() != Fluids.WATER) return false;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
            level.scheduleTick(pos, fluid.getType(), 5);
        }
        return true;
    }

    @Override
    default ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) return ItemStack.EMPTY;
        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    default Optional<SoundEvent> getPickupSound() { return Optional.of(SoundEvents.BUCKET_FILL); }
}
