package net.minecraft.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A typed key: a registry name plus an entry name (reamc-compat). */
public final class ResourceKey<T> {
    private static final Map<String, ResourceKey<?>> INTERNED = new ConcurrentHashMap<>();
    private final ResourceLocation registryName, location;

    private ResourceKey(ResourceLocation registryName, ResourceLocation location) {
        this.registryName = registryName;
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<T> intern(ResourceLocation registry, ResourceLocation location) {
        return (ResourceKey<T>) INTERNED.computeIfAbsent(registry + " / " + location, k -> new ResourceKey<>(registry, location));
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends net.minecraft.core.Registry<T>> registry, ResourceLocation location) {
        return intern(registry.location, location);
    }

    public static <T> ResourceKey<net.minecraft.core.Registry<T>> createRegistryKey(ResourceLocation location) {
        return intern(ResourceLocation.withDefaultNamespace("root"), location);
    }

    public static <T> com.mojang.serialization.Codec<ResourceKey<T>> codec(ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        return ResourceLocation.CODEC.xmap(id -> create(registry, id), ResourceKey::location);
    }

    public static <T> net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, ResourceKey<T>> streamCodec(ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        return ResourceLocation.STREAM_CODEC.map(id -> create(registry, id), ResourceKey::location);
    }

    @SuppressWarnings("unchecked")
    public <E> java.util.Optional<ResourceKey<E>> cast(ResourceKey<? extends net.minecraft.core.Registry<E>> registry) {
        return isFor(registry) ? java.util.Optional.of((ResourceKey<E>) this) : java.util.Optional.empty();
    }

    public <E> ResourceKey<net.minecraft.core.Registry<E>> registryKey() { return createRegistryKey(registryName); }

    public ResourceLocation location() { return location; }
    public ResourceLocation registry() { return registryName; }
    public boolean isFor(ResourceKey<? extends net.minecraft.core.Registry<?>> registry) { return registryName.equals(registry.location()); }

    @Override public String toString() { return "ResourceKey[" + registryName + " / " + location + "]"; }
}
