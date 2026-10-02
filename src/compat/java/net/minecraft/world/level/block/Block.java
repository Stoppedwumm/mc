package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

import java.util.List;
import java.util.function.Function;

/** A block; mods' blocks get an engine block (and their states as metadata) when registered (reamc-compat). */
@SuppressWarnings({"unused", "deprecation"})
public class Block extends BlockBehaviour implements ItemLike, IBlockExtension {
    public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
    public static final int UPDATE_NEIGHBORS = 1, UPDATE_CLIENTS = 2, UPDATE_INVISIBLE = 4, UPDATE_IMMEDIATE = 8,
            UPDATE_KNOWN_SHAPE = 16, UPDATE_SUPPRESS_DROPS = 32, UPDATE_MOVE_BY_PISTON = 64, UPDATE_NONE = 4,
            UPDATE_ALL = 3, UPDATE_ALL_IMMEDIATE = 11, UPDATE_LIMIT = 512;
    public static final float INDESTRUCTIBLE = -1, INSTANT = 0;

    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    /** The engine block this stands for (set when registered). */
    public mc.world.Block reamc$block;
    private String descriptionId;
    private Holder.Reference<Block> holder = Holder.Reference.createIntrusive(null, this);

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        createBlockStateDefinition(builder);
        stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        registerDefaultState(stateDefinition.any());
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    public void reamc$bindHolder(Holder.Reference<Block> h) { holder = h; }

    public boolean reamc$isModded() { return reamc$block != null && reamc$block.modded; }

    public Holder.Reference<Block> builtInRegistryHolder() { return holder; }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { }

    protected final void registerDefaultState(BlockState state) { defaultBlockState = state; }

    public final BlockState defaultBlockState() { return defaultBlockState; }

    public StateDefinition<Block, BlockState> getStateDefinition() { return stateDefinition; }

    /** This block's default state with the values of any properties it shares with {@code from}. */
    public final BlockState withPropertiesOf(BlockState from) {
        BlockState s = defaultBlockState();
        for (Property<?> p : from.getBlock().getStateDefinition().getProperties()) if (s.hasProperty(p)) s = copy(from, s, p);
        return s;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> p) { return to.setValue(p, from.getValue(p)); }

