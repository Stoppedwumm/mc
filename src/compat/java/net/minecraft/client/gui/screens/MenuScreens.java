package net.minecraft.client.gui.screens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MenuScreens {
    private MenuScreens() { }

    @FunctionalInterface
    public interface ScreenConstructor<M extends AbstractContainerMenu, U extends Screen> {
        U create(M menu, Inventory inventory, Component title);
    }
}
