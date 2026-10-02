package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

/** Who is asking for a block's collision shape (reamc-compat). */
public interface CollisionContext {
    static CollisionContext empty() { return EntityCollisionContext.EMPTY; }

    static CollisionContext of(Entity e) { return new EntityCollisionContext(e); }

    boolean isDescending();

    boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend);

    boolean isHoldingItem(net.minecraft.world.item.Item item);

    boolean canStandOnFluid(FluidState above, FluidState fluid);
}
