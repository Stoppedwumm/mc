package net.minecraft.world.level.block.state.properties;

import java.util.List;

public class BooleanProperty extends Property<Boolean> {
    protected BooleanProperty(String name) { super(name, Boolean.class); }

    public static BooleanProperty create(String name) { return new BooleanProperty(name); }

    @Override public List<Boolean> getPossibleValues() { return List.of(true, false); }
    @Override public String getName(Boolean value) { return value.toString(); }
}
