package mc.render;

import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Paints item sprites (tools, materials, food) and HUD icons into a 256x256 atlas of 16x16 tiles.
 * Shapes are defined by pixel predicates, then shaded with an outline and highlight like vanilla items.
 */
public final class ItemTextureGen {
    public static final int HEART = 96, HEART_HALF = 97, HEART_EMPTY = 98, FOOD = 99, FOOD_HALF = 100, FOOD_EMPTY = 101,
            BUBBLE = 102, FLAME = 103, PROGRESS = 104, PROGRESS_FULL = 105, BUBBLE_POP = 106, HEART_HURT = 107,
            ARMOR = 108, ARMOR_HALF = 109, ARMOR_EMPTY = 110, XP_ORB = 111, HEART_GOLD = 134, HEART_GOLD_HALF = 135,
            BOBBER = 144, WHITE = 145;
    /** Base colour per armor material (leather, chainmail, iron, gold, diamond). */
    public static final int[] ARMOR_COLORS = {0xa0653a, 0x8a8a8a, 0xd8d8d8, 0xf8d840, 0x4ae0d8};
    private final int[] px = new int[256 * 256];
    private int tile;
    private Random r;

    private static final int WOOD = 0x8b6a3a, WOOD_D = 0x5a4222;
    private static final int[][] MATERIAL = {
            {0xa8864f, 0x6b5130, 0xc9a66a}, // wood
            {0x8e8e8e, 0x545454, 0xb0b0b0}, // stone
            {0xdedede, 0x8e8e8e, 0xffffff}, // iron
            {0xf8e050, 0xb88a10, 0xfff8a8}, // gold
            {0x5ef0e0, 0x1c9c94, 0xc8fff8}, // diamond
    };

    private static final java.util.Map<Integer, int[]> EXTRA = new java.util.LinkedHashMap<>();
    private static int nextExtra = 146;

    /** Reserves an item-atlas tile for a mod texture (16 x 16 ARGB); returns its index. */
    public static synchronized int addTile(int[] argb) {
        if (nextExtra >= 256) throw new IllegalStateException("The item texture atlas is full");
        EXTRA.put(nextExtra, argb);
        return nextExtra++;
    }

