package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's entity_type tag keys; their contents come from tag files (reamc-compat, see mc.mod.Tags). */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class EntityTypeTags {
    private EntityTypeTags() { }

    private static TagKey<Object> tag(String ns, String path) { return create(ResourceLocation.fromNamespaceAndPath(ns, path)); }

    public static TagKey<Object> create(ResourceLocation id) { return TagKey.create((net.minecraft.resources.ResourceKey) Registries.ENTITY_TYPE, id); }

    public static final TagKey<Object> SKELETONS = tag("minecraft", "skeletons");
    public static final TagKey<Object> ZOMBIES = tag("minecraft", "zombies");
    public static final TagKey<Object> RAIDERS = tag("minecraft", "raiders");
    public static final TagKey<Object> UNDEAD = tag("minecraft", "undead");
    public static final TagKey<Object> BEEHIVE_INHABITORS = tag("minecraft", "beehive_inhabitors");
    public static final TagKey<Object> ARROWS = tag("minecraft", "arrows");
    public static final TagKey<Object> IMPACT_PROJECTILES = tag("minecraft", "impact_projectiles");
    public static final TagKey<Object> POWDER_SNOW_WALKABLE_MOBS = tag("minecraft", "powder_snow_walkable_mobs");
    public static final TagKey<Object> AXOLOTL_ALWAYS_HOSTILES = tag("minecraft", "axolotl_always_hostiles");
    public static final TagKey<Object> AXOLOTL_HUNT_TARGETS = tag("minecraft", "axolotl_hunt_targets");
    public static final TagKey<Object> FREEZE_IMMUNE_ENTITY_TYPES = tag("minecraft", "freeze_immune_entity_types");
    public static final TagKey<Object> FREEZE_HURTS_EXTRA_TYPES = tag("minecraft", "freeze_hurts_extra_types");
    public static final TagKey<Object> CAN_BREATHE_UNDER_WATER = tag("minecraft", "can_breathe_under_water");
    public static final TagKey<Object> FROG_FOOD = tag("minecraft", "frog_food");
    public static final TagKey<Object> FALL_DAMAGE_IMMUNE = tag("minecraft", "fall_damage_immune");
    public static final TagKey<Object> DISMOUNTS_UNDERWATER = tag("minecraft", "dismounts_underwater");
    public static final TagKey<Object> NON_CONTROLLING_RIDER = tag("minecraft", "non_controlling_rider");
    public static final TagKey<Object> DEFLECTS_PROJECTILES = tag("minecraft", "deflects_projectiles");
    public static final TagKey<Object> CAN_TURN_IN_BOATS = tag("minecraft", "can_turn_in_boats");
    public static final TagKey<Object> ILLAGER = tag("minecraft", "illager");
    public static final TagKey<Object> AQUATIC = tag("minecraft", "aquatic");
    public static final TagKey<Object> ARTHROPOD = tag("minecraft", "arthropod");
    public static final TagKey<Object> IGNORES_POISON_AND_REGEN = tag("minecraft", "ignores_poison_and_regen");
    public static final TagKey<Object> INVERTED_HEALING_AND_HARM = tag("minecraft", "inverted_healing_and_harm");
    public static final TagKey<Object> WITHER_FRIENDS = tag("minecraft", "wither_friends");
    public static final TagKey<Object> ILLAGER_FRIENDS = tag("minecraft", "illager_friends");
    public static final TagKey<Object> NOT_SCARY_FOR_PUFFERFISH = tag("minecraft", "not_scary_for_pufferfish");
    public static final TagKey<Object> SENSITIVE_TO_IMPALING = tag("minecraft", "sensitive_to_impaling");
    public static final TagKey<Object> SENSITIVE_TO_BANE_OF_ARTHROPODS = tag("minecraft", "sensitive_to_bane_of_arthropods");
    public static final TagKey<Object> SENSITIVE_TO_SMITE = tag("minecraft", "sensitive_to_smite");
    public static final TagKey<Object> NO_ANGER_FROM_WIND_CHARGE = tag("minecraft", "no_anger_from_wind_charge");
    public static final TagKey<Object> IMMUNE_TO_OOZING = tag("minecraft", "immune_to_oozing");
    public static final TagKey<Object> IMMUNE_TO_INFESTED = tag("minecraft", "immune_to_infested");
    public static final TagKey<Object> REDIRECTABLE_PROJECTILE = tag("minecraft", "redirectable_projectile");
}
