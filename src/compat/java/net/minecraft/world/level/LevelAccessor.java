package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

/** A world that can be changed and ticked (reamc-compat). */
public interface LevelAccessor extends CommonLevelAccessor, LevelTimeAccess {
    MinecraftServer getServer();

    RandomSource getRandom();

    default net.minecraft.world.Difficulty getDifficulty() { return net.minecraft.world.Difficulty.NORMAL; }

    default net.minecraft.world.DifficultyInstance getCurrentDifficultyAt(BlockPos pos) { return new net.minecraft.world.DifficultyInstance(getDifficulty(), dayTime(), 0, 0); }

    void scheduleTick(BlockPos pos, Block block, int delay);

    default void scheduleTick(BlockPos pos, Block block, int delay, net.minecraft.world.ticks.TickPriority priority) { scheduleTick(pos, block, delay); }

    default void scheduleTick(BlockPos pos, Fluid fluid, int delay) { }

    default void blockUpdated(BlockPos pos, Block block) { }

    default void neighborShapeChanged(net.minecraft.core.Direction d, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, BlockPos from, int flags, int recursion) { }

    void playSound(Player except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch);

    default void playSound(Player except, BlockPos pos, SoundEvent sound, SoundSource source) { playSound(except, pos, sound, source, 1, 1); }

    void addParticle(ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz);

    void levelEvent(Player except, int type, BlockPos pos, int data);

    default void levelEvent(int type, BlockPos pos, int data) { levelEvent(null, type, pos, data); }

    void gameEvent(Holder<GameEvent> event, net.minecraft.world.phys.Vec3 pos, GameEvent.Context context);

    default void gameEvent(Entity e, Holder<GameEvent> event, net.minecraft.world.phys.Vec3 pos) { gameEvent(event, pos, GameEvent.Context.of(e)); }

    default void gameEvent(Entity e, Holder<GameEvent> event, BlockPos pos) { gameEvent(event, pos, GameEvent.Context.of(e)); }

    default void gameEvent(Holder<GameEvent> event, BlockPos pos, GameEvent.Context context) { gameEvent(event, net.minecraft.world.phys.Vec3.atCenterOf(pos), context); }

    default void gameEvent(net.minecraft.resources.ResourceKey<GameEvent> event, BlockPos pos, GameEvent.Context context) { }

    @Override
    default long dayTime() { return 0; }
}
