package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

/** A text colour (reamc-compat). */
public final class TextColor {
    private final int value;
    private final String name;

    private TextColor(int value, String name) { this.value = value & 0xFFFFFF; this.name = name; }

    public static TextColor fromRgb(int rgb) { return new TextColor(rgb, null); }

    public static TextColor fromLegacyFormat(ChatFormatting f) { return f.isColor() ? new TextColor(f.getColor(), f.getName()) : null; }

    public int getValue() { return value; }

    public String serialize() { return name != null ? name : String.format("#%06X", value); }

    @Override public boolean equals(Object o) { return o instanceof TextColor t && t.value == value; }
    @Override public int hashCode() { return value; }
    @Override public String toString() { return serialize(); }
}
