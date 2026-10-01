package net.minecraft.core.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Registry keys (reamc-compat). */
public final class Registries {
    private Registries() { }

    private static <T> ResourceKey<Registry<T>> key(String name) { return ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace(name)); }

    public static final ResourceKey<Registry<net.minecraft.world.level.block.Block>> BLOCK = key("block");
    public static final ResourceKey<Registry<net.minecraft.world.item.Item>> ITEM = key("item");
    public static final ResourceKey<Registry<net.minecraft.world.level.block.entity.BlockEntityType<?>>> BLOCK_ENTITY_TYPE = key("block_entity_type");
    public static final ResourceKey<Registry<net.minecraft.world.inventory.MenuType<?>>> MENU = key("menu");
    public static final ResourceKey<Registry<net.minecraft.world.item.CreativeModeTab>> CREATIVE_MODE_TAB = key("creative_mode_tab");
    public static final ResourceKey<Registry<net.minecraft.world.item.ArmorMaterial>> ARMOR_MATERIAL = key("armor_material");
    public static final ResourceKey<Registry<net.minecraft.sounds.SoundEvent>> SOUND_EVENT = key("sound_event");
}
