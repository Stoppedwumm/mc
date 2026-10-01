package net.minecraft.world;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A plain container of stacks (reamc-compat). */
public class SimpleContainer implements Container {
    private final NonNullList<ItemStack> items;

    public SimpleContainer(int size) { items = NonNullList.withSize(size, ItemStack.EMPTY); }

    @Override public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < items.size()) items.set(slot, stack); }
    @Override public void setChanged() { }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { items.clear(); }
}
