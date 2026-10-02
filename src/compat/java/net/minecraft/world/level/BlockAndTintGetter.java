package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface BlockAndTintGetter extends BlockGetter {
    default float getShade(Direction d, boolean shade) { return 1; }

    int getBrightness(LightLayer layer, BlockPos pos);

    default int getRawBrightness(BlockPos pos, int skyDarken) {
        return Math.max(getBrightness(LightLayer.SKY, pos) - skyDarken, getBrightness(LightLayer.BLOCK, pos));
    }

    default boolean canSeeSky(BlockPos pos) { return getBrightness(LightLayer.SKY, pos) >= getMaxLightLevel(); }
}
