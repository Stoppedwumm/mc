package net.minecraft.world.entity;

import net.minecraft.util.StringRepresentable;

public enum MobCategory implements StringRepresentable {
    MONSTER("monster", 70, false, false, 128), CREATURE("creature", 10, true, true, 128), AMBIENT("ambient", 15, true, false, 128),
    AXOLOTLS("axolotls", 5, true, false, 128), UNDERGROUND_WATER_CREATURE("underground_water_creature", 5, true, false, 128),
    WATER_CREATURE("water_creature", 5, true, false, 128), WATER_AMBIENT("water_ambient", 20, true, false, 64), MISC("misc", -1, true, true, 128);

    private final String name;
    private final int max, despawnDistance;
    private final boolean friendly, persistent;

    MobCategory(String name, int max, boolean friendly, boolean persistent, int despawnDistance) {
        this.name = name; this.max = max; this.friendly = friendly; this.persistent = persistent; this.despawnDistance = despawnDistance;
    }

    public String getName() { return name; }
    @Override public String getSerializedName() { return name; }
    public int getMaxInstancesPerChunk() { return max; }
    public boolean isFriendly() { return friendly; }
    public boolean isPersistent() { return persistent; }
    public int getDespawnDistance() { return despawnDistance; }
    public int getNoDespawnDistance() { return 32; }
}
