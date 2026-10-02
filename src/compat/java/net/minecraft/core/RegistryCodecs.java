package net.minecraft.core;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;

/** Codecs for sets of registry entries (reamc-compat). */
public final class RegistryCodecs {
    private RegistryCodecs() { }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registry) { return mc.mod.DataCodecs.holderSet(registry); }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registry, Codec<E> direct) { return homogeneousList(registry); }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registry, boolean onlyLists) { return homogeneousList(registry); }
}
