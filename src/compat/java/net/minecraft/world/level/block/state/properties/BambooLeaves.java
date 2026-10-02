package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum BambooLeaves implements StringRepresentable {
    NONE("none"),
    SMALL("small"),
    LARGE("large");

    private final String name;

    BambooLeaves(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
