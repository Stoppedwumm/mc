package net.neoforged.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Builds a menu from the extra data its opener sent (reamc-compat). */
public interface IContainerFactory<T extends AbstractContainerMenu> extends MenuType.MenuSupplier<T> {
    T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data);

    @Override
    default T create(int windowId, Inventory inv) { return create(windowId, inv, null); }
}
