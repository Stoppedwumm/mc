package net.minecraft.world.entity;

/** An entity's size (reamc-compat). */
public record EntityDimensions(float width, float height, float eyeHeight, boolean fixed) {
    public static EntityDimensions scalable(float w, float h) { return new EntityDimensions(w, h, h * 0.85f, false); }
    public static EntityDimensions fixed(float w, float h) { return new EntityDimensions(w, h, h * 0.85f, true); }
    public EntityDimensions scale(float f) { return fixed ? this : new EntityDimensions(width * f, height * f, eyeHeight * f, false); }
    public EntityDimensions withEyeHeight(float e) { return new EntityDimensions(width, height, e, fixed); }
}
