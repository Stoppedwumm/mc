package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Where a block was clicked (reamc-compat). */
public class BlockHitResult {
    private final BlockPos pos;
    private final Direction direction;
    private final double x, y, z;

    public BlockHitResult(double x, double y, double z, Direction direction, BlockPos pos) {
        this.x = x; this.y = y; this.z = z;
        this.direction = direction;
        this.pos = pos;
    }

    public BlockPos getBlockPos() { return pos; }
    public Direction getDirection() { return direction; }
    public double reamc$x() { return x; }
    public double reamc$y() { return y; }
    public double reamc$z() { return z; }
    public boolean isInside() { return false; }
}
