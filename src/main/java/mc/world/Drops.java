package mc.world;

import mc.item.Item;
import mc.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** What a block drops when broken, depending on the tool used. */
public final class Drops {
    private static final Random RANDOM = new Random();

    public static List<ItemStack> of(Block b, int meta, ItemStack tool) {
        List<ItemStack> out = new ArrayList<>();
        Item t = tool == null ? null : tool.item;
        // Shears harvest leaves and cobweb-like plants as themselves
        if (t == Item.SHEARS && (b == Block.OAK_LEAVES || b == Block.BIRCH_LEAVES || b == Block.SPRUCE_LEAVES || b == Block.TALL_GRASS || b == Block.FERN || b == Block.DEAD_BUSH)) {
            add(out, Item.of(b), 1);
            return out;
        }
        int required = Item.requiredTier(b);
        if (required >= 0 && (t == null || t.tool != Item.Tool.PICKAXE || t.tier < required)) return out;
        switch (b.id) {
            case 0, 7, 40, 6 -> { }
            case 1 -> add(out, Item.of(Block.COBBLESTONE), 1);
            case 2, 21, 62 -> add(out, Item.of(Block.DIRT), 1);
            case 12 -> add(out, Item.COAL, 1);
            case 25 -> add(out, Item.DIAMOND, 1);
            case 15, 23, 89 -> { }
            case 14 -> {
                if (RANDOM.nextInt(20) == 0) add(out, Item.of(Block.SAPLING), 1);
                if (RANDOM.nextInt(200) == 0) add(out, Item.APPLE, 1);
            }
            case 27, 29 -> { if (RANDOM.nextInt(20) == 0) add(out, Item.of(Block.SAPLING), 1); }
            case 17, 47 -> { if (RANDOM.nextInt(8) == 0) add(out, Item.SEEDS, 1); }
            case 9 -> add(out, RANDOM.nextInt(10) == 0 ? Item.FLINT : Item.of(Block.GRAVEL), 1);
            case 32 -> add(out, Item.CLAY_BALL, 4);
            case 38 -> add(out, Item.STICK, RANDOM.nextInt(3));
            case 35 -> add(out, Item.BOOK, 3);
            case 60 -> add(out, Item.of(Block.FURNACE), 1);
            case 100 -> add(out, Item.CARROT, (meta & 7) >= 7 ? 1 + RANDOM.nextInt(4) : 1);
            case 101 -> {
                add(out, Item.POTATO, (meta & 7) >= 7 ? 1 + RANDOM.nextInt(4) : 1);
            }
            case 102, 105 -> { }
            case 103 -> add(out, Item.EMERALD, 1);
            case 106, 110 -> { }
            case 109 -> add(out, Item.QUARTZ, 1);
            case 30 -> add(out, Item.GLOWSTONE_DUST, 2 + RANDOM.nextInt(3));
            case 63 -> {
                if ((meta & 7) >= 7) {
                    add(out, Item.WHEAT, 1);
                    add(out, Item.SEEDS, 1 + RANDOM.nextInt(3));
                } else add(out, Item.SEEDS, 1);
            }
            default -> {
                if (b.isSlab() && (meta & 3) == 2) add(out, Item.get(b.id), 2);
                else if (b.shape == Block.Shape.SNOW_LAYER) add(out, Item.SNOWBALL, (meta & 7) + 1);
                else if (b == Block.SNOW) add(out, Item.SNOWBALL, 4);
                else add(out, Item.get(b.id), 1);
            }
        }
        return out;
    }

    private static void add(List<ItemStack> out, Item item, int n) {
        if (item != null && n > 0) out.add(new ItemStack(item, n));
    }
}
