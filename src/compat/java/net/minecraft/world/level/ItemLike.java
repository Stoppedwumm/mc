package net.minecraft.world.level;

/** Anything that stands for an item: items, blocks, registry holders (reamc-compat). */
public interface ItemLike {
    net.minecraft.world.item.Item asItem();
}
