package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum Half implements StringRepresentable {
    TOP("top"),
    BOTTOM("bottom");

    private final String name;

    Half(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
