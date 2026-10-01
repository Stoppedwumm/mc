package net.minecraft.world;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** A container with per-side access (hoppers, pipes) (reamc-compat). */
public interface WorldlyContainer extends Container {
    int[] getSlotsForFace(Direction side);

    boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side);

    boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side);
}
