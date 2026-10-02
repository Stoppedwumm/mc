package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum BedPart implements StringRepresentable {
    HEAD("head"),
    FOOT("foot");

    private final String name;

    BedPart(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
