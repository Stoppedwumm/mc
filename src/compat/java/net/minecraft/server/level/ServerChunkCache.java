package net.minecraft.server.level;

import net.minecraft.core.BlockPos;

/** The server's chunk storage (reamc-compat: a view onto the level). */
public class ServerChunkCache {
    private final ServerLevel level;

    public ServerChunkCache(ServerLevel level) { this.level = level; }

    public ServerLevel getLevel() { return level; }

    public void blockChanged(BlockPos pos) { }

    public boolean hasChunk(int cx, int cz) { return level.hasChunk(cx, cz); }

    public net.minecraft.world.level.chunk.ChunkGenerator getGenerator() { return net.minecraft.world.level.chunk.ChunkGenerator.REAMC; }
}
