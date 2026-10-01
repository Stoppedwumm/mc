package net.minecraft.tags;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** A named tag of a registry (reamc-compat). */
public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
    public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) { return new TagKey<>(registry, location); }
}
