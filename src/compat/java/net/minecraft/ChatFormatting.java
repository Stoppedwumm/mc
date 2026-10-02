package net.minecraft;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Text colours and styles, with Minecraft's § codes (reamc-compat). */
public enum ChatFormatting implements StringRepresentable {
    BLACK("BLACK", '0', 0, 0x000000), DARK_BLUE("DARK_BLUE", '1', 1, 0x0000AA), DARK_GREEN("DARK_GREEN", '2', 2, 0x00AA00),
    DARK_AQUA("DARK_AQUA", '3', 3, 0x00AAAA), DARK_RED("DARK_RED", '4', 4, 0xAA0000), DARK_PURPLE("DARK_PURPLE", '5', 5, 0xAA00AA),
    GOLD("GOLD", '6', 6, 0xFFAA00), GRAY("GRAY", '7', 7, 0xAAAAAA), DARK_GRAY("DARK_GRAY", '8', 8, 0x555555),
    BLUE("BLUE", '9', 9, 0x5555FF), GREEN("GREEN", 'a', 10, 0x55FF55), AQUA("AQUA", 'b', 11, 0x55FFFF), RED("RED", 'c', 12, 0xFF5555),
    LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13, 0xFF55FF), YELLOW("YELLOW", 'e', 14, 0xFFFF55), WHITE("WHITE", 'f', 15, 0xFFFFFF),
    OBFUSCATED("OBFUSCATED", 'k', true), BOLD("BOLD", 'l', true), STRIKETHROUGH("STRIKETHROUGH", 'm', true),
    UNDERLINE("UNDERLINE", 'n', true), ITALIC("ITALIC", 'o', true), RESET("RESET", 'r', -1, null);

    public static final Codec<ChatFormatting> CODEC = StringRepresentable.fromEnum(ChatFormatting::values);
    public static final char PREFIX_CODE = '§';

    private final String name;
    private final char code;
    private final boolean isFormat;
    private final String toString;
    private final int id;
    private final Integer color;

    ChatFormatting(String name, char code, int id, Integer color) { this(name, code, false, id, color); }

    ChatFormatting(String name, char code, boolean isFormat) { this(name, code, isFormat, -1, null); }

    ChatFormatting(String name, char code, boolean isFormat, int id, Integer color) {
        this.name = name;
        this.code = code;
        this.isFormat = isFormat;
        this.id = id;
        this.color = color;
        this.toString = "§" + code;
    }

    public char getChar() { return code; }
    public int getId() { return id; }
    public boolean isFormat() { return isFormat; }
    public boolean isColor() { return !isFormat && this != RESET; }
    public Integer getColor() { return color; }
    public String getName() { return name().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return toString; }
    @Override public String getSerializedName() { return getName(); }

    public static String stripFormatting(String s) { return s == null ? null : s.replaceAll("(?i)§[0-9a-fk-or]", ""); }

    public static ChatFormatting getByName(String n) {
        if (n == null) return null;
        for (ChatFormatting f : values()) if (f.getName().equals(n.toLowerCase(Locale.ROOT).replace(" ", "_"))) return f;
        return null;
    }

    public static ChatFormatting getById(int id) {
        if (id < 0) return RESET;
        for (ChatFormatting f : values()) if (f.id == id) return f;
        return null;
    }

    public static ChatFormatting getByCode(char c) {
        char l = Character.toLowerCase(c);
        for (ChatFormatting f : values()) if (f.code == l) return f;
        return null;
    }

    public static Collection<String> getNames(boolean colors, boolean formats) {
        List<String> l = new ArrayList<>();
        for (ChatFormatting f : values()) if ((!f.isColor() || colors) && (!f.isFormat() || formats)) l.add(f.getName());
        return l;
    }
}
