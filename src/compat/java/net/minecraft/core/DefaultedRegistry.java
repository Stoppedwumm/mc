package net.minecraft.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Optional;

/** A registry that answers missing lookups with a default entry (air) (reamc-compat). */
public interface DefaultedRegistry<T> extends Registry<T> {
    @Override ResourceLocation getKey(T value);
    @Override T get(ResourceLocation id);
    @Override T byId(int id);
    ResourceLocation getDefaultKey();
}
