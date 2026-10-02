package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.TriState;

/** NeoForge's extra block hooks, with its defaults (reamc-compat). */
public interface IBlockExtension {
    private Block self() { return (Block) this; }

    default float getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity) { return self().getFriction(); }
    default boolean hasDynamicLightEmission(BlockState state) { return false; }
    default int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) { return state.getLightEmission(); }
    default boolean ignitedByLava(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.ignitedByLava(); }
    default boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) { return state.is(net.minecraft.tags.BlockTags.CLIMBABLE); }
    default boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) { return false; }
    default boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) { return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state); }
    default boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        self().playerWillDestroy(level, pos, state, player);
        return level.setBlock(pos, fluid.createLegacyBlock(), level.isClientSide ? 11 : 3);
    }
    default void onDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction direction, FluidState fluid) { level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); }
    default boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) { return false; }
    default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) { return self().getExplosionResistance(); }
    default ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) { return self().getCloneItemStack(level, pos, state); }
    default boolean addLandingEffects(BlockState state, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int particles) { return false; }
    default boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) { return false; }
    default TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) { return TriState.DEFAULT; }
    default boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return state.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE) && state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) > 0;
    }
    default boolean isConduitFrame(BlockState state, LevelReader level, BlockPos pos, BlockPos conduit) { return false; }
    default boolean isPortalFrame(BlockState state, BlockGetter level, BlockPos pos) { return false; }
    default int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity be, Entity breaker, ItemStack tool) { return 0; }
    default BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) { return state.rotate(direction); }
    default float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) { return state.is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER) ? 1 : 0; }
    default void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) { }
    default boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) { return false; }
    default SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) { return state.getSoundType(); }
    default PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, Mob mob) { return null; }
    default PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, Mob mob, PathType originalType) { return null; }
    default boolean isSlimeBlock(BlockState state) { return false; }
    default boolean isStickyBlock(BlockState state) { return false; }
    default boolean canStickTo(BlockState state, BlockState other) { return isStickyBlock(state) || isStickyBlock(other); }
    default int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return mc.mod.Bridge.flammability(state.getBlock()); }
    default boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return state.getFlammability(level, pos, direction) > 0; }
    default void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction direction, LivingEntity igniter) { }
    default int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return mc.mod.Bridge.fireSpreadSpeed(state.getBlock()); }
    default boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) { return state.is(net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD); }
    default boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) { return true; }
    default boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) { return true; }
    default void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) { level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); }
    default boolean collisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity) { return state.is(net.minecraft.tags.BlockTags.FENCES) || state.is(net.minecraft.tags.BlockTags.WALLS) || self() instanceof net.minecraft.world.level.block.FenceGateBlock; }
    default BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) { return null; }
    default boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) { return false; }
    default boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return state.isSignalSource() && direction != null; }
    default boolean supportsExternalFaceHiding(BlockState state) { return true; }
    default void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) { }
    default boolean canBeHydrated(BlockState state, BlockGetter getter, BlockPos pos, FluidState fluid, BlockPos fluidPos) { return fluid.canHydrate(getter, fluidPos, state, pos); }
    default MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) { return defaultColor; }
    default PushReaction getPistonPushReaction(BlockState state) { return null; }
    default boolean isEmpty(BlockState state) { return state.isAir(); }
}
