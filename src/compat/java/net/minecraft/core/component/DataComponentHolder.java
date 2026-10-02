package net.minecraft.core.component;

/** Something with components (reamc-compat). */
public interface DataComponentHolder extends net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension {
    DataComponentMap getComponents();

    default <T> T get(DataComponentType<? extends T> type) { return getComponents().get(type); }

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) { return getComponents().getOrDefault(type, fallback); }

    default boolean has(DataComponentType<?> type) { return getComponents().has(type); }
}
