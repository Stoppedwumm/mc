package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A farmland crop that grows through its ages (reamc-compat). */
public class CropBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape[] SHAPE_BY_AGE = {
            box(0, 0, 0, 16, 2, 16), box(0, 0, 0, 16, 4, 16), box(0, 0, 0, 16, 6, 16), box(0, 0, 0, 16, 8, 16),
            box(0, 0, 0, 16, 10, 16), box(0, 0, 0, 16, 12, 16), box(0, 0, 0, 16, 14, 16), box(0, 0, 0, 16, 16, 16)};

    public CropBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(getAgeProperty(), 0));
    }

    @Override public MapCodec<? extends CropBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[Math.min(SHAPE_BY_AGE.length - 1, getAge(state) * 8 / (getMaxAge() + 1))];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return state.getBlock() instanceof FarmBlock || state.is(Blocks.FARMLAND); }

    protected IntegerProperty getAgeProperty() { return AGE; }

    public int getMaxAge() { return MAX_AGE; }

    public int getAge(BlockState state) { return state.getValue(getAgeProperty()); }

    public BlockState getStateForAge(int age) { return defaultBlockState().setValue(getAgeProperty(), age); }

    public final boolean isMaxAge(BlockState state) { return getAge(state) >= getMaxAge(); }

    @Override protected boolean isRandomlyTicking(BlockState state) { return !isMaxAge(state); }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) >= 9) {
            int age = getAge(state);
            if (age < getMaxAge()) {
                float speed = getGrowthSpeed(state, level, pos);
                if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0f / speed) + 1) == 0)) {
                    level.setBlock(pos, getStateForAge(age + 1), 2);
                    net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
                }
            }
        }
    }

    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int age = Math.min(getMaxAge(), getAge(state) + getBonemealAgeIncrease(level));
        level.setBlock(pos, getStateForAge(age), 2);
    }

    protected int getBonemealAgeIncrease(Level level) { return Mth.nextInt(level.random, 2, 5); }

    /** How fast a crop grows: better with moist soil around it, slower when crowded by its own kind. */
    protected static float getGrowthSpeed(BlockState state, BlockGetter level, BlockPos pos) {
        Block block = state.getBlock();
        float speed = 1;
        BlockPos below = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float f = 0;
                BlockPos p = below.offset(dx, 0, dz);
                BlockState soil = level.getBlockState(p);
                var sustain = soil.canSustainPlant(level, p, net.minecraft.core.Direction.UP, state);
                if (sustain.isDefault() ? soil.getBlock() instanceof FarmBlock || soil.is(Blocks.FARMLAND) : sustain.isTrue()) {
                    f = soil.isFertile(level, p) ? 3 : 1;
                }
                if (dx != 0 || dz != 0) f /= 4;
                speed += f;
            }
        }
        BlockPos n = pos.north(), s = pos.south(), w = pos.west(), e = pos.east();
        boolean row = level.getBlockState(w).is(block) || level.getBlockState(e).is(block);
        boolean column = level.getBlockState(n).is(block) || level.getBlockState(s).is(block);
        if (row && column) speed /= 2;
        else if (level.getBlockState(w.north()).is(block) || level.getBlockState(e.north()).is(block)
                || level.getBlockState(e.south()).is(block) || level.getBlockState(w.south()).is(block)) speed /= 2;
        return speed;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return hasSufficientLight(level, pos) && super.canSurvive(state, level, pos); }

    protected static boolean hasSufficientLight(LevelReader level, BlockPos pos) { return level.getRawBrightness(pos, 0) >= 8; }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
    }

    protected ItemLike getBaseSeedId() { return Items.WHEAT_SEEDS; }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) { return new ItemStack(getBaseSeedId()); }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return !isMaxAge(state); }

    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }

    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { growCrops(level, pos, state); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
}
