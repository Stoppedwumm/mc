package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum ChestType implements StringRepresentable {
    SINGLE("single"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    ChestType(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
