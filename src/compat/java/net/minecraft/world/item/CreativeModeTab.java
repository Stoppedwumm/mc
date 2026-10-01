package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** A creative inventory tab (reamc-compat: its items are listed, in order, in reamc's creative inventory). */
public class CreativeModeTab {
    private final Component title;
    private final Supplier<ItemStack> icon;
    private final DisplayItemsGenerator generator;

    CreativeModeTab(Component title, Supplier<ItemStack> icon, DisplayItemsGenerator generator) {
        this.title = title;
        this.icon = icon;
        this.generator = generator;
    }

    public static Builder builder() { return new Builder(); }

    public Component getDisplayName() { return title; }
    public ItemStack getIconItem() { return icon == null ? ItemStack.EMPTY : icon.get(); }

    /** The tab's items, in the order the mod lists them. */
    public List<ItemStack> reamc$items() {
        List<ItemStack> out = new ArrayList<>();
        if (generator != null) generator.accept(new ItemDisplayParameters(), (stack, visibility) -> out.add(stack));
        return out;
    }

    public enum TabVisibility { PARENT_AND_SEARCH_TABS, PARENT_TAB_ONLY, SEARCH_TAB_ONLY }

    public static class ItemDisplayParameters {
        public boolean hasPermissions() { return false; }
    }

    @FunctionalInterface
    public interface DisplayItemsGenerator {
        void accept(ItemDisplayParameters parameters, Output output);
    }

    @FunctionalInterface
    public interface Output {
        void accept(ItemStack stack, TabVisibility visibility);

        default void accept(ItemStack stack) { accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS); }
        default void accept(ItemLike item) { accept(new ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS); }
        default void accept(ItemLike item, TabVisibility visibility) { accept(new ItemStack(item), visibility); }
        default void acceptAll(Collection<ItemStack> stacks) { for (ItemStack s : stacks) accept(s); }
    }

    public static class Builder {
        private Component title = Component.empty();
        private Supplier<ItemStack> icon;
        private DisplayItemsGenerator generator;

        public Builder title(Component t) { title = t; return this; }
        public Builder icon(Supplier<ItemStack> i) { icon = i; return this; }
        public Builder displayItems(DisplayItemsGenerator g) { generator = g; return this; }
        public Builder withTabsBefore(Object... tabs) { return this; }
        public Builder withTabsAfter(Object... tabs) { return this; }
        public Builder withSearchBar() { return this; }
        public Builder noScrollBar() { return this; }
        public CreativeModeTab build() { return new CreativeModeTab(title, icon, generator); }
    }
}
