package net.minecraft.world.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Effect helpers (reamc-compat). */
public final class MobEffectUtil {
    public MobEffectUtil() { }

    /** "m:ss", or infinity. */
    public static Component formatDuration(MobEffectInstance e, float scale, float tickRate) {
        if (e.isInfiniteDuration()) return Component.translatable("effect.duration.infinite");
        int seconds = (int) Math.floor(e.getDuration() * scale / tickRate);
        return Component.literal(seconds / 60 + ":" + String.format("%02d", seconds % 60));
    }

    public static boolean hasDigSpeed(LivingEntity e) { return e.hasEffect(MobEffects.DIG_SPEED) || e.hasEffect(MobEffects.CONDUIT_POWER); }

    public static int getDigSpeedAmplification(LivingEntity e) {
        int a = e.hasEffect(MobEffects.DIG_SPEED) ? e.getEffect(MobEffects.DIG_SPEED).getAmplifier() : 0;
        int b = e.hasEffect(MobEffects.CONDUIT_POWER) ? e.getEffect(MobEffects.CONDUIT_POWER).getAmplifier() : 0;
        return Math.max(a, b);
    }

    public static boolean hasWaterBreathing(LivingEntity e) { return e.hasEffect(MobEffects.WATER_BREATHING) || e.hasEffect(MobEffects.CONDUIT_POWER); }
}
