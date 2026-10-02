package net.minecraft.world.level.levelgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

public class FeaturePlaceContext<FC extends FeatureConfiguration> {
    private final Optional<ConfiguredFeature<?, ?>> topFeature;
    private final WorldGenLevel level;
    private final ChunkGenerator chunkGenerator;
    private final RandomSource random;
    private final BlockPos origin;
    private final FC config;

    public FeaturePlaceContext(Optional<ConfiguredFeature<?, ?>> top, WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos origin, FC config) {
        this.topFeature = top; this.level = level; this.chunkGenerator = gen; this.random = random; this.origin = origin; this.config = config;
    }

    public Optional<ConfiguredFeature<?, ?>> topFeature() { return topFeature; }
    public WorldGenLevel level() { return level; }
    public ChunkGenerator chunkGenerator() { return chunkGenerator; }
    public RandomSource random() { return random; }
    public BlockPos origin() { return origin; }
    public FC config() { return config; }
}
