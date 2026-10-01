package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** A registry of named values (reamc-compat). */
public interface Registry<T> extends Iterable<T> {
    ResourceKey<? extends Registry<T>> key();

    T get(ResourceLocation id);

    ResourceLocation getKey(T value);

    boolean containsKey(ResourceLocation id);

    Set<ResourceLocation> keySet();

    Holder<T> wrapAsHolder(T value);

    static <T> T register(Registry<? super T> registry, ResourceLocation id, T value) {
        ((MappedRegistry<? super T>) registry).register(id, value);
        return value;
    }

    static <T> T register(Registry<? super T> registry, String id, T value) {
        return register(registry, ResourceLocation.parse(id), value);
    }
}
