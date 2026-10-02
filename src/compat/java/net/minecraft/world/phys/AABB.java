package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Optional;

/** An axis-aligned box (reamc-compat). */
public class AABB {
    public final double minX, minY, minZ, maxX, maxY, maxZ;

    public AABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        minX = Math.min(x0, x1); minY = Math.min(y0, y1); minZ = Math.min(z0, z1);
        maxX = Math.max(x0, x1); maxY = Math.max(y0, y1); maxZ = Math.max(z0, z1);
    }

    public AABB(BlockPos p) { this(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1); }

    public AABB(Vec3 a, Vec3 b) { this(a.x, a.y, a.z, b.x, b.y, b.z); }

    public static AABB unitCubeFromLowerCorner(Vec3 v) { return new AABB(v.x, v.y, v.z, v.x + 1, v.y + 1, v.z + 1); }

    public static AABB encapsulatingFullBlocks(BlockPos a, BlockPos b) {
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    public static AABB ofSize(Vec3 c, double w, double h, double d) { return new AABB(c.x - w / 2, c.y - h / 2, c.z - d / 2, c.x + w / 2, c.y + h / 2, c.z + d / 2); }

    public AABB setMinX(double v) { return new AABB(v, minY, minZ, maxX, maxY, maxZ); }
    public AABB setMinY(double v) { return new AABB(minX, v, minZ, maxX, maxY, maxZ); }
    public AABB setMinZ(double v) { return new AABB(minX, minY, v, maxX, maxY, maxZ); }
    public AABB setMaxX(double v) { return new AABB(minX, minY, minZ, v, maxY, maxZ); }
    public AABB setMaxY(double v) { return new AABB(minX, minY, minZ, maxX, v, maxZ); }
    public AABB setMaxZ(double v) { return new AABB(minX, minY, minZ, maxX, maxY, v); }

    public double min(Direction.Axis a) { return a.choose(minX, minY, minZ); }
    public double max(Direction.Axis a) { return a.choose(maxX, maxY, maxZ); }
    public double getXsize() { return maxX - minX; }
    public double getYsize() { return maxY - minY; }
    public double getZsize() { return maxZ - minZ; }
    public double getSize() { return (getXsize() + getYsize() + getZsize()) / 3; }

    public AABB contract(double x, double y, double z) {
        return new AABB(x < 0 ? minX - x : minX, y < 0 ? minY - y : minY, z < 0 ? minZ - z : minZ, x > 0 ? maxX - x : maxX, y > 0 ? maxY - y : maxY, z > 0 ? maxZ - z : maxZ);
    }

    public AABB expandTowards(double x, double y, double z) {
        return new AABB(x < 0 ? minX + x : minX, y < 0 ? minY + y : minY, z < 0 ? minZ + z : minZ, x > 0 ? maxX + x : maxX, y > 0 ? maxY + y : maxY, z > 0 ? maxZ + z : maxZ);
    }

    public AABB expandTowards(Vec3 v) { return expandTowards(v.x, v.y, v.z); }
    public AABB inflate(double x, double y, double z) { return new AABB(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z); }
    public AABB inflate(double d) { return inflate(d, d, d); }
    public AABB deflate(double x, double y, double z) { return inflate(-x, -y, -z); }
    public AABB deflate(double d) { return inflate(-d); }
    public AABB intersect(AABB o) { return new AABB(Math.max(minX, o.minX), Math.max(minY, o.minY), Math.max(minZ, o.minZ), Math.min(maxX, o.maxX), Math.min(maxY, o.maxY), Math.min(maxZ, o.maxZ)); }
    public AABB minmax(AABB o) { return new AABB(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ), Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ)); }
    public AABB move(double x, double y, double z) { return new AABB(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z); }
    public AABB move(BlockPos p) { return move(p.getX(), p.getY(), p.getZ()); }
    public AABB move(Vec3 v) { return move(v.x, v.y, v.z); }
    public boolean intersects(AABB o) { return intersects(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ); }
    public boolean intersects(double x0, double y0, double z0, double x1, double y1, double z1) { return minX < x1 && maxX > x0 && minY < y1 && maxY > y0 && minZ < z1 && maxZ > z0; }
    public boolean intersects(Vec3 a, Vec3 b) { return intersects(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)); }
    public boolean contains(Vec3 v) { return contains(v.x, v.y, v.z); }
    public boolean contains(double x, double y, double z) { return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ; }
    public Vec3 getCenter() { return new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2); }
    public Vec3 getBottomCenter() { return new Vec3((minX + maxX) / 2, minY, (minZ + maxZ) / 2); }
    public Vec3 getMinPosition() { return new Vec3(minX, minY, minZ); }
    public Vec3 getMaxPosition() { return new Vec3(maxX, maxY, maxZ); }
    public boolean hasNaN() { return Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) || Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ); }

    /** Where a segment first enters the box. */
    public Optional<Vec3> clip(Vec3 from, Vec3 to) {
        double t0 = 0, t1 = 1;
        double[] f = {from.x, from.y, from.z}, d = {to.x - from.x, to.y - from.y, to.z - from.z};
        double[] lo = {minX, minY, minZ}, hi = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (f[i] < lo[i] || f[i] > hi[i]) return Optional.empty();
                continue;
            }
            double a = (lo[i] - f[i]) / d[i], b = (hi[i] - f[i]) / d[i];
            t0 = Math.max(t0, Math.min(a, b));
            t1 = Math.min(t1, Math.max(a, b));
            if (t0 > t1) return Optional.empty();
        }
        return Optional.of(from.add(d[0] * t0, d[1] * t0, d[2] * t0));
    }

    @Override public boolean equals(Object o) { return o instanceof AABB b && b.minX == minX && b.minY == minY && b.minZ == minZ && b.maxX == maxX && b.maxY == maxY && b.maxZ == maxZ; }
    @Override public int hashCode() { return java.util.Objects.hash(minX, minY, minZ, maxX, maxY, maxZ); }
    @Override public String toString() { return "AABB[" + minX + ", " + minY + ", " + minZ + "] -> [" + maxX + ", " + maxY + ", " + maxZ + "]"; }
}
