package net.minecraft.world.level;

import net.minecraft.core.BlockPos;

/** A chunk's coordinates (reamc-compat). */
public class ChunkPos {
    public static final ChunkPos ZERO = new ChunkPos(0, 0);
    public final int x, z;

    public ChunkPos(int x, int z) { this.x = x; this.z = z; }
    public ChunkPos(BlockPos pos) { this(pos.getX() >> 4, pos.getZ() >> 4); }
    public ChunkPos(long packed) { this((int) packed, (int) (packed >> 32)); }

    public static long asLong(int x, int z) { return x & 0xFFFFFFFFL | (z & 0xFFFFFFFFL) << 32; }
    public long toLong() { return asLong(x, z); }
    public int getMinBlockX() { return x << 4; }
    public int getMinBlockZ() { return z << 4; }
    public int getMaxBlockX() { return (x << 4) + 15; }
    public int getMaxBlockZ() { return (z << 4) + 15; }
    public int getMiddleBlockX() { return (x << 4) + 7; }
    public int getMiddleBlockZ() { return (z << 4) + 7; }
    public BlockPos getWorldPosition() { return new BlockPos(getMinBlockX(), 0, getMinBlockZ()); }
    public BlockPos getBlockAt(int dx, int y, int dz) { return new BlockPos(getMinBlockX() + dx, y, getMinBlockZ() + dz); }
    public BlockPos getMiddleBlockPosition(int y) { return new BlockPos(getMiddleBlockX(), y, getMiddleBlockZ()); }
    public int getChessboardDistance(ChunkPos o) { return Math.max(Math.abs(x - o.x), Math.abs(z - o.z)); }

    @Override public boolean equals(Object o) { return o instanceof ChunkPos c && c.x == x && c.z == z; }
    @Override public int hashCode() { return 1664525 * x + 1013904223 ^ 1664525 * (z ^ -559038737) + 1013904223; }
    @Override public String toString() { return "[" + x + ", " + z + "]"; }
}
