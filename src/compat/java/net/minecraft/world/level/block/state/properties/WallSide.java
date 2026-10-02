package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum WallSide implements StringRepresentable {
    NONE("none"),
    LOW("low"),
    TALL("tall");

    private final String name;

    WallSide(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
