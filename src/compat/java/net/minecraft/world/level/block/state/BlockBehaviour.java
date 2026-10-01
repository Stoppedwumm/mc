package net.minecraft.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.ToIntFunction;

/** The overridable behaviour of blocks; reamc calls these from its hooks (reamc-compat). */
public abstract class BlockBehaviour {
    protected final Properties properties;

    protected BlockBehaviour(Properties properties) { this.properties = properties; }

    public Properties reamc$properties() { return properties; }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return InteractionResult.PASS; }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) { }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) level.removeBlockEntity(pos);
    }

    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) { }

    protected boolean isRandomlyTicking(BlockState state) { return properties.randomTicks; }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { tick(state, level, pos, random); }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { }

    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { }

    protected void attack(BlockState state, Level level, BlockPos pos, Player player) { }

    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) { return false; }

    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) { return null; }

    protected boolean hasAnalogOutputSignal(BlockState state) { return false; }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return 0; }

    protected BlockState rotate(BlockState state, Rotation rotation) { return state; }

    protected BlockState mirror(BlockState state, Mirror mirror) { return state; }

    protected int getSignal(BlockState state, Object level, BlockPos pos, net.minecraft.core.Direction direction) { return 0; }

    protected boolean isSignalSource(BlockState state) { return false; }

    // ------------------------------------------------------------------ public entry points for reamc's hooks

    public InteractionResult reamc$use(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) { return useWithoutItem(s, l, p, pl, h); }
    public ItemInteractionResult reamc$useItemOn(ItemStack i, BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) { return useItemOn(i, s, l, p, pl, InteractionHand.MAIN_HAND, h); }
    public void reamc$onPlace(BlockState s, Level l, BlockPos p, BlockState old) { onPlace(s, l, p, old, false); }
    public void reamc$onRemove(BlockState s, Level l, BlockPos p, BlockState now) { onRemove(s, l, p, now, false); }
    public void reamc$randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { if (isRandomlyTicking(s)) randomTick(s, l, p, r); }
    public void reamc$stepOn(Level l, BlockPos p, BlockState s, Entity e) { if (this instanceof Block b) b.stepOn(l, p, s, e); }
    public MenuProvider reamc$menuProvider(BlockState s, Level l, BlockPos p) { return getMenuProvider(s, l, p); }
    public boolean reamc$triggerEvent(BlockState s, Level l, BlockPos p, int id, int param) { return triggerEvent(s, l, p, id, param); }
    public boolean reamc$hasAnalogOutput(BlockState s) { return hasAnalogOutputSignal(s); }

    /** Block settings (reamc-compat keeps those the engine can use). */
    public static class Properties {
        float destroyTime, explosionResistance;
        SoundType soundType = SoundType.STONE;
        ToIntFunction<BlockState> lightEmission = s -> 0;
        boolean requiresTool, noOcclusion, noCollision, randomTicks, replaceable, dynamicShape;
        float friction = 0.6f;

        public static Properties of() { return new Properties(); }

        public static Properties ofFullCopy(BlockBehaviour b) {
            Properties o = b.properties, p = new Properties();
            p.destroyTime = o.destroyTime; p.explosionResistance = o.explosionResistance; p.soundType = o.soundType;
            p.lightEmission = o.lightEmission; p.requiresTool = o.requiresTool; p.noOcclusion = o.noOcclusion;
            p.noCollision = o.noCollision; p.randomTicks = o.randomTicks; p.friction = o.friction;
            return p;
        }

        public static Properties ofLegacyCopy(BlockBehaviour b) { return ofFullCopy(b); }

        public Properties strength(float f) { return strength(f, f); }
        public Properties strength(float destroy, float resistance) { destroyTime = destroy; explosionResistance = Math.max(0, resistance); return this; }
        public Properties destroyTime(float f) { destroyTime = f; return this; }
        public Properties explosionResistance(float f) { explosionResistance = f; return this; }
        public Properties instabreak() { return strength(0); }
        public Properties sound(SoundType s) { soundType = s; return this; }
        public Properties lightLevel(ToIntFunction<BlockState> f) { lightEmission = f; return this; }
        public Properties requiresCorrectToolForDrops() { requiresTool = true; return this; }
        public Properties noOcclusion() { noOcclusion = true; return this; }
        public Properties noCollission() { noCollision = true; noOcclusion = true; return this; }
        public Properties randomTicks() { randomTicks = true; return this; }
        public Properties replaceable() { replaceable = true; return this; }
        public Properties friction(float f) { friction = f; return this; }
        public Properties speedFactor(float f) { return this; }
        public Properties jumpFactor(float f) { return this; }
        public Properties dynamicShape() { dynamicShape = true; return this; }
        public Properties noLootTable() { return this; }
        public Properties mapColor(Object color) { return this; }
        public Properties instrument(Object instrument) { return this; }
        public Properties pushReaction(Object reaction) { return this; }
        public Properties ignitedByLava() { return this; }
        public Properties forceSolidOn() { return this; }
        public Properties forceSolidOff() { return this; }

        public float reamc$destroyTime() { return destroyTime; }
        public SoundType reamc$sound() { return soundType; }
        public int reamc$light(BlockState s) { return lightEmission.applyAsInt(s); }
        public boolean reamc$requiresTool() { return requiresTool; }
        public boolean reamc$noOcclusion() { return noOcclusion; }
        public boolean reamc$noCollision() { return noCollision; }
        public boolean reamc$replaceable() { return replaceable; }
        public boolean reamc$randomTicks() { return randomTicks; }
    }
}
