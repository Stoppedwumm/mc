package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** A mapped registry with a default entry (reamc-compat). */
public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
    private final ResourceLocation defaultKey;

    public DefaultedMappedRegistry(ResourceLocation defaultKey, ResourceKey<? extends Registry<T>> key) {
        super(key);
        this.defaultKey = defaultKey;
    }

    public DefaultedMappedRegistry(String defaultKey, ResourceKey<? extends Registry<T>> key, com.mojang.serialization.Lifecycle l, boolean intrusive) {
        this(ResourceLocation.parse(defaultKey), key);
    }

    private T defaultValue() { return super.get(defaultKey); }

    @Override public ResourceLocation getDefaultKey() { return defaultKey; }

    @Override
    public ResourceLocation getKey(T value) {
        ResourceLocation id = super.getKey(value);
        return id != null ? id : defaultKey;
    }

    @Override
    public T get(ResourceLocation id) {
        T v = super.get(id);
        return v != null ? v : defaultValue();
    }

    @Override
    public Optional<T> getOptional(ResourceLocation id) { return Optional.ofNullable(super.get(id)); }

    @Override
    public T byId(int id) {
        T v = super.byId(id);
        return v != null ? v : defaultValue();
    }
}
