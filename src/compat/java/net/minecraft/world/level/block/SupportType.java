package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** How much of a face must be solid to hold something up (reamc-compat). */
public enum SupportType {
    FULL {
        @Override public boolean isSupporting(BlockState s, BlockGetter l, BlockPos p, Direction d) { return Block.isFaceFull(s.getBlockSupportShape(l, p), d); }
    },
    CENTER {
        @Override public boolean isSupporting(BlockState s, BlockGetter l, BlockPos p, Direction d) { return covers(s.getBlockSupportShape(l, p).getFaceShape(d), d, 7, 9); }
    },
    RIGID {
        @Override public boolean isSupporting(BlockState s, BlockGetter l, BlockPos p, Direction d) { return covers(s.getBlockSupportShape(l, p).getFaceShape(d), d, 2, 14) || Block.isFaceFull(s.getBlockSupportShape(l, p), d); }
    };

    public abstract boolean isSupporting(BlockState state, BlockGetter level, BlockPos pos, Direction direction);

    /** Whether one box of the face covers the square [lo, hi]/16 on the face's two axes. */
    private static boolean covers(VoxelShape face, Direction d, int lo, int hi) {
        double a = lo / 16.0 + 1e-6, b = hi / 16.0 - 1e-6;
        for (var x : face.toAabbs()) {
            boolean ok = switch (d.getAxis()) {
                case X -> x.minY <= a && x.maxY >= b && x.minZ <= a && x.maxZ >= b;
                case Y -> x.minX <= a && x.maxX >= b && x.minZ <= a && x.maxZ >= b;
                case Z -> x.minX <= a && x.maxX >= b && x.minY <= a && x.maxY >= b;
            };
            if (ok) return true;
        }
        return false;
    }
}
