package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** reamc-compat. */
public enum DoubleBlockHalf implements StringRepresentable {
    UPPER("upper"),
    LOWER("lower");

    private final String name;

    DoubleBlockHalf(String name) { this.name = name; }

    @Override public String getSerializedName() { return name; }
    public net.minecraft.core.Direction getDirectionToOther() { return this == LOWER ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN; }

    public DoubleBlockHalf getOtherHalf() { return this == LOWER ? UPPER : LOWER; }

    @Override public String toString() { return name; }
}
