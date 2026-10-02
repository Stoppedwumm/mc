package net.minecraft.world.level.pathfinder;

public enum PathType {
    BLOCKED(-1), OPEN(0), WALKABLE(0), WALKABLE_DOOR(0), TRAPDOOR(0), POWDER_SNOW(-1), DANGER_POWDER_SNOW(0), FENCE(-1), LAVA(-1),
    WATER(8), WATER_BORDER(8), RAIL(0), UNPASSABLE_RAIL(-1), DANGER_FIRE(8), DAMAGE_FIRE(16), DANGER_OTHER(8), DAMAGE_OTHER(-1),
    DOOR_OPEN(0), DOOR_WOOD_CLOSED(-1), DOOR_IRON_CLOSED(-1), BREACH(4), LEAVES(-1), STICKY_HONEY(8), COCOA(0), DAMAGE_CAUTIOUS(0),
    DANGER_TRAPDOOR(0);

    private final float malus;

    PathType(float malus) { this.malus = malus; }

    public float getMalus() { return malus; }
}
