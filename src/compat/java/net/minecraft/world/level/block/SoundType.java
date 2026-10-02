package net.minecraft.world.level.block;

import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/** Block sounds (reamc-compat: mapped onto reamc's synthesised sound sets). */
public class SoundType {
    public static final SoundType EMPTY = new SoundType(mc.world.Block.SoundType.NONE);
    public static final SoundType WOOD = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType GRAVEL = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType GRASS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType LILY_PAD = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType STONE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType METAL = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType GLASS = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType WOOL = new SoundType(mc.world.Block.SoundType.CLOTH);
    public static final SoundType SAND = new SoundType(mc.world.Block.SoundType.SAND);
    public static final SoundType SNOW = new SoundType(mc.world.Block.SoundType.SNOW);
    public static final SoundType POWDER_SNOW = new SoundType(mc.world.Block.SoundType.SNOW);
    public static final SoundType LADDER = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType ANVIL = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType SLIME_BLOCK = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType HONEY_BLOCK = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType WET_GRASS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType CORAL_BLOCK = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType BAMBOO = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType BAMBOO_SAPLING = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType SCAFFOLDING = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType SWEET_BERRY_BUSH = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType CROP = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType HARD_CROP = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType VINE = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType NETHER_WART = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType LANTERN = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType STEM = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType NYLIUM = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType FUNGUS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType ROOTS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SHROOMLIGHT = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType WEEPING_VINES = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType TWISTING_VINES = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SOUL_SAND = new SoundType(mc.world.Block.SoundType.SAND);
    public static final SoundType SOUL_SOIL = new SoundType(mc.world.Block.SoundType.SAND);
    public static final SoundType BASALT = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType WART_BLOCK = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType NETHERRACK = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType NETHER_BRICKS = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType NETHER_SPROUTS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType NETHER_ORE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType BONE_BLOCK = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType NETHERITE_BLOCK = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType ANCIENT_DEBRIS = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType LODESTONE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType CHAIN = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType NETHER_GOLD_ORE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType GILDED_BLACKSTONE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType CANDLE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType AMETHYST = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType AMETHYST_CLUSTER = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType SMALL_AMETHYST_BUD = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType MEDIUM_AMETHYST_BUD = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType LARGE_AMETHYST_BUD = new SoundType(mc.world.Block.SoundType.GLASS);
    public static final SoundType TUFF = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType TUFF_BRICKS = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType POLISHED_TUFF = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType CALCITE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType DRIPSTONE_BLOCK = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType POINTED_DRIPSTONE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType COPPER = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType COPPER_BULB = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType COPPER_GRATE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType CAVE_VINES = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SPORE_BLOSSOM = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType AZALEA = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType FLOWERING_AZALEA = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType MOSS_CARPET = new SoundType(mc.world.Block.SoundType.CLOTH);
    public static final SoundType PINK_PETALS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType MOSS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType BIG_DRIPLEAF = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SMALL_DRIPLEAF = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType ROOTED_DIRT = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType HANGING_ROOTS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType AZALEA_LEAVES = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SCULK_SENSOR = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SCULK_CATALYST = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SCULK = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SCULK_VEIN = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType SCULK_SHRIEKER = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType GLOW_LICHEN = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType DEEPSLATE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType DEEPSLATE_BRICKS = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType DEEPSLATE_TILES = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType POLISHED_DEEPSLATE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType FROGLIGHT = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType FROGSPAWN = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType MANGROVE_ROOTS = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType MUDDY_MANGROVE_ROOTS = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType MUD = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType MUD_BRICKS = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType PACKED_MUD = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType HANGING_SIGN = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType NETHER_WOOD_HANGING_SIGN = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType BAMBOO_WOOD_HANGING_SIGN = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType BAMBOO_WOOD = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType NETHER_WOOD = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType CHERRY_WOOD = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType CHERRY_SAPLING = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType CHERRY_LEAVES = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType CHERRY_WOOD_HANGING_SIGN = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType CHISELED_BOOKSHELF = new SoundType(mc.world.Block.SoundType.WOOD);
    public static final SoundType SUSPICIOUS_SAND = new SoundType(mc.world.Block.SoundType.SAND);
    public static final SoundType SUSPICIOUS_GRAVEL = new SoundType(mc.world.Block.SoundType.GRAVEL);
    public static final SoundType DECORATED_POT = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType DECORATED_POT_CRACKED = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType TRIAL_SPAWNER = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType SPONGE = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType WET_SPONGE = new SoundType(mc.world.Block.SoundType.GRASS);
    public static final SoundType VAULT = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType HEAVY_CORE = new SoundType(mc.world.Block.SoundType.STONE);
    public static final SoundType COBWEB = new SoundType(mc.world.Block.SoundType.GRASS);

    private final mc.world.Block.SoundType engine;
    public final float volume, pitch;
    private final Supplier<SoundEvent> breakSound, stepSound, placeSound, hitSound, fallSound;

    public SoundType(mc.world.Block.SoundType engine) {
        this.engine = engine;
        volume = 1;
        pitch = 1;
        String k = switch (engine) { case CLOTH -> "wool"; case NONE -> null; default -> engine.name().toLowerCase(java.util.Locale.ROOT); };
        breakSound = sound(k, "break");
        stepSound = sound(k, "step");
        placeSound = sound(k, "place");
        hitSound = sound(k, "hit");
        fallSound = sound(k, "fall");
    }

    public SoundType(float volume, float pitch, SoundEvent breakSound, SoundEvent stepSound, SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound) {
        this(volume, pitch, () -> breakSound, () -> stepSound, () -> placeSound, () -> hitSound, () -> fallSound);
    }

    protected SoundType(float volume, float pitch, Supplier<SoundEvent> breakSound, Supplier<SoundEvent> stepSound, Supplier<SoundEvent> placeSound, Supplier<SoundEvent> hitSound, Supplier<SoundEvent> fallSound) {
        this.engine = mc.world.Block.SoundType.STONE;
        this.volume = volume;
        this.pitch = pitch;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    private static Supplier<SoundEvent> sound(String kind, String what) {
        if (kind == null) return () -> net.minecraft.sounds.SoundEvents.EMPTY;
        SoundEvent e = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block." + kind + "." + what));
        return () -> e;
    }

    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public SoundEvent getBreakSound() { return breakSound.get(); }
    public SoundEvent getStepSound() { return stepSound.get(); }
    public SoundEvent getPlaceSound() { return placeSound.get(); }
    public SoundEvent getHitSound() { return hitSound.get(); }
    public SoundEvent getFallSound() { return fallSound.get(); }

    public mc.world.Block.SoundType reamc$sound() { return engine; }
}
