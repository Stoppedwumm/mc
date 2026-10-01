package net.minecraft.world.level.block.state.properties;

import java.util.List;
import java.util.Optional;

/** A named block state property with a fixed list of values (reamc-compat). */
public abstract class Property<T extends Comparable<T>> {
    private final String name;
    private final Class<T> clazz;

    protected Property(String name, Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName() { return name; }
    public Class<T> getValueClass() { return clazz; }

    public abstract List<T> getPossibleValues();

    public abstract String getName(T value);

    public Optional<T> getValue(String name) {
        for (T v : getPossibleValues()) if (getName(v).equals(name)) return Optional.of(v);
        return Optional.empty();
    }

    @Override public String toString() { return name; }
}
