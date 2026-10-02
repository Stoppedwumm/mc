package net.minecraft.world.level.levelgen.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

public class PlacementContext {
    private final WorldGenLevel level;
    private final ChunkGenerator generator;
    private final Optional<PlacedFeature> topFeature;

    public PlacementContext(WorldGenLevel level, ChunkGenerator generator, Optional<PlacedFeature> top) { this.level = level; this.generator = generator; this.topFeature = top; }

    public int getHeight(Heightmap.Types type, int x, int z) { return level.getHeight(type, x, z); }
    public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    public int getGenDepth() { return generator.getGenDepth(); }
    public BlockState getBlockState(BlockPos pos) { return level.getBlockState(pos); }
    public WorldGenLevel getLevel() { return level; }
    public Optional<PlacedFeature> topFeature() { return topFeature; }
    public ChunkGenerator generator() { return generator; }
}
