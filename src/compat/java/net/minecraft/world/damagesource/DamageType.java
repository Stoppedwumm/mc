package net.minecraft.world.damagesource;

/** A kind of damage (reamc-compat). */
public record DamageType(String msgId, DamageScaling scaling, float exhaustion, DamageEffects effects, DeathMessageType deathMessageType) {
    public DamageType(String msgId, DamageScaling scaling, float exhaustion) { this(msgId, scaling, exhaustion, DamageEffects.HURT, DeathMessageType.DEFAULT); }
    public DamageType(String msgId, float exhaustion) { this(msgId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, exhaustion); }
    public DamageType(String msgId, float exhaustion, DamageEffects effects) { this(msgId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, exhaustion, effects, DeathMessageType.DEFAULT); }
}
