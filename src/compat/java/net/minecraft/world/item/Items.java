package net.minecraft.world.item;

/** Built-in items mods refer to directly (reamc-compat: bound to the engine's items by name). */
public final class Items {
    private Items() { }

    public static final Item AIR = mc.mod.Bridge.vanillaItem("minecraft:air");
    public static final Item STICK = mc.mod.Bridge.vanillaItem("minecraft:stick");
    public static final Item DIAMOND = mc.mod.Bridge.vanillaItem("minecraft:diamond");
    public static final Item IRON_INGOT = mc.mod.Bridge.vanillaItem("minecraft:iron_ingot");
    public static final Item GOLD_INGOT = mc.mod.Bridge.vanillaItem("minecraft:gold_ingot");
    public static final Item COAL = mc.mod.Bridge.vanillaItem("minecraft:coal");
    public static final Item APPLE = mc.mod.Bridge.vanillaItem("minecraft:apple");
    public static final Item BREAD = mc.mod.Bridge.vanillaItem("minecraft:bread");
}
