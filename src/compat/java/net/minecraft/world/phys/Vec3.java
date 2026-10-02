package net.minecraft.world.phys;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;

/** An immutable 3D vector (reamc-compat). */
public class Vec3 implements Position {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public final double x, y, z;

    public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public Vec3(org.joml.Vector3f v) { this(v.x, v.y, v.z); }

    public static Vec3 atLowerCornerOf(Vec3i p) { return new Vec3(p.getX(), p.getY(), p.getZ()); }
    public static Vec3 atLowerCornerWithOffset(Vec3i p, double dx, double dy, double dz) { return new Vec3(p.getX() + dx, p.getY() + dy, p.getZ() + dz); }
    public static Vec3 atCenterOf(Vec3i p) { return atLowerCornerWithOffset(p, 0.5, 0.5, 0.5); }
    public static Vec3 atBottomCenterOf(Vec3i p) { return atLowerCornerWithOffset(p, 0.5, 0, 0.5); }
    public static Vec3 upFromBottomCenterOf(Vec3i p, double dy) { return atLowerCornerWithOffset(p, 0.5, dy, 0.5); }

    public static Vec3 directionFromRotation(float xRot, float yRot) {
        double f = Math.cos(-yRot * (Math.PI / 180) - Math.PI), f1 = Math.sin(-yRot * (Math.PI / 180) - Math.PI);
        double f2 = -Math.cos(-xRot * (Math.PI / 180)), f3 = Math.sin(-xRot * (Math.PI / 180));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double z() { return z; }

    public Vec3 add(double dx, double dy, double dz) { return new Vec3(x + dx, y + dy, z + dz); }
    public Vec3 add(Vec3 o) { return add(o.x, o.y, o.z); }
    public Vec3 subtract(double dx, double dy, double dz) { return add(-dx, -dy, -dz); }
    public Vec3 subtract(Vec3 o) { return add(-o.x, -o.y, -o.z); }
    public Vec3 vectorTo(Vec3 o) { return o.subtract(this); }
    public Vec3 scale(double f) { return new Vec3(x * f, y * f, z * f); }
    public Vec3 multiply(double fx, double fy, double fz) { return new Vec3(x * fx, y * fy, z * fz); }
    public Vec3 multiply(Vec3 o) { return multiply(o.x, o.y, o.z); }
    public Vec3 reverse() { return scale(-1); }
    public Vec3 normalize() { double l = length(); return l < 1e-4 ? ZERO : new Vec3(x / l, y / l, z / l); }
    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public double length() { return Math.sqrt(lengthSqr()); }
    public double lengthSqr() { return x * x + y * y + z * z; }
    public double horizontalDistance() { return Math.sqrt(x * x + z * z); }
    public double horizontalDistanceSqr() { return x * x + z * z; }
    public double distanceTo(Vec3 o) { return Math.sqrt(distanceToSqr(o)); }
    public double distanceToSqr(Vec3 o) { return distanceToSqr(o.x, o.y, o.z); }
    public double distanceToSqr(double px, double py, double pz) { double dx = px - x, dy = py - y, dz = pz - z; return dx * dx + dy * dy + dz * dz; }
    public boolean closerThan(Position p, double d) { return distanceToSqr(p.x(), p.y(), p.z()) < d * d; }
    public Vec3 with(Direction.Axis a, double v) { return new Vec3(a == Direction.Axis.X ? v : x, a == Direction.Axis.Y ? v : y, a == Direction.Axis.Z ? v : z); }
    public double get(Direction.Axis a) { return a.choose(x, y, z); }
    public Vec3 relative(Direction d, double n) { return add(d.getStepX() * n, d.getStepY() * n, d.getStepZ() * n); }
    public Vec3 xRot(float a) { double c = Math.cos(a), s = Math.sin(a); return new Vec3(x, y * c + z * s, z * c - y * s); }
    public Vec3 yRot(float a) { double c = Math.cos(a), s = Math.sin(a); return new Vec3(x * c + z * s, y, z * c - x * s); }
    public Vec3 zRot(float a) { double c = Math.cos(a), s = Math.sin(a); return new Vec3(x * c + y * s, y * c - x * s, z); }
    public Vec3 lerp(Vec3 o, double t) { return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t); }
    public org.joml.Vector3f toVector3f() { return new org.joml.Vector3f((float) x, (float) y, (float) z); }

    @Override public boolean equals(Object o) { return o instanceof Vec3 v && v.x == x && v.y == y && v.z == z; }
    @Override public int hashCode() { return Double.hashCode(x) * 31 * 31 + Double.hashCode(y) * 31 + Double.hashCode(z); }
    @Override public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
}
