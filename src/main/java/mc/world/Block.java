package mc.world;

/**
 * Static block registry. Block ids are stored as unsigned bytes inside chunks, so there can be at most 256 blocks.
 */
public final class Block {
    public enum Model { NONE, CUBE, CROSS, LIQUID, TORCH, SHAPE }
    /** Non-cube geometry, see {@link Shapes}. */
    public enum Shape { NONE, SLAB, STAIRS, FENCE, GATE, DOOR, TRAPDOOR, LADDER, PANE, BED, WALL, CARPET, SNOW_LAYER, CAKE, PORTAL, WIRE, LEVER, BUTTON, PLATE, REPEATER, PISTON, PISTON_HEAD, TABLE, ANVIL, BREWING, RAIL }
    public enum Layer { OPAQUE, CUTOUT, TRANSLUCENT }
    public enum Tint { NONE, GRASS, FOLIAGE, BIRCH, SPRUCE }
    public enum SoundType { STONE, WOOD, GRASS, GRAVEL, SAND, GLASS, CLOTH, SNOW, NONE }

    public static final Block[] BY_ID = new Block[256];

    public final int id;
    public final String name;
    public Model model = Model.CUBE;
    public Layer layer = Layer.OPAQUE;
    public Tint tint = Tint.NONE;
    public SoundType sound = SoundType.STONE;
    /** Texture tiles: top, bottom, side (north/south/west/east use side). */
    public int texTop, texBottom, texSide;
    /** Tint only the top face (grass block); otherwise tint all faces. */
    public boolean tintTopOnly;
    /** Fully opaque cube: hides neighbouring faces and blocks light. */
    public boolean opaque = true;
    /** Has a collision box. */
    public boolean solid = true;
    /** Extra light reduction for non-opaque blocks (leaves, water). */
    public int lightFilter = 0;
    public int lightEmission = 0;
    /** Can be replaced by placing a block into it (air, water, plants). */
    public boolean replaceable = false;
    public boolean inCreativeInventory = true;
    public float hardness = 1f;
    public Shape shape = Shape.NONE;
    /** Block whose textures, sound and hardness a slab/stairs copies. */
    public Block base;
    /** Block-atlas tile used as a flat item sprite (-1: texSide), for doors, beds... */
    public int itemTex = -1;
    /** Ladders: entities can climb. */
    public boolean climbable;
    /** Flowing liquid destroys it (torches, carpet, snow layers). */
    public boolean washable;
    /** Fire spreads to and burns this block. */
    public boolean flammable;

    // ------------------------------------------------------------------ blocks added by mods

    /** Namespaced id ("minecraft:stone", "jans_mod:ruby_block"). */
    public String key;
    /** Mod blocks: texture of each face per block state (metadata), faces in mesh order (up, down, north, south, west, east). */
    public int[][] stateFaceTex;
    /** Tool that mines it efficiently and the pickaxe tier it needs to drop (overrides for mod blocks). */
    public mc.item.Item.Tool toolOverride;
    public int requiredTierOverride = Integer.MIN_VALUE;
    /** Fixed drop from a loot table (null = the block itself); a zero count drops nothing. */
    public mc.item.Item dropItem;
    public int dropCount = 1;
    /** Added by a mod: interaction, placement and removal go through World.modHooks. */
    public boolean modded;

    /** Registers a block from a mod in a free id. */
    public static Block register(int id, String name, String key, int tex) {
        if (BY_ID[id] != null) throw new IllegalStateException("Block id " + id + " is taken by " + BY_ID[id].name);
        Block b = new Block(id, name, tex);
        b.key = key;
        b.modded = true;
        return b;
    }

    /** First unused block id, or -1. */
    public static int freeId() {
        for (int i = 1; i < 256; i++) if (BY_ID[i] == null) return i;
        return -1;
    }

    /** Default namespaced id of a built-in block, derived from its name like Minecraft's ("Block of Gold" -> gold_block). */
    public static String vanillaKey(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.startsWith("block of ")) n = n.substring(9) + " block";
        if (n.equals("planks")) n = "oak planks";
        if (n.equals("log")) n = "oak log";
        return "minecraft:" + n.replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    public String key() { return key != null ? key : (key = vanillaKey(name)); }

    public static Block byKey(String key) {
        for (Block b : BY_ID) if (b != null && b.key().equals(key)) return b;
        return null;
    }

    private Block(int id, String name, int tex) {
        this.id = id;
        this.name = name;
        this.texTop = this.texBottom = this.texSide = tex;
        BY_ID[id] = this;
    }

    private Block tex(int top, int bottom, int side) { texTop = top; texBottom = bottom; texSide = side; return this; }
    private Block sound(SoundType s) { sound = s; return this; }
    private Block hardness(float h) { hardness = h; return this; }
    private Block tint(Tint t) { tint = t; return this; }
    private Block light(int l) { lightEmission = l; return this; }
    private Block cutout() { layer = Layer.CUTOUT; opaque = false; return this; }
    private Block translucent() { layer = Layer.TRANSLUCENT; opaque = false; return this; }
    private Block shape(Shape s) { model = Model.SHAPE; shape = s; opaque = false; return this; }
    private Block itemTex(int t) { itemTex = t; return this; }

