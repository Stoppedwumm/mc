package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

/** One combination of property values; reamc stores its index as the block's metadata (reamc-compat). */
public abstract class StateHolder<O, S> {
    protected final O owner;
    private final StateDefinition<O, ?> definition;
    private final int index;

    protected StateHolder(O owner, StateDefinition<O, ?> definition, int index) {
        this.owner = owner;
        this.definition = definition;
        this.index = index;
    }

    public int reamc$index() { return index; }

    public <T extends Comparable<T>> T getValue(Property<T> property) {
        int p = definition.indexOf(property);
        if (p < 0) throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + owner);
        return property.getPossibleValues().get(definition.digit(index, p));
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> property) {
        return hasProperty(property) ? Optional.of(getValue(property)) : Optional.empty();
    }

    public <T extends Comparable<T>> boolean hasProperty(Property<T> property) { return definition.indexOf(property) >= 0; }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V value) {
        int p = definition.indexOf(property);
        if (p < 0) throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + owner);
        int v = property.getPossibleValues().indexOf(value);
        if (v < 0) throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + owner);
        return (S) definition.withDigit(index, p, v);
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> S cycle(Property<T> property) {
        int p = definition.indexOf(property);
        int n = property.getPossibleValues().size();
        return (S) definition.withDigit(index, p, (definition.digit(index, p) + 1) % n);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.valueOf(owner));
        if (!definition.getProperties().isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Property<?> p : definition.getProperties()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(p.getName()).append('=').append(name(p));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private <T extends Comparable<T>> String name(Property<T> p) { return p.getName(getValue(p)); }
}
