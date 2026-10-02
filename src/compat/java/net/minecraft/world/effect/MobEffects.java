package net.minecraft.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's effects; those reamc has natively are applied by the engine (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MobEffects {
    private static Holder<MobEffect> register(String id, MobEffectCategory category, int color, mc.entity.Effect engine) {
        MobEffect e = new MobEffect(category, color) { };
        e.reamc$engine = engine;
        return (Holder) Registry.registerForHolder((Registry) BuiltInRegistries.MOB_EFFECT, ResourceLocation.withDefaultNamespace(id), e);
    }

    public static final Holder<MobEffect> MOVEMENT_SPEED = register("speed", MobEffectCategory.BENEFICIAL, 0x33ebff, mc.entity.Effect.SPEED);
    public static final Holder<MobEffect> MOVEMENT_SLOWDOWN = register("slowness", MobEffectCategory.HARMFUL, 0x8bafe0, mc.entity.Effect.SLOWNESS);
    public static final Holder<MobEffect> DIG_SPEED = register("haste", MobEffectCategory.BENEFICIAL, 0xd9c043, mc.entity.Effect.HASTE);
    public static final Holder<MobEffect> DIG_SLOWDOWN = register("mining_fatigue", MobEffectCategory.HARMFUL, 0x4a4217, null);
    public static final Holder<MobEffect> DAMAGE_BOOST = register("strength", MobEffectCategory.BENEFICIAL, 0xffc700, mc.entity.Effect.STRENGTH);
    public static final Holder<MobEffect> HEAL = register("instant_health", MobEffectCategory.BENEFICIAL, 0xf82423, mc.entity.Effect.INSTANT_HEALTH);
    public static final Holder<MobEffect> HARM = register("instant_damage", MobEffectCategory.HARMFUL, 0xa9656a, mc.entity.Effect.INSTANT_DAMAGE);
    public static final Holder<MobEffect> JUMP = register("jump_boost", MobEffectCategory.BENEFICIAL, 0xfdff84, mc.entity.Effect.JUMP_BOOST);
    public static final Holder<MobEffect> CONFUSION = register("nausea", MobEffectCategory.HARMFUL, 0x551d4a, null);
    public static final Holder<MobEffect> REGENERATION = register("regeneration", MobEffectCategory.BENEFICIAL, 0xcd5cab, mc.entity.Effect.REGENERATION);
    public static final Holder<MobEffect> DAMAGE_RESISTANCE = register("resistance", MobEffectCategory.BENEFICIAL, 0x9146f0, mc.entity.Effect.RESISTANCE);
    public static final Holder<MobEffect> FIRE_RESISTANCE = register("fire_resistance", MobEffectCategory.BENEFICIAL, 0xff9900, mc.entity.Effect.FIRE_RESISTANCE);
    public static final Holder<MobEffect> WATER_BREATHING = register("water_breathing", MobEffectCategory.BENEFICIAL, 0x98dac0, mc.entity.Effect.WATER_BREATHING);
    public static final Holder<MobEffect> INVISIBILITY = register("invisibility", MobEffectCategory.BENEFICIAL, 0xf6f6f6, mc.entity.Effect.INVISIBILITY);
    public static final Holder<MobEffect> BLINDNESS = register("blindness", MobEffectCategory.HARMFUL, 0x1f1f23, null);
    public static final Holder<MobEffect> NIGHT_VISION = register("night_vision", MobEffectCategory.BENEFICIAL, 0xc2ff66, mc.entity.Effect.NIGHT_VISION);
    public static final Holder<MobEffect> HUNGER = register("hunger", MobEffectCategory.HARMFUL, 0x587653, null);
    public static final Holder<MobEffect> WEAKNESS = register("weakness", MobEffectCategory.HARMFUL, 0x484d48, mc.entity.Effect.WEAKNESS);
    public static final Holder<MobEffect> POISON = register("poison", MobEffectCategory.HARMFUL, 0x87a363, mc.entity.Effect.POISON);
    public static final Holder<MobEffect> WITHER = register("wither", MobEffectCategory.HARMFUL, 0x736156, null);
    public static final Holder<MobEffect> HEALTH_BOOST = register("health_boost", MobEffectCategory.BENEFICIAL, 0xf87d23, null);
    public static final Holder<MobEffect> ABSORPTION = register("absorption", MobEffectCategory.BENEFICIAL, 0x2552a5, mc.entity.Effect.ABSORPTION);
    public static final Holder<MobEffect> SATURATION = register("saturation", MobEffectCategory.BENEFICIAL, 0xf82423, null);
    public static final Holder<MobEffect> GLOWING = register("glowing", MobEffectCategory.BENEFICIAL, 0x94a061, null);
    public static final Holder<MobEffect> LEVITATION = register("levitation", MobEffectCategory.HARMFUL, 0xceffff, null);
    public static final Holder<MobEffect> LUCK = register("luck", MobEffectCategory.BENEFICIAL, 0x59c106, null);
    public static final Holder<MobEffect> UNLUCK = register("unluck", MobEffectCategory.HARMFUL, 0xc0a44d, null);
    public static final Holder<MobEffect> SLOW_FALLING = register("slow_falling", MobEffectCategory.BENEFICIAL, 0xf3cfb9, null);
    public static final Holder<MobEffect> CONDUIT_POWER = register("conduit_power", MobEffectCategory.BENEFICIAL, 0x1dc2d1, null);
    public static final Holder<MobEffect> DOLPHINS_GRACE = register("dolphins_grace", MobEffectCategory.BENEFICIAL, 0x88a3be, null);
    public static final Holder<MobEffect> BAD_OMEN = register("bad_omen", MobEffectCategory.HARMFUL, 0x0b6138, null);
    public static final Holder<MobEffect> HERO_OF_THE_VILLAGE = register("hero_of_the_village", MobEffectCategory.BENEFICIAL, 0x44ff44, null);
    public static final Holder<MobEffect> DARKNESS = register("darkness", MobEffectCategory.HARMFUL, 0x292721, null);
    public static final Holder<MobEffect> TRIAL_OMEN = register("trial_omen", MobEffectCategory.HARMFUL, 0x808080, null);
    public static final Holder<MobEffect> RAID_OMEN = register("raid_omen", MobEffectCategory.HARMFUL, 0x808080, null);
    public static final Holder<MobEffect> WIND_CHARGED = register("wind_charged", MobEffectCategory.HARMFUL, 0x808080, null);
    public static final Holder<MobEffect> WEAVING = register("weaving", MobEffectCategory.HARMFUL, 0x808080, null);
    public static final Holder<MobEffect> OOZING = register("oozing", MobEffectCategory.HARMFUL, 0x808080, null);
    public static final Holder<MobEffect> INFESTED = register("infested", MobEffectCategory.HARMFUL, 0x808080, null);
}
