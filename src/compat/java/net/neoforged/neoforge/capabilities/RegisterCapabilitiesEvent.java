package net.neoforged.neoforge.capabilities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** Where mods attach capabilities to block entities, blocks, items and entities (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
    public <T, C, BE extends BlockEntity> void registerBlockEntity(BlockCapability<T, C> capability, BlockEntityType<BE> type, ICapabilityProvider<? super BE, C, T> provider) {
        capability.byType.put(type, (ICapabilityProvider) provider);
    }

    public <T, C> void registerBlock(BlockCapability<T, C> capability, IBlockCapabilityProvider<T, C> provider, Block... blocks) {
        for (Block b : blocks) capability.byBlock.put(b, provider);
    }

    public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items) {
        for (ItemLike i : items) capability.providers.put(i.asItem(), provider);
    }

    public <T, C, E extends Entity> void registerEntity(EntityCapability<T, C> capability, EntityType<E> type, ICapabilityProvider<? super E, C, T> provider) {
        capability.providers.put(type, (ICapabilityProvider) provider);
    }

    public boolean isBlockRegistered(BlockCapability<?, ?> capability, Block block) { return capability.byBlock.containsKey(block); }
    public boolean isItemRegistered(ItemCapability<?, ?> capability, net.minecraft.world.item.Item item) { return capability.providers.containsKey(item); }
    public boolean isEntityRegistered(EntityCapability<?, ?> capability, EntityType<?> type) { return capability.providers.containsKey(type); }
}
