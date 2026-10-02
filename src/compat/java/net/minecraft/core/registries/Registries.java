package net.minecraft.core.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Registry keys, named like Minecraft's (reamc-compat). */
public final class Registries {
    private Registries() { }

    public static final ResourceLocation ROOT_REGISTRY_NAME = ResourceLocation.withDefaultNamespace("root");

    private static <T> ResourceKey<Registry<T>> key(String name) { return ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace(name)); }

    public static final ResourceKey<Registry<Object>> ACTIVITY = key("activity");
    public static final ResourceKey<Registry<Object>> ATTRIBUTE = key("attribute");
    public static final ResourceKey<Registry<Object>> BANNER_PATTERN = key("banner_pattern");
    public static final ResourceKey<Registry<Object>> BIOME_SOURCE = key("worldgen/biome_source");
    public static final ResourceKey<Registry<net.minecraft.world.level.block.Block>> BLOCK = key("block");
    public static final ResourceKey<Registry<Object>> BLOCK_TYPE = key("block_type");
    public static final ResourceKey<Registry<net.minecraft.world.level.block.entity.BlockEntityType<?>>> BLOCK_ENTITY_TYPE = key("block_entity_type");
    public static final ResourceKey<Registry<Object>> BLOCK_PREDICATE_TYPE = key("block_predicate_type");
    public static final ResourceKey<Registry<Object>> BLOCK_STATE_PROVIDER_TYPE = key("worldgen/block_state_provider_type");
    public static final ResourceKey<Registry<Object>> CARVER = key("worldgen/carver");
    public static final ResourceKey<Registry<Object>> CAT_VARIANT = key("cat_variant");
    public static final ResourceKey<Registry<Object>> WOLF_VARIANT = key("wolf_variant");
    public static final ResourceKey<Registry<Object>> CHUNK_GENERATOR = key("worldgen/chunk_generator");
    public static final ResourceKey<Registry<Object>> CHUNK_STATUS = key("chunk_status");
    public static final ResourceKey<Registry<Object>> COMMAND_ARGUMENT_TYPE = key("command_argument_type");
    public static final ResourceKey<Registry<net.minecraft.world.item.CreativeModeTab>> CREATIVE_MODE_TAB = key("creative_mode_tab");
    public static final ResourceKey<Registry<Object>> CUSTOM_STAT = key("custom_stat");
    public static final ResourceKey<Registry<Object>> DAMAGE_TYPE = key("damage_type");
    public static final ResourceKey<Registry<Object>> DENSITY_FUNCTION_TYPE = key("worldgen/density_function_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_ENTITY_EFFECT_TYPE = key("enchantment_entity_effect_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = key("enchantment_level_based_value_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = key("enchantment_location_based_effect_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_PROVIDER_TYPE = key("enchantment_provider_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_VALUE_EFFECT_TYPE = key("enchantment_value_effect_type");
    public static final ResourceKey<Registry<Object>> ENTITY_TYPE = key("entity_type");
    public static final ResourceKey<Registry<Object>> FEATURE = key("worldgen/feature");
    public static final ResourceKey<Registry<Object>> FEATURE_SIZE_TYPE = key("worldgen/feature_size_type");
    public static final ResourceKey<Registry<Object>> FLOAT_PROVIDER_TYPE = key("float_provider_type");
    public static final ResourceKey<Registry<Object>> FLUID = key("fluid");
    public static final ResourceKey<Registry<Object>> FOLIAGE_PLACER_TYPE = key("worldgen/foliage_placer_type");
    public static final ResourceKey<Registry<Object>> FROG_VARIANT = key("frog_variant");
    public static final ResourceKey<Registry<Object>> GAME_EVENT = key("game_event");
    public static final ResourceKey<Registry<Object>> HEIGHT_PROVIDER_TYPE = key("height_provider_type");
    public static final ResourceKey<Registry<Object>> INSTRUMENT = key("instrument");
    public static final ResourceKey<Registry<Object>> INT_PROVIDER_TYPE = key("int_provider_type");
    public static final ResourceKey<Registry<net.minecraft.world.item.Item>> ITEM = key("item");
    public static final ResourceKey<Registry<Object>> JUKEBOX_SONG = key("jukebox_song");
    public static final ResourceKey<Registry<Object>> LOOT_CONDITION_TYPE = key("loot_condition_type");
    public static final ResourceKey<Registry<Object>> LOOT_FUNCTION_TYPE = key("loot_function_type");
    public static final ResourceKey<Registry<Object>> LOOT_NBT_PROVIDER_TYPE = key("loot_nbt_provider_type");
    public static final ResourceKey<Registry<Object>> LOOT_NUMBER_PROVIDER_TYPE = key("loot_number_provider_type");
    public static final ResourceKey<Registry<Object>> LOOT_POOL_ENTRY_TYPE = key("loot_pool_entry_type");
    public static final ResourceKey<Registry<Object>> LOOT_SCORE_PROVIDER_TYPE = key("loot_score_provider_type");
    public static final ResourceKey<Registry<Object>> MATERIAL_CONDITION = key("worldgen/material_condition");
    public static final ResourceKey<Registry<Object>> MATERIAL_RULE = key("worldgen/material_rule");
    public static final ResourceKey<Registry<Object>> MEMORY_MODULE_TYPE = key("memory_module_type");
    public static final ResourceKey<Registry<net.minecraft.world.inventory.MenuType<?>>> MENU = key("menu");
    public static final ResourceKey<Registry<Object>> MOB_EFFECT = key("mob_effect");
    public static final ResourceKey<Registry<Object>> PAINTING_VARIANT = key("painting_variant");
    public static final ResourceKey<Registry<Object>> PARTICLE_TYPE = key("particle_type");
    public static final ResourceKey<Registry<Object>> PLACEMENT_MODIFIER_TYPE = key("worldgen/placement_modifier_type");
    public static final ResourceKey<Registry<Object>> POINT_OF_INTEREST_TYPE = key("point_of_interest_type");
    public static final ResourceKey<Registry<Object>> POSITION_SOURCE_TYPE = key("position_source_type");
    public static final ResourceKey<Registry<Object>> POS_RULE_TEST = key("pos_rule_test");
    public static final ResourceKey<Registry<Object>> POTION = key("potion");
    public static final ResourceKey<Registry<Object>> RECIPE_SERIALIZER = key("recipe_serializer");
    public static final ResourceKey<Registry<Object>> RECIPE_TYPE = key("recipe_type");
    public static final ResourceKey<Registry<Object>> ROOT_PLACER_TYPE = key("worldgen/root_placer_type");
    public static final ResourceKey<Registry<Object>> RULE_TEST = key("rule_test");
    public static final ResourceKey<Registry<Object>> RULE_BLOCK_ENTITY_MODIFIER = key("rule_block_entity_modifier");
    public static final ResourceKey<Registry<Object>> SCHEDULE = key("schedule");
    public static final ResourceKey<Registry<Object>> SENSOR_TYPE = key("sensor_type");
    public static final ResourceKey<Registry<net.minecraft.sounds.SoundEvent>> SOUND_EVENT = key("sound_event");
    public static final ResourceKey<Registry<Object>> STAT_TYPE = key("stat_type");
    public static final ResourceKey<Registry<Object>> STRUCTURE_PIECE = key("worldgen/structure_piece");
    public static final ResourceKey<Registry<Object>> STRUCTURE_PLACEMENT = key("worldgen/structure_placement");
    public static final ResourceKey<Registry<Object>> STRUCTURE_POOL_ELEMENT = key("worldgen/structure_pool_element");
    public static final ResourceKey<Registry<Object>> POOL_ALIAS_BINDING = key("worldgen/pool_alias_binding");
    public static final ResourceKey<Registry<Object>> STRUCTURE_PROCESSOR = key("worldgen/structure_processor");
    public static final ResourceKey<Registry<Object>> STRUCTURE_TYPE = key("worldgen/structure_type");
    public static final ResourceKey<Registry<Object>> TREE_DECORATOR_TYPE = key("worldgen/tree_decorator_type");
    public static final ResourceKey<Registry<Object>> TRUNK_PLACER_TYPE = key("worldgen/trunk_placer_type");
    public static final ResourceKey<Registry<Object>> VILLAGER_PROFESSION = key("villager_profession");
    public static final ResourceKey<Registry<Object>> VILLAGER_TYPE = key("villager_type");
    public static final ResourceKey<Registry<Object>> DECORATED_POT_PATTERN = key("decorated_pot_pattern");
    public static final ResourceKey<Registry<Object>> NUMBER_FORMAT_TYPE = key("number_format_type");
    public static final ResourceKey<Registry<net.minecraft.world.item.ArmorMaterial>> ARMOR_MATERIAL = key("armor_material");
    public static final ResourceKey<Registry<Object>> DATA_COMPONENT_TYPE = key("data_component_type");
    public static final ResourceKey<Registry<Object>> ENTITY_SUB_PREDICATE_TYPE = key("entity_sub_predicate_type");
    public static final ResourceKey<Registry<Object>> ITEM_SUB_PREDICATE_TYPE = key("item_sub_predicate_type");
    public static final ResourceKey<Registry<Object>> MAP_DECORATION_TYPE = key("map_decoration_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_EFFECT_COMPONENT_TYPE = key("enchantment_effect_component_type");
    public static final ResourceKey<Registry<Object>> BIOME = key("worldgen/biome");
    public static final ResourceKey<Registry<Object>> CHAT_TYPE = key("chat_type");
    public static final ResourceKey<Registry<Object>> CONFIGURED_CARVER = key("worldgen/configured_carver");
    public static final ResourceKey<Registry<Object>> CONFIGURED_FEATURE = key("worldgen/configured_feature");
    public static final ResourceKey<Registry<Object>> DENSITY_FUNCTION = key("worldgen/density_function");
    public static final ResourceKey<Registry<Object>> DIMENSION_TYPE = key("dimension_type");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT = key("enchantment");
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_PROVIDER = key("enchantment_provider");
    public static final ResourceKey<Registry<Object>> FLAT_LEVEL_GENERATOR_PRESET = key("worldgen/flat_level_generator_preset");
    public static final ResourceKey<Registry<Object>> NOISE_SETTINGS = key("worldgen/noise_settings");
    public static final ResourceKey<Registry<Object>> NOISE = key("worldgen/noise");
    public static final ResourceKey<Registry<Object>> PLACED_FEATURE = key("worldgen/placed_feature");
    public static final ResourceKey<Registry<Object>> STRUCTURE = key("worldgen/structure");
    public static final ResourceKey<Registry<Object>> PROCESSOR_LIST = key("worldgen/processor_list");
    public static final ResourceKey<Registry<Object>> STRUCTURE_SET = key("worldgen/structure_set");
    public static final ResourceKey<Registry<Object>> TEMPLATE_POOL = key("worldgen/template_pool");
    public static final ResourceKey<Registry<Object>> TRIGGER_TYPE = key("trigger_type");
    public static final ResourceKey<Registry<Object>> TRIM_MATERIAL = key("trim_material");
    public static final ResourceKey<Registry<Object>> TRIM_PATTERN = key("trim_pattern");
    public static final ResourceKey<Registry<Object>> WORLD_PRESET = key("worldgen/world_preset");
    public static final ResourceKey<Registry<Object>> MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = key("worldgen/multi_noise_biome_source_parameter_list");
    public static final ResourceKey<Registry<Object>> DIMENSION = key("dimension");
    public static final ResourceKey<Registry<Object>> LEVEL_STEM = key("dimension");
    public static final ResourceKey<Registry<Object>> LOOT_TABLE = key("loot_table");
    public static final ResourceKey<Registry<Object>> ITEM_MODIFIER = key("item_modifier");
    public static final ResourceKey<Registry<Object>> PREDICATE = key("predicate");
    public static final ResourceKey<Registry<Object>> ADVANCEMENT = key("advancement");
    public static final ResourceKey<Registry<Object>> RECIPE = key("recipe");

    /** "minecraft:worldgen/biome" -> "worldgen/biome": the folder of a registry's data files. */
    public static String elementsDirPath(ResourceKey<? extends Registry<?>> key) { return key.location().getNamespace().equals("minecraft") ? key.location().getPath() : key.location().getNamespace() + "/" + key.location().getPath(); }

    public static String tagsDirPath(ResourceKey<? extends Registry<?>> key) { return "tags/" + elementsDirPath(key); }
}
