package net.minecraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

/** The client (reamc-compat: the open mod screen and the local player). */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    public LocalPlayer player;
    public Screen screen;
    public final Font font = new Font();

    public static Minecraft getInstance() { return INSTANCE; }

    public void setScreen(Screen s) {
        if (s == null) mc.mod.Bridge.closeScreen();
        screen = s;
    }

    public boolean isSameThread() { return true; }
    public void execute(Runnable r) { r.run(); }
}
