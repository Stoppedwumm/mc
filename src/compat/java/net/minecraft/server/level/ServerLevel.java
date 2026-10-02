package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

/** The server's copy of a world (reamc-compat: singleplayer and servers run mods on this). */
public abstract class ServerLevel extends Level implements WorldGenLevel {
    protected ServerLevel() { super(false); }

    @Override public ServerLevel getLevel() { return this; }

    @Override public long getSeed() { return reamc$world().seed; }

    public ServerChunkCache getChunkSource() { return new ServerChunkCache(this); }

    /** Spawns particles for everyone nearby. */
    public <T extends ParticleOptions> int sendParticles(T particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (int i = 0; i < Math.max(1, Math.min(count, 64)); i++)
            addParticle(particle, x + random.nextGaussian() * dx, y + random.nextGaussian() * dy, z + random.nextGaussian() * dz, 0, 0, 0);
        return count;
    }

    public java.util.List<ServerPlayer> players() {
        java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
        for (var p : super.players()) out.add((ServerPlayer) p);
        return out;
    }

    public net.minecraft.server.MinecraftServer getServer() { return mc.mod.Bridge.server(); }

    public boolean isPositionEntityTicking(BlockPos pos) { return hasChunkAt(pos); }

    public void setDayTime(long t) { reamc$world().time = t; }
}
