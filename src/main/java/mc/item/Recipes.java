package mc.item;

import mc.world.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shaped and shapeless crafting recipes plus furnace smelting. */
public final class Recipes {
    private record Recipe(int w, int h, Item[][] options, Item result, int count, boolean shapeless) { }

    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static final Map<Item, ItemStack> SMELTING = new HashMap<>();

    private static final Item[] PLANKS = {Item.of(Block.PLANKS), Item.of(Block.SPRUCE_PLANKS), Item.of(Block.BIRCH_PLANKS)};
    private static final Item[] COALS = {Item.COAL, Item.CHARCOAL};

    /** Pattern rows use single characters; keys map a char to one item or an array of accepted items. */
    private static void shaped(Item result, int count, String[] rows, Object... keys) {
        Map<Character, Item[]> map = new HashMap<>();
        for (int i = 0; i < keys.length; i += 2) {
            Object v = keys[i + 1];
            map.put((Character) keys[i], v instanceof Item[] arr ? arr : v instanceof Block b ? new Item[]{Item.of(b)} : new Item[]{(Item) v});
        }
        int h = rows.length, w = rows[0].length();
        Item[][] opts = new Item[w * h][];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                char c = rows[y].charAt(x);
                opts[y * w + x] = c == ' ' ? null : map.get(c);
            }
        RECIPES.add(new Recipe(w, h, opts, result, count, false));
    }

    private static void shapeless(Item result, int count, Object... ingredients) {
        Item[][] opts = new Item[ingredients.length][];
        for (int i = 0; i < ingredients.length; i++) {
            Object v = ingredients[i];
            opts[i] = v instanceof Item[] arr ? arr : v instanceof Block b ? new Item[]{Item.of(b)} : new Item[]{(Item) v};
        }
        RECIPES.add(new Recipe(ingredients.length, 1, opts, result, count, true));
    }

    private static void smelt(Object input, Item output) {
        Item in = input instanceof Block b ? Item.of(b) : (Item) input;
        SMELTING.put(in, new ItemStack(output, 1));
    }

    static {
        shapeless(Item.of(Block.PLANKS), 4, Block.OAK_LOG);
        shapeless(Item.of(Block.SPRUCE_PLANKS), 4, Block.SPRUCE_LOG);
        shapeless(Item.of(Block.BIRCH_PLANKS), 4, Block.BIRCH_LOG);
        shaped(Item.STICK, 4, new String[]{"#", "#"}, '#', PLANKS);
        shaped(Item.of(Block.CRAFTING_TABLE), 1, new String[]{"##", "##"}, '#', PLANKS);
        shaped(Item.of(Block.TORCH), 4, new String[]{"C", "S"}, 'C', COALS, 'S', Item.STICK);
        shaped(Item.of(Block.FURNACE), 1, new String[]{"###", "# #", "###"}, '#', Block.COBBLESTONE);
        shaped(Item.of(Block.CHEST), 1, new String[]{"###", "# #", "###"}, '#', PLANKS);
        Object[][] mats = {{PLANKS, 0}, {new Item[]{Item.of(Block.COBBLESTONE)}, 1}, {new Item[]{Item.IRON_INGOT}, 2},
                {new Item[]{Item.GOLD_INGOT}, 3}, {new Item[]{Item.DIAMOND}, 4}};
        for (Object[] m : mats) {
            Item[] mat = (Item[]) m[0];
            int k = (int) m[1];
            shaped(Item.get(300 + k), 1, new String[]{"XXX", " S ", " S "}, 'X', mat, 'S', Item.STICK);
            shaped(Item.get(305 + k), 1, new String[]{"XX", "XS", " S"}, 'X', mat, 'S', Item.STICK);
            shaped(Item.get(310 + k), 1, new String[]{"X", "S", "S"}, 'X', mat, 'S', Item.STICK);
            shaped(Item.get(315 + k), 1, new String[]{"X", "X", "S"}, 'X', mat, 'S', Item.STICK);
            shaped(Item.get(320 + k), 1, new String[]{"XX", " S", " S"}, 'X', mat, 'S', Item.STICK);
        }
        Item[][] armorMats = {{Item.LEATHER}, null, {Item.IRON_INGOT}, {Item.GOLD_INGOT}, {Item.DIAMOND}};
        String[][] armorShapes = {{"XXX", "X X"}, {"X X", "XXX", "XXX"}, {"XXX", "X X", "X X"}, {"X X", "X X"}};
        for (int m = 0; m < 5; m++) {
            if (armorMats[m] == null) continue;
            for (int slot = 0; slot < 4; slot++) shaped(Item.armor(m, slot), 1, armorShapes[slot], 'X', armorMats[m]);
        }
        shaped(Item.SHEARS, 1, new String[]{" I", "I "}, 'I', Item.IRON_INGOT);
        shaped(Item.of(Block.CAKE), 1, new String[]{"MMM", "SES", "WWW"}, 'M', Item.MILK_BUCKET, 'S', Item.SUGAR, 'E', Item.EGG, 'W', Item.WHEAT);
        shapeless(Item.of(Block.BLACK_WOOL), 1, Block.WHITE_WOOL, Item.INK_SAC);
        shaped(Item.of(Block.REDSTONE_TORCH), 1, new String[]{"R", "S"}, 'R', Item.REDSTONE, 'S', Item.STICK);
        shaped(Item.of(Block.LEVER), 1, new String[]{"S", "C"}, 'S', Item.STICK, 'C', Block.COBBLESTONE);
        shapeless(Item.of(Block.STONE_BUTTON), 1, Block.STONE);
        shapeless(Item.of(Block.OAK_BUTTON), 1, PLANKS);
        shaped(Item.of(Block.STONE_PRESSURE_PLATE), 1, new String[]{"##"}, '#', Block.STONE);
        shaped(Item.of(Block.OAK_PRESSURE_PLATE), 1, new String[]{"##"}, '#', PLANKS);
        shaped(Item.of(Block.REDSTONE_LAMP), 1, new String[]{" R ", "RGR", " R "}, 'R', Item.REDSTONE, 'G', Block.GLOWSTONE);
        shaped(Item.of(Block.REPEATER), 1, new String[]{"TRT", "SSS"}, 'T', Block.REDSTONE_TORCH, 'R', Item.REDSTONE, 'S', Block.STONE);
        shaped(Item.of(Block.PISTON), 1, new String[]{"PPP", "CIC", "CRC"}, 'P', PLANKS, 'C', Block.COBBLESTONE, 'I', Item.IRON_INGOT, 'R', Item.REDSTONE);
        shaped(Item.of(Block.STICKY_PISTON), 1, new String[]{"S", "P"}, 'S', Item.SLIMEBALL, 'P', Block.PISTON);
        shaped(Item.of(Block.REDSTONE_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.REDSTONE);
        shapeless(Item.REDSTONE, 9, Block.REDSTONE_BLOCK);
        shaped(Item.of(Block.ENCHANTING_TABLE), 1, new String[]{" B ", "DOD", "OOO"}, 'B', Item.BOOK, 'D', Item.DIAMOND, 'O', Block.OBSIDIAN);
        shaped(Item.of(Block.ANVIL), 1, new String[]{"III", " i ", "iii"}, 'I', Block.IRON_BLOCK, 'i', Item.IRON_INGOT);
        shaped(Item.of(Block.LAPIS_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.LAPIS_LAZULI);
        shapeless(Item.LAPIS_LAZULI, 9, Block.LAPIS_BLOCK);
        shaped(Item.FISHING_ROD, 1, new String[]{"  S", " SX", "S X"}, 'S', Item.STICK, 'X', Item.STRING);
        shaped(Item.of(Block.RAIL), 16, new String[]{"I I", "ISI", "I I"}, 'I', Item.IRON_INGOT, 'S', Item.STICK);
        shaped(Item.of(Block.POWERED_RAIL), 6, new String[]{"G G", "GSG", "GRG"}, 'G', Item.GOLD_INGOT, 'S', Item.STICK, 'R', Item.REDSTONE);
        shaped(Item.of(Block.DETECTOR_RAIL), 6, new String[]{"I I", "IPI", "IRI"}, 'I', Item.IRON_INGOT, 'P', Block.STONE_PRESSURE_PLATE, 'R', Item.REDSTONE);
        shaped(Item.MINECART, 1, new String[]{"I I", "III"}, 'I', Item.IRON_INGOT);
        shaped(Item.BOAT, 1, new String[]{"P P", "PPP"}, 'P', PLANKS);
        shaped(Item.GLASS_BOTTLE, 3, new String[]{"# #", " # "}, '#', Block.GLASS);
        shaped(Item.BOWL, 4, new String[]{"# #", " # "}, '#', PLANKS);
        shaped(Item.of(Block.BREWING_STAND), 1, new String[]{" B ", "CCC"}, 'B', Item.BLAZE_ROD, 'C', Block.COBBLESTONE);
        shapeless(Item.BLAZE_POWDER, 2, Item.BLAZE_ROD);
        shapeless(Item.FERMENTED_SPIDER_EYE, 1, Item.SPIDER_EYE, Block.BROWN_MUSHROOM, Item.SUGAR);
        shaped(Item.GLISTERING_MELON, 1, new String[]{"NNN", "NMN", "NNN"}, 'N', Item.GOLD_NUGGET, 'M', Item.MELON_SLICE);
        shaped(Item.GOLDEN_CARROT, 1, new String[]{"NNN", "NCN", "NNN"}, 'N', Item.GOLD_NUGGET, 'C', Item.CARROT);
        shaped(Item.GOLDEN_APPLE, 1, new String[]{"III", "IAI", "III"}, 'I', Item.GOLD_INGOT, 'A', Item.APPLE);
        shapeless(Item.MUSHROOM_STEW, 1, Block.BROWN_MUSHROOM, Block.RED_MUSHROOM, Item.BOWL);
        shaped(Item.of(Block.MELON), 1, new String[]{"###", "###", "###"}, '#', Item.MELON_SLICE);
        shaped(Item.BREAD, 1, new String[]{"WWW"}, 'W', Item.WHEAT);
        shaped(Item.BUCKET, 1, new String[]{"I I", " I "}, 'I', Item.IRON_INGOT);
        shaped(Item.BOW, 1, new String[]{" TS", "T S", " TS"}, 'T', Item.STICK, 'S', Item.STRING);
        shaped(Item.ARROW, 4, new String[]{"F", "S", "E"}, 'F', Item.FLINT, 'S', Item.STICK, 'E', Item.FEATHER);
        shapeless(Item.FLINT_AND_STEEL, 1, Item.IRON_INGOT, Item.FLINT);
        shaped(Item.of(Block.STONE_BRICKS), 4, new String[]{"##", "##"}, '#', Block.STONE);
        shaped(Item.of(Block.SANDSTONE), 1, new String[]{"##", "##"}, '#', Block.SAND);
        shaped(Item.of(Block.BRICKS), 1, new String[]{"##", "##"}, '#', Item.BRICK);
        shaped(Item.of(Block.CLAY), 1, new String[]{"##", "##"}, '#', Item.CLAY_BALL);
        shaped(Item.of(Block.WHITE_WOOL), 1, new String[]{"##", "##"}, '#', Item.STRING);
        shaped(Item.of(Block.SNOW), 1, new String[]{"##", "##"}, '#', Item.SNOWBALL);
        shaped(Item.of(Block.SNOW_LAYER), 6, new String[]{"###"}, '#', Block.SNOW);
        Block[][] derived = {
                {Block.SMOOTH_STONE_SLAB, Block.STONE, null}, {Block.COBBLESTONE_SLAB, Block.COBBLESTONE, Block.COBBLESTONE_STAIRS},
                {Block.OAK_SLAB, Block.PLANKS, Block.OAK_STAIRS}, {Block.SPRUCE_SLAB, Block.SPRUCE_PLANKS, Block.SPRUCE_STAIRS},
                {Block.BIRCH_SLAB, Block.BIRCH_PLANKS, Block.BIRCH_STAIRS}, {Block.SANDSTONE_SLAB, Block.SANDSTONE, Block.SANDSTONE_STAIRS},
                {Block.BRICK_SLAB, Block.BRICKS, Block.BRICK_STAIRS}, {Block.STONE_BRICK_SLAB, Block.STONE_BRICKS, Block.STONE_BRICK_STAIRS}};
        for (Block[] d : derived) {
            shaped(Item.of(d[0]), 6, new String[]{"###"}, '#', d[1]);
            if (d[2] != null) shaped(Item.of(d[2]), 4, new String[]{"#  ", "## ", "###"}, '#', d[1]);
        }
        shaped(Item.of(Block.OAK_FENCE), 3, new String[]{"#S#", "#S#"}, '#', PLANKS, 'S', Item.STICK);
        shaped(Item.of(Block.OAK_FENCE_GATE), 1, new String[]{"S#S", "S#S"}, '#', PLANKS, 'S', Item.STICK);
        shaped(Item.of(Block.OAK_DOOR), 3, new String[]{"##", "##", "##"}, '#', PLANKS);
        shaped(Item.of(Block.IRON_DOOR), 3, new String[]{"##", "##", "##"}, '#', Item.IRON_INGOT);
        shaped(Item.of(Block.OAK_TRAPDOOR), 2, new String[]{"###", "###"}, '#', PLANKS);
        shaped(Item.of(Block.LADDER), 3, new String[]{"S S", "SSS", "S S"}, 'S', Item.STICK);
        shaped(Item.of(Block.GLASS_PANE), 16, new String[]{"###", "###"}, '#', Block.GLASS);
        shaped(Item.of(Block.IRON_BARS), 16, new String[]{"###", "###"}, '#', Item.IRON_INGOT);
        shaped(Item.of(Block.COBBLESTONE_WALL), 6, new String[]{"###", "###"}, '#', Block.COBBLESTONE);
        Item[] wool = {Item.of(Block.WHITE_WOOL), Item.of(Block.RED_WOOL), Item.of(Block.BLUE_WOOL), Item.of(Block.GREEN_WOOL),
                Item.of(Block.YELLOW_WOOL), Item.of(Block.BLACK_WOOL)};
        shaped(Item.of(Block.BED), 1, new String[]{"WWW", "PPP"}, 'W', wool, 'P', PLANKS);
        Block[] carpets = {Block.WHITE_CARPET, Block.RED_CARPET, Block.BLUE_CARPET, Block.GREEN_CARPET, Block.YELLOW_CARPET, Block.BLACK_CARPET};
        for (int i = 0; i < carpets.length; i++) shaped(Item.of(carpets[i]), 3, new String[]{"##"}, '#', wool[i]);
        shaped(Item.of(Block.TNT), 1, new String[]{"GSG", "SGS", "GSG"}, 'G', Item.GUNPOWDER, 'S', Block.SAND);
        shaped(Item.of(Block.GLOWSTONE), 1, new String[]{"GG", "GG"}, 'G', Item.GLOWSTONE_DUST);
        shaped(Item.of(Block.NETHER_BRICKS), 1, new String[]{"##", "##"}, '#', Item.NETHER_BRICK);
        shaped(Item.of(Block.QUARTZ_BLOCK), 1, new String[]{"##", "##"}, '#', Item.QUARTZ);
        shapeless(Item.GOLD_INGOT, 1, Item.GOLD_NUGGET, Item.GOLD_NUGGET, Item.GOLD_NUGGET, Item.GOLD_NUGGET, Item.GOLD_NUGGET,
                Item.GOLD_NUGGET, Item.GOLD_NUGGET, Item.GOLD_NUGGET, Item.GOLD_NUGGET);
        shapeless(Item.GOLD_NUGGET, 9, Item.GOLD_INGOT);
        shapeless(Item.FIRE_CHARGE, 3, Item.GUNPOWDER, Item.COAL, Item.GLOWSTONE_DUST);
        shapeless(Item.MAGMA_CREAM, 1, Item.SLIMEBALL, Item.GLOWSTONE_DUST);
        shaped(Item.PAPER, 3, new String[]{"###"}, '#', Block.SUGAR_CANE);
        shapeless(Item.SUGAR, 1, Block.SUGAR_CANE);
        shapeless(Item.BOOK, 1, Item.PAPER, Item.PAPER, Item.PAPER, Item.LEATHER);
        shaped(Item.of(Block.BOOKSHELF), 1, new String[]{"###", "BBB", "###"}, '#', PLANKS, 'B', Item.BOOK);
        shapeless(Item.BONE_MEAL, 3, Item.BONE);
        shaped(Item.of(Block.IRON_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.IRON_INGOT);
        shaped(Item.of(Block.GOLD_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.GOLD_INGOT);
        shaped(Item.of(Block.DIAMOND_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.DIAMOND);
        shaped(Item.of(Block.EMERALD_BLOCK), 1, new String[]{"###", "###", "###"}, '#', Item.EMERALD);
        shapeless(Item.EMERALD, 9, Block.EMERALD_BLOCK);
        shapeless(Item.IRON_INGOT, 9, Block.IRON_BLOCK);
        shapeless(Item.GOLD_INGOT, 9, Block.GOLD_BLOCK);
        shapeless(Item.DIAMOND, 9, Block.DIAMOND_BLOCK);

        shapeless(Item.of(Block.MOSSY_COBBLESTONE), 1, Block.COBBLESTONE, Block.TALL_GRASS);

        smelt(Block.IRON_ORE, Item.IRON_INGOT);
        smelt(Item.RAW_FISH, Item.COOKED_FISH);
        smelt(Item.RAW_SALMON, Item.COOKED_SALMON);
        smelt(Block.GOLD_ORE, Item.GOLD_INGOT);
        smelt(Block.DIAMOND_ORE, Item.DIAMOND);
        smelt(Block.EMERALD_ORE, Item.EMERALD);
        smelt(Block.REDSTONE_ORE, Item.REDSTONE);
        smelt(Block.LAPIS_ORE, Item.LAPIS_LAZULI);
        smelt(Block.COAL_ORE, Item.COAL);
        smelt(Block.SAND, Item.of(Block.GLASS));
        smelt(Block.COBBLESTONE, Item.of(Block.STONE));
        smelt(Block.CLAY, Item.of(Block.TERRACOTTA));
        smelt(Item.CLAY_BALL, Item.BRICK);
        smelt(Block.OAK_LOG, Item.CHARCOAL);
        smelt(Block.SPRUCE_LOG, Item.CHARCOAL);
        smelt(Block.BIRCH_LOG, Item.CHARCOAL);
        smelt(Item.RAW_PORKCHOP, Item.COOKED_PORKCHOP);
        smelt(Item.RAW_BEEF, Item.STEAK);
        smelt(Item.RAW_CHICKEN, Item.COOKED_CHICKEN);
        smelt(Block.CACTUS, Item.of(Block.GREEN_WOOL));
        smelt(Item.POTATO, Item.BAKED_POTATO);
        smelt(Block.NETHERRACK, Item.NETHER_BRICK);
        smelt(Block.NETHER_QUARTZ_ORE, Item.QUARTZ);
    }

    /** Finds the crafting result for a square grid (size 2 or 3), or null. */
    public static ItemStack match(ItemStack[] grid, int size) {
        int minX = size, minY = size, maxX = -1, maxY = -1;
        List<Item> present = new ArrayList<>();
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                ItemStack s = grid[y * size + x];
                if (!ItemStack.isEmpty(s)) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    present.add(s.item);
                }
            }
        if (maxX < 0) return null;
        int w = maxX - minX + 1, h = maxY - minY + 1;
        for (Recipe r : RECIPES) {
            if (r.shapeless) {
                if (matchesShapeless(r, present)) return new ItemStack(r.result, r.count);
                continue;
            }
            if (r.w != w || r.h != h) continue;
            for (int mirror = 0; mirror < 2; mirror++) {
                boolean ok = true;
                for (int y = 0; y < h && ok; y++)
                    for (int x = 0; x < w && ok; x++) {
                        int rx = mirror == 1 ? w - 1 - x : x;
                        Item[] opt = r.options[y * w + rx];
                        ItemStack s = grid[(y + minY) * size + x + minX];
                        if (opt == null) ok = ItemStack.isEmpty(s);
                        else ok = !ItemStack.isEmpty(s) && contains(opt, s.item);
                    }
                if (ok) return new ItemStack(r.result, r.count);
            }
        }
        return null;
    }

    private static boolean matchesShapeless(Recipe r, List<Item> present) {
        if (present.size() != r.options.length) return false;
        boolean[] used = new boolean[present.size()];
        for (Item[] opt : r.options) {
            boolean found = false;
            for (int i = 0; i < present.size(); i++) {
                if (!used[i] && contains(opt, present.get(i))) { used[i] = true; found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean contains(Item[] arr, Item it) {
        for (Item a : arr) if (a == it) return true;
        return false;
    }

    /** Experience per item smelted (Minecraft's values). */
    public static float smeltingXp(Item input) {
        if (input == Item.of(Block.DIAMOND_ORE) || input == Item.of(Block.GOLD_ORE)) return 1f;
        if (input == Item.of(Block.IRON_ORE)) return 0.7f;
        if (input == Item.RAW_BEEF || input == Item.RAW_PORKCHOP || input == Item.RAW_CHICKEN) return 0.35f;
        if (input == Item.CLAY_BALL || input == Item.of(Block.CLAY) || input == Item.of(Block.CACTUS)) return 0.3f;
        return 0.1f;
    }

    public static ItemStack smelting(Item input) {
        ItemStack s = SMELTING.get(input);
        return s == null ? null : s.copy();
    }
}
