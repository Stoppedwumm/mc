package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** A block bone meal works on (reamc-compat). */
public interface BonemealableBlock {
    boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state);

    boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state);

    void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state);

    default BlockPos getParticlePos(BlockPos pos) {
        return switch (getType()) {
            case NEIGHBOR_SPREADER -> pos.above();
            case GROWER -> pos;
        };
    }

    default Type getType() { return Type.GROWER; }

    static boolean hasSpreadableNeighbourPos(LevelReader level, BlockPos pos, BlockState state) { return findSpreadableNeighbourPos(level, pos, state).isPresent(); }

    static java.util.Optional<BlockPos> findSpreadableNeighbourPos(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) { return findSpreadableNeighbourPos((LevelReader) level, pos, state); }

    private static java.util.Optional<BlockPos> findSpreadableNeighbourPos(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = pos.relative(d);
            if (level.isEmptyBlock(p) && state.canSurvive(level, p)) return java.util.Optional.of(p);
        }
        return java.util.Optional.empty();
    }

    enum Type { NEIGHBOR_SPREADER, GROWER }
}
