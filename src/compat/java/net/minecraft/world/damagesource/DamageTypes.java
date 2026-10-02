package net.minecraft.world.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's damage type keys; reamc fills the damage type registry with them (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface DamageTypes {
    static ResourceKey<DamageType> key(String id) { return ResourceKey.create((ResourceKey) Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace(id)); }

    ResourceKey<DamageType> IN_FIRE = key("in_fire"), CAMPFIRE = key("campfire"), LIGHTNING_BOLT = key("lightning_bolt"), ON_FIRE = key("on_fire"),
            LAVA = key("lava"), HOT_FLOOR = key("hot_floor"), IN_WALL = key("in_wall"), CRAMMING = key("cramming"), DROWN = key("drown"),
            STARVE = key("starve"), CACTUS = key("cactus"), FALL = key("fall"), FLY_INTO_WALL = key("fly_into_wall"),
            FELL_OUT_OF_WORLD = key("out_of_world"), GENERIC = key("generic"), MAGIC = key("magic"), WITHER = key("wither"),
            DRAGON_BREATH = key("dragon_breath"), DRY_OUT = key("dry_out"), SWEET_BERRY_BUSH = key("sweet_berry_bush"), FREEZE = key("freeze"),
            STALAGMITE = key("stalagmite"), FALLING_BLOCK = key("falling_block"), FALLING_ANVIL = key("falling_anvil"),
            FALLING_STALACTITE = key("falling_stalactite"), STING = key("sting"), MOB_ATTACK = key("mob_attack"),
            MOB_ATTACK_NO_AGGRO = key("mob_attack_no_aggro"), PLAYER_ATTACK = key("player_attack"), ARROW = key("arrow"), TRIDENT = key("trident"),
            MOB_PROJECTILE = key("mob_projectile"), SPIT = key("spit"), WIND_CHARGE = key("wind_charge"), FIREWORKS = key("fireworks"),
            FIREBALL = key("fireball"), UNATTRIBUTED_FIREBALL = key("unattributed_fireball"), WITHER_SKULL = key("wither_skull"),
            THROWN = key("thrown"), INDIRECT_MAGIC = key("indirect_magic"), THORNS = key("thorns"), EXPLOSION = key("explosion"),
            PLAYER_EXPLOSION = key("player_explosion"), SONIC_BOOM = key("sonic_boom"), BAD_RESPAWN_POINT = key("bad_respawn_point"),
            OUTSIDE_BORDER = key("outside_border"), GENERIC_KILL = key("generic_kill");
}
