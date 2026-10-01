package net.minecraft.world.entity.player;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * The player's inventory with Minecraft's slot numbers (0-8 hotbar, 9-35 main, 36-39 armor feet to head), backed by
 * reamc's inventory so changes show up at once (reamc-compat).
 */
public class Inventory implements Container {
    public final Player player;

    public Inventory(Player player) { this.player = player; }

    private mc.item.Inventory inv() { return player.reamc$player().inventory; }

    @Override public int getContainerSize() { return 40; }

    @Override
    public boolean isEmpty() {
        for (mc.item.ItemStack s : inv().slots) if (!mc.item.ItemStack.isEmpty(s)) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 36) return ItemStack.reamc$wrap(inv().slots[slot]);
        if (slot < 40) return ItemStack.reamc$wrap(inv().armor[39 - slot]);
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 36) inv().slots[slot] = stack.reamc$handle();
        else if (slot < 40) inv().armor[39 - slot] = stack.reamc$handle();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack s = getItem(slot);
        if (s.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = s.split(amount);
        if (s.isEmpty()) setItem(slot, ItemStack.EMPTY);
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return s;
    }

    public ItemStack getSelected() { return ItemStack.reamc$wrap(inv().held()); }
    public int getSelectionSize() { return 9; }

    /** Adds a stack; true if all of it fitted (the stack keeps any rest). */
    public boolean add(ItemStack stack) {
        if (stack.isEmpty()) return false;
        mc.item.ItemStack left = inv().add(stack.reamc$handle().copy());
        stack.setCount(left == null ? 0 : left.count);
        return stack.isEmpty();
    }

    /** Puts a stack back into the inventory, dropping what doesn't fit. */
    public void placeItemBackInInventory(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!add(stack)) player.drop(stack.copy(), false);
        stack.setCount(0);
    }

    public int findSlotMatchingItem(ItemStack stack) {
        for (int i = 0; i < 36; i++) if (ItemStack.isSameItemSameComponents(getItem(i), stack)) return i;
        return -1;
    }

    public boolean contains(ItemStack stack) { return findSlotMatchingItem(stack) >= 0; }

    @Override public void setChanged() { }
    @Override public boolean stillValid(Player p) { return p == player; }
    @Override public void clearContent() { inv().clear(); }
}
