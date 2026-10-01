package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
    private final List<T> values;

    protected EnumProperty(String name, Class<T> clazz, List<T> values) {
        super(name, clazz);
        this.values = List.copyOf(values);
    }

    @Override public List<T> getPossibleValues() { return values; }
    @Override public String getName(T value) { return value.getSerializedName(); }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz) {
        return new EnumProperty<>(name, clazz, Arrays.asList(clazz.getEnumConstants()));
    }

    @SafeVarargs
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, T... values) {
        return new EnumProperty<>(name, clazz, Arrays.asList(values));
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        return new EnumProperty<>(name, clazz, Arrays.stream(clazz.getEnumConstants()).filter(filter).toList());
    }
}
