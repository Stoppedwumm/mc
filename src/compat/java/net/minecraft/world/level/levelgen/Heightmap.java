package net.minecraft.world.level.levelgen;

import net.minecraft.util.StringRepresentable;

/** Height maps (reamc-compat: computed by scanning the column). */
public class Heightmap {
    public enum Usage { WORLDGEN, LIVE_WORLD, CLIENT }

    public enum Types implements StringRepresentable {
        WORLD_SURFACE_WG("WORLD_SURFACE_WG"), WORLD_SURFACE("WORLD_SURFACE"), OCEAN_FLOOR_WG("OCEAN_FLOOR_WG"), OCEAN_FLOOR("OCEAN_FLOOR"),
        MOTION_BLOCKING("MOTION_BLOCKING"), MOTION_BLOCKING_NO_LEAVES("MOTION_BLOCKING_NO_LEAVES");

        public static final com.mojang.serialization.Codec<Types> CODEC = StringRepresentable.fromEnum(Types::values);
        private final String name;

        Types(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }
    }
}
