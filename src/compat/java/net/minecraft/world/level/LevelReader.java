package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Reading blocks, light and biomes (reamc-compat). */
public interface LevelReader extends BlockAndTintGetter, CollisionGetter, SignalGetter {
    boolean isClientSide();

    int getSkyDarken();

    int getHeight(Heightmap.Types type, int x, int z);

    default BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) { return new BlockPos(pos.getX(), getHeight(type, pos.getX(), pos.getZ()), pos.getZ()); }

    Holder<Biome> getBiome(BlockPos pos);

    default boolean isEmptyBlock(BlockPos pos) { return getBlockState(pos).isAir(); }

    default boolean isWaterAt(BlockPos pos) { return getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER); }

    default boolean canSeeSkyFromBelowWater(BlockPos pos) { return canSeeSky(pos); }

    default int getMaxLocalRawBrightness(BlockPos pos) { return getRawBrightness(pos, getSkyDarken()); }

    default int getSeaLevel() { return 63; }

    RegistryAccess registryAccess();

    default FeatureFlagSet enabledFeatures() { return FeatureFlagSet.of(); }

    default <T> net.minecraft.core.HolderLookup<T> holderLookup(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> key) { return registryAccess().lookupOrThrow(key); }

    boolean hasChunk(int cx, int cz);

    default boolean hasChunkAt(BlockPos pos) { return hasChunk(pos.getX() >> 4, pos.getZ() >> 4); }

    default boolean hasChunkAt(int x, int z) { return hasChunk(x >> 4, z >> 4); }

    @Deprecated
    default boolean isLoaded(BlockPos pos) { return hasChunkAt(pos); }

    default boolean isAreaLoaded(BlockPos center, int range) {
        for (int x = center.getX() - range; x <= center.getX() + range; x += 16)
            for (int z = center.getZ() - range; z <= center.getZ() + range; z += 16)
                if (!hasChunkAt(x, z)) return false;
        return hasChunkAt(center.getX() + range, center.getZ() + range) && hasChunkAt(center.getX() - range, center.getZ() - range);
    }

    default boolean containsAnyLiquid(net.minecraft.world.phys.AABB box) { return getBlockStates(box).anyMatch(s -> !s.getFluidState().isEmpty()); }

    default float getPathfindingCostFromLightLevels(BlockPos pos) { return getMaxLocalRawBrightness(pos) - 0.5f; }

    default net.minecraft.world.level.dimension.DimensionType dimensionType() { return net.minecraft.world.level.dimension.DimensionType.OVERWORLD; }
}
