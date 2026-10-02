package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;

/** Collision context for an entity, or none (reamc-compat). */
public class EntityCollisionContext implements CollisionContext {
    static final CollisionContext EMPTY = new EntityCollisionContext(null);
    private final Entity entity;

    public EntityCollisionContext(Entity entity) { this.entity = entity; }

    public Entity getEntity() { return entity; }

    @Override public boolean isDescending() { return entity != null && entity.isDescending(); }

    @Override public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) { return entity == null || entity.getY() > pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) - 1e-5; }

    @Override public boolean isHoldingItem(Item item) { return entity instanceof net.minecraft.world.entity.LivingEntity l && l.getMainHandItem().is(item); }

    @Override public boolean canStandOnFluid(FluidState above, FluidState fluid) { return false; }
}
