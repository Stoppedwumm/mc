package net.minecraft.world.level.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A chunk of a level (reamc-compat: a view onto the level). */
public class LevelChunk {
    private final Level level;
    private final ChunkPos pos;

    public LevelChunk(Level level, ChunkPos pos) { this.level = level; this.pos = pos; }

    public ChunkPos getPos() { return pos; }
    public Level getLevel() { return level; }
    public BlockState getBlockState(BlockPos p) { return level.getBlockState(p); }
    public BlockEntity getBlockEntity(BlockPos p) { return level.getBlockEntity(p); }
    public void setUnsaved(boolean unsaved) { }
    public boolean isUnsaved() { return false; }
    public void markUnsaved() { }
}
