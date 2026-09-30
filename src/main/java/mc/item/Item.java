package mc.item;

import mc.world.Block;

/**
 * Item registry. Ids 0-127 are the block items (same id as their block), ids 256+ are pure items.
 */
public final class Item {
    public enum Tool { NONE, PICKAXE, AXE, SHOVEL, SWORD, HOE }

    public static final Item[] BY_ID = new Item[512];

    public final int id;
    public final String name;
    /** Non-null for items that place a block. */
    public final Block block;
    /** Tile in the item texture atlas (for non-block items). */
    public int icon;
    public int maxStack = 64;
    public int maxDamage;
    public Tool tool = Tool.NONE;
    /** 0 wood, 1 stone, 2 iron, 3 diamond (gold mines like wood but fast). */
    public int tier;
    public float miningSpeed = 1;
    public float attackDamage = 1;
    public int food;
    public float saturation;
    public int fuelTicks;
    public boolean handheld;
    /** Armor: slot 0 helmet, 1 chestplate, 2 leggings, 3 boots (-1 = not armor). */
    public int armorSlot = -1;
    public int armorPoints;
    public float toughness;
    /** Armor material index (see ARMOR_MATERIALS) for rendering. */
    public int armorMaterial = -1;

    private Item(int id, String name, Block block, int icon) {
        this.id = id;
        this.name = name;
        this.block = block;
        this.icon = icon;
        BY_ID[id] = this;
    }