    public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState(); }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) { }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        spawnDestroyParticles(level, player, pos, state);
        return state;
    }

    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity be, ItemStack tool) {
        player.causeFoodExhaustion(0.005f);
        dropResources(state, level, pos, be, player, tool);
    }

    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) { level.levelEvent(player, 2001, pos, getId(state)); }

    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) { }

    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) { }

    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) { entity.causeFallDamage(distance, 1, entity.damageSources().fall()); }

    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) { entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1)); }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) { }

    public void wasExploded(Level level, BlockPos pos, Explosion explosion) { }

    public boolean dropFromExplosion(Explosion explosion) { return true; }

    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) { }

    public boolean isPossibleToRespawnInThis(BlockState state) { return !state.isSolid() && !state.liquid(); }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) { return new ItemStack(this); }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) { }

    public float getFriction() { return friction; }
    public float getSpeedFactor() { return speedFactor; }
    public float getJumpFactor() { return jumpFactor; }
    public boolean hasDynamicShape() { return dynamicShape; }

    @Override
    public Item asItem() { return mc.mod.Bridge.blockItem(this); }

    @Override protected Block asBlock() { return this; }

    public String getDescriptionId() {
        if (descriptionId == null) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this);
            descriptionId = id == null ? "block.unregistered" : id.toLanguageKey("block");
        }
        return descriptionId;
    }

    public MutableComponent getName() { return Component.translatable(getDescriptionId()); }

    public float getExplosionResistance() { return explosionResistance; }

    protected ImmutableMap<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> f) {
        ImmutableMap.Builder<BlockState, VoxelShape> b = ImmutableMap.builder();
        for (BlockState s : stateDefinition.getPossibleStates()) b.put(s, f.apply(s));
        return b.build();
    }

    protected void popExperience(ServerLevel level, BlockPos pos, int amount) {
        if (amount > 0) ExperienceOrb.award(level, Vec3.atCenterOf(pos), amount);
    }

    protected void tryDropExperience(ServerLevel level, BlockPos pos, ItemStack tool, IntProvider amount) { popExperience(level, pos, amount.sample(level.getRandom())); }

    // ------------------------------------------------------------------ Minecraft's static helpers

    public static int getId(BlockState state) {
        if (state == null) return 0;
        Block b = state.getBlock();
        return (b.reamc$block == null ? 0 : b.reamc$block.id) | state.reamc$index() << 16;
    }

    public static BlockState stateById(int id) { return mc.mod.Bridge.state(id & 0xFFFF, id >>> 16); }

    public static Block byItem(Item item) { return item instanceof net.minecraft.world.item.BlockItem b ? b.getBlock() : Blocks.AIR; }

    public static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) { return Shapes.box(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16); }

    public static BlockState pushEntitiesUp(BlockState from, BlockState to, LevelAccessor level, BlockPos pos) { return to; }

    public static BlockState updateFromNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState s = state;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Direction d : UPDATE_SHAPE_ORDER) {
            m.setWithOffset(pos, d);
            s = s.updateShape(d, level.getBlockState(m), level, pos, m);
        }
        return s;
    }

    public static void updateOrDestroy(BlockState old, BlockState now, LevelAccessor level, BlockPos pos, int flags) { updateOrDestroy(old, now, level, pos, flags, UPDATE_LIMIT); }

    public static void updateOrDestroy(BlockState old, BlockState now, LevelAccessor level, BlockPos pos, int flags, int recursion) {
        if (now == old) return;
        if (now.isAir()) {
            if (!level.isClientSide()) level.destroyBlock(pos, (flags & UPDATE_SUPPRESS_DROPS) == 0, null, recursion);
        } else {
            level.setBlock(pos, now, flags & ~UPDATE_SUPPRESS_DROPS, recursion);
        }
    }

    public static boolean isExceptionForConnection(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(Blocks.BARRIER) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(net.minecraft.tags.BlockTags.SHULKER_BOXES);
    }

    public static boolean shouldRenderFace(BlockState state, BlockGetter level, BlockPos pos, Direction face, BlockPos neighborPos) {
        BlockState n = level.getBlockState(neighborPos);
        if (state.skipRendering(n, face)) return false;
        return !n.canOcclude() || !Shapes.blockOccudes(state.getFaceOcclusionShape(level, pos, face), n.getFaceOcclusionShape(level, neighborPos, face.getOpposite()), face);
    }

    public static boolean canSupportRigidBlock(BlockGetter level, BlockPos pos) { return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP, SupportType.RIGID); }

    public static boolean canSupportCenter(LevelReader level, BlockPos pos, Direction face) {
        BlockState s = level.getBlockState(pos);
        return (face != Direction.DOWN || !s.is(net.minecraft.tags.BlockTags.UNSTABLE_BOTTOM_CENTER)) && s.isFaceSturdy(level, pos, face, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, Direction face) {
        VoxelShape f = shape.getFaceShape(face);
        double area = 0;
        for (var b : f.toAabbs()) area += switch (face.getAxis()) {
            case X -> b.getYsize() * b.getZsize();
            case Y -> b.getXsize() * b.getZsize();
            case Z -> b.getXsize() * b.getYsize();
        };
        return area >= 1 - 1e-6;
    }

    public static boolean isShapeFullBlock(VoxelShape shape) {
        if (shape == Shapes.block()) return true;
        var b = shape.isEmpty() ? null : shape.bounds();
        if (b == null || b.minX > 1e-6 || b.minY > 1e-6 || b.minZ > 1e-6 || b.maxX < 1 - 1e-6 || b.maxY < 1 - 1e-6 || b.maxZ < 1 - 1e-6) return false;
        double v = 0;
        for (var a : shape.toAabbs()) v += a.getXsize() * a.getYsize() * a.getZsize();
        return v >= 1 - 1e-6;
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity be) {
        return state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, be));
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity be, Entity entity, ItemStack tool) {
        return state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool).withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, be));
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel s) for (ItemStack i : getDrops(state, s, pos, null)) popResource(level, pos, i);
    }

    public static void dropResources(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity be) {
        if (level instanceof ServerLevel s) for (ItemStack i : getDrops(state, s, pos, be)) popResource(s, pos, i);
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos, BlockEntity be, Entity entity, ItemStack tool) {
        if (level instanceof ServerLevel s) {
            for (ItemStack i : getDrops(state, s, pos, be, entity, tool)) popResource(level, pos, i);
            state.spawnAfterBreak(s, pos, tool, true);
        }
    }

    public static void popResource(Level level, BlockPos pos, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) return;
        double h = 0.25;
        ItemEntity e = new ItemEntity(level, pos.getX() + 0.5 + net.minecraft.util.Mth.nextDouble(level.random, -h, h),
                pos.getY() + 0.5 + net.minecraft.util.Mth.nextDouble(level.random, -h, h) - 0.125,
                pos.getZ() + 0.5 + net.minecraft.util.Mth.nextDouble(level.random, -h, h), stack);
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }

    public static void popResourceFromFace(Level level, BlockPos pos, Direction face, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) return;
        int x = face.getStepX(), y = face.getStepY(), z = face.getStepZ();
        ItemEntity e = new ItemEntity(level, pos.getX() + 0.5 + (x == 0 ? net.minecraft.util.Mth.nextDouble(level.random, -0.25, 0.25) : x * 0.625),
                pos.getY() + 0.5 + (y == 0 ? net.minecraft.util.Mth.nextDouble(level.random, -0.25, 0.25) : y * 0.625) - 0.125,
                pos.getZ() + 0.5 + (z == 0 ? net.minecraft.util.Mth.nextDouble(level.random, -0.25, 0.25) : z * 0.625), stack,
                x == 0 ? net.minecraft.util.Mth.nextDouble(level.random, -0.1, 0.1) : x * 0.1, y == 0 ? net.minecraft.util.Mth.nextDouble(level.random, 0, 0.1) : y * 0.1 + 0.1,
                z == 0 ? net.minecraft.util.Mth.nextDouble(level.random, -0.1, 0.1) : z * 0.1);
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }

    @Override public String toString() { return "Block{" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this) + "}"; }
}