    public int[] generate() {
        tile(0, () -> handle(2, 14, 12, 4));
        tile(1, () -> lump(0x2a2a2a, 0x121212, 0x5a5a5a));
        tile(2, () -> lump(0x3a2c20, 0x1a120a, 0x6a5440));
        tile(3, () -> ingot(0xdedede, 0x8e8e8e, 0xffffff));
        tile(4, () -> ingot(0xf8e050, 0xb88a10, 0xfff8a8));
        tile(5, this::diamond);
        tile(6, () -> shape((x, y) -> Math.abs(x - 8) + Math.abs(y - 8) * 0.7 < 6 && x + y > 8, 0x3a3a3a, 0x1a1a1a, 0x6a6a6a));
        tile(7, this::string);
        tile(8, this::feather);
        tile(9, () -> pile(0x5a5a5a, 0x3a3a3a, 0x8a8a8a));
        tile(10, this::bone);
        tile(11, () -> pile(0xf0f0e8, 0xb8b8b0, 0xffffff));
        tile(12, this::arrow);
        tile(13, this::bow);
        tile(14, () -> shape((x, y) -> dist(x, y, 8, 8) < 6.5 - ((x * 7 + y * 3) % 4 == 0 ? 1 : 0), 0x8a4f2a, 0x55301a, 0xa86a40));
        tile(15, this::wheat);
        tile(16, this::seeds);
        tile(17, () -> shape((x, y) -> sq((x - 8) / 7.0) + sq((y - 9) / 4.5) < 1, 0xb8823a, 0x6a4418, 0xe0b068));
        tile(18, this::apple);
        tile(19, () -> meat(0xf0a0a0, 0xc06a6a, 0xffd0d0));
        tile(20, () -> meat(0xc88a58, 0x7a4a24, 0xe8b080));
        tile(21, () -> meat(0xc83a32, 0x7a1a14, 0xf0e0d8));
        tile(22, () -> meat(0x8a4a24, 0x4a2410, 0xb07040));
        tile(23, () -> drumstick(0xf4c8b0, 0xc09080));
        tile(24, () -> drumstick(0xd8a060, 0x8a5a24));
        tile(25, () -> shape((x, y) -> dist(x, y, 8, 8) < 6 && (x * 3 + y * 5) % 7 != 0, 0x7a6a3a, 0x3a4a1a, 0x9a8a4a));
        tile(26, () -> bucket(0));
        tile(27, () -> bucket(0x3060e0));
        tile(28, () -> bucket(0xf07010));
        tile(29, this::flintAndSteel);
        tile(30, () -> shape((x, y) -> dist(x, y, 8, 8.5) < 5.5, 0xa0a8b8, 0x707888, 0xc8d0e0));
        tile(31, () -> shape((x, y) -> x >= 2 && x <= 13 && y >= 5 && y <= 11 && x + y > 8, 0xa8503a, 0x6a2a1a, 0xd07a5a));
        tile(32, () -> shape((x, y) -> sq((x - 8) / 5.0) + sq((y - 8.5) / 6.5) < 1, 0xe8dcc0, 0xb0a080, 0xfff8e8));
        tile(33, () -> pile(0xf8f8f8, 0xc8c8c8, 0xffffff));
        tile(34, () -> shape((x, y) -> x >= 3 && x <= 12 && y >= 2 && y <= 13, 0xf0f0e8, 0xb0b0a8, 0xffffff));
        tile(35, this::book);
        tile(36, () -> shape((x, y) -> dist(x, y, 8, 8) < 5.5, 0xf4f8fc, 0xb8c8d8, 0xffffff));
        tile(37, this::shears);
        tile(38, () -> bucket(0xf4f4f0));
        tile(39, () -> {
            shape((x, y) -> dist(x, y, 7.5, 7.5) < 5.5, 0x1a6a5a, 0x0a3a30, 0x3aa890);
            for (int y = 5; y <= 10; y++) for (int x = 5; x <= 10; x++) if (dist(x, y, 7.5, 7.5) < 2.2) set(x, y, 0x0a2a24);
        });
        tile(90, () -> shape((x, y) -> Math.abs(x - 7.5) / 4.5 + Math.abs(y - 7.5) / 6.5 < 1, 0x3ad86a, 0x0a7a2a, 0xa8ffc0));
        tile(94, () -> shape((x, y) -> Math.abs(x - 7.5) / 3.5 + Math.abs(y - 7.5) / 6.5 < 1 && x + y > 5, 0xece6de, 0xa8a098, 0xffffff));
        tile(95, () -> shape((x, y) -> x >= 2 && x <= 13 && y >= 5 && y <= 11 && x + y > 8, 0x4a2228, 0x2a0c10, 0x6a3a40));
        tile(112, () -> pile(0xf8d860, 0xb09020, 0xfff0a0));
        tile(113, () -> shape((x, y) -> dist(x, y, 7.5, 8.5) < 3.5, 0xf8d840, 0xa87a10, 0xfff8a8));
        tile(114, () -> shape((x, y) -> dist(x, y, 7.5, 9.5) < 4 || (y < 7 && y > 1 && Math.abs(x - 7.5) < (y - 1) * 0.7), 0xc8e8f0, 0x6a9aa8, 0xffffff));
        tile(115, () -> shape((x, y) -> dist(x, y, 7.5, 8) < 5.5, 0xf08a20, 0x8a3a0a, 0xffd060));
        tile(116, () -> { shape((x, y) -> dist(x, y, 7.5, 7.5) < 5.5, 0x3a2a20, 0x101010, 0x6a4a3a); for (int i = 0; i < 10; i++) set(4 + r.nextInt(8), 4 + r.nextInt(8), 0xf08a20); });
        tile(117, () -> shape((x, y) -> Math.abs(x - 7.5) + Math.abs(y - 7.5) < 6.5 && (x + y) % 5 != 0, 0x2a50c8, 0x10287a, 0x7a9af0));
        tile(118, this::fishingRod);
        tile(119, () -> bottle(false, false));
        tile(120, () -> shape((x, y) -> dist(x, y, 8, 9) < 4.5 || dist(x, y, 5, 5) < 2.2 || dist(x, y, 11, 5) < 2.2, 0x8a1a1a, 0x4a0808, 0xc84a3a));
        tile(121, () -> { for (int i = 0; i < 12; i++) { set(3 + i, 13 - i, i % 3 == 0 ? 0xfff0a0 : 0xf8b020); set(4 + i, 13 - i, 0x9a5a08); } });
        tile(122, () -> pile(0xf8a020, 0xa05a08, 0xfff080));
        tile(123, () -> { shape((x, y) -> dist(x, y, 8, 8) < 5.5, 0x9a1a2a, 0x4a0810, 0xe05060); set(6, 6, 0x200808); set(9, 7, 0x200808); set(7, 10, 0x200808); });
        tile(124, () -> { shape((x, y) -> dist(x, y, 8, 8) < 5.5, 0x6a3a2a, 0x3a1810, 0x9a6a50); for (int i = 0; i < 6; i++) set(4 + r.nextInt(8), 4 + r.nextInt(8), 0xc02030); });
        tile(125, () -> bowl(-1));
        tile(126, () -> bowl(0xc89a6a));
        tile(127, () -> { melonSlice(); for (int i = 0; i < 7; i++) set(3 + r.nextInt(10), 5 + r.nextInt(7), 0xfff080); });
        tile(128, this::melonSlice);
        tile(129, () -> { carrot(); recolor(0xf8d840); });
        tile(130, () -> { apple(); recolor(0xf8d840); });
        tile(131, () -> bottle(true, false));
        tile(132, () -> bottle(true, true));
        tile(BOBBER, () -> {
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    if (sq((x - 7.5) / 3.5) + sq((y - 8) / 4.5) < 1) set(x, y, y < 8 ? (x < 6 ? 0xff6a5a : 0xd8261a) : (x < 6 ? 0xffffff : 0xd8d8d8));
            for (int y = 1; y < 4; y++) set(7, y, 0x3a3a3a);
        });
        tile(WHITE, () -> { for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) set(x, y, 0xffffff); });
        tile(136, this::minecart);
        tile(137, this::boat);
        tile(138, () -> fish(0x8a9aa8, 0x4a5a68, 0xc0ccd8, 0));
        tile(139, () -> fish(0xa07850, 0x5a3a20, 0xc8a070, 0));
        tile(140, () -> fish(0xc85a4a, 0x6a2a20, 0xf09080, 1));
        tile(141, () -> fish(0xa0583a, 0x5a2a18, 0xc88060, 1));
        tile(142, () -> fish(0xf07a1a, 0x7a3a08, 0xffb070, 2));
        tile(143, this::pufferfish);
        tile(133, () -> { for (int y = 7; y <= 13; y++) for (int x = 4; x <= 11; x++) if (dist(x, y, 7.5, 10) < 3.9) set(x, y, (x < 7 && y < 10) ? 0xffffff : 0xd8d8d8 - (y - 7) * 0x080808); });
        tile(HEART_GOLD, () -> heart(0xf0c020, true, 1));
        tile(HEART_GOLD_HALF, () -> heart(0xf0c020, true, 0.5));
        tile(91, () -> pile(0xd01010, 0x6a0000, 0xff5a5a));
        tile(92, () -> { book(); for (int y = 2; y < 14; y += 3) set(5 + (y % 5), y, 0xe070ff); });
        tile(93, this::xpBottle);
        tile(85, () -> shape((x, y) -> dist(x, y, 8, 8.5) < 5 && !(x > 9 && y < 6), 0x6ac05a, 0x2a7a2a, 0xb0f0a0));
        tile(86, () -> shape((x, y) -> sq((x - 8) / 5.0) + sq((y - 9) / 5.5) < 1 || (y < 5 && Math.abs(x - 8) < 2 && y > 1), 0x2a2a3a, 0x0a0a14, 0x5a5a6a));
        tile(87, this::carrot);
        tile(88, () -> shape((x, y) -> sq((x - 8) / 5.5) + sq((y - 8.5) / 4.5) < 1, 0xc8a060, 0x7a5a2a, 0xe8c888));
        tile(89, () -> shape((x, y) -> sq((x - 8) / 5.5) + sq((y - 8.5) / 4.5) < 1, 0xd8a040, 0x7a4a1a, 0xf8e088));
        for (int type = 0; type < 5; type++)
            for (int mat = 0; mat < 5; mat++) {
                final int t = type, m = mat;
                tile(40 + type * 5 + mat, () -> tool(t, m));
            }
        for (int m = 0; m < 5; m++)
            for (int slot = 0; slot < 4; slot++) {
                final int mm = m, ss = slot;
                tile(65 + m * 4 + slot, () -> armorPiece(mm, ss));
            }
        tile(ARMOR, () -> armorIcon(1));
        tile(ARMOR_HALF, () -> armorIcon(0.5));
        tile(ARMOR_EMPTY, () -> armorIcon(0));
        tile(XP_ORB, this::xpOrb);
        tile(HEART, () -> heart(0xe01818, true, 1));
        tile(HEART_HALF, () -> heart(0xe01818, true, 0.5));
        tile(HEART_EMPTY, () -> heart(0x000000, false, 0));
        tile(HEART_HURT, () -> heart(0xffffff, false, 0));
        tile(FOOD, () -> shank(1));
        tile(FOOD_HALF, () -> shank(0.5));
        tile(FOOD_EMPTY, () -> shank(0));
        tile(BUBBLE, () -> bubble(false));
        tile(BUBBLE_POP, () -> bubble(true));
        tile(FLAME, this::flame);
        tile(PROGRESS, () -> progressArrow(0x8b8b8b));
        tile(PROGRESS_FULL, () -> progressArrow(0xffffff));
        for (var e : EXTRA.entrySet()) {
            tile = e.getKey();
            int[] src = e.getValue();
            for (int i = 0; i < 256; i++) {
                int a = src[i] >>> 24;
                px[((tile >> 4) * 16 + (i >> 4)) * 256 + (tile & 15) * 16 + (i & 15)] = a >= 128 ? 0xFF000000 | src[i] : 0;
            }
        }
        return px;
    }

    private void tile(int t, Runnable painter) {
        tile = t;
        r = new Random(t * 31L + 7);
        painter.run();
    }

    private void set(int x, int y, int rgb) {
        if (x < 0 || y < 0 || x > 15 || y > 15) return;
        px[((tile >> 4) * 16 + y) * 256 + (tile & 15) * 16 + x] = 0xFF000000 | rgb;
    }

    private boolean opaque(int x, int y) {
        if (x < 0 || y < 0 || x > 15 || y > 15) return false;
        return (px[((tile >> 4) * 16 + y) * 256 + (tile & 15) * 16 + x] >>> 24) != 0;
    }

    private static double dist(double x, double y, double cx, double cy) {
        return Math.sqrt(sq(x - cx) + sq(y - cy));
    }

    private static double sq(double v) { return v * v; }

    /** Fills a shape with an outline (dark), body (main) and a highlight on the upper-left inner edge. */
    private void shape(BiPredicate<Integer, Integer> in, int main, int dark, int light) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!in.test(x, y)) continue;
                boolean edge = !in.test(x - 1, y) || !in.test(x + 1, y) || !in.test(x, y - 1) || !in.test(x, y + 1);
                boolean hl = !edge && (!in.test(x - 1, y - 1) || !in.test(x, y - 2));
                int c = edge ? dark : hl ? light : main;
                set(x, y, jitter(c));
            }
    }

    private int jitter(int c) {
        double f = 0.94 + r.nextDouble() * 0.12;
        int rr = Math.min(255, (int) ((c >> 16 & 255) * f)), g = Math.min(255, (int) ((c >> 8 & 255) * f)), b = Math.min(255, (int) ((c & 255) * f));
        return rr << 16 | g << 8 | b;
    }

    private void handle(int x0, int y0, int x1, int y1) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i++) {
            int x = x0 + (x1 - x0) * i / n, y = y0 + (y1 - y0) * i / n;
            set(x, y, i % 2 == 0 ? WOOD : 0x9c7a44);
            set(x + 1, y, WOOD_D);
        }
    }

    private void tool(int type, int mat) {
        int[] c = MATERIAL[mat];
        switch (type) {
            case 0 -> { // pickaxe
                handle(2, 14, 10, 6);
                shape((x, y) -> {
                    double d = dist(x, y, 3.5, 12.5);
                    double a = Math.toDegrees(Math.atan2(12.5 - y, x - 3.5));
                    return d > 8.4 && d < 10.6 && a > 2 && a < 88;
                }, c[0], c[1], c[2]);
            }
            case 1 -> { // axe
                handle(2, 14, 11, 5);
                shape((x, y) -> dist(x, y, 9.5, 4.5) < 4.2 && x - y < 8 && !(x >= 11 && y >= 6) && y >= 1, c[0], c[1], c[2]);
            }
            case 2 -> { // shovel
                handle(2, 14, 9, 7);
                shape((x, y) -> {
                    double u = ((x - 11) - (y - 5)) / 1.414, v = ((x - 11) + (y - 5)) / 1.414;
                    return sq(u / 3.6) + sq(v / 2.6) < 1;
                }, c[0], c[1], c[2]);
            }
            case 3 -> { // sword: blade along x + y = 16 from the guard up to the tip
                shape((x, y) -> {
                    double along = ((x - 9.5) - (y - 6.5)) / 1.414;
                    double perp = ((x - 9.5) + (y - 6.5)) / 1.414;
                    return Math.abs(perp) < 1.1 && along > -5.4 && along < 6.3 && !(along > 5.3 && Math.abs(perp) > 0.4);
                }, c[0], c[1], c[2]);
                for (int i = -2; i <= 2; i++) set(5 + i, 11 + i, 0x4a3520);
                handle(1, 15, 4, 12);
            }
            default -> { // hoe
                handle(2, 14, 11, 5);
                shape((x, y) -> (y >= 3 && y <= 4 && x >= 7 && x <= 12) || (x >= 11 && x <= 12 && y >= 3 && y <= 6), c[0], c[1], c[2]);
            }
        }
    }

    private void lump(int main, int dark, int light) {
        shape((x, y) -> dist(x, y, 8, 8) < 5.8 + Math.sin(x * 1.7 + y) * 0.9, main, dark, light);
        for (int i = 0; i < 4; i++) set(5 + r.nextInt(6), 5 + r.nextInt(6), light);
    }

    private void ingot(int main, int dark, int light) {
        shape((x, y) -> y >= 5 && y <= 11 && x >= 2 + (11 - y) / 2 && x <= 13 - (y - 5) / 3, main, dark, light);
    }

    private void diamond() {
        shape((x, y) -> {
            if (y < 4 || y > 13) return false;
            if (y <= 6) return Math.abs(x - 7.5) <= 2 + (y - 4) * 1.5;
            return Math.abs(x - 7.5) <= 5 - (y - 6) * 0.72;
        }, 0x5ef0e0, 0x1c9c94, 0xe0fffc);
    }

    private void string() {
        for (int x = 1; x < 15; x++) {
            int y = 8 + (int) Math.round(Math.sin(x * 0.9) * 3);
            set(x, y, 0xf0f0f0);
            set(x, y + 1, 0xb8b8b8);
        }
    }

    private void feather() {
        shape((x, y) -> {
            double u = ((x - 8) - (8 - y)) / 1.414, v = ((x - 8) + (8 - y)) / 1.414;
            return Math.abs(u) < 1.8 * (1 - Math.abs(v) / 7.5) + 0.3 && Math.abs(v) < 7;
        }, 0xf4f4f4, 0xb0b0b0, 0xffffff);
        for (int i = 0; i < 11; i++) set(3 + i, 13 - i, 0x9a9a9a);
    }

    private void pile(int main, int dark, int light) {
        shape((x, y) -> y > 7 && Math.abs(x - 8) < (y - 6) * 0.9 && y < 14, main, dark, light);
        for (int i = 0; i < 8; i++) set(4 + r.nextInt(8), 9 + r.nextInt(4), r.nextBoolean() ? light : dark);
    }

    private void bone() {
        for (int i = 0; i < 9; i++) { set(4 + i, 11 - i, 0xf0f0e0); set(5 + i, 11 - i, 0xc8c8b8); }
        shape((x, y) -> dist(x, y, 3.5, 12.5) < 2 || dist(x, y, 12.5, 3.5) < 2, 0xf8f8e8, 0xb8b8a8, 0xffffff);
    }

    private void arrow() {
        for (int i = 0; i < 10; i++) set(3 + i, 12 - i, i % 2 == 0 ? 0x8a6a3a : 0x6a5028);
        shape((x, y) -> x >= 10 && y <= 5 && x - y >= 7 && x + y <= 17 + 0, 0xb0b0b0, 0x5a5a5a, 0xe0e0e0);
        set(2, 12, 0xf0f0f0); set(3, 13, 0xf0f0f0); set(1, 13, 0xd0d0d0); set(2, 14, 0xd0d0d0); set(4, 13, 0xe0e0e0); set(2, 11, 0xe0e0e0);
    }

    private void bow() {
        for (int t = 0; t <= 20; t++) {
            double a = Math.toRadians(-45 + t * 4.5);
            double cx = 3, cy = 13;
            int x = (int) Math.round(cx + Math.cos(Math.toRadians(-45)) * 0 + 11 * Math.sin(a + Math.PI / 4) * 0.9);
            int y = (int) Math.round(cy - 11 * Math.cos(a + Math.PI / 4) * 0.9);
            set(x, y, t % 3 == 0 ? 0x5a4222 : 0x8b6a3a);
        }
        for (int i = 0; i < 11; i++) set(3 + i, 12 - i + 0, 0xe0e0e0);
    }

    private void wheat() {
        for (int s = 0; s < 4; s++) {
            int x = 4 + s * 2;
            for (int y = 5; y < 15; y++) set(x + (y < 9 ? (s - 2) / 2 : 0), y, y < 9 ? 0xd8b848 : 0xa89030);
        }
        set(8, 15, 0x6a5a1a); set(7, 15, 0x6a5a1a);
    }

    private void seeds() {
        for (int i = 0; i < 7; i++) {
            int x = 3 + r.nextInt(10), y = 4 + r.nextInt(9);
            set(x, y, 0x5a9a2a); set(x + 1, y, 0x3a6a1a);
        }
    }

    private void apple() {
        shape((x, y) -> dist(x, y, 8, 9.5) < 5.5 && !(y < 6 && Math.abs(x - 8) < 1), 0xd81c1c, 0x7a0a0a, 0xff8a8a);
        set(8, 3, 0x5a3a1a); set(8, 4, 0x5a3a1a); set(9, 3, 0x3a9a1a); set(10, 2, 0x3a9a1a);
    }

    private void meat(int main, int dark, int light) {
        shape((x, y) -> sq((x - 8) / 6.5) + sq((y - 8) / 4.8) < 1 && !(x > 12 && y < 6), main, dark, light);
        for (int i = 0; i < 4; i++) set(6 + i, 7 + (i % 2), light);
    }

    private void drumstick(int main, int dark) {
        shape((x, y) -> dist(x, y, 9.5, 6.5) < 4.5, main, dark, 0xfff0e0);
        for (int i = 0; i < 5; i++) { set(3 + i, 13 - i, 0xf0f0e0); set(4 + i, 13 - i, 0xc8c8b8); }
        set(2, 13, 0xf8f8f0); set(3, 14, 0xf8f8f0);
    }

    private void bucket(int fill) {
        shape((x, y) -> y >= 4 && y <= 13 && Math.abs(x - 7.5) <= 5.5 - (y - 4) * 0.2, 0xb8b8b8, 0x5a5a5a, 0xe8e8e8);
        for (int x = 3; x <= 12; x++) set(x, 4, fill == 0 ? 0x3a3a3a : fill);
        for (int x = 4; x <= 11; x++) set(x, 5, fill == 0 ? 0x2a2a2a : fill);
        for (int x = 4; x <= 11; x++) set(x, 2, 0x8a8a8a);
    }

    private void xpBottle() {
        shape((x, y) -> (y >= 6 && dist(x, y, 7.5, 10) < 5) || (y >= 2 && y < 6 && Math.abs(x - 7.5) < 2), 0x9ae05a, 0x3a7a1a, 0xe0ffc0);
        for (int x = 6; x <= 9; x++) set(x, 2, 0x8a6a4a);
    }

    /** A glass bottle; potions leave the belly empty for the tinted liquid overlay. */
    private void bottle(boolean potion, boolean splash) {
        BiPredicate<Integer, Integer> in = (x, y) -> (y >= 6 && dist(x, y, 7.5, 10) < 5) || (y >= 2 && y < 6 && Math.abs(x - 7.5) < (splash ? 2.5 : 2));
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!in.test(x, y)) continue;
                boolean edge = !in.test(x - 1, y) || !in.test(x + 1, y) || !in.test(x, y - 1) || !in.test(x, y + 1);
                if (edge) set(x, y, 0x3a4a5a);
                else if (!potion || y < 7) set(x, y, (x < 7 && y < 9) ? 0xf0f8ff : 0xb8d0e0);
            }
        for (int x = 6; x <= 9; x++) set(x, 2, 0x8a6a4a);
    }

    private void bowl(int soup) {
        shape((x, y) -> y >= 7 && y <= 13 && dist(x, y, 7.5, 6) < 7.5, 0x8a6a3a, 0x4a3218, 0xb08a58);
        if (soup >= 0) for (int x = 2; x <= 13; x++) { set(x, 7, jitter(soup)); if (x > 3 && x < 12) set(x, 8, jitter(soup)); }
    }

    private void melonSlice() {
        shape((x, y) -> dist(x, y, 8, 3) < 10 && y > 5 && dist(x, y, 8, 3) > 3, 0xe03a3a, 0x3a8a2a, 0xff7a6a);
        for (int i = 0; i < 5; i++) set(5 + i * 2, 8 + (i % 2) * 2, 0x201010);
    }

    /** Tints every opaque pixel of the current tile towards a colour (golden foods). */
    private void recolor(int c) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int i = ((tile >> 4) * 16 + y) * 256 + (tile & 15) * 16 + x;
                if ((px[i] >>> 24) == 0) continue;
                int v = px[i];
                double l = ((v >> 16 & 255) * 0.3 + (v >> 8 & 255) * 0.59 + (v & 255) * 0.11) / 160.0;
                int rr = Math.min(255, (int) ((c >> 16 & 255) * l)), g = Math.min(255, (int) ((c >> 8 & 255) * l)), b = Math.min(255, (int) ((c & 255) * l));
                px[i] = 0xFF000000 | rr << 16 | g << 8 | b;
            }
    }

    private void minecart() {
        shape((x, y) -> y >= 6 && y <= 11 && x >= 1 && x <= 14 && !(y < 10 && x >= 3 && x <= 12 && y > 6), 0x9a9a9a, 0x4a4a4a, 0xd0d0d0);
        for (int x = 3; x <= 12; x++) for (int y = 7; y <= 9; y++) set(x, y, 0x3a3a3a);
        shape((x, y) -> dist(x, y, 4, 12.5) < 1.6 || dist(x, y, 11, 12.5) < 1.6, 0x5a5a5a, 0x2a2a2a, 0x8a8a8a);
    }

    private void boat() {
        shape((x, y) -> y >= 6 && y <= 11 && x >= 1 + Math.max(0, y - 8) && x <= 14 - Math.max(0, y - 8), WOOD, WOOD_D, 0xb08a52);
        for (int x = 3; x <= 12; x++) set(x, 7, WOOD_D);
        for (int x = 2; x <= 13; x++) set(x, 9, 0x6a4e2a);
    }

    /** A fish facing left: body ellipse, tail fin, eye; kind 1 = salmon stripes, 2 = clownfish bands. */
    private void fish(int main, int dark, int light, int kind) {
        shape((x, y) -> sq((x - 7) / 5.5) + sq((y - 8) / 3.2) < 1 || (x >= 11 && x <= 14 && Math.abs(y - 8) <= (x - 10)), main, dark, light);
        if (kind == 1) for (int x = 3; x <= 11; x++) set(x, 9, 0xe8a090);
        if (kind == 2) for (int y = 5; y <= 11; y++) for (int x : new int[]{5, 9}) if (opaque(x, y)) set(x, y, 0xf8f8f8);
        set(3, 7, 0x101010);
    }

    private void pufferfish() {
        shape((x, y) -> dist(x, y, 7.5, 8) < 5, 0xe0c040, 0x7a6010, 0xfff090);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            set((int) Math.round(7.5 + Math.cos(a) * 6), (int) Math.round(8 + Math.sin(a) * 6), 0xd0d0b0);
        }
        set(5, 7, 0x101010);
        set(10, 7, 0x101010);
    }

    private void fishingRod() {
        handle(2, 14, 13, 3);
        for (int y = 4; y < 13; y++) set(13, y, 0xd8d8d8);
        set(12, 13, 0xa0a0a0);
    }

    private void shears() {
        shape((x, y) -> (Math.abs((x - 2) - (13 - y)) <= 1 && x >= 5 && x <= 12) || (Math.abs((x - 4) - (15 - y)) <= 1 && x >= 7 && x <= 14 && y >= 2),
                0xd8d8d8, 0x5a5a5a, 0xffffff);
        shape((x, y) -> dist(x, y, 4, 12) < 2.6 || dist(x, y, 12, 4) < 2.6, 0xc02020, 0x600a0a, 0xf06060);
    }

    private void carrot() {
        shape((x, y) -> Math.abs((x - 8) + (y - 8)) <= 2.2 - (x - y + 8) * 0.05 && x - y > -9 && x - y < 7 && x >= 2 && y <= 13,
                0xf08a20, 0x9a4a0a, 0xffb860);
        for (int i = 0; i < 4; i++) { set(11 + i % 2, 2 + i, 0x3aa01a); set(12 + i % 2, 3 + i / 2, 0x2a8012); }
    }

    private void flintAndSteel() {
        shape((x, y) -> { double d = dist(x, y, 6, 6); return d > 2.2 && d < 4.2; }, 0xb0b0b0, 0x5a5a5a, 0xe0e0e0);
        shape((x, y) -> dist(x, y, 11, 11) < 3.2, 0x3a3a3a, 0x1a1a1a, 0x6a6a6a);
    }

    private void book() {
        shape((x, y) -> x >= 3 && x <= 12 && y >= 2 && y <= 13, 0x7a3a1a, 0x3a1a0a, 0xa05a2a);
        for (int y = 3; y <= 12; y++) set(12, y, 0xf0f0e0);
    }

    private void heart(int color, boolean filled, double fraction) {
        BiPredicate<Integer, Integer> in = (x, y) -> {
            double fx = (x - 7.5) / 7.0, fy = (8.5 - y) / 7.0;
            return sq(fx * fx + fy * fy - 0.35) - fx * fx * fy * fy * fy * 1.3 < 0.12 && y >= 1 && y <= 14;
        };
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!in.test(x, y)) continue;
                boolean edge = !in.test(x - 1, y) || !in.test(x + 1, y) || !in.test(x, y - 1) || !in.test(x, y + 1);
                if (edge) set(x, y, 0x101010);
                else if (filled && x < 16 * fraction) set(x, y, (x < 6 && y < 6) ? 0xff9090 : jitter(color));
                else set(x, y, color == 0xffffff ? 0xffffff : 0x3a1010);
            }
    }

    private void shank(double fraction) {
        BiPredicate<Integer, Integer> meat = (x, y) -> dist(x, y, 9.5, 6.5) < 5;
        BiPredicate<Integer, Integer> bone = (x, y) -> Math.abs((x - 2) - (14 - y)) <= 1 && x >= 2 && x <= 7;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean m = meat.test(x, y), b = bone.test(x, y);
                if (!m && !b) continue;
                boolean edge = !(meat.test(x - 1, y) || bone.test(x - 1, y)) || !(meat.test(x + 1, y) || bone.test(x + 1, y))
                        || !(meat.test(x, y - 1) || bone.test(x, y - 1)) || !(meat.test(x, y + 1) || bone.test(x, y + 1));
                boolean full = x >= 16 * (1 - fraction);
                if (edge) set(x, y, 0x101010);
                else if (!full) set(x, y, 0x2a1a10);
                else set(x, y, m ? jitter(0xa8602a) : 0xf0e8d8);
            }
    }

    private void bubble(boolean popped) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 7.5);
                if (popped ? (d > 5 && d < 6.5 && (x + y) % 3 == 0) : d < 6.5) {
                    set(x, y, d > 5.3 ? 0x3050c0 : (x < 7 && y < 7 ? 0xe0f0ff : 0x6090f0));
                }
            }
    }

    private void flame() {
        shape((x, y) -> {
            double w = (y - 2) / 12.0 * 5.5;
            return y >= 2 && y <= 14 && Math.abs(x - 7.5 - Math.sin(y * 0.7) * 0.8) < w;
        }, 0xf8a020, 0xc04010, 0xfff080);
    }

    private void progressArrow(int color) {
        for (int y = 3; y <= 12; y++)
            for (int x = 0; x <= 14; x++) {
                boolean shaft = x < 9 && y >= 6 && y <= 9;
                boolean head = x >= 9 && Math.abs(y - 7.5) <= 14 - x - 0.5;
                if (shaft || head) set(x, y, color);
            }
    }

    private static BiPredicate<Integer, Integer> armorShape(int slot) {
        return switch (slot) {
            case 0 -> (x, y) -> x >= 3 && x <= 12 && y >= 4 && y <= 11 && (y <= 6 || x <= 5 || x >= 10);
            case 1 -> (x, y) -> (y >= 2 && y <= 5 && x >= 1 && x <= 14 && !(y <= 3 && x >= 6 && x <= 9))
                    || (y >= 2 && y <= 14 && x >= 4 && x <= 11 && !(y <= 3 && x >= 6 && x <= 9));
            case 2 -> (x, y) -> (y >= 2 && y <= 5 && x >= 3 && x <= 12) || (y >= 6 && y <= 14 && ((x >= 3 && x <= 6) || (x >= 9 && x <= 12)));
            default -> (x, y) -> y >= 7 && y <= 13 && ((x >= 1 && x <= 5) || (x >= 10 && x <= 14) || (y >= 11 && (x == 0 || x == 15)));
        };
    }

    private static int scaleColor(int c, double f) {
        int r = Math.min(255, (int) ((c >> 16 & 255) * f)), g = Math.min(255, (int) ((c >> 8 & 255) * f)), b = Math.min(255, (int) ((c & 255) * f));
        return r << 16 | g << 8 | b;
    }

    private void armorPiece(int material, int slot) {
        int c = ARMOR_COLORS[material];
        shape(armorShape(slot), c, scaleColor(c, 0.45), scaleColor(c, 1.3));
        if (material == 1) {
            // Chainmail: holes in a checker pattern
            BiPredicate<Integer, Integer> in = armorShape(slot);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (in.test(x, y) && (x + y) % 2 == 0 && in.test(x - 1, y) && in.test(x + 1, y)) set(x, y, 0x505050);
        }
    }

    private void armorIcon(double fraction) {
        BiPredicate<Integer, Integer> in = armorShape(1);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!in.test(x, y)) continue;
                boolean edge = !in.test(x - 1, y) || !in.test(x + 1, y) || !in.test(x, y - 1) || !in.test(x, y + 1);
                boolean full = x < 16 * fraction;
                set(x, y, edge ? 0x101010 : full ? ((x < 7 && y < 6) ? 0xffffff : 0xc8c8c8) : 0x303030);
            }
    }

    private void xpOrb() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 7.5);
                if (d < 6.5) set(x, y, d > 5 ? 0x3a7a10 : d > 3 ? 0x9ae02a : 0xf0ff90);
            }
    }
}