    public static Item get(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    public static Item of(Block b) {
        return BY_ID[b.id];
    }

    public boolean isBlock() { return block != null; }

    public boolean isFood() { return food > 0; }

    public boolean isTool() { return tool != Tool.NONE; }

    private Item stack(int n) { maxStack = n; return this; }
    private Item food(int f, float sat) { food = f; saturation = sat; return this; }
    private Item fuel(int t) { fuelTicks = t; return this; }

    private static Item item(int id, String name, int icon) { return new Item(id, name, null, icon); }

    private static Item tool(int id, String name, int icon, Tool tool, int tier, int durability, float speed, float damage) {
        Item i = new Item(id, name, null, icon);
        i.tool = tool;
        i.tier = tier;
        i.maxDamage = durability;
        i.maxStack = 1;
        i.miningSpeed = speed;
        i.attackDamage = damage;
        i.handheld = true;
        return i;
    }

    static {
        for (Block b : Block.BY_ID) {
            if (b != null && b != Block.AIR) new Item(b.id, b.name, b, -1);
        }
        BY_ID[Block.TORCH.id].fuelTicks = 0;
        for (Block b : new Block[]{Block.PLANKS, Block.SPRUCE_PLANKS, Block.BIRCH_PLANKS, Block.OAK_LOG, Block.SPRUCE_LOG,
                Block.BIRCH_LOG, Block.CRAFTING_TABLE, Block.BOOKSHELF, Block.CHEST, Block.OAK_STAIRS, Block.SPRUCE_STAIRS,
                Block.BIRCH_STAIRS, Block.OAK_FENCE, Block.OAK_FENCE_GATE, Block.OAK_TRAPDOOR, Block.LADDER}) BY_ID[b.id].fuelTicks = 300;
        for (Block b : new Block[]{Block.OAK_SLAB, Block.SPRUCE_SLAB, Block.BIRCH_SLAB, Block.OAK_DOOR}) BY_ID[b.id].fuelTicks = 150;
        for (Block b : Block.BY_ID) if (b != null && b.shape == Block.Shape.DOOR) BY_ID[b.id].maxStack = 16;
        BY_ID[Block.BED.id].maxStack = 1;
        BY_ID[Block.SAPLING.id].fuelTicks = 100;
    }

    // Icon tiles in the item atlas (see render.ItemTextureGen)
    public static final Item STICK = item(256, "Stick", 0).fuel(100);
    public static final Item COAL = item(257, "Coal", 1).fuel(1600);
    public static final Item CHARCOAL = item(258, "Charcoal", 2).fuel(1600);
    public static final Item IRON_INGOT = item(259, "Iron Ingot", 3);
    public static final Item GOLD_INGOT = item(260, "Gold Ingot", 4);
    public static final Item DIAMOND = item(261, "Diamond", 5);
    public static final Item FLINT = item(262, "Flint", 6);
    public static final Item STRING = item(263, "String", 7);
    public static final Item FEATHER = item(264, "Feather", 8);
    public static final Item GUNPOWDER = item(265, "Gunpowder", 9);
    public static final Item BONE = item(266, "Bone", 10);
    public static final Item BONE_MEAL = item(267, "Bone Meal", 11);
    public static final Item ARROW = item(268, "Arrow", 12);
    public static final Item BOW = tool(269, "Bow", 13, Tool.NONE, 0, 384, 1, 1);
    public static final Item LEATHER = item(270, "Leather", 14);
    public static final Item WHEAT = item(271, "Wheat", 15);
    public static final Item SEEDS = item(272, "Wheat Seeds", 16);
    public static final Item BREAD = item(273, "Bread", 17).food(5, 6f);
    public static final Item APPLE = item(274, "Apple", 18).food(4, 2.4f);
    public static final Item RAW_PORKCHOP = item(275, "Raw Porkchop", 19).food(3, 1.8f);
    public static final Item COOKED_PORKCHOP = item(276, "Cooked Porkchop", 20).food(8, 12.8f);
    public static final Item RAW_BEEF = item(277, "Raw Beef", 21).food(3, 1.8f);
    public static final Item STEAK = item(278, "Steak", 22).food(8, 12.8f);
    public static final Item RAW_CHICKEN = item(279, "Raw Chicken", 23).food(2, 1.2f);
    public static final Item COOKED_CHICKEN = item(280, "Cooked Chicken", 24).food(6, 7.2f);
    public static final Item ROTTEN_FLESH = item(281, "Rotten Flesh", 25).food(4, 0.8f);
    public static final Item BUCKET = item(282, "Bucket", 26).stack(16);
    public static final Item WATER_BUCKET = item(283, "Water Bucket", 27).stack(1);
    public static final Item LAVA_BUCKET = item(284, "Lava Bucket", 28).stack(1).fuel(20000);
    public static final Item FLINT_AND_STEEL = tool(285, "Flint and Steel", 29, Tool.NONE, 0, 64, 1, 1);
    public static final Item CLAY_BALL = item(286, "Clay Ball", 30);
    public static final Item BRICK = item(287, "Brick", 31);
    public static final Item EGG = item(288, "Egg", 32).stack(16);
    public static final Item SUGAR = item(289, "Sugar", 33);
    public static final Item PAPER = item(290, "Paper", 34);
    public static final Item BOOK = item(291, "Book", 35);
    public static final Item SNOWBALL = item(292, "Snowball", 36).stack(16);
    public static final Item SHEARS = tool(293, "Shears", 37, Tool.NONE, 0, 238, 1, 1);
    public static final Item MILK_BUCKET = item(294, "Milk Bucket", 38).stack(1);
    public static final Item ENDER_PEARL = item(295, "Ender Pearl", 39).stack(16);
    public static final Item SLIMEBALL = item(296, "Slimeball", 85);
    public static final Item INK_SAC = item(297, "Ink Sac", 86);
    public static final Item CARROT = item(298, "Carrot", 87).food(3, 3.6f);
    public static final Item POTATO = item(299, "Potato", 88).food(1, 0.6f);
    public static final Item BAKED_POTATO = item(325, "Baked Potato", 89).food(5, 6f);

    // Tools: icon = 40 + type*5 + material
    public static final Item WOODEN_PICKAXE = tool(300, "Wooden Pickaxe", 40, Tool.PICKAXE, 0, 59, 2, 2);
    public static final Item STONE_PICKAXE = tool(301, "Stone Pickaxe", 41, Tool.PICKAXE, 1, 131, 4, 3);
    public static final Item IRON_PICKAXE = tool(302, "Iron Pickaxe", 42, Tool.PICKAXE, 2, 250, 6, 4);
    public static final Item GOLDEN_PICKAXE = tool(303, "Golden Pickaxe", 43, Tool.PICKAXE, 0, 32, 12, 2);
    public static final Item DIAMOND_PICKAXE = tool(304, "Diamond Pickaxe", 44, Tool.PICKAXE, 3, 1561, 8, 5);
    public static final Item WOODEN_AXE = tool(305, "Wooden Axe", 45, Tool.AXE, 0, 59, 2, 7);
    public static final Item STONE_AXE = tool(306, "Stone Axe", 46, Tool.AXE, 1, 131, 4, 9);
    public static final Item IRON_AXE = tool(307, "Iron Axe", 47, Tool.AXE, 2, 250, 6, 9);
    public static final Item GOLDEN_AXE = tool(308, "Golden Axe", 48, Tool.AXE, 0, 32, 12, 7);
    public static final Item DIAMOND_AXE = tool(309, "Diamond Axe", 49, Tool.AXE, 3, 1561, 8, 9);
    public static final Item WOODEN_SHOVEL = tool(310, "Wooden Shovel", 50, Tool.SHOVEL, 0, 59, 2, 2.5f);
    public static final Item STONE_SHOVEL = tool(311, "Stone Shovel", 51, Tool.SHOVEL, 1, 131, 4, 3.5f);
    public static final Item IRON_SHOVEL = tool(312, "Iron Shovel", 52, Tool.SHOVEL, 2, 250, 6, 4.5f);
    public static final Item GOLDEN_SHOVEL = tool(313, "Golden Shovel", 53, Tool.SHOVEL, 0, 32, 12, 2.5f);
    public static final Item DIAMOND_SHOVEL = tool(314, "Diamond Shovel", 54, Tool.SHOVEL, 3, 1561, 8, 5.5f);
    public static final Item WOODEN_SWORD = tool(315, "Wooden Sword", 55, Tool.SWORD, 0, 59, 1.5f, 4);
    public static final Item STONE_SWORD = tool(316, "Stone Sword", 56, Tool.SWORD, 1, 131, 1.5f, 5);
    public static final Item IRON_SWORD = tool(317, "Iron Sword", 57, Tool.SWORD, 2, 250, 1.5f, 6);
    public static final Item GOLDEN_SWORD = tool(318, "Golden Sword", 58, Tool.SWORD, 0, 32, 1.5f, 4);
    public static final Item DIAMOND_SWORD = tool(319, "Diamond Sword", 59, Tool.SWORD, 3, 1561, 1.5f, 7);
    public static final Item WOODEN_HOE = tool(320, "Wooden Hoe", 60, Tool.HOE, 0, 59, 1, 1);
    public static final Item STONE_HOE = tool(321, "Stone Hoe", 61, Tool.HOE, 1, 131, 1, 1);
    public static final Item IRON_HOE = tool(322, "Iron Hoe", 62, Tool.HOE, 2, 250, 1, 1);
    public static final Item GOLDEN_HOE = tool(323, "Golden Hoe", 63, Tool.HOE, 0, 32, 1, 1);
    public static final Item DIAMOND_HOE = tool(324, "Diamond Hoe", 64, Tool.HOE, 3, 1561, 1, 1);

    // Armor: id = 330 + material * 4 + slot, icon = 65 + material * 4 + slot
    public static final String[] ARMOR_MATERIALS = {"Leather", "Chainmail", "Iron", "Golden", "Diamond"};
    private static final int[][] ARMOR_POINTS = {{1, 3, 2, 1}, {2, 5, 4, 1}, {2, 6, 5, 2}, {2, 5, 3, 1}, {3, 8, 6, 3}};
    private static final int[] ARMOR_DURABILITY = {5, 15, 15, 7, 33}, SLOT_DURABILITY = {11, 16, 15, 13};
    private static final String[] ARMOR_PIECES = {"Helmet", "Chestplate", "Leggings", "Boots"};

    static {
        for (int m = 0; m < 5; m++)
            for (int s = 0; s < 4; s++) {
                String name = ARMOR_MATERIALS[m] + " " + (m == 0 && s == 0 ? "Cap" : m == 0 && s == 1 ? "Tunic" : m == 0 && s == 2 ? "Pants" : ARMOR_PIECES[s]);
                Item i = new Item(330 + m * 4 + s, name, null, 65 + m * 4 + s);
                i.maxStack = 1;
                i.armorSlot = s;
                i.armorMaterial = m;
                i.armorPoints = ARMOR_POINTS[m][s];
                i.toughness = m == 4 ? 2 : 0;
                i.maxDamage = ARMOR_DURABILITY[m] * SLOT_DURABILITY[s];
            }
    }

    public static Item armor(int material, int slot) { return BY_ID[330 + material * 4 + slot]; }

    public boolean isArmor() { return armorSlot >= 0; }

    static {
        for (int i = 300; i <= 324; i++) if (BY_ID[i].tier == 0 && !BY_ID[i].name.startsWith("Golden")) BY_ID[i].fuelTicks = 200;
        STICK.handheld = true;
        SHEARS.handheld = false;
        BY_ID[Block.CAKE.id].maxStack = 1;
        BONE.handheld = true;
    }

    /** Which tool type mines a block efficiently. */
    public static Tool effectiveTool(Block b) {
        if (b == Block.STONE || b == Block.COBBLESTONE || b == Block.MOSSY_COBBLESTONE || b == Block.SANDSTONE || b == Block.BRICKS
                || b == Block.STONE_BRICKS || b == Block.FURNACE || b == Block.LIT_FURNACE || b == Block.OBSIDIAN || b == Block.GRANITE
                || b == Block.DIORITE || b == Block.ANDESITE || b == Block.TERRACOTTA || b == Block.ICE || b.name.endsWith("Ore"))
            return Tool.PICKAXE;
        if (b.base != null && b.shape != Block.Shape.CARPET) return effectiveTool(b.base);
        if (b == Block.IRON_DOOR || b == Block.IRON_BARS) return Tool.PICKAXE;
        if (b.sound == Block.SoundType.WOOD) return Tool.AXE;
        if (b == Block.DIRT || b == Block.GRASS || b == Block.SAND || b == Block.GRAVEL || b == Block.CLAY || b == Block.SNOW
                || b == Block.SNOWY_GRASS || b == Block.COARSE_DIRT || b == Block.FARMLAND || b == Block.SNOW_LAYER) return Tool.SHOVEL;
        return Tool.NONE;
    }

    /** Minimum pickaxe tier needed for the block to drop anything (-1 = no tool needed). */
    public static int requiredTier(Block b) {
        if (b == Block.OBSIDIAN) return 3;
        if (b == Block.DIAMOND_ORE || b == Block.GOLD_ORE) return 2;
        if (b == Block.IRON_ORE) return 1;
        if (effectiveTool(b) == Tool.PICKAXE) return 0;
        return -1;
    }
}
