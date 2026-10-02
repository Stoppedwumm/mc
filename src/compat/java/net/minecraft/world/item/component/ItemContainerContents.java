package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Items stored in an item, such as a shulker box (reamc-compat). */
public final class ItemContainerContents {
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());
    public static final Codec<ItemContainerContents> CODEC = ItemStack.OPTIONAL_CODEC.listOf().xmap(ItemContainerContents::new, c -> c.items);

    private final List<ItemStack> items;

    private ItemContainerContents(List<ItemStack> items) { this.items = items; }

    public static ItemContainerContents fromItems(List<ItemStack> items) {
        List<ItemStack> l = new ArrayList<>();
        for (ItemStack s : items) l.add(s.copy());
        int last = l.size();
        while (last > 0 && l.get(last - 1).isEmpty()) last--;
        return new ItemContainerContents(List.copyOf(l.subList(0, last)));
    }

    public void copyInto(NonNullList<ItemStack> out) {
        for (int i = 0; i < out.size(); i++) out.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
    }

    public ItemStack copyOne() { return items.isEmpty() ? ItemStack.EMPTY : items.get(0).copy(); }

    public Stream<ItemStack> stream() { return items.stream().map(ItemStack::copy); }

    public Stream<ItemStack> nonEmptyStream() { return items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy); }

    public Iterable<ItemStack> nonEmptyItems() { return items.stream().filter(s -> !s.isEmpty()).toList(); }

    public Iterable<ItemStack> nonEmptyItemsCopy() { return nonEmptyStream().toList(); }

    public int getSlots() { return items.size(); }

    public ItemStack getStackInSlot(int i) { return i < items.size() ? items.get(i).copy() : ItemStack.EMPTY; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof ItemContainerContents c) || c.items.size() != items.size()) return false;
        for (int i = 0; i < items.size(); i++) if (!ItemStack.matches(items.get(i), c.items.get(i))) return false;
        return true;
    }

    @Override public int hashCode() { return items.size(); }
}
