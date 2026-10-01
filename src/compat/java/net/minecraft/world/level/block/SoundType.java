package net.minecraft.world.level.block;

/** Block sounds (reamc-compat: mapped onto reamc's synthesised sound sets). */
public class SoundType {
    public static final SoundType EMPTY = new SoundType(mc.world.Block.SoundType.NONE);
    public static final SoundType WOOD = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType GRAVEL = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType GRASS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType STONE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType METAL = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType GLASS = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType WOOL = new SoundType(mc.world.Block.SoundType.CLOTH);
    public static final SoundType SAND = new SoundType(mc.world.Block.SoundType.SAND);
    public static final SoundType SNOW = new SoundType(mc.world.Block.SoundType.SNOW);
    public static final SoundType ANVIL = METAL, LANTERN = METAL, CHAIN = METAL, NETHERITE_BLOCK = METAL, COPPER = METAL;
    public static final SoundType DEEPSLATE = STONE, NETHERRACK = STONE, BASALT = STONE, BONE_BLOCK = STONE, AMETHYST = GLASS;
    public static final SoundType CROP = GRASS, PLANT = GRASS, VINE = GRASS, ROOTS = GRASS, MOSS = GRASS, LADDER = WOOD, BAMBOO = WOOD, CHERRY_WOOD = WOOD;

    private final mc.world.Block.SoundType engine;

    public SoundType(mc.world.Block.SoundType engine) { this.engine = engine; }

    public mc.world.Block.SoundType reamc$sound() { return engine; }
}
