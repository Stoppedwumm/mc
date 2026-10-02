package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

/** Colour and decoration of text (reamc-compat). */
public final class Style {
    public static final Style EMPTY = new Style(null, null, null, null, null, null);
    private final TextColor color;
    private final Boolean bold, italic, underlined, strikethrough, obfuscated;

    private Style(TextColor color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated) {
        this.color = color; this.bold = bold; this.italic = italic; this.underlined = underlined; this.strikethrough = strikethrough; this.obfuscated = obfuscated;
    }

    public TextColor getColor() { return color; }
    public boolean isBold() { return bold == Boolean.TRUE; }
    public boolean isItalic() { return italic == Boolean.TRUE; }
    public boolean isStrikethrough() { return strikethrough == Boolean.TRUE; }
    public boolean isUnderlined() { return underlined == Boolean.TRUE; }
    public boolean isObfuscated() { return obfuscated == Boolean.TRUE; }
    public boolean isEmpty() { return this.equals(EMPTY); }
    public Object getClickEvent() { return null; }
    public Object getHoverEvent() { return null; }
    public String getInsertion() { return null; }
    public ResourceLocation getFont() { return ResourceLocation.withDefaultNamespace("default"); }

    public Style withColor(TextColor c) { return new Style(c, bold, italic, underlined, strikethrough, obfuscated); }
    public Style withColor(ChatFormatting f) { return withColor(f == null ? null : TextColor.fromLegacyFormat(f)); }
    public Style withColor(int rgb) { return withColor(TextColor.fromRgb(rgb)); }
    public Style withBold(Boolean b) { return new Style(color, b, italic, underlined, strikethrough, obfuscated); }
    public Style withItalic(Boolean b) { return new Style(color, bold, b, underlined, strikethrough, obfuscated); }
    public Style withUnderlined(Boolean b) { return new Style(color, bold, italic, b, strikethrough, obfuscated); }
    public Style withStrikethrough(Boolean b) { return new Style(color, bold, italic, underlined, b, obfuscated); }
    public Style withObfuscated(Boolean b) { return new Style(color, bold, italic, underlined, strikethrough, b); }
    public Style withInsertion(String s) { return this; }
    public Style withFont(ResourceLocation font) { return this; }

    public Style applyFormat(ChatFormatting f) {
        return switch (f) {
            case BOLD -> withBold(true);
            case ITALIC -> withItalic(true);
            case UNDERLINE -> withUnderlined(true);
            case STRIKETHROUGH -> withStrikethrough(true);
            case OBFUSCATED -> withObfuscated(true);
            case RESET -> EMPTY;
            default -> withColor(f);
        };
    }

    public Style applyLegacyFormat(ChatFormatting f) { return f.isColor() ? EMPTY.withColor(f) : applyFormat(f); }

    public Style applyFormats(ChatFormatting... fs) { Style s = this; for (ChatFormatting f : fs) s = s.applyFormat(f); return s; }

    public Style applyTo(Style parent) {
        if (this == EMPTY) return parent;
        return new Style(color != null ? color : parent.color, bold != null ? bold : parent.bold, italic != null ? italic : parent.italic,
                underlined != null ? underlined : parent.underlined, strikethrough != null ? strikethrough : parent.strikethrough,
                obfuscated != null ? obfuscated : parent.obfuscated);
    }

    /** Legacy § codes for reamc's text renderer (colour only). */
    public String reamc$codes() {
        if (color == null) return "";
        for (ChatFormatting f : ChatFormatting.values()) if (f.isColor() && f.getColor() == color.getValue()) return f.toString();
        return "";
    }

    @Override public boolean equals(Object o) {
        return o instanceof Style s && java.util.Objects.equals(s.color, color) && java.util.Objects.equals(s.bold, bold) && java.util.Objects.equals(s.italic, italic)
                && java.util.Objects.equals(s.underlined, underlined) && java.util.Objects.equals(s.strikethrough, strikethrough) && java.util.Objects.equals(s.obfuscated, obfuscated);
    }

    @Override public int hashCode() { return java.util.Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated); }
}
