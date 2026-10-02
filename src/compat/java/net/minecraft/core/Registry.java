package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** A registry of named values (reamc-compat). */
public interface Registry<T> extends Keyable, IdMap<T> {
    ResourceKey<? extends Registry<T>> key();

    default Codec<T> byNameCodec() {
        return ResourceLocation.CODEC.comapFlatMap(id -> {
            T v = get(id);
            return v != null ? DataResult.success(v) : DataResult.error(() -> "Unknown registry key in " + key() + ": " + id);
        }, v -> {
            ResourceLocation id = getKey(v);
            if (id == null) throw new IllegalStateException("Unregistered value in " + key() + ": " + v);
            return id;
        });
    }

    default Codec<Holder<T>> holderByNameCodec() {
        return ResourceLocation.CODEC.comapFlatMap(id -> {
            Optional<Holder.Reference<T>> h = getHolder(id);
            return h.isPresent() ? DataResult.success((Holder<T>) h.get()) : DataResult.error(() -> "Unknown registry key in " + key() + ": " + id);
        }, h -> h.unwrapKey().map(ResourceKey::location).orElseThrow(() -> new IllegalStateException("Unregistered holder in " + key() + ": " + h)));
    }

    @Override
    default <U> Stream<U> keys(DynamicOps<U> ops) { return keySet().stream().map(id -> ops.createString(id.toString())); }

    ResourceLocation getKey(T value);

    Optional<ResourceKey<T>> getResourceKey(T value);

    @Override
    int getId(T value);

    T get(ResourceKey<T> key);

    T get(ResourceLocation id);

    default Optional<T> getOptional(ResourceLocation id) { return Optional.ofNullable(get(id)); }

    default Optional<T> getOptional(ResourceKey<T> key) { return Optional.ofNullable(get(key)); }

    default T getOrThrow(ResourceKey<T> key) {
        T v = get(key);
        if (v == null) throw new IllegalStateException("Missing key in " + key() + ": " + key);
        return v;
    }

    Set<ResourceLocation> keySet();

    Set<Map.Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    Optional<Holder.Reference<T>> getRandom(RandomSource random);

    default Stream<T> stream() { return java.util.stream.StreamSupport.stream(spliterator(), false); }

    boolean containsKey(ResourceLocation id);

    boolean containsKey(ResourceKey<T> key);

    Holder<T> wrapAsHolder(T value);

    Optional<Holder.Reference<T>> getHolder(int id);

    Optional<Holder.Reference<T>> getHolder(ResourceLocation id);

    Optional<Holder.Reference<T>> getHolder(ResourceKey<T> key);

    default Holder.Reference<T> getHolderOrThrow(ResourceKey<T> key) {
        return getHolder(key).orElseThrow(() -> new IllegalStateException("Missing key in " + key() + ": " + key));
    }

    Stream<Holder.Reference<T>> holders();

    Optional<HolderSet.Named<T>> getTag(TagKey<T> tag);

    default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> tag) {
        return getTag(tag).<Iterable<Holder<T>>>map(s -> s).orElse(java.util.List.of());
    }

    HolderSet.Named<T> getOrCreateTag(TagKey<T> tag);

    Stream<TagKey<T>> getTagNames();

    HolderLookup.RegistryLookup<T> asLookup();

    default Optional<Holder.Reference<T>> getAny() { return holders().findFirst(); }

    static <T> T register(Registry<? super T> registry, String id, T value) { return register(registry, ResourceLocation.parse(id), value); }

    @SuppressWarnings("unchecked")
    static <V, T extends V> T register(Registry<V> registry, ResourceLocation id, T value) {
        ((MappedRegistry<V>) registry).register(id, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value) {
        ((MappedRegistry<V>) registry).register(key.location(), value);
        return value;
    }

    @SuppressWarnings("unchecked")
    static <T> Holder.Reference<T> registerForHolder(Registry<T> registry, ResourceKey<T> key, T value) {
        return ((MappedRegistry<T>) registry).register(key.location(), value);
    }

    @SuppressWarnings("unchecked")
    static <T> Holder.Reference<T> registerForHolder(Registry<T> registry, ResourceLocation id, T value) {
        return ((MappedRegistry<T>) registry).register(id, value);
    }
}
