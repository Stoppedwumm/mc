package mc.render;

import mc.world.Block.Tex;

import java.util.Random;

/**
 * Procedurally paints 16x16 pixel-art block textures into an atlas of 16x16 tiles (see {@link Atlas}).
 * Alpha conventions for opaque blocks: 255 = plain pixel, 153 = biome-tinted pixel (grass), 0 = hole.
 */
public final class TextureGen {
    public static final int ATLAS = Atlas.SIZE;
    private final int[] px = new int[ATLAS * ATLAS];
    private Random r;
    private int tile;

    /** 16 x 16 ARGB tiles added by mods, painted after the built-in ones. */
    private static final java.util.Map<Integer, int[]> EXTRA = new java.util.LinkedHashMap<>();
    private static int nextExtra = Tex.COUNT;

    /** Reserves an atlas tile for a mod texture (16 x 16 ARGB, row by row); returns its index. */
    public static synchronized int addTile(int[] argb) {
        if (nextExtra == Tex.BREAK_0) nextExtra = Tex.BREAK_0 + 10; // the crack stages
        if (nextExtra >= Atlas.TILES) throw new IllegalStateException("The block texture atlas is full");
        EXTRA.put(nextExtra, argb);
        return nextExtra++;
    }

    public int[] generate() {
        paint(Tex.STONE, this::stone);
        paint(Tex.DIRT, this::dirt);
        paint(Tex.GRASS_TOP, this::grassTop);
        paint(Tex.GRASS_SIDE, () -> grassSide(false));
        paint(Tex.COBBLE, () -> cobble(false));
        paint(Tex.PLANKS, () -> planks(0xa2824e));
        paint(Tex.BEDROCK, this::bedrock);
        paint(Tex.WATER, this::water);
        paint(Tex.SAND, () -> speckle(0xdbcfa3, 0.06, 0xc9bb8c, 0.12));
        paint(Tex.GRAVEL, this::gravel);
        paint(Tex.GOLD_ORE, () -> ore(0xfcee4b, 0xc4a022));
        paint(Tex.IRON_ORE, () -> ore(0xd8af93, 0xaf8e77));
        paint(Tex.COAL_ORE, () -> ore(0x2d2d2d, 0x151515));
        paint(Tex.DIAMOND_ORE, () -> ore(0x5decf5, 0x2cb6c3));
        paint(Tex.LOG_SIDE, () -> bark(0x6b5130, 0x4f3b22));
        paint(Tex.LOG_TOP, () -> logTop(0xb28f57, 0x8f6f40, 0x6b5130));
        paint(Tex.SPRUCE_LOG, () -> bark(0x3b2912, 0x2a1d0c));
        paint(Tex.SPRUCE_LOG_TOP, () -> logTop(0x7a5a33, 0x5e4426, 0x3b2912));
        paint(Tex.BIRCH_LOG, this::birchBark);
        paint(Tex.BIRCH_LOG_TOP, () -> logTop(0xc8b77a, 0xa89a62, 0xd8d8d0));
        paint(Tex.LEAVES, () -> leaves(0.22));
        paint(Tex.SPRUCE_LEAVES, () -> leaves(0.18));
        paint(Tex.BIRCH_LEAVES, () -> leaves(0.24));
        paint(Tex.GLASS, this::glass);
        paint(Tex.SANDSTONE_SIDE, () -> sandstone(true));
        paint(Tex.SANDSTONE_TOP, () -> speckle(0xdcd0a4, 0.04, 0xcfc190, 0.1));
        paint(Tex.SANDSTONE_BOTTOM, () -> sandstone(false));
        paint(Tex.TALL_GRASS, this::tallGrass);
        paint(Tex.FERN, this::fern);
        paint(Tex.DANDELION, () -> flower(0xf6e03b, 0xd0a820, false));
        paint(Tex.POPPY, () -> flower(0xd3261e, 0x8d1510, false));
        paint(Tex.BLUE_ORCHID, () -> flower(0x2fa8e0, 0x1c73b8, true));
        paint(Tex.BRICKS, this::bricks);
        paint(Tex.SNOW, () -> speckle(0xf0fbfb, 0.02, 0xdae6f0, 0.1));
        paint(Tex.SNOWY_GRASS_SIDE, () -> grassSide(true));
        paint(Tex.ICE, this::ice);
        paint(Tex.CACTUS_SIDE, this::cactusSide);
        paint(Tex.CACTUS_TOP, this::cactusTop);
        paint(Tex.GLOWSTONE, this::glowstone);
        paint(Tex.TORCH, this::torch);
        paint(Tex.CLAY, () -> speckle(0xa0a6b3, 0.03, 0x9097a4, 0.1));
        paint(Tex.MOSSY_COBBLE, () -> cobble(true));
        paint(Tex.OBSIDIAN, this::obsidian);
        paint(Tex.BOOKSHELF, this::bookshelf);
        paint(Tex.WOOL, () -> wool(0xeaecec));
        paint(Tex.RED_WOOL, () -> wool(0xa12722));
        paint(Tex.BLUE_WOOL, () -> wool(0x35399d));
        paint(Tex.GREEN_WOOL, () -> wool(0x546d1b));
        paint(Tex.YELLOW_WOOL, () -> wool(0xf8c627));
        paint(Tex.BLACK_WOOL, () -> wool(0x16161b));
        paint(Tex.STONE_BRICKS, this::stoneBricks);
        paint(Tex.DEAD_BUSH, this::deadBush);
        paint(Tex.SPRUCE_PLANKS, () -> planks(0x735533));
        paint(Tex.BIRCH_PLANKS, () -> planks(0xc4b37b));
        paint(Tex.LAVA, this::lava);
        paint(Tex.CRAFTING_TOP, this::craftingTop);
        paint(Tex.CRAFTING_SIDE, this::craftingSide);
        paint(Tex.FURNACE_SIDE, () -> furnace(false));
        paint(Tex.FURNACE_FRONT, () -> furnace(true));
        paint(Tex.FURNACE_TOP, () -> speckle(0x6e6e6e, 0.08, 0x5a5a5a, 0.2));
        paint(Tex.PUMPKIN_SIDE, () -> ribbed(0xe38a1d, 0xc0700f, false));
        paint(Tex.PUMPKIN_TOP, this::pumpkinTop);
        paint(Tex.MELON_SIDE, () -> ribbed(0x8ea22a, 0x4f6a12, true));
        paint(Tex.MELON_TOP, () -> logTop(0x8ea22a, 0x7a8d20, 0x5d7a17));
        paint(Tex.TNT_SIDE, this::tntSide);
        paint(Tex.TNT_TOP, () -> tntEnd(true));
        paint(Tex.TNT_BOTTOM, () -> tntEnd(false));
        paint(Tex.SUGAR_CANE, this::sugarCane);
        paint(Tex.COARSE_DIRT, this::coarseDirt);
        paint(Tex.GRANITE, () -> speckle(0x9a6b58, 0.06, 0xc08d78, 0.25));
        paint(Tex.DIORITE, () -> speckle(0xbcbcbc, 0.06, 0x7d7d80, 0.22));
        paint(Tex.ANDESITE, () -> speckle(0x888889, 0.06, 0x6c6c6e, 0.28));
        paint(Tex.TERRACOTTA, () -> speckle(0x985e43, 0.03, 0x8a5339, 0.1));
        paint(Tex.FURNACE_FRONT_LIT, this::furnaceLit);
        paint(Tex.CHEST_TOP, () -> chest(0));
        paint(Tex.CHEST_SIDE, () -> chest(1));
        paint(Tex.CHEST_FRONT, () -> chest(2));
        paint(Tex.FARMLAND, () -> farmland(false));
        paint(Tex.FARMLAND_WET, () -> farmland(true));
        for (int i = 0; i < 8; i++) {
            final int stage = i;
            paint(Tex.WHEAT_0 + i, () -> wheat(stage));
        }
        paint(Tex.SAPLING, this::sapling);
        paint(Tex.IRON_BLOCK, () -> metalBlock(0xdcdcdc, 0xa8a8a8));
        paint(Tex.GOLD_BLOCK, () -> metalBlock(0xf8d840, 0xc89a18));
        paint(Tex.DIAMOND_BLOCK, () -> metalBlock(0x68e8e0, 0x2aa8a4));
        paint(Tex.SMOOTH_STONE, this::smoothStone);
        for (int i = 0; i < 4; i++) {
            final int stage = i;
            paint(Tex.CARROTS_0 + i, () -> rootCrop(stage, 0xf08a20));
            paint(Tex.POTATOES_0 + i, () -> rootCrop(stage, 0xc8a060));
        }
        paint(Tex.CAKE_TOP, () -> cake(0));
        paint(Tex.CAKE_SIDE, () -> cake(1));
        paint(Tex.CAKE_INNER, () -> cake(2));
        paint(Tex.CAKE_BOTTOM, () -> cake(3));
        paint(Tex.CAKE_ITEM, this::cakeItem);
        paint(Tex.EMERALD_ORE, () -> ore(0x41f384, 0x0f9a3a));
        paint(Tex.EMERALD_BLOCK, () -> metalBlock(0x51e37a, 0x17a64a));
        paint(Tex.SPAWNER, this::spawner);
        paint(Tex.GRAVEL_PATH, this::gravel);
        paint(Tex.NETHERRACK, this::netherrack);
        paint(Tex.REDSTONE_DUST, this::redstoneDust);
        paint(Tex.REDSTONE_TORCH_ON, () -> redstoneTorch(true));
        paint(Tex.REDSTONE_TORCH_OFF, () -> redstoneTorch(false));
        paint(Tex.LEVER, this::lever);
        paint(Tex.LAMP_OFF, () -> lamp(false));
        paint(Tex.LAMP_ON, () -> lamp(true));
        paint(Tex.REPEATER_OFF, () -> repeater(false));
        paint(Tex.REPEATER_ON, () -> repeater(true));
        paint(Tex.PISTON_TOP, () -> pistonFace(false));
        paint(Tex.PISTON_STICKY, () -> pistonFace(true));
        paint(Tex.PISTON_SIDE, this::pistonSide);
        paint(Tex.PISTON_BOTTOM, () -> pistonBottom(false));
        paint(Tex.PISTON_INNER, () -> pistonBottom(true));
        paint(Tex.REDSTONE_ORE, () -> ore(0xe01010, 0x8a0000));
        paint(Tex.REDSTONE_BLOCK, () -> metalBlock(0xc81a10, 0x7a0a04));
        paint(Tex.REPEATER_ITEM, this::repeaterItem);
        paint(Tex.RAIL, () -> rail(0, false));
        paint(Tex.RAIL_CORNER, this::railCorner);
        paint(Tex.POWERED_RAIL, () -> rail(1, false));
        paint(Tex.POWERED_RAIL_ON, () -> rail(1, true));
        paint(Tex.DETECTOR_RAIL, () -> rail(2, false));
        paint(Tex.DETECTOR_RAIL_ON, () -> rail(2, true));
        paint(Tex.ENCH_TOP, this::enchTop);
        paint(Tex.ENCH_SIDE, this::enchSide);
        paint(Tex.ENCH_BOTTOM, this::obsidian);
        paint(Tex.ANVIL_TOP, () -> anvil(true));
        paint(Tex.ANVIL_SIDE, () -> anvil(false));
        paint(Tex.LAPIS_ORE, () -> ore(0x2a50c8, 0x10287a));
        paint(Tex.LAPIS_BLOCK, () -> metalBlock(0x2a50c8, 0x142a80));
        paint(Tex.BREWING_BASE, () -> { speckle(0x7a7a7a, 0.08, 0x5a5a5a, 0.3); for (int i = 0; i < 16; i++) { set(i, 0, 0x4a4a4a); set(i, 15, 0x4a4a4a); } });
        paint(Tex.BREWING_ROD, () -> { for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) set(x, y, scale((x + y) % 4 == 0 ? 0xfff0a0 : 0xe8b030, jitter(0.08))); });
        for (int i = 0; i < 3; i++) {
            final int stage = i;
            paint(Tex.NETHER_WART_0 + i, () -> netherWart(stage));
        }
        paint(Tex.BROWN_MUSHROOM, () -> mushroom(0x9a7050, 0x7a5638, false));
        paint(Tex.RED_MUSHROOM, () -> mushroom(0xd82020, 0xa01010, true));
        paint(Tex.BREWING_ITEM, this::brewingItem);
        paint(Tex.SOUL_SAND, this::soulSand);
        paint(Tex.QUARTZ_ORE, this::quartzOre);
        paint(Tex.PORTAL, this::portal);
        paint(Tex.FIRE, this::fire);
        paint(Tex.NETHER_BRICKS, this::netherBricks);
        paint(Tex.QUARTZ_SIDE, () -> speckle(0xece6de, 0.02, 0xdcd4ca, 0.12));
        paint(Tex.QUARTZ_TOP, () -> { speckle(0xf0eae2, 0.02, 0xe0d8ce, 0.1); for (int i = 0; i < 16; i++) { set(i, 0, 0xd8d0c4); set(0, i, 0xd8d0c4); } });
        paint(Tex.OAK_DOOR_TOP, () -> woodDoor(true));
        paint(Tex.OAK_DOOR_BOTTOM, () -> woodDoor(false));
        paint(Tex.IRON_DOOR_TOP, () -> ironDoor(true));
        paint(Tex.IRON_DOOR_BOTTOM, () -> ironDoor(false));
        paint(Tex.TRAPDOOR, this::trapdoor);
        paint(Tex.LADDER, this::ladder);
        paint(Tex.IRON_BARS, this::ironBars);
        paint(Tex.BED_HEAD, () -> bedTop(true));
        paint(Tex.BED_FOOT, () -> bedTop(false));
        paint(Tex.BED_SIDE, this::bedSide);
        paint(Tex.OAK_DOOR_ITEM, () -> doorItem(false));
        paint(Tex.IRON_DOOR_ITEM, () -> doorItem(true));
        paint(Tex.BED_ITEM, this::bedItem);
        paint(Tex.GRASS_SIDE_ITEM, () -> tintedCopy(Tex.GRASS_SIDE, 0x7fb238));
        paint(Tex.GRASS_TOP_ITEM, () -> tintedCopy(Tex.GRASS_TOP, 0x7fb238));
        for (int i = 0; i < 10; i++) {
            final int stage = i;
            paint(Tex.BREAK_0 + i, () -> crack(stage));
        }
        for (var e : EXTRA.entrySet()) {
            tile = e.getKey();
            int[] src = e.getValue();
            // Mostly-opaque pixels become plain pixels; the rest are holes (alpha 153 would mean "tinted" here)
            for (int i = 0; i < 256; i++) set(i & 15, i >> 4, src[i] & 0xFFFFFF, (src[i] >>> 24) >= 128 ? 255 : 0);
            fixTransparentColors();
        }
        return px;
    }

    // ------------------------------------------------------------------ helpers

    private void paint(int t, Runnable painter) {
        tile = t;
        r = new Random(t * 7919L + 12345);
        painter.run();
        fixTransparentColors();
    }

    private void set(int x, int y, int rgb, int a) {
        if (x < 0 || y < 0 || x > 15 || y > 15) return;
        int ax = (tile % Atlas.ROW) * 16 + x, ay = (tile / Atlas.ROW) * 16 + y;
        px[ay * ATLAS + ax] = (a << 24) | (rgb & 0xFFFFFF);
    }

    private void set(int x, int y, int rgb) { set(x, y, rgb, 255); }

    private int get(int x, int y) {
        int ax = (tile % Atlas.ROW) * 16 + (x & 15), ay = (tile / Atlas.ROW) * 16 + (y & 15);
        return px[ay * ATLAS + ax];
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    private static int scale(int rgb, double f) {
        return clamp((int) ((rgb >> 16 & 255) * f)) << 16 | clamp((int) ((rgb >> 8 & 255) * f)) << 8 | clamp((int) ((rgb & 255) * f));
    }

    private static int mix(int a, int b, double t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int gray(int v) { v = clamp(v); return v << 16 | v << 8 | v; }

    private double jitter(double amount) { return 1 + (r.nextDouble() * 2 - 1) * amount; }

    /** Smooth tileable value noise at a given cell size. */
    private double[] valueNoise(int cell) {
        int n = 16 / cell;
        double[] grid = new double[n * n];
        for (int i = 0; i < grid.length; i++) grid[i] = r.nextDouble();
        double[] out = new double[256];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double fx = (double) x / cell, fy = (double) y / cell;
                int x0 = (int) fx, y0 = (int) fy;
                double tx = fx - x0, ty = fy - y0;
                tx = tx * tx * (3 - 2 * tx);
                ty = ty * ty * (3 - 2 * ty);
                double a = grid[(y0 % n) * n + x0 % n], b = grid[(y0 % n) * n + (x0 + 1) % n];
                double c = grid[((y0 + 1) % n) * n + x0 % n], d = grid[((y0 + 1) % n) * n + (x0 + 1) % n];
                out[y * 16 + x] = (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty;
            }
        return out;
    }

    /** Gives fully transparent pixels the average colour of the tile so mipmaps don't darken edges. */
    private void fixTransparentColors() {
        long sr = 0, sg = 0, sb = 0, n = 0;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = get(x, y);
                if ((c >>> 24) > 0) { sr += c >> 16 & 255; sg += c >> 8 & 255; sb += c & 255; n++; }
            }
        if (n == 0 || n == 256) return;
        int avg = (int) (sr / n) << 16 | (int) (sg / n) << 8 | (int) (sb / n);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if ((get(x, y) >>> 24) == 0) set(x, y, avg, 0);
    }

    private void speckle(int base, double var, int spot, double spotChance) {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = r.nextDouble() < spotChance ? spot : base;
                set(x, y, scale(c, jitter(var) * (0.94 + n[y * 16 + x] * 0.12)));
            }
    }

    private void furnaceLit() {
        furnace(true);
        for (int y = 9; y < 14; y++) for (int x = 4; x < 12; x++) {
            double v = r.nextDouble();
            set(x, y, v < 0.3 ? 0xffe070 : v < 0.7 ? 0xf09020 : 0xc04010);
        }
    }

    private void chest(int kind) {
        planks(0xa0782e);
        for (int i = 0; i < 16; i++) {
            set(i, 0, 0x4a3515); set(i, 15, 0x4a3515); set(0, i, 0x4a3515); set(15, i, 0x4a3515);
        }
        if (kind > 0) for (int x = 1; x < 15; x++) set(x, 5, 0x4a3515);
        if (kind == 2) {
            for (int y = 4; y < 8; y++) for (int x = 7; x < 9; x++) set(x, y, y == 4 ? 0xd8d8d8 : 0xa8a8a8);
            set(7, 7, 0x303030);
        }
    }

    private void farmland(boolean wet) {
        dirt();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = get(x, y) & 0xFFFFFF;
                if (y % 4 == 0) c = scale(c, 0.7);
                if (wet) c = scale(c, 0.62);
                set(x, y, c);
            }
    }

    private void wheat(int stage) {
        clearTile();
        int h = 3 + stage * 12 / 7;
        int stem = stage < 7 ? mix(0x3a8a1a, 0x9aa030, stage / 7.0) : 0xb09a38;
        for (int s = 0; s < 5; s++) {
            int x = 1 + s * 3 + r.nextInt(2);
            for (int k = 0; k < h; k++) set(x, 15 - k, scale(stem, jitter(0.1)));
            if (stage >= 5) for (int k = h - 4; k < h; k++) { set(x - 1, 15 - k, stage == 7 ? 0xdcc050 : 0x9ab040); set(x + 1, 15 - k, stage == 7 ? 0xc8a838 : 0x8aa038); }
        }
    }

    private void sapling() {
        clearTile();
        for (int y = 9; y < 16; y++) set(7, y, 0x6b4a26);
        for (int i = 0; i < 40; i++) {
            int x = 3 + r.nextInt(10), y = 2 + r.nextInt(9);
            double d = Math.abs(x - 7.5) + Math.abs(y - 6);
            if (d < 6) set(x, y, scale(0x3c8a24, jitter(0.2)));
        }
    }

    private void metalBlock(int light, int dark) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                int c = edge ? dark : (x + y) % 7 == 0 ? scale(light, 1.08) : light;
                set(x, y, scale(c, jitter(0.03)));
            }
    }

    /** Copies another tile, baking the biome tint into its tintable pixels (for GUI/held items). */
    private void tintedCopy(int source, int tint) {
        int saved = tile;
        int[] copy = new int[256];
        tile = source;
        for (int i = 0; i < 256; i++) copy[i] = get(i & 15, i >> 4);
        tile = saved;
        for (int i = 0; i < 256; i++) {
            int c = copy[i];
            int a = c >>> 24;
            if (a > 0 && a < 250) {
                int r = (c >> 16 & 255) * (tint >> 16 & 255) / 255, g = (c >> 8 & 255) * (tint >> 8 & 255) / 255, b = (c & 255) * (tint & 255) / 255;
                c = r << 16 | g << 8 | b;
            }
            set(i & 15, i >> 4, c, 255);
        }
    }

    // ------------------------------------------------------------------ blocks

    private void stone() {
        double[] n = valueNoise(4), n2 = valueNoise(2);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = 0.86 + n[y * 16 + x] * 0.14 + n2[y * 16 + x] * 0.08 + (r.nextDouble() - 0.5) * 0.08;
                if (r.nextDouble() < 0.07) v -= 0.12;
                set(x, y, gray((int) (122 * v)));
            }
        // Faint horizontal fractures, like vanilla stone
        for (int i = 0; i < 3; i++) {
            int y = r.nextInt(16), x = r.nextInt(16), len = 2 + r.nextInt(4);
            for (int k = 0; k < len; k++) set((x + k) & 15, y, scale(get((x + k) & 15, y), 0.86));
        }
    }

    private void dirt() {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = r.nextDouble();
                int c = v < 0.12 ? 0x6c4b30 : v < 0.22 ? 0x96694a : v < 0.26 ? 0x7f7f7f : 0x866043;
                set(x, y, scale(c, jitter(0.05) * (0.92 + n[y * 16 + x] * 0.16)));
            }
    }

    private void coarseDirt() {
        dirt();
        for (int i = 0; i < 26; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            set(x, y, gray(90 + r.nextInt(50)));
        }
    }

    private int grassPixel(double n) {
        double v = r.nextDouble();
        int g = (int) (150 + n * 30 + (v < 0.15 ? -28 : v > 0.9 ? 22 : (v - 0.5) * 24));
        return gray(g);
    }

    private void grassTop() {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                set(x, y, grassPixel(n[y * 16 + x] - 0.5), 153);
    }

    private void grassSide(boolean snowy) {
        dirt();
        double[] n = valueNoise(4);
        for (int x = 0; x < 16; x++) {
            int depth = 3 + (r.nextDouble() < 0.5 ? 1 : 0) + (r.nextDouble() < 0.25 ? 1 : 0);
            if (r.nextDouble() < 0.12) depth += 2;
            for (int y = 0; y < depth; y++) {
                if (snowy) set(x, y, scale(0xf2fbfb, jitter(0.03) * (y == depth - 1 ? 0.9 : 1)));
                else set(x, y, grassPixel(n[y * 16 + x] - 0.5 - (y == depth - 1 ? 0.3 : 0)), 153);
            }
        }
    }

    private void cobble(boolean mossy) {
        int cells = 11;
        int[] cx = new int[cells], cy = new int[cells];
        double[] shade = new double[cells];
        for (int i = 0; i < cells; i++) { cx[i] = r.nextInt(16); cy[i] = r.nextInt(16); shade[i] = 0.8 + r.nextDouble() * 0.35; }
        double[] moss = valueNoise(8);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d1 = 1e9, d2 = 1e9; int best = 0;
                for (int i = 0; i < cells; i++) {
                    for (int ox = -16; ox <= 16; ox += 16)
                        for (int oy = -16; oy <= 16; oy += 16) {
                            double dx = x - cx[i] - ox, dy = y - cy[i] - oy;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < d1) { d2 = d1; d1 = d; best = i; } else if (d < d2) d2 = d;
                        }
                }
                int c;
                if (d2 - d1 < 1.1) c = gray((int) (70 * jitter(0.1)));
                else {
                    double hl = (d2 - d1) > 3 ? 1.08 : 1.0;
                    c = gray((int) (118 * shade[best] * hl * jitter(0.06)));
                }
                if (mossy && moss[y * 16 + x] > 0.55 && r.nextDouble() < 0.85) c = scale(0x5a7a35, jitter(0.15) * (d2 - d1 < 1.1 ? 0.7 : 1));
                set(x, y, c);
            }
    }

    private void planks(int base) {
        for (int board = 0; board < 4; board++) {
            int seam = r.nextInt(16);
            double bshade = 0.93 + r.nextDouble() * 0.1;
            for (int yy = 0; yy < 4; yy++) {
                int y = board * 4 + yy;
                for (int x = 0; x < 16; x++) {
                    double grain = Math.sin((x + board * 5) * 0.9 + yy * 2.1) * 0.03;
                    int c = scale(base, bshade * (1 + grain) * jitter(0.03));
                    if (yy == 3) c = scale(base, 0.62);
                    else if (x == seam) c = scale(base, 0.7);
                    else if (r.nextDouble() < 0.06) c = scale(base, 0.85);
                    set(x, y, c);
                }
            }
        }
    }

    private void bedrock() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = r.nextDouble();
                set(x, y, gray(v < 0.3 ? 40 + r.nextInt(20) : v < 0.7 ? 85 + r.nextInt(30) : 130 + r.nextInt(40)));
            }
    }

    private void water() {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double w = Math.sin((x + y * 0.5) * 0.8 + n[y * 16 + x] * 4) * 0.5 + 0.5;
                int c = mix(0x2f5fc8, 0x4a7ee0, w * 0.6 + n[y * 16 + x] * 0.3);
                set(x, y, c, 170);
            }
    }

    private void gravel() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, gray(120 + r.nextInt(20)));
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            int c = r.nextDouble() < 0.3 ? scale(0x8a7a6a, jitter(0.1)) : gray(r.nextDouble() < 0.5 ? 80 + r.nextInt(30) : 150 + r.nextInt(40));
            set(x, y, c); set(x + 1, y, scale(c, 0.9)); if (r.nextBoolean()) set(x, y + 1, scale(c, 0.8));
        }
    }

    private void ore(int color, int dark) {
        stone();
        int veins = 4 + r.nextInt(2);
        for (int i = 0; i < veins; i++) {
            int x = 2 + r.nextInt(12), y = 2 + r.nextInt(12);
            int size = 2 + r.nextInt(3);
            for (int k = 0; k < size; k++) {
                int ox = x + r.nextInt(3) - 1, oy = y + r.nextInt(3) - 1;
                set(ox, oy, scale(color, jitter(0.08)));
                set(ox + 1, oy, dark);
                set(ox, oy + 1, scale(color, 1.15));
            }
        }
    }

    private void bark(int base, int dark) {
        for (int x = 0; x < 16; x++) {
            double col = 0.9 + r.nextDouble() * 0.2;
            boolean groove = r.nextDouble() < 0.3;
            for (int y = 0; y < 16; y++) {
                int c = groove && r.nextDouble() < 0.8 ? dark : scale(base, col * jitter(0.06));
                set(x, y, c);
            }
        }
    }

    private void birchBark() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(0xd8d8d0, jitter(0.04)));
        for (int i = 0; i < 9; i++) {
            int x = r.nextInt(16), y = r.nextInt(16), len = 1 + r.nextInt(4);
            for (int k = 0; k < len; k++) set((x + k) & 15, y, r.nextDouble() < 0.7 ? 0x2c2c28 : 0x5a5a52);
        }
    }

    private void logTop(int light, int dark, int barkColor) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5;
                double d = Math.max(Math.abs(dx), Math.abs(dy)) * 0.6 + Math.sqrt(dx * dx + dy * dy) * 0.4;
                int ring = (int) d;
                int c = (ring & 1) == 0 ? light : dark;
                if (x == 0 || y == 0 || x == 15 || y == 15) c = barkColor;
                set(x, y, scale(c, jitter(0.04)));
            }
    }

    private void leaves(double holes) {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (r.nextDouble() < holes) { set(x, y, 0, 0); continue; }
                double v = r.nextDouble();
                int g = (int) (120 + n[y * 16 + x] * 50 + (v < 0.2 ? -35 : v > 0.85 ? 25 : 0));
                set(x, y, gray(g));
            }
    }

    private void glass() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean border = x == 0 || y == 0 || x == 15 || y == 15;
                if (border) set(x, y, (x + y) % 5 == 0 ? 0xffffff : 0xc0e0e8);
                else set(x, y, 0xffffff, 0);
            }
        for (int k = 0; k < 4; k++) { set(3 + k, 5 - k, 0xffffff); set(4 + k, 5 - k, 0xe0f0f4); }
        for (int k = 0; k < 3; k++) set(10 + k, 12 - k, 0xffffff);
    }

    private void sandstone(boolean side) {
        speckle(0xd8cb9a, 0.04, 0xcbbd8a, 0.1);
        if (side) {
            for (int x = 0; x < 16; x++) {
                set(x, 0, 0xe5dbb3); set(x, 1, 0xdcd0a4); set(x, 2, 0xc2b27f);
                set(x, 12, 0xc2b27f); set(x, 13, 0xd5c794);
                if (r.nextDouble() < 0.3) set(x, 7, 0xc8b985);
            }
        } else {
            for (int i = 0; i < 10; i++) set(r.nextInt(16), r.nextInt(16), 0xb8a878);
        }
    }

    private void clearTile() {
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) set(x, y, 0, 0);
    }

    private void tallGrass() {
        clearTile();
        for (int b = 0; b < 11; b++) {
            double x = 1 + r.nextInt(14);
            int h = 5 + r.nextInt(10);
            double lean = (r.nextDouble() - 0.5) * 0.35;
            for (int k = 0; k < h; k++) {
                int y = 15 - k;
                set((int) Math.round(x), y, gray(150 + (int) (k * 4) + r.nextInt(20) - 10));
                x += lean;
            }
        }
    }

    private void fern() {
        clearTile();
        for (int f = 0; f < 3; f++) {
            double x = 4 + f * 4, lean = (f - 1) * 0.35;
            for (int k = 0; k < 13; k++) {
                int y = 15 - k;
                int ix = (int) Math.round(x);
                set(ix, y, gray(130 + k * 3));
                if (k > 2 && k % 2 == 0) { set(ix - 1, y, gray(150)); set(ix + 1, y, gray(150)); if (k < 10) { set(ix - 2, y + 1, gray(140)); set(ix + 2, y + 1, gray(140)); } }
                x += lean * 0.3;
            }
        }
    }

    private void flower(int petal, int petalDark, boolean orchid) {
        clearTile();
        for (int y = 7; y < 16; y++) set(7, y, 0x3f8c1f);
        set(6, 11, 0x4d9e27); set(5, 10, 0x4d9e27); set(8, 12, 0x4d9e27); set(9, 11, 0x4d9e27);
        if (orchid) {
            int[][] p = {{7, 3}, {6, 4}, {8, 4}, {5, 5}, {9, 5}, {7, 5}, {6, 6}, {8, 6}, {7, 4}};
            for (int[] q : p) set(q[0], q[1], q[1] == 5 ? petalDark : petal);
            set(7, 5, 0xb8e0f8);
        } else {
            for (int y = 3; y <= 7; y++) for (int x = 5; x <= 9; x++) {
                if ((x == 5 || x == 9) && (y == 3 || y == 7)) continue;
                set(x, y, (x + y) % 3 == 0 ? petalDark : petal);
            }
            set(7, 5, petal == 0xd3261e ? 0x2a1a10 : 0xe8a000);
        }
    }

    private void bricks() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y / 4;
                int off = (row & 1) * 4;
                boolean mortar = y % 4 == 3 || (x + off) % 8 == 7;
                if (mortar) set(x, y, scale(0xb4aaa0, jitter(0.05)));
                else set(x, y, scale(0x965040, jitter(0.08) * (y % 4 == 0 ? 1.1 : 1)));
            }
    }

    private void ice() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(0x8cb4fc, jitter(0.03)), 190);
        for (int i = 0; i < 4; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            for (int k = 0; k < 5; k++) set((x + k) & 15, (y + k) & 15, 0xd0e4ff, 210);
        }
    }

    private void cactusSide() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = (x % 4 == 1) ? 0x0e5f1c : (x % 4 == 3) ? 0x23892e : 0x167a26;
                set(x, y, scale(c, jitter(0.05)));
            }
        for (int i = 0; i < 10; i++) set(r.nextInt(16), r.nextInt(16), 0xd8d8b0);
    }

    private void cactusTop() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
                int c = d > 6.5 ? 0x0e5f1c : ((int) d & 1) == 0 ? 0x2a9235 : 0x1f8a2c;
                set(x, y, scale(c, jitter(0.04)));
            }
    }

    private void glowstone() {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = n[y * 16 + x] + (r.nextDouble() - 0.5) * 0.4;
                int c = v > 0.65 ? 0xfff0c0 : v > 0.45 ? 0xf0c878 : v > 0.3 ? 0xc89a50 : 0x8a6630;
                set(x, y, c);
            }
    }

    private void torch() {
        clearTile();
        for (int y = 8; y < 16; y++) { set(7, y, 0x6b4a26); set(8, y, 0x87603a); }
        set(7, 6, 0xfff8c0); set(8, 6, 0xffe070);
        set(7, 7, 0xffc030); set(8, 7, 0xf0a020);
        set(7, 5, 0xfff8e0, 255); set(8, 5, 0xffffff, 255);
    }

    private void obsidian() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = r.nextDouble();
                int c = v < 0.1 ? 0x3b2754 : v < 0.2 ? 0x241a36 : 0x0f0b18;
                set(x, y, scale(c, jitter(0.1)));
            }
    }

    private void bookshelf() {
        planks(0xa2824e);
        int[] colors = {0x8c2020, 0x2a4a8c, 0x3a7a2a, 0x7a5a2a, 0x6a2a6a, 0x2a6a6a, 0xa08030};
        for (int shelf = 0; shelf < 2; shelf++) {
            int top = shelf == 0 ? 1 : 9, bottom = shelf == 0 ? 6 : 14;
            int x = 1;
            while (x < 15) {
                int w = 1 + r.nextInt(2);
                int c = colors[r.nextInt(colors.length)];
                int h = r.nextInt(2);
                for (int xx = x; xx < Math.min(15, x + w); xx++)
                    for (int y = top + h; y <= bottom; y++) set(xx, y, scale(c, y == top + h ? 1.2 : jitter(0.05)));
                x += w;
            }
            for (int y = top; y <= bottom; y++) { set(0, y, 0x6b5130); set(15, y, 0x6b5130); }
        }
    }

    private void wool(int base) {
        double[] n = valueNoise(2);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                set(x, y, scale(base, (0.9 + n[y * 16 + x] * 0.14) * jitter(0.03)));
    }

    private void stoneBricks() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y / 8;
                int off = row * 8;
                boolean mortar = y % 8 == 7 || (x + off) % 16 == 15;
                boolean hl = y % 8 == 0 || (x + off) % 16 == 0;
                int c = mortar ? gray(78) : gray((int) ((hl ? 140 : 122) * jitter(0.06)));
                set(x, y, c);
            }
        for (int i = 0; i < 3; i++) set(r.nextInt(16), r.nextInt(16), gray(96));
    }

    private void deadBush() {
        clearTile();
        int c = 0x6b4a23;
        for (int y = 9; y < 16; y++) set(7, y, c);
        int[][] branches = {{7, 9, -1, -1}, {7, 9, 1, -1}, {7, 12, -1, -1}, {7, 11, 1, -1}};
        for (int[] b : branches) {
            int x = b[0], y = b[1];
            for (int k = 0; k < 5; k++) { x += b[2]; y += b[3]; set(x, y, k % 2 == 0 ? c : 0x8a6232); if (r.nextBoolean()) y--; }
        }
    }

    private void lava() {
        double[] n = valueNoise(4), n2 = valueNoise(2);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = n[y * 16 + x] * 0.7 + n2[y * 16 + x] * 0.3;
                int c = v > 0.7 ? 0xffe070 : v > 0.55 ? 0xfcac30 : v > 0.35 ? 0xe06a10 : 0xc04808;
                set(x, y, c);
            }
    }

    private void craftingTop() {
        planks(0xa2824e);
        for (int i = 0; i < 16; i++) {
            set(i, 0, 0x4a3520); set(i, 15, 0x4a3520); set(0, i, 0x4a3520); set(15, i, 0x4a3520);
            if (i > 1 && i < 14) { set(i, 5, 0x6b5130); set(i, 10, 0x6b5130); set(5, i, 0x6b5130); set(10, i, 0x6b5130); }
        }
    }

    private void craftingSide() {
        planks(0xa2824e);
        for (int i = 0; i < 16; i++) { set(i, 0, 0x4a3520); set(i, 1, 0x6b5130); }
        // A saw and a hammer
        for (int x = 2; x < 8; x++) { set(x, 5, 0xb0b0b0); set(x, 6, 0x8a8a8a); }
        set(8, 5, 0x6b4a26); set(9, 5, 0x6b4a26);
        for (int y = 4; y < 12; y++) set(12, y, 0x6b4a26);
        for (int x = 10; x < 15; x++) set(x, 4, 0x7a7a7a);
    }

    private void furnace(boolean front) {
        cobble(false);
        for (int i = 0; i < 16; i++) { set(i, 0, gray(90)); set(i, 15, gray(90)); set(0, i, gray(90)); set(15, i, gray(90)); }
        if (front) {
            for (int y = 8; y < 14; y++) for (int x = 3; x < 13; x++) set(x, y, y > 11 ? 0x301808 : 0x141414);
            for (int x = 3; x < 13; x++) set(x, 7, gray(60));
            for (int x = 4; x < 12; x += 2) set(x, 13, 0xe06010);
        }
    }

    private void ribbed(int base, int dark, boolean melon) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean rib = melon ? (x % 5 == 0 || x % 5 == 1) : (x % 4 == 0);
                set(x, y, scale(rib ? dark : base, jitter(0.05)));
            }
    }

    private void pumpkinTop() {
        ribbed(0xe38a1d, 0xc0700f, false);
        for (int y = 6; y < 10; y++) for (int x = 6; x < 10; x++) set(x, y, 0x5a4a1a);
    }

    private void tntSide() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = (y >= 5 && y <= 10) ? 0xe8e8e8 : (x % 4 == 0 ? 0xa02010 : 0xdb3a1f);
                set(x, y, scale(c, jitter(0.03)));
            }
        // "TNT" in tiny letters
        int[][] t = {{0, 0}, {1, 0}, {2, 0}, {1, 1}, {1, 2}, {1, 3}};
        int[][] n = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {1, 1}, {2, 2}, {3, 0}, {3, 1}, {3, 2}, {3, 3}};
        for (int[] p : t) set(2 + p[0], 6 + p[1], 0x202020);
        for (int[] p : n) set(6 + p[0], 6 + p[1], 0x202020);
        for (int[] p : t) set(11 + p[0], 6 + p[1], 0x202020);
    }

    private void tntEnd(boolean top) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(0xdb3a1f, jitter(0.05)));
        for (int i = 0; i < 16; i++) { set(i, 0, 0xa02010); set(i, 15, 0xa02010); set(0, i, 0xa02010); set(15, i, 0xa02010); }
        if (top) { for (int y = 6; y < 10; y++) for (int x = 6; x < 10; x++) set(x, y, 0xd8d8c8); set(7, 7, 0x303030); set(8, 8, 0x303030); }
    }

    private void sugarCane() {
        clearTile();
        int[] xs = {3, 8, 12};
        for (int s = 0; s < 3; s++) {
            int x = xs[s];
            for (int y = 0; y < 16; y++) {
                boolean joint = (y + s * 3) % 6 == 0;
                set(x, y, gray(joint ? 205 : 165));
                set(x + 1, y, gray(joint ? 185 : 140));
            }
            set(x - 1, (s * 5 + 3) & 15, gray(150));
            set(x + 2, (s * 5 + 9) & 15, gray(150));
        }
    }

    private void crack(int stage) {
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) set(x, y, 0x808080, 255);
        Random cr = new Random(4242);
        int lines = 2 + stage * 2;
        for (int i = 0; i < lines; i++) {
            double x = 8 + (cr.nextDouble() - 0.5) * 4, y = 8 + (cr.nextDouble() - 0.5) * 4;
            double ang = cr.nextDouble() * Math.PI * 2;
            int len = 3 + stage + cr.nextInt(3);
            for (int k = 0; k < len; k++) {
                set((int) x & 15, (int) y & 15, 0x2a2a2a, 255);
                ang += (cr.nextDouble() - 0.5) * 0.9;
                x += Math.cos(ang);
                y += Math.sin(ang);
            }
        }
    }

    // ------------------------------------------------------------------ building blocks

    private void smoothStone() {
        speckle(0xa0a0a0, 0.03, 0x979797, 0.1);
        for (int i = 0; i < 16; i++) { set(i, 0, 0xb4b4b4); set(i, 15, 0x7c7c7c); set(0, i, 0xacacac); set(15, i, 0x858585); }
    }

    private static final int OAK = 0xa2824e;

    private void woodDoor(boolean top) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double grain = Math.sin(x * 1.7 + y * 0.15) * 0.04;
                int c = scale(OAK, (x % 4 == 0 ? 0.8 : 1) * (1 + grain) * jitter(0.03));
                set(x, y, c);
            }
        for (int i = 0; i < 16; i++) {
            set(i, top ? 0 : 15, scale(OAK, 0.6));
            set(0, i, scale(OAK, 0.65));
            set(15, i, scale(OAK, 0.65));
        }
        if (top) {
            // Two small windows
            for (int y = 3; y < 8; y++)
                for (int x = 3; x < 13; x++) {
                    if (x == 7 || x == 8) { set(x, y, scale(OAK, 0.7)); continue; }
                    set(x, y, 0, 0);
                }
            for (int x = 2; x < 14; x++) { set(x, 2, scale(OAK, 0.65)); set(x, 8, scale(OAK, 0.65)); }
            for (int y = 2; y < 9; y++) { set(2, y, scale(OAK, 0.65)); set(13, y, scale(OAK, 0.65)); }
        } else {
            for (int x = 2; x < 14; x++) { set(x, 3, scale(OAK, 0.7)); set(x, 11, scale(OAK, 0.7)); }
            set(12, 1, 0x3a3a3a); set(12, 2, 0x3a3a3a); set(13, 1, 0x555555);
        }
    }

    private void ironDoor(boolean top) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(0xc8c8c8, jitter(0.03) * (x == 0 || x == 15 ? 0.75 : 1)));
        for (int i = 0; i < 16; i++) set(i, top ? 0 : 15, 0x8a8a8a);
        if (top) {
            for (int y = 3; y < 9; y++) for (int x = 3; x < 13; x++) set(x, y, (x + y) % 3 == 0 ? 0x9a9a9a : 0xb0b0b0);
        } else {
            for (int y = 2; y < 14; y += 4) for (int x = 2; x < 14; x++) set(x, y, 0xa0a0a0);
            set(12, 1, 0x505050); set(12, 2, 0x505050);
        }
    }

    private void trapdoor() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean frame = x < 2 || y < 2 || x > 13 || y > 13 || x == 7 || x == 8 || y == 7 || y == 8;
                if (!frame && ((x + y) % 5 == 0)) { set(x, y, 0, 0); continue; }
                set(x, y, scale(OAK, (frame ? 0.85 : 1) * jitter(0.04)));
            }
    }

    private void ladder() {
        clearTile();
        for (int y = 0; y < 16; y++) {
            for (int x : new int[]{2, 3, 12, 13}) set(x, y, scale(0x7c5c32, (x == 3 || x == 13 ? 0.8 : 1) * jitter(0.05)));
        }
        for (int y = 1; y < 16; y += 4)
            for (int x = 4; x < 12; x++) { set(x, y, scale(0x9a7644, jitter(0.05))); set(x, y + 1, scale(0x6a4e2a, jitter(0.05))); }
    }

    private void ironBars() {
        clearTile();
        for (int y = 0; y < 16; y++)
            for (int x = 1; x < 16; x += 4) { set(x, y, 0x9a9a9a); set(x + 1, y, 0x6e6e6e); }
        for (int x = 0; x < 16; x++) { set(x, 0, 0x8a8a8a); set(x, 15, 0x6a6a6a); }
    }

    private static final int BED_RED = 0xa02525;

    private void bedTop(boolean head) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(BED_RED, jitter(0.05) * (x == 0 || x == 15 ? 0.8 : 1)));
        if (head) {
            // White sheet with a pillow in the middle
            for (int y = 0; y < 16; y++) for (int x = 1; x < 15; x++) set(x, y, scale(0xe6e6e6, jitter(0.03)));
            for (int y = 3; y < 13; y++) for (int x = 3; x < 13; x++) set(x, y, scale(0xfafafa, jitter(0.02)));
            for (int i = 3; i < 13; i++) { set(i, 3, 0xd0d0d0); set(i, 12, 0xc0c0c0); set(3, i, 0xd0d0d0); set(12, i, 0xc0c0c0); }
        } else {
            for (int x = 0; x < 16; x++) set(x, 2, scale(BED_RED, 0.8));
        }
    }

    private void bedSide() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (y < 3 || y > 9) { set(x, y, 0, 0); continue; }
                set(x, y, y < 6 ? scale(BED_RED, jitter(0.05)) : scale(OAK, jitter(0.04) * 0.85));
            }
        for (int y = 9; y < 16; y++) for (int x = 0; x < 3; x++) { set(x, y, scale(OAK, 0.7)); set(15 - x, y, scale(OAK, 0.7)); }
        for (int y = 3; y < 7; y++) for (int x = 0; x < 16; x++) if (y == 3) set(x, y, scale(BED_RED, 1.15));
    }

    private void doorItem(boolean iron) {
        clearTile();
        int base = iron ? 0xc8c8c8 : OAK;
        for (int y = 0; y < 16; y++)
            for (int x = 4; x < 12; x++) {
                int c = scale(base, (x == 4 || x == 11 ? 0.7 : 1) * jitter(0.04));
                if (y == 0 || y == 15) c = scale(base, 0.6);
                set(x, y, c);
            }
        for (int y = 2; y < 6; y++) for (int x = 6; x < 10; x++) set(x, y, iron ? 0x9a9a9a : 0, iron ? 255 : 0);
        set(10, 9, 0x303030);
    }

    private void bedItem() {
        clearTile();
        for (int x = 0; x < 16; x++) {
            for (int y = 6; y < 10; y++) set(x, y, x < 4 ? 0xeeeeee : scale(BED_RED, jitter(0.05)));
            for (int y = 10; y < 12; y++) set(x, y, scale(OAK, 0.85));
        }
        for (int y = 12; y < 14; y++) { set(0, y, scale(OAK, 0.7)); set(1, y, scale(OAK, 0.7)); set(14, y, scale(OAK, 0.7)); set(15, y, scale(OAK, 0.7)); }
    }

    // ------------------------------------------------------------------ food blocks

    private void rootCrop(int stage, int rootColor) {
        clearTile();
        int h = 3 + stage * 3;
        for (int p = 0; p < 4; p++) {
            int x = 2 + p * 4;
            for (int k = 0; k < h; k++) {
                int y = 15 - k;
                set(x, y, scale(0x3a9a1a, jitter(0.15)));
                if (k > 1 && k % 2 == 0) { set(x - 1, y, scale(0x4aaa2a, jitter(0.15))); set(x + 1, y - 1, scale(0x2a8a12, jitter(0.15))); }
            }
            if (stage == 3) { set(x, 15, rootColor); set(x + 1, 15, scale(rootColor, 0.8)); }
        }
    }

    private void cake(int face) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c;
                switch (face) {
                    case 0 -> c = (x + y * 3) % 11 == 0 ? 0xd02020 : scale(0xf8f4f0, jitter(0.03));
                    case 1 -> c = y < 8 ? 0 : y < 10 ? scale(0xf4f0ec, jitter(0.03)) : scale(0xb06a3a, jitter(0.08));
                    case 2 -> c = y < 8 ? 0 : y < 10 ? scale(0xf4f0ec, jitter(0.03)) : (y == 12 ? 0xd05050 : scale(0xe0c090, jitter(0.06)));
                    default -> c = scale(0xa06030, jitter(0.08));
                }
                if (c == 0) set(x, y, 0, 0); else set(x, y, c);
            }
    }

    private void cakeItem() {
        clearTile();
        for (int y = 6; y < 13; y++)
            for (int x = 2; x < 14; x++) {
                int c = y < 8 ? 0xf8f4f0 : y == 8 ? 0xe8e0d8 : scale(0xb06a3a, jitter(0.08));
                if (y == 6 && x % 4 == 1) c = 0xd02020;
                set(x, y, c);
            }
    }

    private void spawner() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean bar = x % 4 == 0 || y % 4 == 0 || x == 15 || y == 15;
                if (bar) set(x, y, (x + y) % 3 == 0 ? 0x2a3440 : 0x1a2028);
                else set(x, y, 0, 0);
            }
    }

    // ------------------------------------------------------------------ nether

    private void netherrack() {
        double[] n = valueNoise(4);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = n[y * 16 + x] + r.nextDouble() * 0.3;
                int c = v < 0.45 ? 0x6a2a2a : v < 0.8 ? 0x7e3432 : 0x984646;
                if (r.nextDouble() < 0.06) c = 0x4a1a1a;
                set(x, y, scale(c, jitter(0.05)));
            }
    }

    private void soulSand() {
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) set(x, y, scale(0x5a4232, jitter(0.12)));
        // Faint screaming faces
        for (int k = 0; k < 3; k++) {
            int fx = 1 + r.nextInt(11), fy = 1 + r.nextInt(10);
            set(fx, fy, 0x3a2820); set(fx + 3, fy, 0x3a2820);
            set(fx + 1, fy + 3, 0x2e2018); set(fx + 2, fy + 3, 0x2e2018); set(fx + 1, fy + 4, 0x2e2018); set(fx + 2, fy + 4, 0x2e2018);
        }
    }

    private void quartzOre() {
        netherrack();
        for (int i = 0; i < 6; i++) {
            int x = 1 + r.nextInt(13), y = 1 + r.nextInt(13);
            set(x, y, 0xf0ece4); set(x + 1, y, 0xd8d0c8); set(x, y + 1, 0xe4dcd4);
        }
    }

    private void portal() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double sw = Math.sin((x - 7.5) * 0.6 + Math.cos((y - 7.5) * 0.5) * 2) * 0.5 + 0.5;
                int c = mix(0x3a0a8a, 0xa050f0, sw * 0.7 + r.nextDouble() * 0.3);
                set(x, y, c, 190);
            }
    }

    private void fire() {
        clearTile();
        for (int x = 0; x < 16; x++) {
            int h = 7 + r.nextInt(9);
            for (int k = 0; k < h; k++) {
                double t = (double) k / h;
                int c = t < 0.35 ? 0xfff0a0 : t < 0.7 ? 0xffb030 : 0xe05010;
                if (r.nextDouble() < 0.15 && k > 3) continue;
                set(x, 15 - k, c);
            }
        }
    }

    private void netherBricks() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y / 4, off = row % 2 == 0 ? 0 : 4;
                boolean mortar = y % 4 == 3 || (x + off) % 8 == 7;
                set(x, y, mortar ? 0x1a0a0e : scale(0x442228, jitter(0.1)));
            }
    }

    // ------------------------------------------------------------------ redstone

    /** Grey-scale dust cross (tinted by power level in the mesher). */
    private void redstoneDust() {
        clearTile();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean cross = (x >= 5 && x <= 10) || (y >= 5 && y <= 10);
                if (!cross || r.nextDouble() < 0.06) continue;
                set(x, y, gray(200 + r.nextInt(56)));
            }
    }

    private void redstoneTorch(boolean lit) {
        clearTile();
        for (int y = 6; y < 16; y++) { set(7, y, 0x6b4a26); set(8, y, 0x5a3a1a); }
        int tip = lit ? 0xff3020 : 0x5a1a14;
        set(7, 6, tip); set(8, 6, tip); set(7, 7, lit ? 0xd81a10 : 0x4a1410); set(8, 7, lit ? 0xd81a10 : 0x4a1410);
        if (lit) { set(7, 5, 0xff8060); set(8, 5, 0xff8060); }
    }

    private void lever() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, y < 6 ? scale(0x6b4a26, jitter(0.08)) : scale(0x8a8a8a, jitter(0.08)));
    }

    private void lamp(boolean on) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean frame = x == 0 || y == 0 || x == 15 || y == 15 || x == 7 || y == 7 || x == 8 || y == 8;
                int c = frame ? (on ? 0x8a6a3a : 0x4a3220) : on ? mix(0xffe8a0, 0xf8b040, r.nextDouble() * 0.5) : mix(0x6a4428, 0x8a5a34, r.nextDouble());
                set(x, y, c);
            }
    }

    private void repeater(boolean on) {
        speckle(0xa0a0a0, 0.03, 0x949494, 0.1);
        for (int y = 2; y < 14; y++) set(7, y, on ? 0xff2010 : 0x6a1a14);
        for (int y = 2; y < 14; y++) set(8, y, on ? 0xd01a10 : 0x5a1410);
    }

    private static final int TIE = 0x6b5030, IRON = 0xa8a8a8, IRON_D = 0x6a6a6a;

    /** Straight rail running top to bottom: wooden ties under two rails (iron, gold, or iron with a sensor plate). */
    private void rail(int kind, boolean on) {
        clearTile();
        for (int y = 1; y < 16; y += 4)
            for (int x = 1; x < 15; x++) {
                set(x, y, scale(TIE, jitter(0.08)));
                set(x, y + 1, scale(TIE, 0.75 * jitter(0.08)));
            }
        int light = kind == 1 ? 0xf0d040 : IRON, dark = kind == 1 ? 0xa07818 : IRON_D;
        for (int y = 0; y < 16; y++)
            for (int x : new int[]{2, 12}) {
                set(x, y, scale(light, jitter(0.04)));
                set(x + 1, y, scale(dark, jitter(0.04)));
            }
        if (kind == 1) {
            // Redstone strip between the rails: dark when unpowered, glowing when on
            for (int y = 0; y < 16; y++) for (int x = 7; x <= 8; x++) set(x, y, on ? scale(0xff3020, jitter(0.1)) : scale(0x5a2018, jitter(0.1)));
        } else if (kind == 2) {
            for (int y = 4; y < 12; y++) for (int x = 5; x < 11; x++) set(x, y, scale(y == 4 || x == 5 ? 0x9a9a9a : 0x7a7a7a, jitter(0.04)));
            int dot = on ? 0xff2010 : 0x6a1a14;
            set(7, 7, dot); set(8, 8, dot); set(7, 8, scale(dot, 0.8)); set(8, 7, scale(dot, 0.8));
        }
    }

    /** Curved rail joining the bottom (south) and right (east) edges. */
    private void railCorner() {
        clearTile();
        for (double a : new double[]{8, 30, 52, 74}) {
            double rad = Math.toRadians(a);
            for (double d = 1.5; d < 15.5; d += 0.25) {
                for (double w = -0.9; w <= 0.9; w += 0.3) {
                    double px = 15.5 - Math.cos(rad) * d - Math.sin(rad) * w, py = 15.5 - Math.sin(rad) * d + Math.cos(rad) * w;
                    set((int) px, (int) py, scale(TIE, (w > 0.3 ? 0.75 : 1) * jitter(0.05)));
                }
            }
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x + 0.5 - 16, y + 0.5 - 16);
                if ((d >= 2 && d < 3) || (d >= 12 && d < 13)) set(x, y, scale(IRON, jitter(0.04)));
                else if ((d >= 3 && d < 4) || (d >= 13 && d < 14)) set(x, y, scale(IRON_D, jitter(0.04)));
            }
    }

    private void pistonFace(boolean sticky) {
        planks(0xa2824e);
        for (int i = 0; i < 16; i++) { set(i, 0, 0x6a6a6a); set(i, 15, 0x6a6a6a); set(0, i, 0x6a6a6a); set(15, i, 0x6a6a6a); }
        if (sticky) for (int y = 3; y < 13; y++) for (int x = 3; x < 13; x++) set(x, y, scale(0x78c060, jitter(0.1)));
    }

    private void pistonSide() {
        cobble(false);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 16; x++) set(x, y, scale(0xa2824e, jitter(0.05)));
        for (int x = 0; x < 16; x++) set(x, 4, 0x6a6a6a);
    }

    private void pistonBottom(boolean inner) {
        cobble(false);
        int a = inner ? 6 : 5, b = inner ? 9 : 10;
        for (int y = a; y <= b; y++) for (int x = a; x <= b; x++) set(x, y, inner ? 0xa2824e : 0x3a3a3a);
    }

    private void netherWart(int stage) {
        clearTile();
        int h = 5 + stage * 3;
        for (int s = 0; s < 3; s++) {
            int x = 3 + s * 4;
            for (int y = 16 - h; y < 16; y++) set(x + ((y + s) % 3 == 0 ? 1 : 0), y, scale(0x8a1a1a, jitter(0.1)));
            int top = 16 - h;
            for (int dy = 0; dy < 2 + stage; dy++) for (int dx = -1; dx <= 1; dx++) set(x + dx, top + dy, scale(stage == 2 ? 0xb02020 : 0x8a1818, jitter(0.15)));
        }
    }

    private void mushroom(int cap, int dark, boolean spots) {
        clearTile();
        for (int y = 9; y < 15; y++) for (int x = 7; x < 9; x++) set(x, y, scale(0xe8dcc8, jitter(0.05)));
        for (int y = 4; y < 10; y++)
            for (int x = 3; x < 13; x++) {
                double d = Math.pow((x - 7.5) / 5, 2) + Math.pow((y - 9.5) / 5.5, 2);
                if (d < 1 && y < 10) set(x, y, scale(y == 9 ? dark : cap, jitter(0.08)));
            }
        if (spots) for (int[] p : new int[][]{{5, 6}, {9, 5}, {10, 8}, {7, 8}}) set(p[0], p[1], 0xf8f0f0);
    }

    private void brewingItem() {
        clearTile();
        for (int y = 1; y < 14; y++) set(8, y, 0xe8b030);
        for (int x = 3; x < 14; x++) { set(x, 13, 0x6a6a6a); set(x, 14, 0x4a4a4a); }
        for (int[] c : new int[][]{{4, 10}, {12, 10}, {8, 7}})
            for (int y = -2; y <= 2; y++) for (int x = -1; x <= 1; x++) set(c[0] + x, c[1] + y, y < 0 ? 0xc8d8f0 : 0xd070c0);
    }

    private void repeaterItem() {
        clearTile();
        for (int y = 10; y < 14; y++) for (int x = 1; x < 15; x++) set(x, y, scale(0xa0a0a0, jitter(0.05)));
        for (int t = 0; t < 2; t++) {
            int x = 4 + t * 7;
            for (int y = 4; y < 10; y++) set(x, y, 0x6b4a26);
            set(x, 3, 0xff2010); set(x, 4, 0xd01a10);
        }
    }

    // ------------------------------------------------------------------ enchanting

    private void enchTop() {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale((x + y) % 7 == 0 ? 0xa01818 : 0xc02424, jitter(0.06)));
        for (int[] c : new int[][]{{0, 0}, {14, 0}, {0, 14}, {14, 14}})
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) set(c[0] + x, c[1] + y, 0x5aece8);
        for (int i = 2; i < 14; i++) { set(i, 1, 0x8a1010); set(i, 14, 0x8a1010); set(1, i, 0x8a1010); set(14, i, 0x8a1010); }
    }

    private void enchSide() {
        obsidian();
        for (int y = 4; y < 7; y++) for (int x = 0; x < 16; x++) set(x, y, scale(0xc02424, jitter(0.06)));
        for (int x = 0; x < 16; x += 5) { set(x, 7, 0x5aece8); }
    }

    private void anvil(boolean top) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) set(x, y, scale(0x4a4a4a, jitter(0.08) * (top && (x == 0 || x == 15) ? 0.7 : 1)));
        if (top) for (int x = 3; x < 13; x++) set(x, 7, 0x3a3a3a);
    }
}