    private static Block derived(int id, String name, Block base, Shape s) {
        Block b = new Block(id, name, 0).tex(base.texTop, base.texBottom, base.texSide).shape(s);
        b.base = base;
        b.sound = base.sound;
        b.hardness = base.hardness;
        return b;
    }

    private Block plant() { model = Model.CROSS; layer = Layer.CUTOUT; opaque = false; solid = false; replaceable = true; sound = SoundType.GRASS; hardness = 0; return this; }

    // Texture tile indices in the generated atlas (see render.TextureGen)
    public static final class Tex {
        public static final int STONE = 0, DIRT = 1, GRASS_TOP = 2, GRASS_SIDE = 3, COBBLE = 4, PLANKS = 5, BEDROCK = 6,
                WATER = 7, SAND = 8, GRAVEL = 9, GOLD_ORE = 10, IRON_ORE = 11, COAL_ORE = 12, LOG_SIDE = 13, LOG_TOP = 14,
                LEAVES = 15, GLASS = 16, SANDSTONE_SIDE = 17, SANDSTONE_TOP = 18, SANDSTONE_BOTTOM = 19, TALL_GRASS = 20,
                DANDELION = 21, POPPY = 22, BRICKS = 23, SNOW = 24, SNOWY_GRASS_SIDE = 25, ICE = 26, CACTUS_SIDE = 27,
                CACTUS_TOP = 28, DIAMOND_ORE = 29, SPRUCE_LOG = 30, SPRUCE_LEAVES = 31, BIRCH_LOG = 32, BIRCH_LEAVES = 33,
                GLOWSTONE = 34, TORCH = 35, CLAY = 36, MOSSY_COBBLE = 37, OBSIDIAN = 38, BOOKSHELF = 39, WOOL = 40,
                STONE_BRICKS = 41, DEAD_BUSH = 42, SPRUCE_PLANKS = 43, LAVA = 44, BIRCH_PLANKS = 45, CRAFTING_TOP = 46,
                CRAFTING_SIDE = 47, FURNACE_FRONT = 48, FURNACE_SIDE = 49, FURNACE_TOP = 50, PUMPKIN_TOP = 51,
                PUMPKIN_SIDE = 52, TNT_SIDE = 53, TNT_TOP = 54, TNT_BOTTOM = 55, BLUE_ORCHID = 56, FERN = 57,
                SPRUCE_LOG_TOP = 58, BIRCH_LOG_TOP = 59, MELON_SIDE = 60, MELON_TOP = 61, RED_WOOL = 62, BLUE_WOOL = 63,
                GREEN_WOOL = 64, YELLOW_WOOL = 65, BLACK_WOOL = 66, SUGAR_CANE = 67, COARSE_DIRT = 68, GRANITE = 69,
                DIORITE = 70, ANDESITE = 71, TERRACOTTA = 72, GRASS_SIDE_ITEM = 73, GRASS_TOP_ITEM = 74,
                FURNACE_FRONT_LIT = 75, CHEST_TOP = 76, CHEST_SIDE = 77, CHEST_FRONT = 78, FARMLAND = 79, FARMLAND_WET = 80,
                WHEAT_0 = 81, SAPLING = 89, IRON_BLOCK = 90, GOLD_BLOCK = 91, DIAMOND_BLOCK = 92,
                OAK_DOOR_TOP = 93, OAK_DOOR_BOTTOM = 94, IRON_DOOR_TOP = 95, IRON_DOOR_BOTTOM = 96, TRAPDOOR = 97, LADDER = 98,
                IRON_BARS = 99, BED_HEAD = 100, BED_FOOT = 101, BED_SIDE = 102, OAK_DOOR_ITEM = 103, IRON_DOOR_ITEM = 104,
                BED_ITEM = 105, SMOOTH_STONE = 106, GLASS_PANE_TOP = 107, CARROTS_0 = 108, POTATOES_0 = 112,
                CAKE_TOP = 116, CAKE_SIDE = 117, CAKE_INNER = 118, CAKE_BOTTOM = 119, CAKE_ITEM = 120,
                EMERALD_ORE = 121, EMERALD_BLOCK = 122, SPAWNER = 123, GRAVEL_PATH = 124,
                NETHERRACK = 125, SOUL_SAND = 126, QUARTZ_ORE = 127, PORTAL = 128, FIRE = 129, NETHER_BRICKS = 130,
                QUARTZ_SIDE = 131, QUARTZ_TOP = 132, REDSTONE_DUST = 133, REDSTONE_TORCH_ON = 134, REDSTONE_TORCH_OFF = 135,
                LEVER = 136, LAMP_OFF = 137, LAMP_ON = 138, REPEATER_OFF = 139, REPEATER_ON = 140, PISTON_TOP = 141,
                PISTON_STICKY = 142, PISTON_SIDE = 143, PISTON_BOTTOM = 144, PISTON_INNER = 145, REDSTONE_ORE = 146,
                REDSTONE_BLOCK = 147, REPEATER_ITEM = 148, REDSTONE_DUST_LINE = 149, ENCH_TOP = 150, ENCH_SIDE = 151,
                ENCH_BOTTOM = 152, ANVIL_TOP = 153, ANVIL_SIDE = 154, LAPIS_ORE = 155, LAPIS_BLOCK = 156,
                BREWING_BASE = 157, BREWING_ROD = 158, NETHER_WART_0 = 159, BROWN_MUSHROOM = 162, RED_MUSHROOM = 163,
                BREWING_ITEM = 164, RAIL = 165, RAIL_CORNER = 166, POWERED_RAIL = 167, POWERED_RAIL_ON = 168,
                DETECTOR_RAIL = 169, DETECTOR_RAIL_ON = 170,
                BREAK_0 = 240; // 240..249 crack stages
        public static final int COUNT = 171;
    }

