package net.neoforged.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge's hooks that vanilla code calls (reamc-compat). */
public final class CommonHooks {
    private CommonHooks() { }

    /** Whether a crop may grow this tick; {@code def} is vanilla's own roll. */
    public static boolean canCropGrow(Level level, BlockPos pos, BlockState state, boolean def) { return def; }

    public static void fireCropGrowPost(Level level, BlockPos pos, BlockState state) { }

    /** Whether a fall turns farmland to dirt: players always can, mobs only with mob griefing on. */
    public static boolean onFarmlandTrample(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
        if (level.random.nextFloat() >= fallDistance - 0.5f) return false;
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity)) return false;
        if (!(entity instanceof Player) && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        return entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > 0.512f;
    }
}
