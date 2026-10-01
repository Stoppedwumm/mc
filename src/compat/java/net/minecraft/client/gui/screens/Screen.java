package net.minecraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A GUI screen (reamc-compat: shown and fed input by reamc's GUI). */
public class Screen {
    protected final Component title;
    public int width, height;
    protected Minecraft minecraft;
    protected Font font;

    protected Screen(Component title) { this.title = title; }

    public Component getTitle() { return title; }

    public final void init(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
        this.width = width;
        this.height = height;
        init();
    }

    protected void init() { }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double x, double y, int button) { return false; }
    public boolean shouldCloseOnEsc() { return true; }
    public boolean isPauseScreen() { return false; }
    public void onClose() { minecraft.setScreen(null); }
    public void removed() { }
    public void tick() { }
}
