package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

/** What a ray hit (reamc-compat). */
public abstract class HitResult {
    protected final Vec3 location;

    protected HitResult(Vec3 location) { this.location = location; }

    public double distanceTo(Entity e) { return location.distanceToSqr(e.getX(), e.getY(), e.getZ()); }

    public abstract Type getType();

    public Vec3 getLocation() { return location; }

    public enum Type { MISS, BLOCK, ENTITY }
}
