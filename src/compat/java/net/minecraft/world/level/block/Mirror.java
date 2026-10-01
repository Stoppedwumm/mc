package net.minecraft.world.level.block;

import net.minecraft.core.Direction;

public enum Mirror {
    NONE, LEFT_RIGHT, FRONT_BACK;

    public Rotation getRotation(Direction d) {
        boolean flip = (this == LEFT_RIGHT && d.getAxis() == Direction.Axis.Z) || (this == FRONT_BACK && d.getAxis() == Direction.Axis.X);
        return flip ? Rotation.CLOCKWISE_180 : Rotation.NONE;
    }

    public Direction mirror(Direction d) { return getRotation(d) == Rotation.CLOCKWISE_180 ? d.getOpposite() : d; }
}
