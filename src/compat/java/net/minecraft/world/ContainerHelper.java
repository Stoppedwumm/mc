package net.minecraft.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/** Saves and loads a list of stacks as an "Items" list of {Slot, item} compounds (reamc-compat). */
public final class ContainerHelper {
    private ContainerHelper() { }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider lookup) {
        return saveAllItems(tag, items, true, lookup);
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items, boolean saveEmpty, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            CompoundTag c = s.reamc$save();
            c.putByte("Slot", (byte) i);
            list.add(c);
        }
        if (!list.isEmpty() || saveEmpty) tag.put("Items", list);
        return tag;
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider lookup) {
        ListTag list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int slot = c.getByte("Slot") & 255;
            if (slot < items.size()) items.set(slot, ItemStack.reamc$load(c));
        }
    }

    public static ItemStack removeItem(java.util.List<ItemStack> items, int slot, int amount) {
        return slot >= 0 && slot < items.size() && !items.get(slot).isEmpty() && amount > 0 ? items.get(slot).split(amount) : ItemStack.EMPTY;
    }

    public static ItemStack takeItem(java.util.List<ItemStack> items, int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack s = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return s;
    }
}
