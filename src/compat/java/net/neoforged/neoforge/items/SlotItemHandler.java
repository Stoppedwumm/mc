package net.neoforged.neoforge.items;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A menu slot backed by an item handler (reamc-compat). */
public class SlotItemHandler extends Slot {
    private final IItemHandler itemHandler;
    private final int index;

    public SlotItemHandler(IItemHandler itemHandler, int index, int x, int y) {
        super(new SimpleContainer(0), index, x, y);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public IItemHandler getItemHandler() { return itemHandler; }

    @Override public boolean mayPlace(ItemStack stack) { return !stack.isEmpty() && itemHandler.isItemValid(index, stack); }
    @Override public ItemStack getItem() { return itemHandler.getStackInSlot(index); }

    @Override
    public void set(ItemStack stack) {
        ((IItemHandlerModifiable) itemHandler).setStackInSlot(index, stack);
        setChanged();
    }

    @Override public void setChanged() { }
    @Override public int getMaxStackSize() { return itemHandler.getSlotLimit(index); }

    @Override
    public int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }

    @Override public boolean mayPickup(Player player) { return !itemHandler.extractItem(index, 1, true).isEmpty(); }
    @Override public ItemStack remove(int amount) { return itemHandler.extractItem(index, amount, false); }
}
