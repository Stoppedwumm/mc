package net.minecraft.world.level.chunk;

/** Terrain generation (reamc-compat: reamc's own generator does the work). */
public abstract class ChunkGenerator {
    public static final ChunkGenerator REAMC = new ChunkGenerator() { };

    public int getSeaLevel() { return 63; }
    public int getGenDepth() { return 256; }
    public int getMinY() { return 0; }
}
