package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum BellAttachType implements StringRepresentable {
    FLOOR("floor"),
    CEILING("ceiling"),
    SINGLE_WALL("single_wall"),
    DOUBLE_WALL("double_wall");

    private final String name;

    BellAttachType(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
