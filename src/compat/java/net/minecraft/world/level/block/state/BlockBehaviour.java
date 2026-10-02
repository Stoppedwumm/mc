package net.minecraft.world.level.block.state;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/** The overridable behaviour of blocks, with Minecraft's defaults; reamc calls these from its hooks (reamc-compat). */
@SuppressWarnings({"unused", "deprecation", "unchecked", "rawtypes"})
public abstract class BlockBehaviour implements FeatureElement {
    protected static final Direction[] UPDATE_SHAPE_ORDER = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};
    protected final boolean hasCollision;
    protected final float explosionResistance;
    protected final boolean isRandomlyTicking;
    protected final SoundType soundType;
    protected final float friction, speedFactor, jumpFactor;
    protected final boolean dynamicShape;
    protected final FeatureFlagSet requiredFeatures = FeatureFlagSet.of();
    protected final Properties properties;
    protected ResourceKey<net.minecraft.world.level.storage.loot.LootTable> drops;

    public BlockBehaviour(Properties properties) {
        this.properties = properties;
        hasCollision = !properties.noCollision;
        explosionResistance = properties.explosionResistance;
        isRandomlyTicking = properties.randomTicks;
        soundType = properties.soundType;
        friction = properties.friction;
        speedFactor = properties.speedFactor;
        jumpFactor = properties.jumpFactor;
        dynamicShape = properties.dynamicShape;
    }

    public Properties properties() { return properties; }

    public Properties reamc$properties() { return properties; }

    protected abstract MapCodec<? extends Block> codec();

    protected static <B extends Block> RecordCodecBuilder<B, Properties> propertiesCodec() { return Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties); }

    public static <B extends Block> MapCodec<B> simpleCodec(Function<Properties, B> f) {
        return RecordCodecBuilder.mapCodec(i -> i.group(BlockBehaviour.<B>propertiesCodec()).apply(i, f));
    }

    @Override public FeatureFlagSet requiredFeatures() { return requiredFeatures; }

    // ------------------------------------------------------------------ Minecraft's overridables

    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursion) { }
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND -> !state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER -> state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            case AIR -> !state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        };
    }
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { return state; }
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) { return false; }
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) { }
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) { }
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) level.removeBlockEntity(pos);
    }
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> drops) { }
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return InteractionResult.PASS; }
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) { return false; }
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    protected boolean useShapeForLightOcclusion(BlockState state) { return false; }
    protected boolean isSignalSource(BlockState state) { return false; }
    protected FluidState getFluidState(BlockState state) { return Fluids.EMPTY.defaultFluidState(); }
    protected boolean hasAnalogOutputSignal(BlockState state) { return false; }
    protected float getMaxHorizontalOffset() { return 0.25f; }
    protected float getMaxVerticalOffset() { return 0.2f; }
    protected BlockState rotate(BlockState state, Rotation rotation) { return state; }
    protected BlockState mirror(BlockState state, Mirror mirror) { return state; }
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return properties.replaceable && (context.getItemInHand().isEmpty() || !context.getItemInHand().is(asItem()));
    }
    protected boolean canBeReplaced(BlockState state, Fluid fluid) { return properties.replaceable || !properties.solid(); }
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return mc.mod.Loot.blockDrops(state, params); }
    protected long getSeed(BlockState state, BlockPos pos) { return net.minecraft.util.Mth.getSeed(pos); }
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return state.getShape(level, pos); }
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return getCollisionShape(state, level, pos, CollisionContext.empty()); }
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isSolidRender(level, pos)) return level.getMaxLightLevel();
        return state.propagatesSkylightDown(level, pos) ? 0 : 1;
    }
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) { return null; }
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return true; }
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return state.isCollisionShapeFullBlock(level, pos) ? 0.2f : 1; }
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return 0; }
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hasCollision ? state.getShape(level, pos) : Shapes.empty();
    }
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) { return Block.isShapeFullBlock(state.getCollisionShape(level, pos)); }
    protected boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) { return Block.isShapeFullBlock(state.getOcclusionShape(level, pos)); }
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getCollisionShape(state, level, pos, context); }
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { }
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { }
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness == -1) return 0;
        int f = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / hardness / f;
    }
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) { }
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) { }
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 0; }
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { }
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 0; }
    public final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getLootTable() {
        if (drops == null) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(asBlock());
            drops = properties.drops != null ? properties.drops.get()
                    : properties.noLoot || id == null ? net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY
                    : (ResourceKey) ResourceKey.create((ResourceKey) net.minecraft.core.registries.Registries.LOOT_TABLE, id.withPrefix("blocks/"));
        }
        return drops;
    }
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) { }
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !Block.isShapeFullBlock(state.getShape(level, pos)) && state.getFluidState().isEmpty();
    }
    protected boolean isRandomlyTicking(BlockState state) { return isRandomlyTicking; }
    protected SoundType getSoundType(BlockState state) { return soundType; }
    public abstract Item asItem();
    protected abstract Block asBlock();
    public MapColor defaultMapColor() { return properties.mapColor.apply(asBlock().defaultBlockState()); }
    public float defaultDestroyTime() { return properties.destroyTime; }

    // ------------------------------------------------------------------ public entry points for reamc's hooks

    public InteractionResult reamc$use(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) { return useWithoutItem(s, l, p, pl, h); }
    public ItemInteractionResult reamc$useItemOn(ItemStack i, BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) { return useItemOn(i, s, l, p, pl, InteractionHand.MAIN_HAND, h); }
    public void reamc$onPlace(BlockState s, Level l, BlockPos p, BlockState old) { onPlace(s, l, p, old, false); }
    public void reamc$onRemove(BlockState s, Level l, BlockPos p, BlockState now) { onRemove(s, l, p, now, false); }
    public void reamc$randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { if (isRandomlyTicking(s)) randomTick(s, l, p, r); }
    public void reamc$tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { tick(s, l, p, r); }
    public void reamc$stepOn(Level l, BlockPos p, BlockState s, Entity e) { if (this instanceof Block b) b.stepOn(l, p, s, e); }
    public MenuProvider reamc$menuProvider(BlockState s, Level l, BlockPos p) { return getMenuProvider(s, l, p); }
    public boolean reamc$triggerEvent(BlockState s, Level l, BlockPos p, int id, int param) { return triggerEvent(s, l, p, id, param); }
    public boolean reamc$hasAnalogOutput(BlockState s) { return hasAnalogOutputSignal(s); }

    public enum OffsetType { NONE, XZ, XYZ }

    @FunctionalInterface
    public interface StateArgumentPredicate<A> { boolean test(BlockState state, BlockGetter level, BlockPos pos, A arg); }

    @FunctionalInterface
    public interface StatePredicate { boolean test(BlockState state, BlockGetter level, BlockPos pos); }

    /** Block settings (reamc-compat keeps those the engine can use). */
    public static class Properties {
        public static final com.mojang.serialization.Codec<Properties> CODEC = com.mojang.serialization.Codec.unit(Properties::of);
        float destroyTime, explosionResistance;
        SoundType soundType = SoundType.STONE;
        ToIntFunction<BlockState> lightEmission = s -> 0;
        Function<BlockState, MapColor> mapColor = s -> MapColor.NONE;
        boolean requiresTool, noOcclusion, noCollision, randomTicks, replaceable, dynamicShape, noLoot, isAir, liquid, ignitedByLava, noParticles;
        Boolean forceSolid;
        float friction = 0.6f, speedFactor = 1, jumpFactor = 1;
        PushReaction pushReaction = PushReaction.NORMAL;
        NoteBlockInstrument instrument = NoteBlockInstrument.HARP;
        OffsetType offsetType = OffsetType.NONE;
        Supplier<ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> drops;
        StatePredicate redstoneConductor = (s, l, p) -> s.isCollisionShapeFullBlock(l, p), suffocating = (s, l, p) -> s.blocksMotion() && s.isCollisionShapeFullBlock(l, p),
                viewBlocking = suffocating, postProcess = (s, l, p) -> false, emissive = (s, l, p) -> false;
        StateArgumentPredicate<EntityType<?>> validSpawn = (s, l, p, e) -> s.isFaceSturdy(l, p, Direction.UP) && s.getLightEmission() < 14;

        public static Properties of() { return new Properties(); }

        public static Properties ofFullCopy(BlockBehaviour b) {
            Properties p = ofLegacyCopy(b), o = b.properties;
            p.mapColor = o.mapColor; p.pushReaction = o.pushReaction; p.instrument = o.instrument; p.replaceable = o.replaceable;
            p.forceSolid = o.forceSolid; p.ignitedByLava = o.ignitedByLava; p.liquid = o.liquid; p.offsetType = o.offsetType;
            p.redstoneConductor = o.redstoneConductor; p.suffocating = o.suffocating; p.viewBlocking = o.viewBlocking;
            p.postProcess = o.postProcess; p.emissive = o.emissive; p.validSpawn = o.validSpawn;
            return p;
        }

        public static Properties ofLegacyCopy(BlockBehaviour b) {
            Properties o = b.properties, p = new Properties();
            p.destroyTime = o.destroyTime; p.explosionResistance = o.explosionResistance; p.soundType = o.soundType;
            p.lightEmission = o.lightEmission; p.requiresTool = o.requiresTool; p.noOcclusion = o.noOcclusion;
            p.noCollision = o.noCollision; p.randomTicks = o.randomTicks; p.friction = o.friction; p.speedFactor = o.speedFactor;
            p.jumpFactor = o.jumpFactor; p.dynamicShape = o.dynamicShape; p.isAir = o.isAir; p.noParticles = o.noParticles;
            return p;
        }

        public Properties mapColor(DyeColor c) { MapColor m = c.getMapColor(); mapColor = s -> m; return this; }
        public Properties mapColor(MapColor c) { mapColor = s -> c; return this; }
        public Properties mapColor(Function<BlockState, MapColor> f) { mapColor = f; return this; }
        public Properties strength(float f) { return strength(f, f); }
        public Properties strength(float destroy, float resistance) { destroyTime = destroy; explosionResistance = Math.max(0, resistance); return this; }
        public Properties destroyTime(float f) { destroyTime = f; return this; }
        public Properties explosionResistance(float f) { explosionResistance = Math.max(0, f); return this; }
        public Properties instabreak() { return strength(0); }
        public Properties sound(SoundType s) { soundType = s; return this; }
        public Properties lightLevel(ToIntFunction<BlockState> f) { lightEmission = f; return this; }
        public Properties requiresCorrectToolForDrops() { requiresTool = true; return this; }
        public Properties noOcclusion() { noOcclusion = true; return this; }
        public Properties noCollission() { noCollision = true; noOcclusion = true; return this; }
        public Properties randomTicks() { randomTicks = true; return this; }
        public Properties replaceable() { replaceable = true; return this; }
        public Properties friction(float f) { friction = f; return this; }
        public Properties speedFactor(float f) { speedFactor = f; return this; }
        public Properties jumpFactor(float f) { jumpFactor = f; return this; }
        public Properties dynamicShape() { dynamicShape = true; return this; }
        public Properties noLootTable() { noLoot = true; return this; }
        public Properties dropsLike(Block b) { drops = b::getLootTable; return this; }
        public Properties lootFrom(Supplier<? extends Block> b) { drops = () -> b.get().getLootTable(); return this; }
        public Properties instrument(NoteBlockInstrument i) { instrument = i; return this; }
        public Properties pushReaction(PushReaction r) { pushReaction = r; return this; }
        public Properties ignitedByLava() { ignitedByLava = true; return this; }
        public Properties liquid() { liquid = true; return this; }
        public Properties air() { isAir = true; return this; }
        public Properties forceSolidOn() { forceSolid = true; return this; }
        public Properties forceSolidOff() { forceSolid = false; return this; }
        public Properties isValidSpawn(StateArgumentPredicate<EntityType<?>> p) { validSpawn = p; return this; }
        public Properties isRedstoneConductor(StatePredicate p) { redstoneConductor = p; return this; }
        public Properties isSuffocating(StatePredicate p) { suffocating = p; return this; }
        public Properties isViewBlocking(StatePredicate p) { viewBlocking = p; return this; }
        public Properties hasPostProcess(StatePredicate p) { postProcess = p; return this; }
        public Properties emissiveRendering(StatePredicate p) { emissive = p; return this; }
        public Properties offsetType(OffsetType t) { offsetType = t; return this; }
        public Properties noTerrainParticles() { noParticles = true; return this; }
        public Properties requiredFeatures(net.minecraft.world.flag.FeatureFlag... flags) { return this; }

        boolean solid() { return forceSolid != null ? forceSolid : !noCollision && !liquid; }

        public float reamc$destroyTime() { return destroyTime; }
        public SoundType reamc$sound() { return soundType; }
        public int reamc$light(BlockState s) { return lightEmission.applyAsInt(s); }
        public boolean reamc$requiresTool() { return requiresTool; }
        public boolean reamc$noOcclusion() { return noOcclusion; }
        public boolean reamc$noCollision() { return noCollision; }
        public boolean reamc$replaceable() { return replaceable; }
        public boolean reamc$randomTicks() { return randomTicks; }
    }

    /** A block with its property values and the cached answers Minecraft keeps per state. */
    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        protected BlockStateBase(Block owner, StateDefinition<Block, ?> definition, int index) { super(owner, definition, index); }

        protected abstract BlockState asState();

        public void initCache() { }
        public Block getBlock() { return owner; }
        public Holder<Block> getBlockHolder() { return owner.builtInRegistryHolder(); }
        public boolean blocksMotion() { return !owner.properties.noCollision && !owner.properties.liquid; }
        public boolean isSolid() { return owner.properties.solid(); }
        public boolean isValidSpawn(BlockGetter l, BlockPos p, EntityType<?> e) { return owner.properties.validSpawn.test(asState(), l, p, e); }
        public boolean propagatesSkylightDown(BlockGetter l, BlockPos p) { return owner.propagatesSkylightDown(asState(), l, p); }
        public int getLightBlock(BlockGetter l, BlockPos p) { return owner.getLightBlock(asState(), l, p); }
        public VoxelShape getFaceOcclusionShape(BlockGetter l, BlockPos p, Direction d) { return getOcclusionShape(l, p).getFaceShape(d); }
        public VoxelShape getOcclusionShape(BlockGetter l, BlockPos p) { return canOcclude() ? owner.getOcclusionShape(asState(), l, p) : Shapes.empty(); }
        public boolean hasLargeCollisionShape() { return false; }
        public boolean useShapeForLightOcclusion() { return owner.useShapeForLightOcclusion(asState()); }
        public int getLightEmission() { return owner.properties.lightEmission.applyAsInt(asState()); }
        public boolean isAir() { return owner.properties.isAir || owner.reamc$block == mc.world.Block.AIR; }
        public boolean ignitedByLava() { return owner.properties.ignitedByLava; }
        public boolean liquid() { return owner.properties.liquid; }
        public MapColor getMapColor(BlockGetter l, BlockPos p) { return owner.properties.mapColor.apply(asState()); }
        public BlockState rotate(Rotation r) { return owner.rotate(asState(), r); }
        public BlockState mirror(Mirror m) { return owner.mirror(asState(), m); }
        public RenderShape getRenderShape() { return owner.getRenderShape(asState()); }
        public boolean emissiveRendering(BlockGetter l, BlockPos p) { return owner.properties.emissive.test(asState(), l, p); }
        public float getShadeBrightness(BlockGetter l, BlockPos p) { return owner.getShadeBrightness(asState(), l, p); }
        public boolean isRedstoneConductor(BlockGetter l, BlockPos p) { return owner.properties.redstoneConductor.test(asState(), l, p); }
        public boolean isSignalSource() { return owner.isSignalSource(asState()); }
        public int getSignal(BlockGetter l, BlockPos p, Direction d) { return owner.getSignal(asState(), l, p, d); }
        public boolean hasAnalogOutputSignal() { return owner.hasAnalogOutputSignal(asState()); }
        public int getAnalogOutputSignal(Level l, BlockPos p) { return owner.getAnalogOutputSignal(asState(), l, p); }
        public float getDestroySpeed(BlockGetter l, BlockPos p) { return owner.properties.destroyTime; }
        public float getDestroyProgress(Player pl, BlockGetter l, BlockPos p) { return owner.getDestroyProgress(asState(), pl, l, p); }
        public int getDirectSignal(BlockGetter l, BlockPos p, Direction d) { return owner.getDirectSignal(asState(), l, p, d); }
        public PushReaction getPistonPushReaction() { return owner.properties.pushReaction; }
        public boolean isSolidRender(BlockGetter l, BlockPos p) { return canOcclude() && Block.isShapeFullBlock(owner.getOcclusionShape(asState(), l, p)); }
        public boolean canOcclude() { return !owner.properties.noOcclusion; }
        public boolean skipRendering(BlockState s, Direction d) { return owner.skipRendering(asState(), s, d); }
        public VoxelShape getShape(BlockGetter l, BlockPos p) { return getShape(l, p, CollisionContext.empty()); }
        public VoxelShape getShape(BlockGetter l, BlockPos p, CollisionContext c) { return owner.getShape(asState(), l, p, c); }
        public VoxelShape getCollisionShape(BlockGetter l, BlockPos p) { return getCollisionShape(l, p, CollisionContext.empty()); }
        public VoxelShape getCollisionShape(BlockGetter l, BlockPos p, CollisionContext c) { return owner.getCollisionShape(asState(), l, p, c); }
        public VoxelShape getBlockSupportShape(BlockGetter l, BlockPos p) { return owner.getBlockSupportShape(asState(), l, p); }
        public VoxelShape getVisualShape(BlockGetter l, BlockPos p, CollisionContext c) { return owner.getVisualShape(asState(), l, p, c); }
        public VoxelShape getInteractionShape(BlockGetter l, BlockPos p) { return owner.getInteractionShape(asState(), l, p); }
        public final boolean entityCanStandOn(BlockGetter l, BlockPos p, Entity e) { return entityCanStandOnFace(l, p, e, Direction.UP); }
        public final boolean entityCanStandOnFace(BlockGetter l, BlockPos p, Entity e, Direction d) { return Block.isFaceFull(getCollisionShape(l, p, CollisionContext.of(e)), d); }
        public Vec3 getOffset(BlockGetter l, BlockPos p) { return Vec3.ZERO; }
        public boolean hasOffsetFunction() { return owner.properties.offsetType != OffsetType.NONE; }
        public boolean triggerEvent(Level l, BlockPos p, int id, int param) { return owner.triggerEvent(asState(), l, p, id, param); }
        public void handleNeighborChanged(Level l, BlockPos p, Block b, BlockPos from, boolean moved) { owner.neighborChanged(asState(), l, p, b, from, moved); }
        public final void updateNeighbourShapes(LevelAccessor l, BlockPos p, int flags) { updateNeighbourShapes(l, p, flags, 512); }
        public final void updateNeighbourShapes(LevelAccessor l, BlockPos p, int flags, int recursion) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (Direction d : UPDATE_SHAPE_ORDER) {
                m.setWithOffset(p, d);
                l.neighborShapeChanged(d.getOpposite(), asState(), m, p, flags, recursion);
            }
        }
        public final void updateIndirectNeighbourShapes(LevelAccessor l, BlockPos p, int flags) { updateIndirectNeighbourShapes(l, p, flags, 512); }
        public void updateIndirectNeighbourShapes(LevelAccessor l, BlockPos p, int flags, int recursion) { owner.updateIndirectNeighbourShapes(asState(), l, p, flags, recursion); }
        public void onPlace(Level l, BlockPos p, BlockState old, boolean moved) { owner.onPlace(asState(), l, p, old, moved); }
        public void onRemove(Level l, BlockPos p, BlockState now, boolean moved) { owner.onRemove(asState(), l, p, now, moved); }
        public void onExplosionHit(Level l, BlockPos p, Explosion e, BiConsumer<ItemStack, BlockPos> drops) { owner.onExplosionHit(asState(), l, p, e, drops); }
        public void tick(ServerLevel l, BlockPos p, RandomSource r) { owner.tick(asState(), l, p, r); }
        public void randomTick(ServerLevel l, BlockPos p, RandomSource r) { owner.randomTick(asState(), l, p, r); }
        public void entityInside(Level l, BlockPos p, Entity e) { owner.entityInside(asState(), l, p, e); }
        public void spawnAfterBreak(ServerLevel l, BlockPos p, ItemStack tool, boolean xp) { owner.spawnAfterBreak(asState(), l, p, tool, xp); }
        public List<ItemStack> getDrops(LootParams.Builder params) { return owner.getDrops(asState(), params); }
        public ItemInteractionResult useItemOn(ItemStack s, Level l, Player pl, InteractionHand h, BlockHitResult hit) { return owner.useItemOn(s, asState(), l, hit.getBlockPos(), pl, h, hit); }
        public InteractionResult useWithoutItem(Level l, Player pl, BlockHitResult hit) { return owner.useWithoutItem(asState(), l, hit.getBlockPos(), pl, hit); }
        public void attack(Level l, BlockPos p, Player pl) { owner.attack(asState(), l, p, pl); }
        public boolean isSuffocating(BlockGetter l, BlockPos p) { return owner.properties.suffocating.test(asState(), l, p); }
        public boolean isViewBlocking(BlockGetter l, BlockPos p) { return owner.properties.viewBlocking.test(asState(), l, p); }
        public BlockState updateShape(Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) { return owner.updateShape(asState(), d, n, l, p, np); }
        public boolean isPathfindable(PathComputationType t) { return owner.isPathfindable(asState(), t); }
        public boolean canBeReplaced(BlockPlaceContext c) { return owner.canBeReplaced(asState(), c); }
        public boolean canBeReplaced(Fluid f) { return owner.canBeReplaced(asState(), f); }
        public boolean canBeReplaced() { return owner.properties.replaceable || (owner.reamc$block != null && owner.reamc$block.replaceable); }
        public boolean canSurvive(LevelReader l, BlockPos p) { return owner.canSurvive(asState(), l, p); }
        public boolean hasPostProcess(BlockGetter l, BlockPos p) { return owner.properties.postProcess.test(asState(), l, p); }
        public MenuProvider getMenuProvider(Level l, BlockPos p) { return owner.getMenuProvider(asState(), l, p); }
        public boolean is(TagKey<Block> tag) { return mc.mod.Tags.has(tag, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(owner)); }
        public boolean is(TagKey<Block> tag, Predicate<BlockStateBase> p) { return is(tag) && p.test(this); }
        public boolean is(HolderSet<Block> set) { return set.contains(owner.builtInRegistryHolder()); }
        public boolean is(Holder<Block> h) { return h.value() == owner; }
        public Stream<TagKey<Block>> getTags() { return owner.builtInRegistryHolder().tags(); }
        public boolean hasBlockEntity() { return owner instanceof EntityBlock; }
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockEntityType<T> type) {
            return owner instanceof EntityBlock eb ? (BlockEntityTicker<T>) eb.getTicker(l, asState(), type) : null;
        }
        public boolean is(Block b) { return owner == b; }
        public boolean is(ResourceKey<Block> key) { return owner.builtInRegistryHolder().is(key); }
        public FluidState getFluidState() { return owner.getFluidState(asState()); }
        public boolean isRandomlyTicking() { return owner.isRandomlyTicking(asState()); }
        public long getSeed(BlockPos p) { return owner.getSeed(asState(), p); }
        public SoundType getSoundType() { return owner.getSoundType(asState()); }
        public void onProjectileHit(Level l, BlockState s, BlockHitResult h, Projectile p) { owner.onProjectileHit(l, s, h, p); }
        public boolean isFaceSturdy(BlockGetter l, BlockPos p, Direction d) { return isFaceSturdy(l, p, d, SupportType.FULL); }
        public boolean isFaceSturdy(BlockGetter l, BlockPos p, Direction d, SupportType t) { return t.isSupporting(asState(), l, p, d); }
        public boolean isCollisionShapeFullBlock(BlockGetter l, BlockPos p) { return owner.isCollisionShapeFullBlock(asState(), l, p); }
        public boolean requiresCorrectToolForDrops() { return owner.properties.requiresTool; }
        public boolean shouldSpawnTerrainParticles() { return !owner.properties.noParticles; }
        public NoteBlockInstrument instrument() { return owner.properties.instrument; }
    }
}
