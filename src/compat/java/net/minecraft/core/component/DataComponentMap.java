package net.minecraft.core.component;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Component values by type (reamc-compat). */
public interface DataComponentMap extends Iterable<TypedDataComponent<?>> {
    DataComponentMap EMPTY = new DataComponentMap() {
        @Override public <T> T get(DataComponentType<? extends T> type) { return null; }
        @Override public Set<DataComponentType<?>> keySet() { return Set.of(); }
    };

    static DataComponentMap composite(DataComponentMap first, DataComponentMap second) {
        return new DataComponentMap() {
            @Override public <T> T get(DataComponentType<? extends T> type) { T v = second.get(type); return v != null ? v : first.get(type); }
            @Override public Set<DataComponentType<?>> keySet() { Set<DataComponentType<?>> s = new java.util.LinkedHashSet<>(first.keySet()); s.addAll(second.keySet()); return s; }
        };
    }

    static Builder builder() { return new Builder(); }

    <T> T get(DataComponentType<? extends T> type);

    Set<DataComponentType<?>> keySet();

    default boolean has(DataComponentType<?> type) { return get(type) != null; }

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) { T v = get(type); return v != null ? v : fallback; }

    @SuppressWarnings("unchecked")
    default <T> TypedDataComponent<T> getTyped(DataComponentType<T> type) { T v = get(type); return v == null ? null : new TypedDataComponent<>(type, v); }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default Iterator<TypedDataComponent<?>> iterator() {
        return (Iterator) keySet().stream().map(t -> new TypedDataComponent(t, get(t))).iterator();
    }

    default Stream<TypedDataComponent<?>> stream() { return StreamSupport.stream(spliterator(), false); }

    default int size() { return keySet().size(); }

    default boolean isEmpty() { return size() == 0; }

    default DataComponentMap filter(Predicate<DataComponentType<?>> p) {
        DataComponentMap self = this;
        return new DataComponentMap() {
            @Override public <T> T get(DataComponentType<? extends T> type) { return p.test(type) ? self.get(type) : null; }
            @Override public Set<DataComponentType<?>> keySet() { Set<DataComponentType<?>> s = new java.util.LinkedHashSet<>(self.keySet()); s.removeIf(p.negate()); return s; }
        };
    }

    class Builder {
        private final Map<DataComponentType<?>, Object> map = new IdentityHashMap<>();

        public <T> Builder set(DataComponentType<T> type, T value) { if (value == null) map.remove(type); else map.put(type, value); return this; }

        public <T> Builder set(java.util.function.Supplier<? extends DataComponentType<T>> type, T value) { return set(type.get(), value); }

        public Builder addAll(DataComponentMap m) { for (TypedDataComponent<?> t : m) map.put(t.type(), t.value()); return this; }

        public DataComponentMap build() {
            Map<DataComponentType<?>, Object> copy = new IdentityHashMap<>(map);
            return new DataComponentMap() {
                @Override @SuppressWarnings("unchecked") public <T> T get(DataComponentType<? extends T> type) { return (T) copy.get(type); }
                @Override public Set<DataComponentType<?>> keySet() { return copy.keySet(); }
            };
        }
    }
}
