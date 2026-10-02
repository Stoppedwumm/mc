package net.neoforged.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/** Extra places mods say keep farmland moist (reamc-compat: none). */
public final class FarmlandWaterManager {
    private FarmlandWaterManager() { }

    public static boolean hasBlockWaterTicket(LevelReader level, BlockPos pos) { return false; }
}
