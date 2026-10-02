package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

/** An entity a ray hit (reamc-compat). */
public class EntityHitResult extends HitResult {
    private final Entity entity;

    public EntityHitResult(Entity entity) { this(entity, entity.position()); }

    public EntityHitResult(Entity entity, Vec3 location) {
        super(location);
        this.entity = entity;
    }

    public Entity getEntity() { return entity; }

    @Override public Type getType() { return Type.ENTITY; }
}
