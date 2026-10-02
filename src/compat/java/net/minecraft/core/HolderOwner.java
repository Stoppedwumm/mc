package net.minecraft.core;

/** Something holders belong to (a registry); holders only serialize within their owner (reamc-compat). */
public interface HolderOwner<T> {
    default boolean canSerializeIn(HolderOwner<T> owner) { return owner == this; }
}
