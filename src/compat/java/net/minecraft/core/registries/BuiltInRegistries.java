package net.minecraft.core.registries;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;

/** The registries mods add to (reamc-compat). */
public final class BuiltInRegistries {
    private BuiltInRegistries() { }

    public static final Registry<net.minecraft.sounds.SoundEvent> SOUND_EVENT = new MappedRegistry<>(Registries.SOUND_EVENT);
    public static final Registry<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIAL = new MappedRegistry<>(Registries.ARMOR_MATERIAL);
    public static final Registry<net.minecraft.world.level.block.Block> BLOCK = new MappedRegistry<>(Registries.BLOCK);
    public static final Registry<net.minecraft.world.item.Item> ITEM = new MappedRegistry<>(Registries.ITEM);
    public static final Registry<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPE = new MappedRegistry<>(Registries.BLOCK_ENTITY_TYPE);
    public static final Registry<net.minecraft.world.inventory.MenuType<?>> MENU = new MappedRegistry<>(Registries.MENU);
    public static final Registry<net.minecraft.world.item.CreativeModeTab> CREATIVE_MODE_TAB = new MappedRegistry<>(Registries.CREATIVE_MODE_TAB);

    /** Registries in the order mods' registration events fire (armor materials before the items that use them). */
    public static final java.util.List<Registry<?>> ORDER = java.util.List.of(SOUND_EVENT, ARMOR_MATERIAL, BLOCK, ITEM, BLOCK_ENTITY_TYPE, MENU, CREATIVE_MODE_TAB);
}
