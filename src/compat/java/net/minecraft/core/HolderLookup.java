package net.minecraft.core;

/** Registry lookups passed to saving code; reamc needs none (reamc-compat). */
public interface HolderLookup<T> {
    interface Provider {
        Provider EMPTY = new Provider() { };
    }
}
