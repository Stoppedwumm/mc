package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

/** Read and write access to one item slot (reamc-compat). */
public interface SlotAccess {
    SlotAccess NULL = new SlotAccess() {
        @Override public ItemStack get() { return ItemStack.EMPTY; }
        @Override public boolean set(ItemStack s) { return false; }
    };

    ItemStack get();

    boolean set(ItemStack stack);
}
