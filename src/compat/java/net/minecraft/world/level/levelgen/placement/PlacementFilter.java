package net.minecraft.world.level.levelgen.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.stream.Stream;

public abstract class PlacementFilter extends PlacementModifier {
    @Override
    public final Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return shouldPlace(context, random, pos) ? Stream.of(pos) : Stream.empty();
    }

    protected abstract boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos);
}
