package net.minecraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 2D drawing for mod screens, drawn by reamc's GUI renderer (reamc-compat). */
public class GuiGraphics {
    private final mc.mod.ModUi ui;
    private final PoseStack pose = new PoseStack();

    public GuiGraphics(mc.mod.ModUi ui) { this.ui = ui; }

    public PoseStack pose() { return pose; }
    public int guiWidth() { return (int) ui.width(); }
    public int guiHeight() { return (int) ui.height(); }

    private float x(float v) { return pose.reamc$x() + v * pose.reamc$scale(); }
    private float y(float v) { return pose.reamc$y() + v * pose.reamc$scale(); }

    /** Draws part of a texture: (u, v) and size in texture pixels, the whole texture being texW x texH. */
    public void blit(ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        ui.texture(texture, x(x), y(y), w * pose.reamc$scale(), h * pose.reamc$scale(), u / texW, v / texH, (u + w) / texW, (v + h) / texH);
    }

    public void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h) { blit(texture, x, y, u, v, w, h, 256, 256); }

    public void blit(ResourceLocation texture, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH) {
        ui.texture(texture, x(x), y(y), w * pose.reamc$scale(), h * pose.reamc$scale(), u / texW, v / texH, (u + uw) / texW, (v + vh) / texH);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        ui.fill(x(Math.min(x0, x1)), y(Math.min(y0, y1)), Math.abs(x1 - x0) * pose.reamc$scale(), Math.abs(y1 - y0) * pose.reamc$scale(), color);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        ui.text(text, x(x), y(y), color, shadow);
        return x + font.width(text);
    }

    public int drawString(Font font, String text, int x, int y, int color) { return drawString(font, text, x, y, color, true); }
    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) { return drawString(font, text.getString(), x, y, color, shadow); }
    public int drawString(Font font, Component text, int x, int y, int color) { return drawString(font, text.getString(), x, y, color, true); }

    public void drawCenteredString(Font font, String text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color, true); }
    public void drawCenteredString(Font font, Component text, int x, int y, int color) { drawCenteredString(font, text.getString(), x, y, color); }

    public void renderItem(ItemStack stack, int x, int y) { if (!stack.isEmpty()) ui.item(stack.reamc$handle(), x(x), y(y)); }
    public void renderFakeItem(ItemStack stack, int x, int y) { renderItem(stack, x, y); }
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) { }

    public void renderTooltip(Font font, Component text, int x, int y) { ui.tooltip(java.util.List.of(text.getString()), x(x), y(y)); }
    public void renderTooltip(Font font, ItemStack stack, int x, int y) { if (!stack.isEmpty()) ui.tooltip(java.util.List.of(stack.getHoverName().getString()), x(x), y(y)); }
}
