package net.minecraft.core.component;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** Changes to an item's default components: set values and removals (reamc-compat). */
public final class DataComponentPatch {
    public static final DataComponentPatch EMPTY = new DataComponentPatch(Map.of());
    final Map<DataComponentType<?>, Optional<?>> changes;

    DataComponentPatch(Map<DataComponentType<?>, Optional<?>> changes) { this.changes = changes; }

    public static Builder builder() { return new Builder(); }

    @SuppressWarnings("unchecked")
    public <T> Optional<? extends T> get(DataComponentType<? extends T> type) { return (Optional<? extends T>) changes.get(type); }

    public boolean isEmpty() { return changes.isEmpty(); }

    public java.util.Set<Map.Entry<DataComponentType<?>, Optional<?>>> entrySet() { return changes.entrySet(); }

    public int size() { return changes.size(); }

    public static class Builder {
        private final Map<DataComponentType<?>, Optional<?>> changes = new IdentityHashMap<>();

        public <T> Builder set(DataComponentType<T> type, T value) { changes.put(type, Optional.of(value)); return this; }
        public <T> Builder remove(DataComponentType<T> type) { changes.put(type, Optional.empty()); return this; }
        public DataComponentPatch build() { return new DataComponentPatch(new IdentityHashMap<>(changes)); }
    }
}
