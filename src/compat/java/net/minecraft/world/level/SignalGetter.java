package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface SignalGetter extends BlockGetter {
    int getSignal(BlockPos pos, Direction from);

    default int getDirectSignal(BlockPos pos, Direction from) { return getSignal(pos, from); }

    default boolean hasSignal(BlockPos pos, Direction from) { return getSignal(pos, from) > 0; }

    default boolean hasNeighborSignal(BlockPos pos) {
        for (Direction d : Direction.values()) if (hasSignal(pos.relative(d), d)) return true;
        return false;
    }

    default int getBestNeighborSignal(BlockPos pos) {
        int best = 0;
        for (Direction d : Direction.values()) best = Math.max(best, getSignal(pos.relative(d), d));
        return best;
    }

    default int getDirectSignalTo(BlockPos pos) { return getBestNeighborSignal(pos); }
}
