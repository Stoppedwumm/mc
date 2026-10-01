package mc.mod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Shown for a mod menu that registered no screen of its own: a plain panel with slot frames. */
final class DefaultContainerScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    DefaultContainerScreen(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        int bottom = 0;
        for (Slot s : menu.slots) bottom = Math.max(bottom, s.y + 24);
        imageHeight = Math.max(166, bottom);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (Slot s : menu.slots) {
            g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF373737);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 17, topPos + s.y + 17, 0xFFFFFFFF);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xFF8B8B8B);
        }
    }
}
