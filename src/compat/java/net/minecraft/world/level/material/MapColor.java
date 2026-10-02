package net.minecraft.world.level.material;

/** Colours of blocks on maps (reamc-compat: kept for mods' block properties). */
public class MapColor {
    public static final MapColor NONE = new MapColor(0, 0x000000);
    public static final MapColor GRASS = new MapColor(1, 0x7FB238);
    public static final MapColor SAND = new MapColor(2, 0xF7E9A3);
    public static final MapColor WOOL = new MapColor(3, 0xC7C7C7);
    public static final MapColor FIRE = new MapColor(4, 0xFF0000);
    public static final MapColor ICE = new MapColor(5, 0xA0A0FF);
    public static final MapColor METAL = new MapColor(6, 0xA7A7A7);
    public static final MapColor PLANT = new MapColor(7, 0x007C00);
    public static final MapColor SNOW = new MapColor(8, 0xFFFFFF);
    public static final MapColor CLAY = new MapColor(9, 0xA4A8B8);
    public static final MapColor DIRT = new MapColor(10, 0x976D4D);
    public static final MapColor STONE = new MapColor(11, 0x707070);
    public static final MapColor WATER = new MapColor(12, 0x4040FF);
    public static final MapColor WOOD = new MapColor(13, 0x8F7748);
    public static final MapColor QUARTZ = new MapColor(14, 0xFFFCF5);
    public static final MapColor COLOR_ORANGE = new MapColor(15, 0xD87F33);
    public static final MapColor COLOR_MAGENTA = new MapColor(16, 0xB24CD8);
    public static final MapColor COLOR_LIGHT_BLUE = new MapColor(17, 0x6699D8);
    public static final MapColor COLOR_YELLOW = new MapColor(18, 0xE5E533);
    public static final MapColor COLOR_LIGHT_GREEN = new MapColor(19, 0x7FCC19);
    public static final MapColor COLOR_PINK = new MapColor(20, 0xF27FA5);
    public static final MapColor COLOR_GRAY = new MapColor(21, 0x4C4C4C);
    public static final MapColor COLOR_LIGHT_GRAY = new MapColor(22, 0x999999);
    public static final MapColor COLOR_CYAN = new MapColor(23, 0x4C7F99);
    public static final MapColor COLOR_PURPLE = new MapColor(24, 0x7F3FB2);
    public static final MapColor COLOR_BLUE = new MapColor(25, 0x334CB2);
    public static final MapColor COLOR_BROWN = new MapColor(26, 0x664C33);
    public static final MapColor COLOR_GREEN = new MapColor(27, 0x667F33);
    public static final MapColor COLOR_RED = new MapColor(28, 0x993333);
    public static final MapColor COLOR_BLACK = new MapColor(29, 0x191919);
    public static final MapColor GOLD = new MapColor(30, 0xFAEE4D);
    public static final MapColor DIAMOND = new MapColor(31, 0x5CDBD5);
    public static final MapColor LAPIS = new MapColor(32, 0x4A80FF);
    public static final MapColor EMERALD = new MapColor(33, 0x00D93A);
    public static final MapColor PODZOL = new MapColor(34, 0x815631);
    public static final MapColor NETHER = new MapColor(35, 0x700200);
    public static final MapColor TERRACOTTA_WHITE = new MapColor(36, 0xD1B1A1);
    public static final MapColor TERRACOTTA_ORANGE = new MapColor(37, 0x9F5224);
    public static final MapColor TERRACOTTA_MAGENTA = new MapColor(38, 0x95576C);
    public static final MapColor TERRACOTTA_LIGHT_BLUE = new MapColor(39, 0x706C8A);
    public static final MapColor TERRACOTTA_YELLOW = new MapColor(40, 0xBA8524);
    public static final MapColor TERRACOTTA_LIGHT_GREEN = new MapColor(41, 0x677535);
    public static final MapColor TERRACOTTA_PINK = new MapColor(42, 0xA04D4E);
    public static final MapColor TERRACOTTA_GRAY = new MapColor(43, 0x392923);
    public static final MapColor TERRACOTTA_LIGHT_GRAY = new MapColor(44, 0x876B62);
    public static final MapColor TERRACOTTA_CYAN = new MapColor(45, 0x575C5C);
    public static final MapColor TERRACOTTA_PURPLE = new MapColor(46, 0x7A4958);
    public static final MapColor TERRACOTTA_BLUE = new MapColor(47, 0x4C3E5C);
    public static final MapColor TERRACOTTA_BROWN = new MapColor(48, 0x4C3223);
    public static final MapColor TERRACOTTA_GREEN = new MapColor(49, 0x4C522A);
    public static final MapColor TERRACOTTA_RED = new MapColor(50, 0x8E3C2E);
    public static final MapColor TERRACOTTA_BLACK = new MapColor(51, 0x251610);
    public static final MapColor CRIMSON_NYLIUM = new MapColor(52, 0xBD3031);
    public static final MapColor CRIMSON_STEM = new MapColor(53, 0x943F61);
    public static final MapColor CRIMSON_HYPHAE = new MapColor(54, 0x5C191D);
    public static final MapColor WARPED_NYLIUM = new MapColor(55, 0x167E86);
    public static final MapColor WARPED_STEM = new MapColor(56, 0x3A8E8C);
    public static final MapColor WARPED_HYPHAE = new MapColor(57, 0x562C3E);
    public static final MapColor WARPED_WART_BLOCK = new MapColor(58, 0x14B485);
    public static final MapColor DEEPSLATE = new MapColor(59, 0x646464);
    public static final MapColor RAW_IRON = new MapColor(60, 0xD8AF93);
    public static final MapColor GLOW_LICHEN = new MapColor(61, 0x7FA796);

    public final int id, col;

    private MapColor(int id, int col) { this.id = id; this.col = col; }

    public int calculateRGBColor(Brightness b) { return col; }

    public enum Brightness { LOW, NORMAL, HIGH, LOWEST }
}
