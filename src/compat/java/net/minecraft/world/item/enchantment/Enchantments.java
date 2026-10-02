package net.minecraft.world.item.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's enchantment keys; reamc fills the enchantment registry with the ones it has (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface Enchantments {
    private static ResourceKey<Enchantment> key(String id) { return ResourceKey.create((ResourceKey) Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(id)); }

    ResourceKey<Enchantment> PROTECTION = key("protection");
    ResourceKey<Enchantment> FIRE_PROTECTION = key("fire_protection");
    ResourceKey<Enchantment> FEATHER_FALLING = key("feather_falling");
    ResourceKey<Enchantment> BLAST_PROTECTION = key("blast_protection");
    ResourceKey<Enchantment> PROJECTILE_PROTECTION = key("projectile_protection");
    ResourceKey<Enchantment> RESPIRATION = key("respiration");
    ResourceKey<Enchantment> AQUA_AFFINITY = key("aqua_affinity");
    ResourceKey<Enchantment> THORNS = key("thorns");
    ResourceKey<Enchantment> DEPTH_STRIDER = key("depth_strider");
    ResourceKey<Enchantment> FROST_WALKER = key("frost_walker");
    ResourceKey<Enchantment> BINDING_CURSE = key("binding_curse");
    ResourceKey<Enchantment> SOUL_SPEED = key("soul_speed");
    ResourceKey<Enchantment> SWIFT_SNEAK = key("swift_sneak");
    ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    ResourceKey<Enchantment> SMITE = key("smite");
    ResourceKey<Enchantment> BANE_OF_ARTHROPODS = key("bane_of_arthropods");
    ResourceKey<Enchantment> KNOCKBACK = key("knockback");
    ResourceKey<Enchantment> FIRE_ASPECT = key("fire_aspect");
    ResourceKey<Enchantment> LOOTING = key("looting");
    ResourceKey<Enchantment> SWEEPING_EDGE = key("sweeping_edge");
    ResourceKey<Enchantment> EFFICIENCY = key("efficiency");
    ResourceKey<Enchantment> SILK_TOUCH = key("silk_touch");
    ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
    ResourceKey<Enchantment> FORTUNE = key("fortune");
    ResourceKey<Enchantment> POWER = key("power");
    ResourceKey<Enchantment> PUNCH = key("punch");
    ResourceKey<Enchantment> FLAME = key("flame");
    ResourceKey<Enchantment> INFINITY = key("infinity");
    ResourceKey<Enchantment> LUCK_OF_THE_SEA = key("luck_of_the_sea");
    ResourceKey<Enchantment> LURE = key("lure");
    ResourceKey<Enchantment> LOYALTY = key("loyalty");
    ResourceKey<Enchantment> IMPALING = key("impaling");
    ResourceKey<Enchantment> RIPTIDE = key("riptide");
    ResourceKey<Enchantment> CHANNELING = key("channeling");
    ResourceKey<Enchantment> MULTISHOT = key("multishot");
    ResourceKey<Enchantment> QUICK_CHARGE = key("quick_charge");
    ResourceKey<Enchantment> PIERCING = key("piercing");
    ResourceKey<Enchantment> DENSITY = key("density");
    ResourceKey<Enchantment> BREACH = key("breach");
    ResourceKey<Enchantment> WIND_BURST = key("wind_burst");
    ResourceKey<Enchantment> MENDING = key("mending");
    ResourceKey<Enchantment> VANISHING_CURSE = key("vanishing_curse");
}
