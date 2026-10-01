package net.minecraft.core;

/** An immutable block position (reamc-compat). */
public class BlockPos implements Comparable<BlockPos> {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);
    private final int x, y, z;

    public BlockPos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public BlockPos offset(int dx, int dy, int dz) { return new BlockPos(x + dx, y + dy, z + dz); }
    public BlockPos relative(Direction d) { return offset(d.getStepX(), d.getStepY(), d.getStepZ()); }
    public BlockPos relative(Direction d, int n) { return offset(d.getStepX() * n, d.getStepY() * n, d.getStepZ() * n); }
    public BlockPos above() { return offset(0, 1, 0); }
    public BlockPos above(int n) { return offset(0, n, 0); }
    public BlockPos below() { return offset(0, -1, 0); }
    public BlockPos below(int n) { return offset(0, -n, 0); }
    public BlockPos north() { return offset(0, 0, -1); }
    public BlockPos south() { return offset(0, 0, 1); }
    public BlockPos west() { return offset(-1, 0, 0); }
    public BlockPos east() { return offset(1, 0, 0); }
    public BlockPos immutable() { return this; }

    public double distSqr(BlockPos o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public long asLong() { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF); }

    public static BlockPos of(long packed) {
        int px = (int) (packed >> 38), pz = (int) (packed << 26 >> 38), py = (int) (packed << 52 >> 52);
        return new BlockPos(px, py, pz);
    }

    @Override public boolean equals(Object o) { return o instanceof BlockPos p && p.x == x && p.y == y && p.z == z; }
    @Override public int hashCode() { return (y + z * 31) * 31 + x; }
    @Override public String toString() { return "BlockPos{x=" + x + ", y=" + y + ", z=" + z + "}"; }
    @Override public int compareTo(BlockPos o) { return y != o.y ? Integer.compare(y, o.y) : z != o.z ? Integer.compare(z, o.z) : Integer.compare(x, o.x); }
}
