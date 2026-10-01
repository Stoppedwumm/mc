package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Direction;

/** Common block state properties (reamc-compat). */
public final class BlockStateProperties {
    private BlockStateProperties() { }

    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final DirectionProperty HORIZONTAL_FACING = DirectionProperty.create("facing", Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty WATERLOGGED = BooleanProperty.create("waterlogged");
    public static final IntegerProperty AGE_3 = IntegerProperty.create("age", 0, 3);
    public static final IntegerProperty AGE_7 = IntegerProperty.create("age", 0, 7);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
}
