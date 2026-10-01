package net.minecraft.world;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Something that holds item stacks in numbered slots (reamc-compat). */
public interface Container {
    int getContainerSize();

    boolean isEmpty();

    ItemStack getItem(int slot);

    ItemStack removeItem(int slot, int amount);

    ItemStack removeItemNoUpdate(int slot);

    void setItem(int slot, ItemStack stack);

    default int getMaxStackSize() { return 99; }

    default int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }

    void setChanged();

    boolean stillValid(Player player);

    default void startOpen(Player player) { }

    default void stopOpen(Player player) { }

    default boolean canPlaceItem(int slot, ItemStack stack) { return true; }

    default boolean canTakeItem(Container target, int slot, ItemStack stack) { return true; }

    void clearContent();

    default int countItem(net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < getContainerSize(); i++) if (getItem(i).getItem() == item) n += getItem(i).getCount();
        return n;
    }
}
