package net.minecraft.world.level.block.state.properties;

import java.util.ArrayList;
import java.util.List;

public class IntegerProperty extends Property<Integer> {
    private final List<Integer> values = new ArrayList<>();

    protected IntegerProperty(String name, int min, int max) {
        super(name, Integer.class);
        for (int i = min; i <= max; i++) values.add(i);
    }

    public static IntegerProperty create(String name, int min, int max) { return new IntegerProperty(name, min, max); }

    @Override public List<Integer> getPossibleValues() { return values; }
    @Override public String getName(Integer value) { return value.toString(); }
}
