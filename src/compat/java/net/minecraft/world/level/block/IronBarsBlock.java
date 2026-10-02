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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Iron bars and glass panes (reamc-compat). */
public class IronBarsBlock extends CrossCollisionBlock {
    public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);

    public IronBarsBlock(Properties properties) {
        super(1, 1, 16, 16, 16, properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false));
    }

    @Override public MapCodec<? extends IronBarsBlock> codec() { return CODEC; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos n = pos.north(), s = pos.south(), w = pos.west(), e = pos.east();
        BlockState ns = level.getBlockState(n), ss = level.getBlockState(s), ws = level.getBlockState(w), es = level.getBlockState(e);
        return defaultBlockState()
                .setValue(NORTH, attachsTo(ns, ns.isFaceSturdy(level, n, Direction.SOUTH)))
                .setValue(SOUTH, attachsTo(ss, ss.isFaceSturdy(level, s, Direction.NORTH)))
                .setValue(WEST, attachsTo(ws, ws.isFaceSturdy(level, w, Direction.EAST)))
                .setValue(EAST, attachsTo(es, es.isFaceSturdy(level, e, Direction.WEST)))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return direction.getAxis().isHorizontal()
                ? state.setValue(PROPERTY_BY_DIRECTION.get(direction), attachsTo(neighbor, neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite())))
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        if (neighbor.is(this)) {
            if (!direction.getAxis().isHorizontal()) return true;
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) && neighbor.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))) return true;
        }
        return super.skipRendering(state, neighbor, direction);
    }

    public final boolean attachsTo(BlockState state, boolean sturdy) {
        return !isExceptionForConnection(state) && sturdy || state.getBlock() instanceof IronBarsBlock || state.is(BlockTags.WALLS);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED); }
}
