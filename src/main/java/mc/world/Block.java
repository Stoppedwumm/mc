package mc.world;

/**
 * Static block registry. Block ids are stored as bytes inside chunks, so there can be at most 128 blocks.
 */
public final class Block {
    public enum Model { NONE, CUBE, CROSS, LIQUID, TORCH }
    public enum Layer { OPAQUE, CUTOUT, TRANSLUCENT }
    public enum Tint { NONE, GRASS, FOLIAGE, BIRCH, SPRUCE }
    public enum SoundType { STONE, WOOD, GRASS, GRAVEL, SAND, GLASS, CLOTH, SNOW, NONE }

    public static final Block[] BY_ID = new Block[128];

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
                DIORITE = 70, ANDESITE = 71, TERRACOTTA = 72, GRASS_SIDE_ITEM = 73, GRASS_TOP_ITEM = 74, BREAK_0 = 240; // 240..249 crack stages
        public static final int COUNT = 75;
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

    static {
        AIR.model = Model.NONE; AIR.opaque = false; AIR.solid = false; AIR.replaceable = true; AIR.inCreativeInventory = false; AIR.sound = SoundType.NONE;
        WATER.model = Model.LIQUID; WATER.solid = false; WATER.replaceable = true; WATER.lightFilter = 2; WATER.inCreativeInventory = false; WATER.sound = SoundType.NONE;
        LAVA.model = Model.LIQUID; LAVA.opaque = false; LAVA.solid = false; LAVA.replaceable = true; LAVA.layer = Layer.OPAQUE; LAVA.inCreativeInventory = false; LAVA.sound = SoundType.NONE;
        OAK_LEAVES.lightFilter = 1; SPRUCE_LEAVES.lightFilter = 1; BIRCH_LEAVES.lightFilter = 1;
        ICE.lightFilter = 2;
        TORCH.model = Model.TORCH; TORCH.layer = Layer.CUTOUT; TORCH.opaque = false; TORCH.solid = false; TORCH.sound = SoundType.WOOD; TORCH.hardness = 0;
        BEDROCK.inCreativeInventory = true;
        GRASS.tintTopOnly = true;
    }

    public static Block get(int id) {
        Block b = BY_ID[id & 127];
        return b == null ? AIR : b;
    }

    public int textureFor(int face) {
        return face == 0 ? texTop : face == 1 ? texBottom : texSide;
    }

    /** Front face texture override (furnace, pumpkin); faces facing south get the front. */
    public int textureForFace(int face) {
        if (this == FURNACE && face == 3) return Tex.FURNACE_FRONT;
        return textureFor(face);
    }

    public boolean isLiquid() { return model == Model.LIQUID; }
}
