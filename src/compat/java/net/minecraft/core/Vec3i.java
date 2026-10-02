package net.minecraft.core;

/** An integer vector (reamc-compat). */
public class Vec3i implements Comparable<Vec3i> {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    private int x, y, z;

    public Vec3i(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    protected Vec3i setX(int v) { x = v; return this; }
    protected Vec3i setY(int v) { y = v; return this; }
    protected Vec3i setZ(int v) { z = v; return this; }

    public Vec3i offset(int dx, int dy, int dz) { return new Vec3i(x + dx, y + dy, z + dz); }
    public Vec3i offset(Vec3i o) { return offset(o.x, o.y, o.z); }
    public Vec3i multiply(int f) { return new Vec3i(x * f, y * f, z * f); }
    public Vec3i above() { return offset(0, 1, 0); }
    public Vec3i below() { return offset(0, -1, 0); }
    public Vec3i relative(Direction d, int n) { return offset(d.getStepX() * n, d.getStepY() * n, d.getStepZ() * n); }
    public Vec3i cross(Vec3i o) { return new Vec3i(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }

    public double distSqr(Vec3i o) {
        double dx = x - o.getX(), dy = y - o.getY(), dz = z - o.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public double distToCenterSqr(double px, double py, double pz) {
        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distToCenterSqr(Position p) { return distToCenterSqr(p.x(), p.y(), p.z()); }

    public boolean closerThan(Vec3i o, double d) { return distSqr(o) < d * d; }

    public int distManhattan(Vec3i o) { return Math.abs(o.getX() - x) + Math.abs(o.getY() - y) + Math.abs(o.getZ() - z); }

    public int get(Direction.Axis axis) { return axis.choose(x, y, z); }

    @Override public boolean equals(Object o) { return o instanceof Vec3i p && p.getX() == x && p.getY() == y && p.getZ() == z; }
    @Override public int hashCode() { return (y + z * 31) * 31 + x; }
    @Override public int compareTo(Vec3i o) { return y != o.getY() ? Integer.compare(y, o.getY()) : z != o.getZ() ? Integer.compare(z, o.getZ()) : Integer.compare(x, o.getX()); }
    @Override public String toString() { return "[" + x + ", " + y + ", " + z + "]"; }
}
