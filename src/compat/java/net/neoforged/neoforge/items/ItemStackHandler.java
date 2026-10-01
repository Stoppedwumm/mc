package net.neoforged.neoforge.items;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** A plain list of stacks with insert/extract rules (reamc-compat). */
public class ItemStackHandler implements IItemHandlerModifiable {
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler() { this(1); }

    public ItemStackHandler(int size) { stacks = NonNullList.withSize(size, ItemStack.EMPTY); }

    public ItemStackHandler(NonNullList<ItemStack> stacks) { this.stacks = stacks; }

    public void setSize(int size) { stacks = NonNullList.withSize(size, ItemStack.EMPTY); }

    @Override public int getSlots() { return stacks.size(); }
    @Override public ItemStack getStackInSlot(int slot) { return stacks.get(slot); }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override public int getSlotLimit(int slot) { return 99; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }

    protected int getStackLimit(int slot, ItemStack stack) { return Math.min(getSlotLimit(slot), stack.getMaxStackSize()); }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        ItemStack in = stacks.get(slot);
        int limit = getStackLimit(slot, stack);
        if (!in.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, in)) return stack;
            limit -= in.getCount();
        }
        if (limit <= 0) return stack;
        boolean tooMany = stack.getCount() > limit;
        if (!simulate) {
            if (in.isEmpty()) stacks.set(slot, tooMany ? stack.copyWithCount(limit) : stack);
            else in.grow(tooMany ? limit : stack.getCount());
            onContentsChanged(slot);
        }
        return tooMany ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack in = stacks.get(slot);
        if (in.isEmpty()) return ItemStack.EMPTY;
        int n = Math.min(amount, in.getMaxStackSize());
        if (in.getCount() <= n) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
                return in;
            }
            return in.copy();
        }
        if (!simulate) {
            stacks.set(slot, in.copyWithCount(in.getCount() - n));
            onContentsChanged(slot);
        }
        return in.copyWithCount(n);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, stacks, provider);
        tag.putInt("Size", stacks.size());
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setSize(tag.contains("Size") ? tag.getInt("Size") : stacks.size());
        ContainerHelper.loadAllItems(tag, stacks, provider);
    }

    protected void onContentsChanged(int slot) { }
}
