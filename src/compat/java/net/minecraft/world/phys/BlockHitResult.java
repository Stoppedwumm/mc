package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Where a block was clicked (reamc-compat). */
public class BlockHitResult extends HitResult {
    private final BlockPos pos;
    private final Direction direction;
    private final boolean miss, inside;

    public BlockHitResult(Vec3 location, Direction direction, BlockPos pos, boolean inside) { this(false, location, direction, pos, inside); }

    private BlockHitResult(boolean miss, Vec3 location, Direction direction, BlockPos pos, boolean inside) {
        super(location);
        this.miss = miss;
        this.direction = direction;
        this.pos = pos;
        this.inside = inside;
    }

    /** reamc's own constructor (click point, face, block). */
    public BlockHitResult(double x, double y, double z, Direction direction, BlockPos pos) { this(new Vec3(x, y, z), direction, pos, false); }

    public static BlockHitResult miss(Vec3 location, Direction direction, BlockPos pos) { return new BlockHitResult(true, location, direction, pos, false); }

    public BlockHitResult withDirection(Direction d) { return new BlockHitResult(miss, location, d, pos, inside); }
    public BlockHitResult withPosition(BlockPos p) { return new BlockHitResult(miss, location, direction, p, inside); }
    public BlockPos getBlockPos() { return pos; }
    public Direction getDirection() { return direction; }
    @Override public Type getType() { return miss ? Type.MISS : Type.BLOCK; }
    public boolean isInside() { return inside; }
    public double reamc$x() { return location.x; }
    public double reamc$y() { return location.y; }
    public double reamc$z() { return location.z; }
}
