package mc.world.gen;

public enum Biome {
    OCEAN("Ocean"),
    FROZEN_OCEAN("Frozen Ocean"),
    BEACH("Beach"),
    PLAINS("Plains"),
    FOREST("Forest"),
    BIRCH_FOREST("Birch Forest"),
    TAIGA("Taiga"),
    SNOWY_TAIGA("Snowy Taiga"),
    DESERT("Desert"),
    MOUNTAINS("Mountains"),
    SNOWY_PEAKS("Snowy Peaks"),
    RIVER("River"),
    BADLANDS("Badlands");

    public final String displayName;
    public static final Biome[] VALUES = values();

    Biome(String displayName) {
        this.displayName = displayName;
    }
}
