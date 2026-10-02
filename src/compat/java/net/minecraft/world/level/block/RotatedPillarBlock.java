package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** A log-like block lying along an axis (reamc-compat). */
public class RotatedPillarBlock extends Block {
    public static final MapCodec<RotatedPillarBlock> CODEC = simpleCodec(RotatedPillarBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public RotatedPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    @Override protected MapCodec<? extends RotatedPillarBlock> codec() { return CODEC; }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return rotatePillar(state, rotation); }

    public static BlockState rotatePillar(BlockState state, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            Direction.Axis a = state.getValue(AXIS);
            if (a == Direction.Axis.X) return state.setValue(AXIS, Direction.Axis.Z);
            if (a == Direction.Axis.Z) return state.setValue(AXIS, Direction.Axis.X);
        }
        return state;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()); }
}