    public static final Block AIR = new Block(0, "Air", 0);
    public static final Block STONE = new Block(1, "Stone", Tex.STONE).hardness(1.5f);
    public static final Block GRASS = new Block(2, "Grass Block", 0).tex(Tex.GRASS_TOP, Tex.DIRT, Tex.GRASS_SIDE).tint(Tint.GRASS).sound(SoundType.GRASS).hardness(0.6f);
    public static final Block DIRT = new Block(3, "Dirt", Tex.DIRT).sound(SoundType.GRAVEL).hardness(0.5f);
    public static final Block COBBLESTONE = new Block(4, "Cobblestone", Tex.COBBLE).hardness(2f);
    public static final Block PLANKS = new Block(5, "Oak Planks", Tex.PLANKS).sound(SoundType.WOOD).hardness(2f);
    public static final Block BEDROCK = new Block(6, "Bedrock", Tex.BEDROCK).hardness(-1);
    public static final Block WATER = new Block(7, "Water", Tex.WATER).translucent();
    public static final Block SAND = new Block(8, "Sand", Tex.SAND).sound(SoundType.SAND).hardness(0.5f);
    public static final Block GRAVEL = new Block(9, "Gravel", Tex.GRAVEL).sound(SoundType.GRAVEL).hardness(0.6f);
    public static final Block GOLD_ORE = new Block(10, "Gold Ore", Tex.GOLD_ORE).hardness(3f);
    public static final Block IRON_ORE = new Block(11, "Iron Ore", Tex.IRON_ORE).hardness(3f);
    public static final Block COAL_ORE = new Block(12, "Coal Ore", Tex.COAL_ORE).hardness(3f);
    public static final Block OAK_LOG = new Block(13, "Oak Log", 0).tex(Tex.LOG_TOP, Tex.LOG_TOP, Tex.LOG_SIDE).sound(SoundType.WOOD).hardness(2f);
    public static final Block OAK_LEAVES = new Block(14, "Oak Leaves", Tex.LEAVES).cutout().tint(Tint.FOLIAGE).sound(SoundType.GRASS).hardness(0.2f);
    public static final Block GLASS = new Block(15, "Glass", Tex.GLASS).cutout().sound(SoundType.GLASS).hardness(0.3f);
    public static final Block SANDSTONE = new Block(16, "Sandstone", 0).tex(Tex.SANDSTONE_TOP, Tex.SANDSTONE_BOTTOM, Tex.SANDSTONE_SIDE).hardness(0.8f);
    public static final Block TALL_GRASS = new Block(17, "Grass", Tex.TALL_GRASS).plant().tint(Tint.GRASS);
    public static final Block DANDELION = new Block(18, "Dandelion", Tex.DANDELION).plant();
    public static final Block POPPY = new Block(19, "Poppy", Tex.POPPY).plant();
    public static final Block BRICKS = new Block(20, "Bricks", Tex.BRICKS).hardness(2f);
    public static final Block SNOWY_GRASS = new Block(21, "Snowy Grass Block", 0).tex(Tex.SNOW, Tex.DIRT, Tex.SNOWY_GRASS_SIDE).sound(SoundType.SNOW).hardness(0.6f);
    public static final Block SNOW = new Block(22, "Snow Block", Tex.SNOW).sound(SoundType.SNOW).hardness(0.2f);
    public static final Block ICE = new Block(23, "Ice", Tex.ICE).translucent().sound(SoundType.GLASS).hardness(0.5f);
    public static final Block CACTUS = new Block(24, "Cactus", 0).tex(Tex.CACTUS_TOP, Tex.CACTUS_TOP, Tex.CACTUS_SIDE).cutout().sound(SoundType.CLOTH).hardness(0.4f);
    public static final Block DIAMOND_ORE = new Block(25, "Diamond Ore", Tex.DIAMOND_ORE).hardness(3f);
    public static final Block SPRUCE_LOG = new Block(26, "Spruce Log", 0).tex(Tex.SPRUCE_LOG_TOP, Tex.SPRUCE_LOG_TOP, Tex.SPRUCE_LOG).sound(SoundType.WOOD).hardness(2f);
    public static final Block SPRUCE_LEAVES = new Block(27, "Spruce Leaves", Tex.SPRUCE_LEAVES).cutout().tint(Tint.SPRUCE).sound(SoundType.GRASS).hardness(0.2f);
    public static final Block BIRCH_LOG = new Block(28, "Birch Log", 0).tex(Tex.BIRCH_LOG_TOP, Tex.BIRCH_LOG_TOP, Tex.BIRCH_LOG).sound(SoundType.WOOD).hardness(2f);
    public static final Block BIRCH_LEAVES = new Block(29, "Birch Leaves", Tex.BIRCH_LEAVES).cutout().tint(Tint.BIRCH).sound(SoundType.GRASS).hardness(0.2f);
    public static final Block GLOWSTONE = new Block(30, "Glowstone", Tex.GLOWSTONE).light(15).sound(SoundType.GLASS).hardness(0.3f);
    public static final Block TORCH = new Block(31, "Torch", Tex.TORCH).light(14);
    public static final Block CLAY = new Block(32, "Clay", Tex.CLAY).sound(SoundType.GRAVEL).hardness(0.6f);
    public static final Block MOSSY_COBBLESTONE = new Block(33, "Mossy Cobblestone", Tex.MOSSY_COBBLE).hardness(2f);
    public static final Block OBSIDIAN = new Block(34, "Obsidian", Tex.OBSIDIAN).hardness(50f);
    public static final Block BOOKSHELF = new Block(35, "Bookshelf", 0).tex(Tex.PLANKS, Tex.PLANKS, Tex.BOOKSHELF).sound(SoundType.WOOD).hardness(1.5f);
    public static final Block WHITE_WOOL = new Block(36, "White Wool", Tex.WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block STONE_BRICKS = new Block(37, "Stone Bricks", Tex.STONE_BRICKS).hardness(1.5f);
    public static final Block DEAD_BUSH = new Block(38, "Dead Bush", Tex.DEAD_BUSH).plant();
    public static final Block SPRUCE_PLANKS = new Block(39, "Spruce Planks", Tex.SPRUCE_PLANKS).sound(SoundType.WOOD).hardness(2f);
    public static final Block LAVA = new Block(40, "Lava", Tex.LAVA).light(15);
    public static final Block BIRCH_PLANKS = new Block(41, "Birch Planks", Tex.BIRCH_PLANKS).sound(SoundType.WOOD).hardness(2f);
    public static final Block CRAFTING_TABLE = new Block(42, "Crafting Table", 0).tex(Tex.CRAFTING_TOP, Tex.PLANKS, Tex.CRAFTING_SIDE).sound(SoundType.WOOD).hardness(2.5f);
    public static final Block FURNACE = new Block(43, "Furnace", 0).tex(Tex.FURNACE_TOP, Tex.FURNACE_TOP, Tex.FURNACE_SIDE).hardness(3.5f);
    public static final Block PUMPKIN = new Block(44, "Pumpkin", 0).tex(Tex.PUMPKIN_TOP, Tex.PUMPKIN_TOP, Tex.PUMPKIN_SIDE).sound(SoundType.WOOD).hardness(1f);
    public static final Block TNT = new Block(45, "TNT", 0).tex(Tex.TNT_TOP, Tex.TNT_BOTTOM, Tex.TNT_SIDE).sound(SoundType.GRASS).hardness(0f);
    public static final Block BLUE_ORCHID = new Block(46, "Blue Orchid", Tex.BLUE_ORCHID).plant();
    public static final Block FERN = new Block(47, "Fern", Tex.FERN).plant().tint(Tint.GRASS);
    public static final Block MELON = new Block(48, "Melon", 0).tex(Tex.MELON_TOP, Tex.MELON_TOP, Tex.MELON_SIDE).sound(SoundType.WOOD).hardness(1f);
    public static final Block RED_WOOL = new Block(49, "Red Wool", Tex.RED_WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block BLUE_WOOL = new Block(50, "Blue Wool", Tex.BLUE_WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block GREEN_WOOL = new Block(51, "Green Wool", Tex.GREEN_WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block YELLOW_WOOL = new Block(52, "Yellow Wool", Tex.YELLOW_WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block BLACK_WOOL = new Block(53, "Black Wool", Tex.BLACK_WOOL).sound(SoundType.CLOTH).hardness(0.8f);
    public static final Block SUGAR_CANE = new Block(54, "Sugar Cane", Tex.SUGAR_CANE).plant().tint(Tint.GRASS);
    public static final Block COARSE_DIRT = new Block(55, "Coarse Dirt", Tex.COARSE_DIRT).sound(SoundType.GRAVEL).hardness(0.5f);
    public static final Block GRANITE = new Block(56, "Granite", Tex.GRANITE).hardness(1.5f);
    public static final Block DIORITE = new Block(57, "Diorite", Tex.DIORITE).hardness(1.5f);
    public static final Block ANDESITE = new Block(58, "Andesite", Tex.ANDESITE).hardness(1.5f);
    public static final Block TERRACOTTA = new Block(59, "Terracotta", Tex.TERRACOTTA).hardness(1.25f);
    public static final Block LIT_FURNACE = new Block(60, "Furnace", 0).tex(Tex.FURNACE_TOP, Tex.FURNACE_TOP, Tex.FURNACE_SIDE).light(13).hardness(3.5f);
    public static final Block CHEST = new Block(61, "Chest", 0).tex(Tex.CHEST_TOP, Tex.CHEST_TOP, Tex.CHEST_SIDE).sound(SoundType.WOOD).hardness(2.5f);
    public static final Block FARMLAND = new Block(62, "Farmland", 0).tex(Tex.FARMLAND, Tex.DIRT, Tex.DIRT).sound(SoundType.GRAVEL).hardness(0.6f);
    public static final Block WHEAT = new Block(63, "Wheat Crops", Tex.WHEAT_0).plant();
    public static final Block SAPLING = new Block(64, "Oak Sapling", Tex.SAPLING).plant();
    public static final Block IRON_BLOCK = new Block(65, "Block of Iron", Tex.IRON_BLOCK).hardness(5f);
    public static final Block GOLD_BLOCK = new Block(66, "Block of Gold", Tex.GOLD_BLOCK).hardness(3f);
    public static final Block DIAMOND_BLOCK = new Block(67, "Block of Diamond", Tex.DIAMOND_BLOCK).hardness(5f);

    // Building blocks with non-cube shapes (meta layouts documented in Shapes)
    public static final Block SMOOTH_STONE_SLAB = derived(68, "Smooth Stone Slab", STONE, Shape.SLAB).tex(Tex.SMOOTH_STONE, Tex.SMOOTH_STONE, Tex.SMOOTH_STONE);
    public static final Block COBBLESTONE_SLAB = derived(69, "Cobblestone Slab", COBBLESTONE, Shape.SLAB);
    public static final Block OAK_SLAB = derived(70, "Oak Slab", PLANKS, Shape.SLAB);
    public static final Block SPRUCE_SLAB = derived(71, "Spruce Slab", SPRUCE_PLANKS, Shape.SLAB);
    public static final Block BIRCH_SLAB = derived(72, "Birch Slab", BIRCH_PLANKS, Shape.SLAB);
    public static final Block SANDSTONE_SLAB = derived(73, "Sandstone Slab", SANDSTONE, Shape.SLAB);
    public static final Block BRICK_SLAB = derived(74, "Brick Slab", BRICKS, Shape.SLAB);
    public static final Block STONE_BRICK_SLAB = derived(75, "Stone Brick Slab", STONE_BRICKS, Shape.SLAB);
    public static final Block OAK_STAIRS = derived(76, "Oak Stairs", PLANKS, Shape.STAIRS);
    public static final Block SPRUCE_STAIRS = derived(77, "Spruce Stairs", SPRUCE_PLANKS, Shape.STAIRS);
    public static final Block BIRCH_STAIRS = derived(78, "Birch Stairs", BIRCH_PLANKS, Shape.STAIRS);
    public static final Block COBBLESTONE_STAIRS = derived(79, "Cobblestone Stairs", COBBLESTONE, Shape.STAIRS);
    public static final Block STONE_BRICK_STAIRS = derived(80, "Stone Brick Stairs", STONE_BRICKS, Shape.STAIRS);
    public static final Block BRICK_STAIRS = derived(81, "Brick Stairs", BRICKS, Shape.STAIRS);
    public static final Block SANDSTONE_STAIRS = derived(82, "Sandstone Stairs", SANDSTONE, Shape.STAIRS);
    public static final Block OAK_FENCE = derived(83, "Oak Fence", PLANKS, Shape.FENCE);
    public static final Block OAK_FENCE_GATE = derived(84, "Oak Fence Gate", PLANKS, Shape.GATE);
    public static final Block OAK_DOOR = new Block(85, "Oak Door", 0).tex(Tex.OAK_DOOR_TOP, Tex.OAK_DOOR_BOTTOM, Tex.OAK_DOOR_BOTTOM).shape(Shape.DOOR).sound(SoundType.WOOD).hardness(3f).itemTex(Tex.OAK_DOOR_ITEM);
    public static final Block IRON_DOOR = new Block(86, "Iron Door", 0).tex(Tex.IRON_DOOR_TOP, Tex.IRON_DOOR_BOTTOM, Tex.IRON_DOOR_BOTTOM).shape(Shape.DOOR).hardness(5f).itemTex(Tex.IRON_DOOR_ITEM);
    public static final Block OAK_TRAPDOOR = new Block(87, "Oak Trapdoor", Tex.TRAPDOOR).shape(Shape.TRAPDOOR).sound(SoundType.WOOD).hardness(3f);
    public static final Block LADDER = new Block(88, "Ladder", Tex.LADDER).shape(Shape.LADDER).sound(SoundType.WOOD).hardness(0.4f);
    public static final Block GLASS_PANE = new Block(89, "Glass Pane", Tex.GLASS).shape(Shape.PANE).sound(SoundType.GLASS).hardness(0.3f);
    public static final Block IRON_BARS = new Block(90, "Iron Bars", Tex.IRON_BARS).shape(Shape.PANE).hardness(5f);
    public static final Block BED = new Block(91, "Red Bed", 0).tex(Tex.BED_HEAD, Tex.PLANKS, Tex.BED_SIDE).shape(Shape.BED).sound(SoundType.WOOD).hardness(0.2f).itemTex(Tex.BED_ITEM);
    public static final Block COBBLESTONE_WALL = derived(92, "Cobblestone Wall", COBBLESTONE, Shape.WALL);
    public static final Block WHITE_CARPET = derived(93, "White Carpet", WHITE_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block RED_CARPET = derived(94, "Red Carpet", RED_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block BLUE_CARPET = derived(95, "Blue Carpet", BLUE_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block GREEN_CARPET = derived(96, "Green Carpet", GREEN_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block YELLOW_CARPET = derived(97, "Yellow Carpet", YELLOW_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block BLACK_CARPET = derived(98, "Black Carpet", BLACK_WOOL, Shape.CARPET).hardness(0.1f);
    public static final Block SNOW_LAYER = derived(99, "Snow", SNOW, Shape.SNOW_LAYER).hardness(0.1f);
    public static final Block CARROTS = new Block(100, "Carrots", Tex.CARROTS_0).plant();
    public static final Block POTATOES = new Block(101, "Potatoes", Tex.POTATOES_0).plant();
    public static final Block CAKE = new Block(102, "Cake", 0).tex(Tex.CAKE_TOP, Tex.CAKE_BOTTOM, Tex.CAKE_SIDE).shape(Shape.CAKE).sound(SoundType.CLOTH).hardness(0.5f).itemTex(Tex.CAKE_ITEM);
    public static final Block EMERALD_ORE = new Block(103, "Emerald Ore", Tex.EMERALD_ORE).hardness(3f);
    public static final Block EMERALD_BLOCK = new Block(104, "Block of Emerald", Tex.EMERALD_BLOCK).hardness(5f);
    public static final Block SPAWNER = new Block(105, "Monster Spawner", Tex.SPAWNER).cutout().hardness(5f);
    public static final Block NETHER_PORTAL = new Block(106, "Nether Portal", Tex.PORTAL).shape(Shape.PORTAL).translucent().light(11).hardness(-1);
    public static final Block NETHERRACK = new Block(107, "Netherrack", Tex.NETHERRACK).hardness(0.4f);
    public static final Block SOUL_SAND = new Block(108, "Soul Sand", Tex.SOUL_SAND).sound(SoundType.SAND).hardness(0.5f);
    public static final Block NETHER_QUARTZ_ORE = new Block(109, "Nether Quartz Ore", Tex.QUARTZ_ORE).hardness(3f);
    public static final Block FIRE = new Block(110, "Fire", Tex.FIRE).plant().light(15);
    public static final Block NETHER_BRICKS = new Block(111, "Nether Bricks", Tex.NETHER_BRICKS).hardness(2f);
    public static final Block QUARTZ_BLOCK = new Block(112, "Block of Quartz", 0).tex(Tex.QUARTZ_TOP, Tex.QUARTZ_TOP, Tex.QUARTZ_SIDE).hardness(0.8f);
    // Redstone (meta layouts documented in Shapes and Redstone)
    public static final Block REDSTONE_WIRE = new Block(113, "Redstone Wire", Tex.REDSTONE_DUST).shape(Shape.WIRE).cutout().hardness(0);
    public static final Block REDSTONE_TORCH = new Block(114, "Redstone Torch", Tex.REDSTONE_TORCH_ON).light(7);
    public static final Block UNLIT_REDSTONE_TORCH = new Block(115, "Redstone Torch", Tex.REDSTONE_TORCH_OFF);
    public static final Block LEVER = new Block(116, "Lever", Tex.LEVER).shape(Shape.LEVER).cutout().hardness(0.5f);
    public static final Block STONE_BUTTON = derived(117, "Stone Button", STONE, Shape.BUTTON).hardness(0.5f);
    public static final Block OAK_BUTTON = derived(118, "Oak Button", PLANKS, Shape.BUTTON).hardness(0.5f);
    public static final Block STONE_PRESSURE_PLATE = derived(119, "Stone Pressure Plate", STONE, Shape.PLATE).hardness(0.5f);
    public static final Block OAK_PRESSURE_PLATE = derived(120, "Oak Pressure Plate", PLANKS, Shape.PLATE).hardness(0.5f);
    public static final Block REDSTONE_LAMP = new Block(121, "Redstone Lamp", Tex.LAMP_OFF).sound(SoundType.GLASS).hardness(0.3f);
    public static final Block LIT_REDSTONE_LAMP = new Block(122, "Redstone Lamp", Tex.LAMP_ON).light(15).sound(SoundType.GLASS).hardness(0.3f);
    public static final Block REPEATER = new Block(123, "Redstone Repeater", 0).tex(Tex.REPEATER_OFF, Tex.SMOOTH_STONE, Tex.SMOOTH_STONE).shape(Shape.REPEATER).hardness(0).itemTex(Tex.REPEATER_ITEM);
    public static final Block POWERED_REPEATER = new Block(124, "Redstone Repeater", 0).tex(Tex.REPEATER_ON, Tex.SMOOTH_STONE, Tex.SMOOTH_STONE).shape(Shape.REPEATER).hardness(0).itemTex(Tex.REPEATER_ITEM);
    public static final Block PISTON = new Block(125, "Piston", 0).tex(Tex.PISTON_TOP, Tex.PISTON_BOTTOM, Tex.PISTON_SIDE).shape(Shape.PISTON).hardness(1.5f);
    public static final Block STICKY_PISTON = new Block(126, "Sticky Piston", 0).tex(Tex.PISTON_STICKY, Tex.PISTON_BOTTOM, Tex.PISTON_SIDE).shape(Shape.PISTON).hardness(1.5f);
    public static final Block PISTON_HEAD = new Block(127, "Piston Head", 0).tex(Tex.PISTON_TOP, Tex.PISTON_TOP, Tex.PISTON_SIDE).shape(Shape.PISTON_HEAD).hardness(1.5f);
    public static final Block REDSTONE_ORE = new Block(128, "Redstone Ore", Tex.REDSTONE_ORE).hardness(3f);
    public static final Block LIT_REDSTONE_ORE = new Block(129, "Redstone Ore", Tex.REDSTONE_ORE).light(9).hardness(3f);
    public static final Block REDSTONE_BLOCK = new Block(130, "Block of Redstone", Tex.REDSTONE_BLOCK).hardness(5f);
    public static final Block ENCHANTING_TABLE = new Block(131, "Enchanting Table", 0).tex(Tex.ENCH_TOP, Tex.ENCH_BOTTOM, Tex.ENCH_SIDE).shape(Shape.TABLE).light(7).hardness(5f);
    public static final Block ANVIL = new Block(132, "Anvil", 0).tex(Tex.ANVIL_TOP, Tex.ANVIL_SIDE, Tex.ANVIL_SIDE).shape(Shape.ANVIL).hardness(5f);
    public static final Block LAPIS_ORE = new Block(133, "Lapis Lazuli Ore", Tex.LAPIS_ORE).hardness(3f);
    public static final Block LAPIS_BLOCK = new Block(134, "Block of Lapis Lazuli", Tex.LAPIS_BLOCK).hardness(3f);
    public static final Block BREWING_STAND = new Block(135, "Brewing Stand", 0).tex(Tex.BREWING_BASE, Tex.BREWING_BASE, Tex.BREWING_ROD).shape(Shape.BREWING).cutout().light(1).hardness(0.5f).itemTex(Tex.BREWING_ITEM);
    public static final Block NETHER_WART = new Block(136, "Nether Wart", Tex.NETHER_WART_0).plant();
    public static final Block BROWN_MUSHROOM = new Block(137, "Brown Mushroom", Tex.BROWN_MUSHROOM).plant().light(1);
    public static final Block RED_MUSHROOM = new Block(138, "Red Mushroom", Tex.RED_MUSHROOM).plant();
    public static final Block RAIL = new Block(139, "Rail", Tex.RAIL).shape(Shape.RAIL).cutout().hardness(0.7f);
    public static final Block POWERED_RAIL = new Block(140, "Powered Rail", Tex.POWERED_RAIL).shape(Shape.RAIL).cutout().hardness(0.7f);
    public static final Block DETECTOR_RAIL = new Block(141, "Detector Rail", Tex.DETECTOR_RAIL).shape(Shape.RAIL).cutout().hardness(0.7f);

    static {
        AIR.model = Model.NONE; AIR.opaque = false; AIR.solid = false; AIR.replaceable = true; AIR.inCreativeInventory = false; AIR.sound = SoundType.NONE;
        WATER.model = Model.LIQUID; WATER.solid = false; WATER.replaceable = true; WATER.lightFilter = 2; WATER.inCreativeInventory = false; WATER.sound = SoundType.NONE;
        LAVA.model = Model.LIQUID; LAVA.opaque = false; LAVA.solid = false; LAVA.replaceable = true; LAVA.layer = Layer.OPAQUE; LAVA.inCreativeInventory = false; LAVA.sound = SoundType.NONE;
        OAK_LEAVES.lightFilter = 1; SPRUCE_LEAVES.lightFilter = 1; BIRCH_LEAVES.lightFilter = 1;
        ICE.lightFilter = 2;
        TORCH.model = Model.TORCH; TORCH.layer = Layer.CUTOUT; TORCH.opaque = false; TORCH.solid = false; TORCH.sound = SoundType.WOOD; TORCH.hardness = 0;
        BEDROCK.inCreativeInventory = true;
        GRASS.tintTopOnly = true;
        LIT_FURNACE.inCreativeInventory = false;
        for (Block crop : new Block[]{WHEAT, CARROTS, POTATOES, NETHER_WART}) { crop.inCreativeInventory = false; crop.replaceable = false; }
        BROWN_MUSHROOM.replaceable = false; RED_MUSHROOM.replaceable = false;
        SAPLING.replaceable = false;
        DANDELION.replaceable = false; POPPY.replaceable = false; BLUE_ORCHID.replaceable = false; SUGAR_CANE.replaceable = false;
        DEAD_BUSH.replaceable = true;
        LADDER.climbable = true;
        for (Block b : new Block[]{REDSTONE_TORCH, UNLIT_REDSTONE_TORCH}) {
            b.model = Model.TORCH; b.layer = Layer.CUTOUT; b.opaque = false; b.solid = false; b.sound = SoundType.WOOD; b.hardness = 0; b.washable = true;
        }
        REDSTONE_WIRE.solid = false; REDSTONE_WIRE.washable = true; REDSTONE_WIRE.inCreativeInventory = false; REDSTONE_WIRE.sound = SoundType.NONE;
        UNLIT_REDSTONE_TORCH.inCreativeInventory = false; LIT_REDSTONE_LAMP.inCreativeInventory = false; POWERED_REPEATER.inCreativeInventory = false;
        PISTON_HEAD.inCreativeInventory = false; LIT_REDSTONE_ORE.inCreativeInventory = false;
        for (Block b : new Block[]{LEVER, STONE_BUTTON, OAK_BUTTON, STONE_PRESSURE_PLATE, OAK_PRESSURE_PLATE}) { b.solid = false; b.washable = true; }
        REPEATER.washable = true; POWERED_REPEATER.washable = true;
        OAK_BUTTON.sound = SoundType.WOOD; OAK_PRESSURE_PLATE.sound = SoundType.WOOD;
        NETHER_PORTAL.solid = false; NETHER_PORTAL.inCreativeInventory = false; NETHER_PORTAL.sound = SoundType.GLASS;
        FIRE.inCreativeInventory = false; FIRE.sound = SoundType.NONE; FIRE.tint = Tint.NONE;
        for (Block b : BY_ID) {
            if (b == null) continue;
            if (b.sound == SoundType.WOOD && b != CRAFTING_TABLE || b.sound == SoundType.CLOTH && b != CACTUS && b != CAKE
                    || b == OAK_LEAVES || b == BIRCH_LEAVES || b == SPRUCE_LEAVES || b == TALL_GRASS || b == FERN || b == DEAD_BUSH || b == TNT)
                b.flammable = true;
        }
        TORCH.washable = true;
        for (Block b : BY_ID) if (b != null && (b.shape == Shape.CARPET || b.shape == Shape.SNOW_LAYER)) b.washable = true;
        for (Block b : new Block[]{RAIL, POWERED_RAIL, DETECTOR_RAIL}) { b.solid = false; b.washable = true; b.sound = SoundType.STONE; }
        OAK_TRAPDOOR.layer = Layer.CUTOUT; LADDER.layer = Layer.CUTOUT; GLASS_PANE.layer = Layer.CUTOUT; IRON_BARS.layer = Layer.CUTOUT; OAK_DOOR.layer = Layer.CUTOUT;
    }

    public static Block get(int id) {
        Block b = BY_ID[id & 255];
        return b == null ? AIR : b;
    }

    public int textureFor(int face) {
        return face == 0 ? texTop : face == 1 ? texBottom : texSide;
    }

    /** Mesh face index (0 up, 1 down, 2 north, 3 south, 4 west, 5 east) for 6-way facing 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. */
    public static final int[] PISTON_FACE = {1, 0, 2, 3, 4, 5};

    /** Face index of the front for facing meta 0-3 (south, west, north, east). */
    public static final int[] FRONT_FACE = {3, 4, 2, 5};

    public int textureForFace(int face) {
        return textureForFace(face, 0);
    }

    /** Texture of a face, taking metadata (facing, growth stage, wetness) into account. */
    public int textureForFace(int face, int meta) {
        if (stateFaceTex != null) return stateFaceTex[Math.min(meta & 255, stateFaceTex.length - 1)][face];
        boolean front = face == FRONT_FACE[meta & 3];
        if (this == FURNACE && front) return Tex.FURNACE_FRONT;
        if (this == LIT_FURNACE && front) return Tex.FURNACE_FRONT_LIT;
        if (this == CHEST && front) return Tex.CHEST_FRONT;
        if (this == WHEAT) return Tex.WHEAT_0 + Math.min(7, meta & 7);
        if (this == CARROTS) return Tex.CARROTS_0 + cropStage(meta);
        if (this == NETHER_WART) return Tex.NETHER_WART_0 + Math.min(2, (meta & 3) * 2 / 3);
        if (this == POTATOES) return Tex.POTATOES_0 + cropStage(meta);
        if (this == CAKE && face == 4 && (meta & 7) > 0) return Tex.CAKE_INNER;
        if (shape == Shape.PISTON) {
            int f = meta & 7, opposite = f ^ 1;
            if (face == PISTON_FACE[f]) return (meta & 8) != 0 ? Tex.PISTON_INNER : texTop;
            if (face == PISTON_FACE[opposite]) return Tex.PISTON_BOTTOM;
            return Tex.PISTON_SIDE;
        }
        if (this == PISTON_HEAD) {
            int f = meta & 7;
            if (face == PISTON_FACE[f]) return (meta & 8) != 0 ? Tex.PISTON_STICKY : Tex.PISTON_TOP;
            return face == PISTON_FACE[f ^ 1] ? Tex.PISTON_TOP : Tex.PISTON_SIDE;
        }
        if (shape == Shape.REPEATER && face != 0) return Tex.SMOOTH_STONE;
        if (shape == Shape.DOOR) return (meta & 8) != 0 ? texTop : texBottom;
        if (this == BED) return face == 0 ? ((meta & 4) != 0 ? Tex.BED_HEAD : Tex.BED_FOOT) : face == 1 ? Tex.PLANKS : Tex.BED_SIDE;
        if (this == FARMLAND && face == 0 && meta > 0) return Tex.FARMLAND_WET;
        return textureFor(face);
    }

    /** Blocks with a front that faces the player when placed. */
    public boolean hasFacing() {
        return this == FURNACE || this == LIT_FURNACE || this == CHEST || this == PUMPKIN || this == ANVIL;
    }

    public boolean isLiquid() { return model == Model.LIQUID; }

    public boolean isSlab() { return shape == Shape.SLAB; }

    /** Crops grow through ages 0-7 on farmland. */
    public boolean isCrop() { return this == WHEAT || this == CARROTS || this == POTATOES; }

    /** Carrots and potatoes show 4 visual stages over 8 ages. */
    private static int cropStage(int meta) {
        int age = meta & 7;
        return age < 2 ? 0 : age < 4 ? 1 : age < 7 ? 2 : 3;
    }

    /** Items for these shapes are drawn as 3D models in the GUI and in hand; others use a flat sprite. */
    public boolean has3dItem() {
        return model == Model.CUBE || (model == Model.SHAPE && shape != Shape.DOOR && shape != Shape.LADDER
                && shape != Shape.PANE && shape != Shape.BED && shape != Shape.CAKE && shape != Shape.REPEATER && shape != Shape.LEVER
                && shape != Shape.WIRE && shape != Shape.BREWING && shape != Shape.RAIL);
    }

    /** Tile used when the block is shown as a flat sprite. */
    public int spriteTex() { return itemTex >= 0 ? itemTex : texSide; }
}
