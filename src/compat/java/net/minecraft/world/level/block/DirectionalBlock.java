package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** A block facing any of the six directions (reamc-compat). */
public abstract class DirectionalBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected DirectionalBlock(Properties properties) { super(properties); }

    @Override protected abstract MapCodec<? extends DirectionalBlock> codec();

    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
}
