package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class BlockTags {
    private BlockTags() { }

    private static TagKey<Block> tag(String name) { return TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace(name)); }

    public static final TagKey<Block> MINEABLE_WITH_PICKAXE = tag("mineable/pickaxe");
    public static final TagKey<Block> MINEABLE_WITH_AXE = tag("mineable/axe");
    public static final TagKey<Block> MINEABLE_WITH_SHOVEL = tag("mineable/shovel");
    public static final TagKey<Block> MINEABLE_WITH_HOE = tag("mineable/hoe");
    public static final TagKey<Block> NEEDS_STONE_TOOL = tag("needs_stone_tool");
    public static final TagKey<Block> NEEDS_IRON_TOOL = tag("needs_iron_tool");
    public static final TagKey<Block> NEEDS_DIAMOND_TOOL = tag("needs_diamond_tool");
    public static final TagKey<Block> INCORRECT_FOR_WOODEN_TOOL = tag("incorrect_for_wooden_tool");
    public static final TagKey<Block> INCORRECT_FOR_STONE_TOOL = tag("incorrect_for_stone_tool");
    public static final TagKey<Block> INCORRECT_FOR_IRON_TOOL = tag("incorrect_for_iron_tool");
    public static final TagKey<Block> INCORRECT_FOR_GOLD_TOOL = tag("incorrect_for_gold_tool");
    public static final TagKey<Block> INCORRECT_FOR_DIAMOND_TOOL = tag("incorrect_for_diamond_tool");
    public static final TagKey<Block> INCORRECT_FOR_NETHERITE_TOOL = tag("incorrect_for_netherite_tool");
    public static final TagKey<Block> STONE_ORE_REPLACEABLES = tag("stone_ore_replaceables");
}
