package net.minecraft.world.level.block;

import net.minecraft.core.Direction;

public enum Rotation {
    NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90;

    public Direction rotate(Direction d) {
        if (d.getAxis() == Direction.Axis.Y) return d;
        return switch (this) {
            case NONE -> d;
            case CLOCKWISE_90 -> d.getClockWise();
            case CLOCKWISE_180 -> d.getOpposite();
            case COUNTERCLOCKWISE_90 -> d.getCounterClockWise();
        };
    }

    public Rotation getRotated(Rotation o) { return values()[(ordinal() + o.ordinal()) % 4]; }
}
