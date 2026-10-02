package net.neoforged.neoforge.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** NeoForge's own registries, added after Minecraft's (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class NeoForgeRegistries {
    private NeoForgeRegistries() { }

    private static <T> Registry<T> make(ResourceKey<? extends Registry<T>> key) {
        MappedRegistry<T> r = new MappedRegistry<>((ResourceKey) key);
        BuiltInRegistries.REGISTRY.register(key.location(), (Registry) r);
        return r;
    }

    public static final Registry<Object> ENTITY_DATA_SERIALIZERS = make(Keys.ENTITY_DATA_SERIALIZERS);
    public static final Registry<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = make(Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
    public static final Registry<MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = make(Keys.BIOME_MODIFIER_SERIALIZERS);
    public static final Registry<Object> STRUCTURE_MODIFIER_SERIALIZERS = make(Keys.STRUCTURE_MODIFIER_SERIALIZERS);
    public static final Registry<Object> FLUID_TYPES = make(Keys.FLUID_TYPES);
    public static final Registry<Object> HOLDER_SET_TYPES = make(Keys.HOLDER_SET_TYPES);
    public static final Registry<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENT_TYPES = make(Keys.INGREDIENT_TYPES);
    public static final Registry<Object> FLUID_INGREDIENT_TYPES = make(Keys.FLUID_INGREDIENT_TYPES);
    public static final Registry<MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>> CONDITION_SERIALIZERS = make(Keys.CONDITION_CODECS);
    public static final Registry<Object> ATTACHMENT_TYPES = make(Keys.ATTACHMENT_TYPES);

    /** Creates the registries (and adds them to the registry of registries). */
    public static void init() { }

    public static final class Keys {
        private Keys() { }

        private static <T> ResourceKey<Registry<T>> key(String name) { return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("neoforge", name)); }

        public static final ResourceKey<Registry<Object>> ENTITY_DATA_SERIALIZERS = key("entity_data_serializers");
        public static final ResourceKey<Registry<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = key("global_loot_modifier_serializers");
        public static final ResourceKey<Registry<MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>>> BIOME_MODIFIER_SERIALIZERS = key("biome_modifier_serializers");
        public static final ResourceKey<Registry<Object>> STRUCTURE_MODIFIER_SERIALIZERS = key("structure_modifier_serializers");
        public static final ResourceKey<Registry<Object>> FLUID_TYPES = key("fluid_type");
        public static final ResourceKey<Registry<Object>> HOLDER_SET_TYPES = key("holder_set_type");
        public static final ResourceKey<Registry<net.neoforged.neoforge.common.crafting.IngredientType<?>>> INGREDIENT_TYPES = key("ingredient_serializer");
        public static final ResourceKey<Registry<Object>> FLUID_INGREDIENT_TYPES = key("fluid_ingredient_type");
        public static final ResourceKey<Registry<MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>>> CONDITION_CODECS = key("condition_codecs");
        public static final ResourceKey<Registry<Object>> ATTACHMENT_TYPES = key("attachment_types");
        public static final ResourceKey<Registry<net.neoforged.neoforge.common.world.BiomeModifier>> BIOME_MODIFIERS = key("biome_modifier");
        public static final ResourceKey<Registry<Object>> STRUCTURE_MODIFIERS = key("structure_modifier");
    }
}
