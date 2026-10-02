package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiConsumer;

/** A fence gate (reamc-compat). */
public class FenceGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FenceGateBlock> CODEC = simpleCodec(p -> new FenceGateBlock(WoodType.OAK, p));
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN, POWERED = BlockStateProperties.POWERED, IN_WALL = BlockStateProperties.IN_WALL;
    protected static final VoxelShape Z_SHAPE = box(0, 0, 6, 16, 16, 10), X_SHAPE = box(6, 0, 0, 10, 16, 16);
    protected static final VoxelShape Z_SHAPE_LOW = box(0, 0, 6, 16, 13, 10), X_SHAPE_LOW = box(6, 0, 0, 10, 13, 16);
    protected static final VoxelShape Z_COLLISION_SHAPE = box(0, 0, 6, 16, 24, 10), X_COLLISION_SHAPE = box(6, 0, 0, 10, 24, 16);
    protected static final VoxelShape Z_OCCLUSION_SHAPE = Shapes.or(box(0, 5, 7, 2, 16, 9), box(14, 5, 7, 16, 16, 9));
    protected static final VoxelShape X_OCCLUSION_SHAPE = Shapes.or(box(7, 5, 0, 9, 16, 2), box(7, 5, 14, 9, 16, 16));
    protected static final VoxelShape Z_OCCLUSION_SHAPE_LOW = Z_OCCLUSION_SHAPE.move(0, -3 / 16.0, 0), X_OCCLUSION_SHAPE_LOW = X_OCCLUSION_SHAPE.move(0, -3 / 16.0, 0);
    private final SoundEvent openSound, closeSound;

    public FenceGateBlock(WoodType type, Properties properties) { this(properties, type.fenceGateOpen(), type.fenceGateClose()); }

    public FenceGateBlock(Properties properties, SoundEvent openSound, SoundEvent closeSound) {
        super(properties);
        this.openSound = openSound;
        this.closeSound = closeSound;
        registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(POWERED, false).setValue(IN_WALL, false));
    }

    @Override public MapCodec<? extends FenceGateBlock> codec() { return CODEC; }

    private static boolean xAxis(BlockState state) { return state.getValue(FACING).getAxis() == Direction.Axis.X; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(IN_WALL) ? (xAxis(state) ? X_SHAPE_LOW : Z_SHAPE_LOW) : (xAxis(state) ? X_SHAPE : Z_SHAPE);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = direction.getAxis();
        if (state.getValue(FACING).getClockWise().getAxis() != axis) return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        boolean wall = isWall(neighbor) || isWall(level.getBlockState(pos.relative(direction.getOpposite())));
        return state.setValue(IN_WALL, wall);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN) ? Shapes.empty() : (xAxis(state) ? X_COLLISION_SHAPE : Z_COLLISION_SHAPE);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : (xAxis(state) ? X_COLLISION_SHAPE : Z_COLLISION_SHAPE);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(IN_WALL) ? (xAxis(state) ? X_OCCLUSION_SHAPE_LOW : Z_OCCLUSION_SHAPE_LOW) : (xAxis(state) ? X_OCCLUSION_SHAPE : Z_OCCLUSION_SHAPE);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> state.getValue(OPEN);
            case WATER -> false;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean powered = level.hasNeighborSignal(pos);
        Direction facing = context.getHorizontalDirection();
        Direction.Axis axis = facing.getAxis();
        boolean wall = axis == Direction.Axis.Z && (isWall(level.getBlockState(pos.west())) || isWall(level.getBlockState(pos.east())))
                || axis == Direction.Axis.X && (isWall(level.getBlockState(pos.north())) || isWall(level.getBlockState(pos.south())));
        return defaultBlockState().setValue(FACING, facing).setValue(OPEN, powered).setValue(POWERED, powered).setValue(IN_WALL, wall);
    }

    private boolean isWall(BlockState state) { return state.is(BlockTags.WALLS); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN)) {
            state = state.setValue(OPEN, false);
        } else {
            Direction d = player.getDirection();
            if (state.getValue(FACING) == d.getOpposite()) state = state.setValue(FACING, d);
            state = state.setValue(OPEN, true);
        }
        level.setBlock(pos, state, 10);
        boolean open = state.getValue(OPEN);
        level.playSound(player, pos, open ? openSound : closeSound, SoundSource.BLOCKS, 1, level.getRandom().nextFloat() * 0.1f + 0.9f);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<net.minecraft.world.item.ItemStack, BlockPos> drops) {
        if (explosion.canTriggerBlocks() && !state.getValue(POWERED)) {
            boolean open = state.getValue(OPEN);
            level.setBlockAndUpdate(pos, state.setValue(OPEN, !open));
            level.playSound(null, pos, open ? closeSound : openSound, SoundSource.BLOCKS, 1, level.getRandom().nextFloat() * 0.1f + 0.9f);
        }
        super.onExplosionHit(state, level, pos, explosion, drops);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered).setValue(OPEN, powered), 2);
            if (state.getValue(OPEN) != powered) {
                level.playSound(null, pos, powered ? openSound : closeSound, SoundSource.BLOCKS, 1, level.getRandom().nextFloat() * 0.1f + 0.9f);
            }
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, OPEN, POWERED, IN_WALL); }

    public static boolean connectsToDirection(BlockState state, Direction direction) { return state.getValue(FACING).getAxis() == direction.getClockWise().getAxis(); }
}
