package net.minecraft.core;

/** A reference to a registry value (reamc-compat). */
public interface Holder<T> {
    T value();

    default boolean isBound() { return true; }

    static <T> Holder<T> direct(T value) { return new Direct<>(value); }

    record Direct<T>(T value) implements Holder<T> { }
}
