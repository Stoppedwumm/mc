package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.component.DataComponentType;

import java.util.function.Supplier;

/** NeoForge's supplier-based component access (reamc-compat). */
public interface IDataComponentHolderExtension {
    <T> T get(DataComponentType<? extends T> type);

    <T> T getOrDefault(DataComponentType<? extends T> type, T fallback);

    boolean has(DataComponentType<?> type);

    default <T> T get(Supplier<? extends DataComponentType<? extends T>> type) { return get(type.get()); }

    default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T fallback) { return getOrDefault(type.get(), fallback); }

    default boolean has(Supplier<? extends DataComponentType<?>> type) { return has(type.get()); }
}
