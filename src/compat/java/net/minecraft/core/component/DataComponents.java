package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.function.UnaryOperator;

/**
 * Minecraft's item components (reamc-compat). Count, damage and enchantments are reamc's own stack fields; these
 * hold what mods attach. Components without a codec here are kept in memory but not saved.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DataComponents {
    private static <T> DataComponentType<T> register(String id, UnaryOperator<DataComponentType.Builder<T>> f) {
        return (DataComponentType<T>) Registry.register((Registry) BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.withDefaultNamespace(id), f.apply(DataComponentType.builder()).build());
    }

    public static final DataComponentType<net.minecraft.world.item.component.CustomData> CUSTOM_DATA = register("custom_data", b -> b.persistent(net.minecraft.world.item.component.CustomData.CODEC));
    public static final DataComponentType<Integer> MAX_STACK_SIZE = register("max_stack_size", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Integer> MAX_DAMAGE = register("max_damage", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Integer> DAMAGE = register("damage", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Object> UNBREAKABLE = register("unbreakable", b -> b);
    public static final DataComponentType<net.minecraft.network.chat.Component> CUSTOM_NAME = register("custom_name", b -> b.persistent(net.minecraft.network.chat.ComponentSerialization.CODEC));
    public static final DataComponentType<net.minecraft.network.chat.Component> ITEM_NAME = register("item_name", b -> b.persistent(net.minecraft.network.chat.ComponentSerialization.CODEC));
    public static final DataComponentType<net.minecraft.world.item.component.ItemLore> LORE = register("lore", b -> b.persistent(net.minecraft.world.item.component.ItemLore.CODEC));
    public static final DataComponentType<net.minecraft.world.item.Rarity> RARITY = register("rarity", b -> b.persistent(net.minecraft.world.item.Rarity.CODEC));
    public static final DataComponentType<Object> ENCHANTMENTS = register("enchantments", b -> b);
    public static final DataComponentType<Object> CAN_PLACE_ON = register("can_place_on", b -> b);
    public static final DataComponentType<Object> CAN_BREAK = register("can_break", b -> b);
    public static final DataComponentType<net.minecraft.world.item.component.ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = register("attribute_modifiers", b -> b);
    public static final DataComponentType<Integer> CUSTOM_MODEL_DATA = register("custom_model_data", b -> b.persistent(Codec.INT));
    public static final DataComponentType<net.minecraft.util.Unit> HIDE_ADDITIONAL_TOOLTIP = register("hide_additional_tooltip", b -> b.persistent(net.minecraft.util.Unit.CODEC));
    public static final DataComponentType<net.minecraft.util.Unit> HIDE_TOOLTIP = register("hide_tooltip", b -> b.persistent(net.minecraft.util.Unit.CODEC));
    public static final DataComponentType<Integer> REPAIR_COST = register("repair_cost", b -> b.persistent(Codec.INT));
    public static final DataComponentType<net.minecraft.util.Unit> CREATIVE_SLOT_LOCK = register("creative_slot_lock", b -> b);
    public static final DataComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = register("enchantment_glint_override", b -> b.persistent(Codec.BOOL));
    public static final DataComponentType<net.minecraft.util.Unit> INTANGIBLE_PROJECTILE = register("intangible_projectile", b -> b.persistent(net.minecraft.util.Unit.CODEC));
    public static final DataComponentType<net.minecraft.world.food.FoodProperties> FOOD = register("food", b -> b);
    public static final DataComponentType<net.minecraft.util.Unit> FIRE_RESISTANT = register("fire_resistant", b -> b.persistent(net.minecraft.util.Unit.CODEC));
    public static final DataComponentType<Object> TOOL = register("tool", b -> b);
    public static final DataComponentType<Object> STORED_ENCHANTMENTS = register("stored_enchantments", b -> b);
    public static final DataComponentType<Integer> DYED_COLOR = register("dyed_color", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Integer> MAP_COLOR = register("map_color", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Object> MAP_ID = register("map_id", b -> b);
    public static final DataComponentType<Object> MAP_DECORATIONS = register("map_decorations", b -> b);
    public static final DataComponentType<Object> MAP_POST_PROCESSING = register("map_post_processing", b -> b);
    public static final DataComponentType<Object> CHARGED_PROJECTILES = register("charged_projectiles", b -> b);
    public static final DataComponentType<Object> BUNDLE_CONTENTS = register("bundle_contents", b -> b);
    public static final DataComponentType<Object> POTION_CONTENTS = register("potion_contents", b -> b);
    public static final DataComponentType<Object> SUSPICIOUS_STEW_EFFECTS = register("suspicious_stew_effects", b -> b);
    public static final DataComponentType<Object> WRITABLE_BOOK_CONTENT = register("writable_book_content", b -> b);
    public static final DataComponentType<Object> WRITTEN_BOOK_CONTENT = register("written_book_content", b -> b);
    public static final DataComponentType<Object> TRIM = register("trim", b -> b);
    public static final DataComponentType<Object> DEBUG_STICK_STATE = register("debug_stick_state", b -> b);
    public static final DataComponentType<net.minecraft.world.item.component.CustomData> ENTITY_DATA = register("entity_data", b -> b.persistent(net.minecraft.world.item.component.CustomData.CODEC));
    public static final DataComponentType<net.minecraft.world.item.component.CustomData> BUCKET_ENTITY_DATA = register("bucket_entity_data", b -> b.persistent(net.minecraft.world.item.component.CustomData.CODEC));
    public static final DataComponentType<net.minecraft.world.item.component.CustomData> BLOCK_ENTITY_DATA = register("block_entity_data", b -> b.persistent(net.minecraft.world.item.component.CustomData.CODEC));
    public static final DataComponentType<Object> INSTRUMENT = register("instrument", b -> b);
    public static final DataComponentType<Integer> OMINOUS_BOTTLE_AMPLIFIER = register("ominous_bottle_amplifier", b -> b.persistent(Codec.INT));
    public static final DataComponentType<Object> JUKEBOX_PLAYABLE = register("jukebox_playable", b -> b);
    public static final DataComponentType<Object> RECIPES = register("recipes", b -> b);
    public static final DataComponentType<Object> LODESTONE_TRACKER = register("lodestone_tracker", b -> b);
    public static final DataComponentType<Object> FIREWORK_EXPLOSION = register("firework_explosion", b -> b);
    public static final DataComponentType<Object> FIREWORKS = register("fireworks", b -> b);
    public static final DataComponentType<Object> PROFILE = register("profile", b -> b);
    public static final DataComponentType<Object> NOTE_BLOCK_SOUND = register("note_block_sound", b -> b);
    public static final DataComponentType<Object> BANNER_PATTERNS = register("banner_patterns", b -> b);
    public static final DataComponentType<Object> BASE_COLOR = register("base_color", b -> b);
    public static final DataComponentType<Object> POT_DECORATIONS = register("pot_decorations", b -> b);
    public static final DataComponentType<net.minecraft.world.item.component.ItemContainerContents> CONTAINER = register("container", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC));
    public static final DataComponentType<Object> BLOCK_STATE = register("block_state", b -> b);
    public static final DataComponentType<Object> BEES = register("bees", b -> b);
    public static final DataComponentType<Object> LOCK = register("lock", b -> b);
    public static final DataComponentType<Object> CONTAINER_LOOT = register("container_loot", b -> b);

    public static final DataComponentMap COMMON_ITEM_COMPONENTS = DataComponentMap.builder().set(MAX_STACK_SIZE, 64).build();
}
