package net.minecraft.world.entity.ai.attributes;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's attributes (reamc-compat: registered for lookups; the engine has its own stats). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Attributes {
    private static Holder<Attribute> register(String id, double def, double min, double max) {
        String name = id.substring(id.indexOf('.') + 1);
        return (Holder) Registry.registerForHolder((Registry) BuiltInRegistries.ATTRIBUTE, ResourceLocation.withDefaultNamespace(name),
                new RangedAttribute("attribute.name." + id, def, min, max));
    }

    public static final Holder<Attribute> ARMOR = register("generic.armor", 0, 0, 30);
    public static final Holder<Attribute> ARMOR_TOUGHNESS = register("generic.armor_toughness", 0, 0, 20);
    public static final Holder<Attribute> ATTACK_DAMAGE = register("generic.attack_damage", 2, 0, 2048);
    public static final Holder<Attribute> ATTACK_KNOCKBACK = register("generic.attack_knockback", 0, 0, 5);
    public static final Holder<Attribute> ATTACK_SPEED = register("generic.attack_speed", 4, 0, 1024);
    public static final Holder<Attribute> BLOCK_BREAK_SPEED = register("generic.block_break_speed", 1, 0, 1024);
    public static final Holder<Attribute> BLOCK_INTERACTION_RANGE = register("generic.block_interaction_range", 4.5, 0, 64);
    public static final Holder<Attribute> BURNING_TIME = register("generic.burning_time", 1, 0, 1024);
    public static final Holder<Attribute> ENTITY_INTERACTION_RANGE = register("generic.entity_interaction_range", 3, 0, 64);
    public static final Holder<Attribute> EXPLOSION_KNOCKBACK_RESISTANCE = register("generic.explosion_knockback_resistance", 0, 0, 1);
    public static final Holder<Attribute> FALL_DAMAGE_MULTIPLIER = register("generic.fall_damage_multiplier", 1, 0, 100);
    public static final Holder<Attribute> FLYING_SPEED = register("generic.flying_speed", 0.4, 0, 1024);
    public static final Holder<Attribute> FOLLOW_RANGE = register("generic.follow_range", 32, 0, 2048);
    public static final Holder<Attribute> GRAVITY = register("generic.gravity", 0.08, -1, 1);
    public static final Holder<Attribute> JUMP_STRENGTH = register("generic.jump_strength", 0.42, 0, 32);
    public static final Holder<Attribute> KNOCKBACK_RESISTANCE = register("generic.knockback_resistance", 0, 0, 1);
    public static final Holder<Attribute> LUCK = register("generic.luck", 0, -1024, 1024);
    public static final Holder<Attribute> MAX_ABSORPTION = register("generic.max_absorption", 0, 0, 2048);
    public static final Holder<Attribute> MAX_HEALTH = register("generic.max_health", 20, 1, 1024);
    public static final Holder<Attribute> MINING_EFFICIENCY = register("generic.mining_efficiency", 0, 0, 1024);
    public static final Holder<Attribute> MOVEMENT_EFFICIENCY = register("generic.movement_efficiency", 0, 0, 1);
    public static final Holder<Attribute> MOVEMENT_SPEED = register("generic.movement_speed", 0.7, 0, 1024);
    public static final Holder<Attribute> OXYGEN_BONUS = register("generic.oxygen_bonus", 0, 0, 1024);
    public static final Holder<Attribute> SAFE_FALL_DISTANCE = register("generic.safe_fall_distance", 3, -1024, 1024);
    public static final Holder<Attribute> SCALE = register("generic.scale", 1, 0.0625, 16);
    public static final Holder<Attribute> SNEAKING_SPEED = register("generic.sneaking_speed", 0.3, 0, 1);
    public static final Holder<Attribute> SPAWN_REINFORCEMENTS_CHANCE = register("generic.spawn_reinforcements", 0, 0, 1);
    public static final Holder<Attribute> STEP_HEIGHT = register("generic.step_height", 0.6, 0, 10);
    public static final Holder<Attribute> SUBMERGED_MINING_SPEED = register("generic.submerged_mining_speed", 0.2, 0, 20);
    public static final Holder<Attribute> SWEEPING_DAMAGE_RATIO = register("generic.sweeping_damage_ratio", 0, 0, 1);
    public static final Holder<Attribute> WATER_MOVEMENT_EFFICIENCY = register("generic.water_movement_efficiency", 0, 0, 1);
}
