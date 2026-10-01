package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** A world as mods see it; reamc-compat backs it with an engine world (see mc.mod.Bridge). */
public abstract class Level {
    public static final int UPDATE_NEIGHBORS = 1, UPDATE_CLIENTS = 2, UPDATE_ALL = 3;
    public final boolean isClientSide;
    protected final RandomSource random = RandomSource.create();

    protected Level(boolean isClientSide) { this.isClientSide = isClientSide; }

    /** The engine world behind this level. */
    public abstract mc.world.World reamc$world();

    public BlockState getBlockState(BlockPos pos) {
        mc.world.World w = reamc$world();
        return mc.mod.Bridge.state(w.getBlock(pos.getX(), pos.getY(), pos.getZ()), w.getMeta(pos.getX(), pos.getY(), pos.getZ()));
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        mc.world.Block b = mc.mod.Bridge.engineBlock(state.getBlock());
        if (b == null) return false;
        return reamc$world().setBlock(pos.getX(), pos.getY(), pos.getZ(), b.id, state.reamc$index(), (flags & UPDATE_NEIGHBORS) != 0);
    }

    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) { return setBlock(pos, state, UPDATE_ALL); }

    public boolean removeBlock(BlockPos pos, boolean isMoving) { return reamc$world().setBlock(pos.getX(), pos.getY(), pos.getZ(), 0, 0, true); }

    public boolean destroyBlock(BlockPos pos, boolean drop) {
        reamc$world().breakBlock(pos.getX(), pos.getY(), pos.getZ(), null, drop);
        return true;
    }

    public BlockEntity getBlockEntity(BlockPos pos) { return mc.mod.Bridge.blockEntity(reamc$world(), pos); }

    public void removeBlockEntity(BlockPos pos) { mc.mod.Bridge.removeBlockEntity(reamc$world(), pos); }

    public void setBlockEntity(BlockEntity be) { mc.mod.Bridge.putBlockEntity(reamc$world(), be); }

    public Entity getEntity(int id) { return mc.mod.Bridge.entity(reamc$world(), id); }

    public List<? extends Player> players() { return mc.mod.Bridge.players(reamc$world()); }

    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) { }

    public void updateNeighborsAt(BlockPos pos, Block block) { reamc$world().notifyAround(pos.getX(), pos.getY(), pos.getZ()); }

    public void blockEvent(BlockPos pos, Block block, int id, int param) { getBlockState(pos).triggerEvent(this, pos, id, param); }

    public boolean isLoaded(BlockPos pos) { return reamc$world().isLoaded(pos.getX(), pos.getZ()); }

    public long getGameTime() { return reamc$world().time; }
    public long getDayTime() { return reamc$world().time; }
    public boolean isDay() { return reamc$world().isDaytime(); }
    public boolean isNight() { return !isDay(); }
    public boolean isRaining() { return reamc$world().raining; }
    public RandomSource getRandom() { return random; }
    public int getMinBuildHeight() { return 0; }
    public int getMaxBuildHeight() { return mc.world.Chunk.HEIGHT; }

    public MinecraftServer getServer() { return mc.mod.Bridge.server(); }

    public boolean addFreshEntity(Entity e) { return mc.mod.Bridge.addEntity(reamc$world(), e); }

    public void playSound(Player except, BlockPos pos, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        reamc$world().playSound(sound.reamc$name(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch);
    }

    public void addParticle(Object particle, double x, double y, double z, double vx, double vy, double vz) { reamc$world().addParticle("smoke", x, y, z); }
}
