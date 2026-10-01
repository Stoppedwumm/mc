package net.minecraft.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opens a container menu with a title (reamc-compat). */
public interface MenuProvider {
    Component getDisplayName();

    AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player);
}
