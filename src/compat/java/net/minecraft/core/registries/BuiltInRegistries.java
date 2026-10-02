package net.minecraft.core.registries;

import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The game's registries, created in Minecraft's order (which is also the order of mods' RegisterEvents).
 * Their contents are whatever mods register plus stand-ins for reamc's own blocks and items (reamc-compat).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class BuiltInRegistries {
    private BuiltInRegistries() { }

    /** Every registry by key, in creation order. */
    public static final MappedRegistry<Registry<?>> REGISTRY = new MappedRegistry<>((ResourceKey) ResourceKey.createRegistryKey(Registries.ROOT_REGISTRY_NAME));

    private static <T> Registry<T> simple(ResourceKey<? extends Registry<?>> key) {
        MappedRegistry<T> r = new MappedRegistry<>((ResourceKey) key);
        REGISTRY.register(key.location(), r);
        return r;
    }

    private static <T> DefaultedRegistry<T> defaulted(ResourceKey<? extends Registry<?>> key, String defaultKey) {
        DefaultedMappedRegistry<T> r = new DefaultedMappedRegistry<>(ResourceLocation.withDefaultNamespace(defaultKey), (ResourceKey) key);
        REGISTRY.register(key.location(), r);
        return r;
    }

    public static final DefaultedRegistry<Object> GAME_EVENT = (DefaultedRegistry) defaulted(Registries.GAME_EVENT, "step");
    public static final Registry<net.minecraft.sounds.SoundEvent> SOUND_EVENT = (Registry) simple(Registries.SOUND_EVENT);
    public static final DefaultedRegistry<Object> FLUID = (DefaultedRegistry) defaulted(Registries.FLUID, "empty");
    public static final Registry<Object> MOB_EFFECT = (Registry) simple(Registries.MOB_EFFECT);
    public static final DefaultedRegistry<net.minecraft.world.level.block.Block> BLOCK = (DefaultedRegistry) defaulted(Registries.BLOCK, "air");
    public static final DefaultedRegistry<Object> ENTITY_TYPE = (DefaultedRegistry) defaulted(Registries.ENTITY_TYPE, "pig");
    public static final DefaultedRegistry<net.minecraft.world.item.Item> ITEM = (DefaultedRegistry) defaulted(Registries.ITEM, "air");
    public static final Registry<Object> POTION = (Registry) simple(Registries.POTION);
    public static final Registry<Object> PARTICLE_TYPE = (Registry) simple(Registries.PARTICLE_TYPE);
    public static final Registry<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPE = (Registry) simple(Registries.BLOCK_ENTITY_TYPE);
    public static final Registry<Object> CUSTOM_STAT = (Registry) simple(Registries.CUSTOM_STAT);
    public static final DefaultedRegistry<Object> CHUNK_STATUS = (DefaultedRegistry) defaulted(Registries.CHUNK_STATUS, "empty");
    public static final Registry<Object> RULE_TEST = (Registry) simple(Registries.RULE_TEST);
    public static final Registry<Object> RULE_BLOCK_ENTITY_MODIFIER = (Registry) simple(Registries.RULE_BLOCK_ENTITY_MODIFIER);
    public static final Registry<Object> POS_RULE_TEST = (Registry) simple(Registries.POS_RULE_TEST);
    public static final Registry<net.minecraft.world.inventory.MenuType<?>> MENU = (Registry) simple(Registries.MENU);
    public static final Registry<Object> RECIPE_TYPE = (Registry) simple(Registries.RECIPE_TYPE);
    public static final Registry<Object> RECIPE_SERIALIZER = (Registry) simple(Registries.RECIPE_SERIALIZER);
    public static final Registry<Object> ATTRIBUTE = (Registry) simple(Registries.ATTRIBUTE);
    public static final Registry<Object> POSITION_SOURCE_TYPE = (Registry) simple(Registries.POSITION_SOURCE_TYPE);
    public static final Registry<Object> COMMAND_ARGUMENT_TYPE = (Registry) simple(Registries.COMMAND_ARGUMENT_TYPE);
    public static final Registry<Object> STAT_TYPE = (Registry) simple(Registries.STAT_TYPE);
    public static final DefaultedRegistry<Object> VILLAGER_TYPE = (DefaultedRegistry) defaulted(Registries.VILLAGER_TYPE, "plains");
    public static final DefaultedRegistry<Object> VILLAGER_PROFESSION = (DefaultedRegistry) defaulted(Registries.VILLAGER_PROFESSION, "none");
    public static final Registry<Object> POINT_OF_INTEREST_TYPE = (Registry) simple(Registries.POINT_OF_INTEREST_TYPE);
    public static final DefaultedRegistry<Object> MEMORY_MODULE_TYPE = (DefaultedRegistry) defaulted(Registries.MEMORY_MODULE_TYPE, "dummy");
    public static final DefaultedRegistry<Object> SENSOR_TYPE = (DefaultedRegistry) defaulted(Registries.SENSOR_TYPE, "dummy");
    public static final Registry<Object> SCHEDULE = (Registry) simple(Registries.SCHEDULE);
    public static final Registry<Object> ACTIVITY = (Registry) simple(Registries.ACTIVITY);
    public static final Registry<Object> LOOT_POOL_ENTRY_TYPE = (Registry) simple(Registries.LOOT_POOL_ENTRY_TYPE);
    public static final Registry<Object> LOOT_FUNCTION_TYPE = (Registry) simple(Registries.LOOT_FUNCTION_TYPE);
    public static final Registry<Object> LOOT_CONDITION_TYPE = (Registry) simple(Registries.LOOT_CONDITION_TYPE);
    public static final Registry<Object> LOOT_NUMBER_PROVIDER_TYPE = (Registry) simple(Registries.LOOT_NUMBER_PROVIDER_TYPE);
    public static final Registry<Object> LOOT_NBT_PROVIDER_TYPE = (Registry) simple(Registries.LOOT_NBT_PROVIDER_TYPE);
    public static final Registry<Object> LOOT_SCORE_PROVIDER_TYPE = (Registry) simple(Registries.LOOT_SCORE_PROVIDER_TYPE);
    public static final Registry<Object> FLOAT_PROVIDER_TYPE = (Registry) simple(Registries.FLOAT_PROVIDER_TYPE);
    public static final Registry<Object> INT_PROVIDER_TYPE = (Registry) simple(Registries.INT_PROVIDER_TYPE);
    public static final Registry<Object> HEIGHT_PROVIDER_TYPE = (Registry) simple(Registries.HEIGHT_PROVIDER_TYPE);
    public static final Registry<Object> BLOCK_PREDICATE_TYPE = (Registry) simple(Registries.BLOCK_PREDICATE_TYPE);
    public static final Registry<Object> CARVER = (Registry) simple(Registries.CARVER);
    public static final Registry<Object> FEATURE = (Registry) simple(Registries.FEATURE);
    public static final Registry<Object> STRUCTURE_PLACEMENT = (Registry) simple(Registries.STRUCTURE_PLACEMENT);
    public static final Registry<Object> STRUCTURE_PIECE = (Registry) simple(Registries.STRUCTURE_PIECE);
    public static final Registry<Object> STRUCTURE_TYPE = (Registry) simple(Registries.STRUCTURE_TYPE);
    public static final Registry<Object> PLACEMENT_MODIFIER_TYPE = (Registry) simple(Registries.PLACEMENT_MODIFIER_TYPE);
    public static final Registry<Object> BLOCKSTATE_PROVIDER_TYPE = (Registry) simple(Registries.BLOCK_STATE_PROVIDER_TYPE);
    public static final Registry<Object> FOLIAGE_PLACER_TYPE = (Registry) simple(Registries.FOLIAGE_PLACER_TYPE);
    public static final Registry<Object> TRUNK_PLACER_TYPE = (Registry) simple(Registries.TRUNK_PLACER_TYPE);
    public static final Registry<Object> ROOT_PLACER_TYPE = (Registry) simple(Registries.ROOT_PLACER_TYPE);
    public static final Registry<Object> TREE_DECORATOR_TYPE = (Registry) simple(Registries.TREE_DECORATOR_TYPE);
    public static final Registry<Object> FEATURE_SIZE_TYPE = (Registry) simple(Registries.FEATURE_SIZE_TYPE);
    public static final Registry<Object> BIOME_SOURCE = (Registry) simple(Registries.BIOME_SOURCE);
    public static final Registry<Object> CHUNK_GENERATOR = (Registry) simple(Registries.CHUNK_GENERATOR);
    public static final Registry<Object> MATERIAL_CONDITION = (Registry) simple(Registries.MATERIAL_CONDITION);
    public static final Registry<Object> MATERIAL_RULE = (Registry) simple(Registries.MATERIAL_RULE);
    public static final Registry<Object> DENSITY_FUNCTION_TYPE = (Registry) simple(Registries.DENSITY_FUNCTION_TYPE);
    public static final Registry<Object> BLOCK_TYPE = (Registry) simple(Registries.BLOCK_TYPE);
    public static final Registry<Object> STRUCTURE_PROCESSOR = (Registry) simple(Registries.STRUCTURE_PROCESSOR);
    public static final Registry<Object> STRUCTURE_POOL_ELEMENT = (Registry) simple(Registries.STRUCTURE_POOL_ELEMENT);
    public static final Registry<Object> POOL_ALIAS_BINDING_TYPE = (Registry) simple(Registries.POOL_ALIAS_BINDING);
    public static final Registry<Object> CAT_VARIANT = (Registry) simple(Registries.CAT_VARIANT);
    public static final Registry<Object> FROG_VARIANT = (Registry) simple(Registries.FROG_VARIANT);
    public static final Registry<Object> INSTRUMENT = (Registry) simple(Registries.INSTRUMENT);
    public static final Registry<Object> DECORATED_POT_PATTERN = (Registry) simple(Registries.DECORATED_POT_PATTERN);
    public static final Registry<net.minecraft.world.item.CreativeModeTab> CREATIVE_MODE_TAB = (Registry) simple(Registries.CREATIVE_MODE_TAB);
    public static final Registry<Object> TRIGGER_TYPES = (Registry) simple(Registries.TRIGGER_TYPE);
    public static final Registry<Object> NUMBER_FORMAT_TYPE = (Registry) simple(Registries.NUMBER_FORMAT_TYPE);
    public static final Registry<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIAL = (Registry) simple(Registries.ARMOR_MATERIAL);
    public static final Registry<Object> DATA_COMPONENT_TYPE = (Registry) simple(Registries.DATA_COMPONENT_TYPE);
    public static final Registry<Object> ENTITY_SUB_PREDICATE_TYPE = (Registry) simple(Registries.ENTITY_SUB_PREDICATE_TYPE);
    public static final Registry<Object> ITEM_SUB_PREDICATE_TYPE = (Registry) simple(Registries.ITEM_SUB_PREDICATE_TYPE);
    public static final Registry<Object> MAP_DECORATION_TYPE = (Registry) simple(Registries.MAP_DECORATION_TYPE);
    public static final Registry<Object> ENCHANTMENT_EFFECT_COMPONENT_TYPE = (Registry) simple(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE);
    public static final Registry<Object> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = (Registry) simple(Registries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE);
    public static final Registry<Object> ENCHANTMENT_ENTITY_EFFECT_TYPE = (Registry) simple(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE);
    public static final Registry<Object> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = (Registry) simple(Registries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE);
    public static final Registry<Object> ENCHANTMENT_VALUE_EFFECT_TYPE = (Registry) simple(Registries.ENCHANTMENT_VALUE_EFFECT_TYPE);
    public static final Registry<Object> ENCHANTMENT_PROVIDER_TYPE = (Registry) simple(Registries.ENCHANTMENT_PROVIDER_TYPE);
}
