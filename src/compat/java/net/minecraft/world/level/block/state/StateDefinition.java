package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Every state of a block: the property values in mixed radix, first property varying fastest (reamc-compat). */
public class StateDefinition<O, S extends StateHolder<O, S>> {
    private final O owner;
    private final List<Property<?>> properties;
    private final int[] strides;
    private final List<S> states = new ArrayList<>();

    @FunctionalInterface
    public interface Factory<O, S> {
        S create(O owner, StateDefinition<O, ?> definition, int index);
    }

    StateDefinition(O owner, List<Property<?>> properties, Factory<O, S> factory) {
        this.owner = owner;
        this.properties = List.copyOf(properties);
        strides = new int[properties.size()];
        int total = 1;
        for (int i = 0; i < strides.length; i++) {
            strides[i] = total;
            total *= properties.get(i).getPossibleValues().size();
        }
        if (total > 256) throw new IllegalArgumentException(owner + " has " + total + " block states; reamc supports up to 256");
        for (int i = 0; i < total; i++) states.add(factory.create(owner, this, i));
    }

    public S any() { return states.get(0); }
    public List<S> getPossibleStates() { return states; }
    public Collection<Property<?>> getProperties() { return properties; }
    public O getOwner() { return owner; }

    public Property<?> getProperty(String name) {
        for (Property<?> p : properties) if (p.getName().equals(name)) return p;
        return null;
    }

    int indexOf(Property<?> p) { return properties.indexOf(p); }

    int digit(int state, int property) { return state / strides[property] % properties.get(property).getPossibleValues().size(); }

    S withDigit(int state, int property, int value) {
        return states.get(state - digit(state, property) * strides[property] + value * strides[property]);
    }

    public S reamc$state(int index) { return states.get(Math.max(0, Math.min(states.size() - 1, index))); }

    public static class Builder<O, S extends StateHolder<O, S>> {
        private final O owner;
        private final List<Property<?>> properties = new ArrayList<>();

        public Builder(O owner) { this.owner = owner; }

        public Builder<O, S> add(Property<?>... ps) {
            for (Property<?> p : ps) if (!properties.contains(p)) properties.add(p);
            return this;
        }

        public StateDefinition<O, S> create(Function<O, S> defaultState, Factory<O, S> factory) {
            return new StateDefinition<>(owner, properties, factory);
        }
    }
}
