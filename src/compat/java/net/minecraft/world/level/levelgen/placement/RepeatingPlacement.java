package net.minecraft.world.level.levelgen.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class RepeatingPlacement extends PlacementModifier {
    protected abstract int count(RandomSource random, BlockPos pos);

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return IntStream.range(0, count(random, pos)).mapToObj(i -> pos);
    }
}
