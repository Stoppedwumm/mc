package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum RedstoneSide implements StringRepresentable {
    UP("up"),
    SIDE("side"),
    NONE("none");

    private final String name;

    RedstoneSide(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
