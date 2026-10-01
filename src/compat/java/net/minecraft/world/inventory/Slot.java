package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One slot of a menu: a container slot at a position in the GUI (reamc-compat). */
public class Slot {
    private final int slot;
    public final Container container;
    public int index;
    public final int x;
    public final int y;

    public Slot(Container container, int slot, int x, int y) {
        this.container = container;
        this.slot = slot;
        this.x = x;
        this.y = y;
    }

    public int getContainerSlot() { return slot; }
    public ItemStack getItem() { return container.getItem(slot); }
    public boolean hasItem() { return !getItem().isEmpty(); }

    public void set(ItemStack stack) {
        container.setItem(slot, stack);
        setChanged();
    }

    public void setByPlayer(ItemStack stack) { set(stack); }
    public void setByPlayer(ItemStack stack, ItemStack previous) { setByPlayer(stack); }
    public void setChanged() { container.setChanged(); }
    public boolean mayPlace(ItemStack stack) { return true; }
    public boolean mayPickup(Player player) { return true; }
    public boolean isActive() { return true; }
    public boolean allowModification(Player player) { return mayPickup(player) && mayPlace(getItem()); }
    public int getMaxStackSize() { return container.getMaxStackSize(); }
    public int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }
    public ItemStack remove(int amount) { return container.removeItem(slot, amount); }
    public void onTake(Player player, ItemStack stack) { setChanged(); }
    public void onQuickCraft(ItemStack a, ItemStack b) { }
    public boolean isSameInventory(Slot other) { return container == other.container; }

    /** Places as much of the stack as fits; returns the rest. */
    public ItemStack safeInsert(ItemStack stack, int amount) {
        if (stack.isEmpty() || !mayPlace(stack)) return stack;
        ItemStack in = getItem();
        int n = Math.min(Math.min(amount, stack.getCount()), getMaxStackSize(stack) - in.getCount());
        if (n <= 0) return stack;
        if (in.isEmpty()) setByPlayer(stack.split(n));
        else if (ItemStack.isSameItemSameComponents(in, stack)) {
            stack.shrink(n);
            in.grow(n);
            setByPlayer(in);
        }
        return stack;
    }

    public ItemStack safeInsert(ItemStack stack) { return safeInsert(stack, stack.getCount()); }
}
