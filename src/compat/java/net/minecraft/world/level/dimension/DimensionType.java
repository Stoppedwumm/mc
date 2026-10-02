package net.minecraft.world.level.dimension;

/** A dimension's properties (reamc-compat: the overworld's and the nether's). */
public record DimensionType(boolean hasSkyLight, boolean hasCeiling, boolean ultraWarm, boolean natural, int minY, int height) {
    public static final DimensionType OVERWORLD = new DimensionType(true, false, false, true, 0, 256);
    public static final DimensionType NETHER = new DimensionType(false, true, true, false, 0, 256);
}
