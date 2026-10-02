package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** A post with arms toward its four neighbours, like fences and panes (reamc-compat). */
public abstract class CrossCollisionBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST, WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    protected final VoxelShape[] collisionShapeByIndex, shapeByIndex;

    protected CrossCollisionBlock(float nodeWidth, float extensionWidth, float nodeHeight, float extensionHeight, float collisionHeight, Properties properties) {
        super(properties);
        collisionShapeByIndex = makeShapes(nodeWidth, extensionWidth, collisionHeight, 0, collisionHeight);
        shapeByIndex = makeShapes(nodeWidth, extensionWidth, nodeHeight, 0, extensionHeight);
    }

    @Override protected abstract MapCodec<? extends CrossCollisionBlock> codec();

    protected VoxelShape[] makeShapes(float nodeWidth, float extensionWidth, float nodeHeight, float extensionBottom, float extensionHeight) {
        float a = 8 - nodeWidth, b = 8 + nodeWidth, c = 8 - extensionWidth, d = 8 + extensionWidth;
        VoxelShape post = Block.box(a, 0, a, b, nodeHeight, b);
        VoxelShape north = Block.box(c, extensionBottom, 0, d, extensionHeight, d), south = Block.box(c, extensionBottom, c, d, extensionHeight, 16);
        VoxelShape west = Block.box(0, extensionBottom, c, d, extensionHeight, d), east = Block.box(c, extensionBottom, c, 16, extensionHeight, d);
        VoxelShape[] out = new VoxelShape[16];
        for (int i = 0; i < 16; i++) {
            VoxelShape s = post;
            if ((i & 1) != 0) s = Shapes.or(s, south);
            if ((i & 2) != 0) s = Shapes.or(s, west);
            if ((i & 4) != 0) s = Shapes.or(s, north);
            if ((i & 8) != 0) s = Shapes.or(s, east);
            out[i] = s;
        }
        return out;
    }

    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !state.getValue(WATERLOGGED); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeByIndex[getAABBIndex(state)]; }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return collisionShapeByIndex[getAABBIndex(state)]; }

    protected int getAABBIndex(BlockState state) {
        int i = 0;
        if (state.getValue(SOUTH)) i |= 1;
        if (state.getValue(WEST)) i |= 2;
        if (state.getValue(NORTH)) i |= 4;
        if (state.getValue(EAST)) i |= 8;
        return i;
    }

    @Override protected FluidState getFluidState(BlockState state) { return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }

    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST)).setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH)).setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }
}
