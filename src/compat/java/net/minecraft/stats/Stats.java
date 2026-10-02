package net.minecraft.stats;

import net.minecraft.resources.ResourceLocation;

/** Statistic types (reamc-compat). */
public final class Stats {
    private Stats() { }

    public static final StatType<net.minecraft.world.level.block.Block> BLOCK_MINED = new StatType<>(null, null);
    public static final StatType<net.minecraft.world.item.Item> ITEM_CRAFTED = new StatType<>(null, null);
    public static final StatType<net.minecraft.world.item.Item> ITEM_USED = new StatType<>(null, null);
    public static final StatType<net.minecraft.world.item.Item> ITEM_BROKEN = new StatType<>(null, null);
    public static final StatType<net.minecraft.world.item.Item> ITEM_PICKED_UP = new StatType<>(null, null);
    public static final StatType<net.minecraft.world.item.Item> ITEM_DROPPED = new StatType<>(null, null);
    public static final StatType<ResourceLocation> CUSTOM = new StatType<>(null, null);
    public static final ResourceLocation INTERACT_WITH_CRAFTING_TABLE = ResourceLocation.withDefaultNamespace("interact_with_crafting_table");
    public static final ResourceLocation INTERACT_WITH_FURNACE = ResourceLocation.withDefaultNamespace("interact_with_furnace");
    public static final ResourceLocation INTERACT_WITH_SMOKER = ResourceLocation.withDefaultNamespace("interact_with_smoker");
    public static final ResourceLocation OPEN_CHEST = ResourceLocation.withDefaultNamespace("open_chest");
    public static final ResourceLocation OPEN_BARREL = ResourceLocation.withDefaultNamespace("open_barrel");
    public static final ResourceLocation EAT_CAKE_SLICE = ResourceLocation.withDefaultNamespace("eat_cake_slice");
}
