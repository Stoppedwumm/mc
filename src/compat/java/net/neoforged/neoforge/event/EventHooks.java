package net.neoforged.neoforge.event;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.EffectCure;

/** NeoForge's event-firing helpers (reamc-compat). */
public final class EventHooks {
    private EventHooks() { }

    public static boolean canEntityGrief(Level level, Entity entity) { return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING); }

    /** True when something cancels removing the effect (nothing does in reamc). */
    public static boolean onEffectRemoved(LivingEntity entity, Holder<MobEffect> effect, EffectCure cure) { return false; }

    public static boolean onEffectRemoved(LivingEntity entity, MobEffectInstance effect, EffectCure cure) { return false; }
}
