package net.minecraft.world.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** A kind of menu and how to build it on the client (reamc-compat). */
public class MenuType<T extends AbstractContainerMenu> {
    private final net.neoforged.neoforge.network.IContainerFactory<T> factory;

    public MenuType(net.neoforged.neoforge.network.IContainerFactory<T> factory) { this.factory = factory; }

    public T create(int id, Inventory inventory) { return factory.create(id, inventory, null); }

    public T create(int id, Inventory inventory, RegistryFriendlyByteBuf data) { return factory.create(id, inventory, data); }

    @FunctionalInterface
    public interface MenuSupplier<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory);
    }
}
