package mc.client;

import mc.item.ItemStack;
import mc.mod.Bridge;
import mc.mod.ModLoader;
import mc.mod.ModUi;
import mc.render.Gui;
import mc.render.Texture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/** Shows mods' screens (their container GUIs) in reamc's GUI and passes clicks and keys to their menus. */
final class ModScreens implements ModUi {
    private final Game g;
    private Gui gui;
    Screen screen;
    private final Map<ResourceLocation, Texture> textures = new HashMap<>();
    private boolean closing;

    ModScreens(Game g) { this.g = g; }

    @Override public mc.entity.Player localPlayer() { return g.player; }
    @Override public float width() { return gui == null ? 427 : gui.width; }
    @Override public float height() { return gui == null ? 240 : gui.height; }

    @Override
    public void openScreen(Screen s) {
        screen = s;
        g.setScreen(Game.Screen.MOD);
    }

    @Override
    public void closeScreen() {
        screen = null;
        if (g.screen() == Game.Screen.MOD && !closing) {
            closing = true;
            try {
                g.closeScreen();
            } finally {
                closing = false;
            }
        }
    }

    /** The game left the mod screen some other way (death, pause...): close the menu too. */
    void leaving() {
        if (screen != null && !closing) {
            closing = true;
            try {
                Bridge.closeMenu(Bridge.localPlayer(g.player));
            } finally {
                closing = false;
                screen = null;
            }
        }
    }

    private Texture texture(ResourceLocation rl) {
        return textures.computeIfAbsent(rl, k -> {
            byte[] b = ModLoader.resource("assets/" + k.getNamespace() + "/" + k.getPath());
            try {
                BufferedImage img = b == null ? null : ImageIO.read(new java.io.ByteArrayInputStream(b));
                if (img == null) return new Texture(new int[]{0xFFF800F8, 0xFF000000, 0xFF000000, 0xFFF800F8}, 2, 2, false, false);
                int[] px = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
                return new Texture(px, img.getWidth(), img.getHeight(), false, false);
            } catch (Exception e) {
                return new Texture(new int[]{0xFFF800F8}, 1, 1, false, false);
            }
        });
    }

    private static int opaque(int color) { return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color; }

    @Override
    public void texture(ResourceLocation rl, float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        float[] c = com.mojang.blaze3d.systems.RenderSystem.getShaderColor();
        int argb = (int) (c[3] * 255) << 24 | (int) (c[0] * 255) << 16 | (int) (c[1] * 255) << 8 | (int) (c[2] * 255);
        gui.image(texture(rl), x, y, w, h, u0, v0, u1, v1, argb);
    }

    @Override public void fill(float x, float y, float w, float h, int argb) { gui.fill(x, y, w, h, argb); }
    @Override public void text(String s, float x, float y, int argb, boolean shadow) { gui.textScaled(s, x, y, 1, opaque(argb), shadow); }
    @Override public int textWidth(String s) { return gui == null ? s.length() * 6 : gui.textWidth(s); }
    @Override public void item(ItemStack stack, float x, float y) { gui.stack(stack, x, y); }
    @Override public void tooltip(List<String> lines, float x, float y) { gui.tooltip(lines, x, y); }
    @Override public void stackTooltip(ItemStack stack, float x, float y) { gui.tooltip(Screens.tooltipLines(stack), x, y); }
    @Override public void chat(String message) { g.hud.chat(message); }

    /** Draws the open mod screen and feeds it input (once per frame). */
    void render(Gui gui, Input input, float pt) {
        this.gui = gui;
        if (screen == null) {
            g.closeScreen();
            return;
        }
        gui.fill(0, 0, gui.width, gui.height, 0x90101010);
        int mx = (int) (input.mouseX / gui.scale), my = (int) (input.mouseY / gui.scale);
        if (screen.width != (int) gui.width || screen.height != (int) gui.height) screen.init(net.minecraft.client.Minecraft.getInstance(), (int) gui.width, (int) gui.height);
        Screen s = screen;
        try {
            s.render(new GuiGraphics(this), mx, my, pt);
        } catch (RuntimeException | LinkageError e) {
            System.err.println("[mods] Screen failed to draw: " + e);
            e.printStackTrace();
            Bridge.closeMenu(Bridge.localPlayer(g.player));
            return;
        }
        if (!(s instanceof AbstractContainerScreen<?> cs)) {
            if (input.pressed(GLFW_KEY_ESCAPE)) s.keyPressed(256, 0, 0);
            return;
        }
        AbstractContainerMenu menu = cs.getMenu();
        var player = Bridge.serverPlayer(g.player);
        Slot hover = cs.getSlotUnderMouse();
        boolean shift = input.down(GLFW_KEY_LEFT_SHIFT) || input.down(GLFW_KEY_RIGHT_SHIFT);
        try {
            for (int button = 0; button < 2; button++) {
                if (!input.clicked(button == 0 ? GLFW_MOUSE_BUTTON_LEFT : GLFW_MOUSE_BUTTON_RIGHT)) continue;
                if (hover != null) menu.clicked(hover.index, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, player);
                else {
                    boolean outside = mx < cs.getGuiLeft() || my < cs.getGuiTop() || mx >= cs.getGuiLeft() + cs.getXSize() || my >= cs.getGuiTop() + cs.getYSize();
                    if (outside) menu.clicked(-999, button, ClickType.PICKUP, player);
                }
            }
            if (hover != null) {
                for (int k = 0; k < 9; k++) if (input.pressed(GLFW_KEY_1 + k)) menu.clicked(hover.index, k, ClickType.SWAP, player);
                if (input.pressed(GLFW_KEY_Q)) menu.clicked(hover.index, input.down(GLFW_KEY_LEFT_CONTROL) ? 1 : 0, ClickType.THROW, player);
            }
            g.player.inventory.cleanup();
            if (input.pressed(GLFW_KEY_ESCAPE)) s.keyPressed(256, 0, 0);
            else if (input.pressed(GLFW_KEY_E)) s.keyPressed(69, 0, 0);
            if (screen != null && !menu.stillValid(player)) Bridge.closeMenu(player);
        } catch (RuntimeException | LinkageError e) {
            System.err.println("[mods] Menu failed: " + e);
            e.printStackTrace();
            Bridge.closeMenu(player);
        }
    }
}
