package net.neoforged.neoforge.capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** Where mods attach capabilities (reamc-compat records them; nothing in reamc queries them yet). */
public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
    public <T, C, BE extends BlockEntity> void registerBlockEntity(BlockCapability<T, C> capability, BlockEntityType<BE> type, ICapabilityProvider<? super BE, C, T> provider) { }

    public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<net.minecraft.world.item.ItemStack, C, T> provider, net.minecraft.world.level.ItemLike... items) { }
}
