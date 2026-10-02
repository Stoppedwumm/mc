package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's particle types (reamc-compat: drawn as the closest of reamc's particles). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ParticleTypes {
    private static SimpleParticleType simple(String id) {
        return (SimpleParticleType) Registry.register((Registry) BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.withDefaultNamespace(id), new SimpleParticleType(false));
    }

    private static ParticleType<BlockParticleOption> block(String id) {
        ParticleType<BlockParticleOption>[] self = new ParticleType[1];
        self[0] = new ParticleType<>(false) {
            @Override public MapCodec<BlockParticleOption> codec() { return BlockParticleOption.codec(self[0]); }
            @Override public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() { return BlockParticleOption.streamCodec(self[0]); }
        };
        return (ParticleType) Registry.register((Registry) BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.withDefaultNamespace(id), self[0]);
    }

    private static ParticleType<ItemParticleOption> item(String id) {
        ParticleType<ItemParticleOption>[] self = new ParticleType[1];
        self[0] = new ParticleType<>(false) {
            @Override public MapCodec<ItemParticleOption> codec() { return ItemParticleOption.codec(self[0]); }
            @Override public StreamCodec<? super RegistryFriendlyByteBuf, ItemParticleOption> streamCodec() { return ItemParticleOption.streamCodec(self[0]); }
        };
        return (ParticleType) Registry.register((Registry) BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.withDefaultNamespace(id), self[0]);
    }

    public static final SimpleParticleType ANGRY_VILLAGER = simple("angry_villager");
    public static final ParticleType<BlockParticleOption> BLOCK = block("block");
    public static final ParticleType<BlockParticleOption> BLOCK_MARKER = block("block_marker");
    public static final SimpleParticleType BUBBLE = simple("bubble");
    public static final SimpleParticleType CLOUD = simple("cloud");
    public static final SimpleParticleType CRIT = simple("crit");
    public static final SimpleParticleType DAMAGE_INDICATOR = simple("damage_indicator");
    public static final SimpleParticleType DRAGON_BREATH = simple("dragon_breath");
    public static final SimpleParticleType DRIPPING_LAVA = simple("dripping_lava");
    public static final SimpleParticleType FALLING_LAVA = simple("falling_lava");
    public static final SimpleParticleType LANDING_LAVA = simple("landing_lava");
    public static final SimpleParticleType DRIPPING_WATER = simple("dripping_water");
    public static final SimpleParticleType FALLING_WATER = simple("falling_water");
    public static final ParticleType<SimpleParticleType> DUST = (ParticleType) simple("dust");
    public static final ParticleType<SimpleParticleType> DUST_COLOR_TRANSITION = (ParticleType) simple("dust_color_transition");
    public static final SimpleParticleType EFFECT = simple("effect");
    public static final SimpleParticleType ELDER_GUARDIAN = simple("elder_guardian");
    public static final SimpleParticleType ENCHANTED_HIT = simple("enchanted_hit");
    public static final SimpleParticleType ENCHANT = simple("enchant");
    public static final SimpleParticleType END_ROD = simple("end_rod");
    public static final ParticleType<SimpleParticleType> ENTITY_EFFECT = (ParticleType) simple("entity_effect");
    public static final SimpleParticleType EXPLOSION_EMITTER = simple("explosion_emitter");
    public static final SimpleParticleType EXPLOSION = simple("explosion");
    public static final SimpleParticleType GUST = simple("gust");
    public static final SimpleParticleType SMALL_GUST = simple("small_gust");
    public static final SimpleParticleType GUST_EMITTER_LARGE = simple("gust_emitter_large");
    public static final SimpleParticleType GUST_EMITTER_SMALL = simple("gust_emitter_small");
    public static final SimpleParticleType SONIC_BOOM = simple("sonic_boom");
    public static final ParticleType<BlockParticleOption> FALLING_DUST = block("falling_dust");
    public static final SimpleParticleType FIREWORK = simple("firework");
    public static final SimpleParticleType FISHING = simple("fishing");
    public static final SimpleParticleType FLAME = simple("flame");
    public static final SimpleParticleType INFESTED = simple("infested");
    public static final SimpleParticleType CHERRY_LEAVES = simple("cherry_leaves");
    public static final SimpleParticleType SCULK_SOUL = simple("sculk_soul");
    public static final ParticleType<SimpleParticleType> SCULK_CHARGE = (ParticleType) simple("sculk_charge");
    public static final SimpleParticleType SCULK_CHARGE_POP = simple("sculk_charge_pop");
    public static final SimpleParticleType SOUL_FIRE_FLAME = simple("soul_fire_flame");
    public static final SimpleParticleType SOUL = simple("soul");
    public static final SimpleParticleType FLASH = simple("flash");
    public static final SimpleParticleType HAPPY_VILLAGER = simple("happy_villager");
    public static final SimpleParticleType COMPOSTER = simple("composter");
    public static final SimpleParticleType HEART = simple("heart");
    public static final SimpleParticleType INSTANT_EFFECT = simple("instant_effect");
    public static final ParticleType<ItemParticleOption> ITEM = item("item");
    public static final ParticleType<SimpleParticleType> VIBRATION = (ParticleType) simple("vibration");
    public static final SimpleParticleType ITEM_SLIME = simple("item_slime");
    public static final SimpleParticleType ITEM_COBWEB = simple("item_cobweb");
    public static final SimpleParticleType ITEM_SNOWBALL = simple("item_snowball");
    public static final SimpleParticleType LARGE_SMOKE = simple("large_smoke");
    public static final SimpleParticleType LAVA = simple("lava");
    public static final SimpleParticleType MYCELIUM = simple("mycelium");
    public static final SimpleParticleType NOTE = simple("note");
    public static final SimpleParticleType POOF = simple("poof");
    public static final SimpleParticleType PORTAL = simple("portal");
    public static final SimpleParticleType RAIN = simple("rain");
    public static final SimpleParticleType SMOKE = simple("smoke");
    public static final SimpleParticleType WHITE_SMOKE = simple("white_smoke");
    public static final SimpleParticleType SNEEZE = simple("sneeze");
    public static final SimpleParticleType SPIT = simple("spit");
    public static final SimpleParticleType SQUID_INK = simple("squid_ink");
    public static final SimpleParticleType SWEEP_ATTACK = simple("sweep_attack");
    public static final SimpleParticleType TOTEM_OF_UNDYING = simple("totem_of_undying");
    public static final SimpleParticleType UNDERWATER = simple("underwater");
    public static final SimpleParticleType SPLASH = simple("splash");
    public static final SimpleParticleType WITCH = simple("witch");
    public static final SimpleParticleType BUBBLE_POP = simple("bubble_pop");
    public static final SimpleParticleType CURRENT_DOWN = simple("current_down");
    public static final SimpleParticleType BUBBLE_COLUMN_UP = simple("bubble_column_up");
    public static final SimpleParticleType NAUTILUS = simple("nautilus");
    public static final SimpleParticleType DOLPHIN = simple("dolphin");
    public static final SimpleParticleType CAMPFIRE_COSY_SMOKE = simple("campfire_cosy_smoke");
    public static final SimpleParticleType CAMPFIRE_SIGNAL_SMOKE = simple("campfire_signal_smoke");
    public static final SimpleParticleType DRIPPING_HONEY = simple("dripping_honey");
    public static final SimpleParticleType FALLING_HONEY = simple("falling_honey");
    public static final SimpleParticleType LANDING_HONEY = simple("landing_honey");
    public static final SimpleParticleType FALLING_NECTAR = simple("falling_nectar");
    public static final SimpleParticleType FALLING_SPORE_BLOSSOM = simple("falling_spore_blossom");
    public static final SimpleParticleType ASH = simple("ash");
    public static final SimpleParticleType CRIMSON_SPORE = simple("crimson_spore");
    public static final SimpleParticleType WARPED_SPORE = simple("warped_spore");
    public static final SimpleParticleType SPORE_BLOSSOM_AIR = simple("spore_blossom_air");
    public static final SimpleParticleType DRIPPING_OBSIDIAN_TEAR = simple("dripping_obsidian_tear");
    public static final SimpleParticleType FALLING_OBSIDIAN_TEAR = simple("falling_obsidian_tear");
    public static final SimpleParticleType LANDING_OBSIDIAN_TEAR = simple("landing_obsidian_tear");
    public static final SimpleParticleType REVERSE_PORTAL = simple("reverse_portal");
    public static final SimpleParticleType WHITE_ASH = simple("white_ash");
    public static final SimpleParticleType SMALL_FLAME = simple("small_flame");
    public static final SimpleParticleType SNOWFLAKE = simple("snowflake");
    public static final SimpleParticleType DRIPPING_DRIPSTONE_LAVA = simple("dripping_dripstone_lava");
    public static final SimpleParticleType FALLING_DRIPSTONE_LAVA = simple("falling_dripstone_lava");
    public static final SimpleParticleType DRIPPING_DRIPSTONE_WATER = simple("dripping_dripstone_water");
    public static final SimpleParticleType FALLING_DRIPSTONE_WATER = simple("falling_dripstone_water");
    public static final SimpleParticleType GLOW_SQUID_INK = simple("glow_squid_ink");
    public static final SimpleParticleType GLOW = simple("glow");
    public static final SimpleParticleType WAX_ON = simple("wax_on");
    public static final SimpleParticleType WAX_OFF = simple("wax_off");
    public static final SimpleParticleType ELECTRIC_SPARK = simple("electric_spark");
    public static final SimpleParticleType SCRAPE = simple("scrape");
    public static final ParticleType<SimpleParticleType> SHRIEK = (ParticleType) simple("shriek");
    public static final SimpleParticleType EGG_CRACK = simple("egg_crack");
    public static final SimpleParticleType DUST_PLUME = simple("dust_plume");
    public static final SimpleParticleType TRIAL_SPAWNER_DETECTED_PLAYER = simple("trial_spawner_detection");
    public static final SimpleParticleType TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS = simple("trial_spawner_detection_ominous");
    public static final SimpleParticleType VAULT_CONNECTION = simple("vault_connection");
    public static final ParticleType<BlockParticleOption> DUST_PILLAR = block("dust_pillar");
    public static final SimpleParticleType OMINOUS_SPAWNING = simple("ominous_spawning");
    public static final SimpleParticleType RAID_OMEN = simple("raid_omen");
    public static final SimpleParticleType TRIAL_OMEN = simple("trial_omen");
}
