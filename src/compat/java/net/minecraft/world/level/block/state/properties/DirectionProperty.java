package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class DirectionProperty extends EnumProperty<Direction> {
    protected DirectionProperty(String name, List<Direction> values) { super(name, Direction.class, values); }

    public static DirectionProperty create(String name, Direction... values) { return new DirectionProperty(name, Arrays.asList(values)); }

    public static DirectionProperty create(String name) { return create(name, Direction.values()); }

    public static DirectionProperty create(String name, Predicate<Direction> filter) {
        return new DirectionProperty(name, Arrays.stream(Direction.values()).filter(filter).toList());
    }
}
