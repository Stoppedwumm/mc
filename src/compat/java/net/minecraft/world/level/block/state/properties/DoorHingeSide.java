package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum DoorHingeSide implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    DoorHingeSide(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
