package net.minecraft.world.level;

public interface CollisionGetter extends BlockGetter {
    default boolean isUnobstructed(net.minecraft.world.entity.Entity e) { return true; }
}
