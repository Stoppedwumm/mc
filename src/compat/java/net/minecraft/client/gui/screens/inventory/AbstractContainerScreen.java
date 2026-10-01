package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * A container screen: the mod draws the background and labels, reamc draws the slots, items and cursor and turns
 * clicks into menu clicks (reamc-compat).
 */
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    protected int imageWidth = 176, imageHeight = 166;
    protected int titleLabelX = 8, titleLabelY = 6, inventoryLabelX = 8, inventoryLabelY;
    protected int leftPos, topPos;
    protected final T menu;
    protected final Component playerInventoryTitle;
    protected Slot hoveredSlot;

    protected AbstractContainerScreen(T menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.playerInventoryTitle = Component.translatable("container.inventory");
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override public T getMenu() { return menu; }

    @Override
    protected void init() {
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
    }

    public int getGuiLeft() { return leftPos; }
    public int getGuiTop() { return topPos; }
    public int getXSize() { return imageWidth; }
    public int getYSize() { return imageHeight; }
    public Slot getSlotUnderMouse() { return hoveredSlot; }

    protected abstract void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderBg(graphics, partialTick, mouseX, mouseY);
        hoveredSlot = null;
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            int x = leftPos + s.x, y = topPos + s.y;
            graphics.renderItem(s.getItem(), x, y);
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                hoveredSlot = s;
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        renderLabels(graphics, mouseX, mouseY);
        graphics.pose().popPose();
        graphics.renderItem(menu.getCarried(), mouseX - 8, mouseY - 8);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem())
            mc.mod.Bridge.ui().stackTooltip(hoveredSlot.getItem().reamc$handle(), mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || key == 69) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
    }
}
