package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fence (reamc-compat). */
public class FenceBlock extends CrossCollisionBlock {
    public static final MapCodec<FenceBlock> CODEC = simpleCodec(FenceBlock::new);
    private final VoxelShape[] occlusionByIndex;

    public FenceBlock(Properties properties) {
        super(2, 2, 16, 16, 24, properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false));
        occlusionByIndex = makeShapes(2, 1, 16, 6, 15);
    }

    @Override public MapCodec<FenceBlock> codec() { return CODEC; }

    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return occlusionByIndex[getAABBIndex(state)]; }

    @Override protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }

    public boolean connectsTo(BlockState state, boolean sturdy, Direction direction) {
        Block b = state.getBlock();
        boolean sameFence = state.is(BlockTags.FENCES) && state.is(BlockTags.WOODEN_FENCES) == defaultBlockState().is(BlockTags.WOODEN_FENCES);
        boolean gate = b instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
        return !isExceptionForConnection(state) && sturdy || sameFence || gate;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos n = pos.north(), e = pos.east(), s = pos.south(), w = pos.west();
        BlockState ns = level.getBlockState(n), es = level.getBlockState(e), ss = level.getBlockState(s), ws = level.getBlockState(w);
        return super.getStateForPlacement(context)
                .setValue(NORTH, connectsTo(ns, ns.isFaceSturdy(level, n, Direction.SOUTH), Direction.SOUTH))
                .setValue(EAST, connectsTo(es, es.isFaceSturdy(level, e, Direction.WEST), Direction.WEST))
                .setValue(SOUTH, connectsTo(ss, ss.isFaceSturdy(level, s, Direction.NORTH), Direction.NORTH))
                .setValue(WEST, connectsTo(ws, ws.isFaceSturdy(level, w, Direction.EAST), Direction.EAST))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return direction.getAxis().isHorizontal()
                ? state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite()), direction.getOpposite()))
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED); }
}
