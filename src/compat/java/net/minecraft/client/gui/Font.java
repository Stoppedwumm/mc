package net.minecraft.client.gui;

import net.minecraft.network.chat.Component;

/** The GUI font (reamc-compat: reamc's bitmap font). */
public class Font {
    public final int lineHeight = 9;

    public int width(String text) { return mc.mod.Bridge.ui() == null ? text.length() * 6 : mc.mod.Bridge.ui().textWidth(text); }

    public int width(Component text) { return width(text.getString()); }
}
