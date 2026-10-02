package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum Tilt implements StringRepresentable {
    NONE("none"),
    UNSTABLE("unstable"),
    PARTIAL("partial"),
    FULL("full");

    private final String name;

    Tilt(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
