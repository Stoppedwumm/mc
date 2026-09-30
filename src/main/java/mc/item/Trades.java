package mc.item;

import mc.world.Block;

import java.util.ArrayList;
import java.util.List;

/** Villager trade offers per profession, modelled on Minecraft's trade tables. */
public final class Trades {
    public record Offer(ItemStack cost, ItemStack cost2, ItemStack result) { }

    private Trades() { }

    private static ItemStack s(Item i, int n) { return new ItemStack(i, n); }

    private static ItemStack s(Block b, int n) { return new ItemStack(Item.of(b), n); }

    private static final Item E = Item.EMERALD;

    public static List<Offer> offers(int profession) {
        List<Offer> o = new ArrayList<>();
        switch (profession) {
            case 0 -> { // farmer
                o.add(new Offer(s(Item.WHEAT, 20), null, s(E, 1)));
                o.add(new Offer(s(Item.POTATO, 26), null, s(E, 1)));
                o.add(new Offer(s(Item.CARROT, 22), null, s(E, 1)));
                o.add(new Offer(s(E, 1), null, s(Item.BREAD, 6)));
                o.add(new Offer(s(E, 1), null, s(Item.APPLE, 4)));
                o.add(new Offer(s(E, 1), null, s(Block.CAKE, 1)));
            }
            case 1 -> { // librarian
                o.add(new Offer(s(Item.PAPER, 24), null, s(E, 1)));
                o.add(new Offer(s(Item.BOOK, 4), null, s(E, 1)));
                o.add(new Offer(s(E, 9), null, s(Block.BOOKSHELF, 1)));
                o.add(new Offer(s(E, 1), null, s(Block.GLASS, 4)));
                o.add(new Offer(s(E, 5), s(Item.BOOK, 1), s(Item.ENCHANTED_BOOK, 1)));
                o.add(new Offer(s(E, 1), null, s(Block.LADDER, 8)));
            }
            case 2 -> { // priest (cleric)
                o.add(new Offer(s(Item.ROTTEN_FLESH, 32), null, s(E, 1)));
                o.add(new Offer(s(Item.GOLD_INGOT, 3), null, s(E, 1)));
                o.add(new Offer(s(E, 1), null, s(Item.REDSTONE, 2)));
                o.add(new Offer(s(E, 4), null, s(Block.GLOWSTONE, 1)));
                o.add(new Offer(s(E, 5), null, s(Item.ENDER_PEARL, 1)));
                o.add(new Offer(s(E, 1), null, s(Item.EXPERIENCE_BOTTLE, 1)));
            }
            case 3 -> { // smith
                o.add(new Offer(s(Item.COAL, 15), null, s(E, 1)));
                o.add(new Offer(s(Item.IRON_INGOT, 4), null, s(E, 1)));
                o.add(new Offer(s(Item.DIAMOND, 1), null, s(E, 1)));
                o.add(new Offer(s(E, 5), null, s(Item.armor(2, 0), 1)));
                o.add(new Offer(s(E, 9), null, s(Item.armor(2, 1), 1)));
                o.add(new Offer(s(E, 7), null, s(Item.IRON_SWORD, 1)));
                o.add(new Offer(s(E, 13), null, s(Item.armor(4, 3), 1)));
                o.add(new Offer(s(E, 17), null, s(Item.DIAMOND_PICKAXE, 1)));
            }
            default -> { // butcher
                o.add(new Offer(s(Item.RAW_CHICKEN, 14), null, s(E, 1)));
                o.add(new Offer(s(Item.RAW_PORKCHOP, 7), null, s(E, 1)));
                o.add(new Offer(s(Item.RAW_BEEF, 10), null, s(E, 1)));
                o.add(new Offer(s(Item.COAL, 15), null, s(E, 1)));
                o.add(new Offer(s(E, 1), null, s(Item.COOKED_PORKCHOP, 5)));
                o.add(new Offer(s(E, 1), null, s(Item.COOKED_CHICKEN, 8)));
            }
        }
        return o;
    }

    public static boolean canAfford(Inventory inv, Offer o) {
        if (inv.count(o.cost.item) < o.cost.count) return false;
        return o.cost2 == null || inv.count(o.cost2.item) >= o.cost2.count;
    }

    /** Removes the costs and returns a copy of the result, or null if the player can't afford it. */
    public static ItemStack trade(Inventory inv, Offer o) {
        if (!canAfford(inv, o)) return null;
        inv.remove(o.cost.item, o.cost.count);
        if (o.cost2 != null) inv.remove(o.cost2.item, o.cost2.count);
        ItemStack out = o.result.copy();
        if (out.item == Item.ENCHANTED_BOOK && !out.isEnchanted()) {
            // Librarians sell a random enchantment at a random level
            java.util.Random r = new java.util.Random();
            Enchantment e = Enchantment.values()[r.nextInt(Enchantment.values().length)];
            out.enchant(e, 1 + r.nextInt(e.maxLevel));
        }
        return out;
    }
}
