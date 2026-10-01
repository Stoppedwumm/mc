package net.neoforged.neoforge.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike {
    protected DeferredItem(ResourceKey<Item> key) { super(key); }

    @Override public Item asItem() { return get(); }

    public ItemStack toStack() { return new ItemStack((ItemLike) this); }
    public ItemStack toStack(int count) { return new ItemStack((ItemLike) this, count); }
}
