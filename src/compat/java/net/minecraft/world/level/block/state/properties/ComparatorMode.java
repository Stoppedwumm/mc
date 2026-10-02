package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum ComparatorMode implements StringRepresentable {
    COMPARE("compare"),
    SUBTRACT("subtract");

    private final String name;

    ComparatorMode(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
