package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.neoforged.neoforge.common.util.TriState;

/** A small plant that needs the right ground under it (reamc-compat). */
public abstract class BushBlock extends Block {
    protected BushBlock(Properties properties) { super(properties); }

    @Override protected abstract MapCodec<? extends BushBlock> codec();

    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.getBlock() instanceof FarmBlock || state.is(Blocks.FARMLAND);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState soil = level.getBlockState(below);
        TriState t = soil.canSustainPlant(level, below, Direction.UP, state);
        if (!t.isDefault()) return t.isTrue();
        return mayPlaceOn(soil, level, below);
    }

    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return state.getFluidState().isEmpty(); }

    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return type == PathComputationType.AIR && !hasCollision || super.isPathfindable(state, type); }
}
