package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum PistonType implements StringRepresentable {
    DEFAULT("normal"),
    STICKY("sticky");

    private final String name;

    PistonType(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
}
