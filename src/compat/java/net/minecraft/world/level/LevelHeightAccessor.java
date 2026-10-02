package net.minecraft.world.level;

import net.minecraft.core.BlockPos;

public interface LevelHeightAccessor {
    int getHeight();

    int getMinBuildHeight();

    default int getMaxBuildHeight() { return getMinBuildHeight() + getHeight(); }

    default boolean isOutsideBuildHeight(BlockPos pos) { return isOutsideBuildHeight(pos.getY()); }

    default boolean isOutsideBuildHeight(int y) { return y < getMinBuildHeight() || y >= getMaxBuildHeight(); }

    default int getSectionsCount() { return getHeight() / 16; }

    default int getMinSection() { return getMinBuildHeight() >> 4; }

    default int getMaxSection() { return (getMaxBuildHeight() - 1 >> 4) + 1; }

    default int getSectionIndex(int y) { return (y >> 4) - getMinSection(); }
}
