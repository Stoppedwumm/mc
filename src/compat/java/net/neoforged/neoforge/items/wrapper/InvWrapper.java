package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** An item handler view of a container (reamc-compat). */
public class InvWrapper implements IItemHandlerModifiable {
    private final Container inv;

    public InvWrapper(Container inv) { this.inv = inv; }

    public Container getInv() { return inv; }

    @Override public int getSlots() { return inv.getContainerSize(); }
    @Override public ItemStack getStackInSlot(int slot) { return inv.getItem(slot); }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) { inv.setItem(slot, stack); }

    @Override public int getSlotLimit(int slot) { return inv.getMaxStackSize(); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return inv.canPlaceItem(slot, stack); }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !inv.canPlaceItem(slot, stack)) return stack;
        ItemStack in = inv.getItem(slot);
        int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (!in.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, in)) return stack;
            limit -= in.getCount();
        }
        if (limit <= 0) return stack;
        int n = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (in.isEmpty()) inv.setItem(slot, stack.copyWithCount(n));
            else in.grow(n);
            inv.setChanged();
        }
        return n >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack in = inv.getItem(slot);
        if (amount <= 0 || in.isEmpty()) return ItemStack.EMPTY;
        if (simulate) return in.copyWithCount(Math.min(amount, in.getCount()));
        ItemStack out = inv.removeItem(slot, Math.min(amount, in.getCount()));
        inv.setChanged();
        return out;
    }
}
