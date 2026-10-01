package net.minecraft.world.level.block;

/** Built-in blocks mods refer to directly (reamc-compat: bound to the engine's blocks by name). */
public final class Blocks {
    private Blocks() { }

    public static final Block AIR = mc.mod.Bridge.vanillaBlock("minecraft:air");
    public static final Block STONE = mc.mod.Bridge.vanillaBlock("minecraft:stone");
    public static final Block DIRT = mc.mod.Bridge.vanillaBlock("minecraft:dirt");
    public static final Block GRASS_BLOCK = mc.mod.Bridge.vanillaBlock("minecraft:grass_block");
    public static final Block COBBLESTONE = mc.mod.Bridge.vanillaBlock("minecraft:cobblestone");
    public static final Block SAND = mc.mod.Bridge.vanillaBlock("minecraft:sand");
    public static final Block GRAVEL = mc.mod.Bridge.vanillaBlock("minecraft:gravel");
    public static final Block GLASS = mc.mod.Bridge.vanillaBlock("minecraft:glass");
    public static final Block WATER = mc.mod.Bridge.vanillaBlock("minecraft:water");
    public static final Block LAVA = mc.mod.Bridge.vanillaBlock("minecraft:lava");
    public static final Block OAK_PLANKS = mc.mod.Bridge.vanillaBlock("minecraft:oak_planks");
    public static final Block IRON_BLOCK = mc.mod.Bridge.vanillaBlock("minecraft:iron_block");
    public static final Block GOLD_BLOCK = mc.mod.Bridge.vanillaBlock("minecraft:gold_block");
    public static final Block DIAMOND_BLOCK = mc.mod.Bridge.vanillaBlock("minecraft:diamond_block");
}
