package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A campfire (reamc-compat: Minecraft's own campfire is reamc's; this is for type checks and mods). */
public class CampfireBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<CampfireBlock> CODEC = simpleCodec(p -> new CampfireBlock(true, 1, p));
    protected static final VoxelShape SHAPE = box(0, 0, 0, 16, 7, 16);
    public static final BooleanProperty LIT = BlockStateProperties.LIT, SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE, WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean spawnParticles;
    private final int fireDamage;

    public CampfireBlock(boolean spawnParticles, int fireDamage, Properties properties) {
        super(properties);
        this.spawnParticles = spawnParticles;
        this.fireDamage = fireDamage;
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
    }

    @Override public MapCodec<CampfireBlock> codec() { return CODEC; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(LIT) && entity instanceof net.minecraft.world.entity.LivingEntity) entity.hurt(level.damageSources().campfire(), fireDamage);
        super.entityInside(state, level, pos, entity);
    }

    public static boolean isLitCampfire(BlockState state) {
        return state.hasProperty(LIT) && state.is(BlockTags.CAMPFIRES) && state.getValue(LIT) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    public static boolean canLight(BlockState state) {
        return state.is(BlockTags.CAMPFIRES, s -> s.hasProperty(WATERLOGGED) && s.hasProperty(LIT)) && !state.getValue(WATERLOGGED) && !state.getValue(LIT);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING); }
}
