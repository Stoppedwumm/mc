package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** A block a bucket can take liquid from (reamc-compat). */
public interface BucketPickup {
    ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state);

    Optional<SoundEvent> getPickupSound();

    default Optional<SoundEvent> getPickupSound(BlockState state) { return getPickupSound(); }
}
