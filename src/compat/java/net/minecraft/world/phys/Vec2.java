package net.minecraft.world.phys;

/** A 2D vector (reamc-compat). */
public class Vec2 {
    public static final Vec2 ZERO = new Vec2(0, 0), ONE = new Vec2(1, 1), UNIT_X = new Vec2(1, 0), UNIT_Y = new Vec2(0, 1);
    public final float x, y;

    public Vec2(float x, float y) { this.x = x; this.y = y; }

    public Vec2 scale(float f) { return new Vec2(x * f, y * f); }
    public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 add(float f) { return new Vec2(x + f, y + f); }
    public Vec2 negated() { return new Vec2(-x, -y); }
    public float dot(Vec2 o) { return x * o.x + y * o.y; }
    public float length() { return (float) Math.sqrt(x * x + y * y); }
    public float lengthSquared() { return x * x + y * y; }
    public Vec2 normalized() { float l = length(); return l < 1e-4f ? ZERO : new Vec2(x / l, y / l); }
    public boolean equals(Vec2 o) { return x == o.x && y == o.y; }
}
