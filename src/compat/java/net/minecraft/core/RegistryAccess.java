package net.minecraft.core;

import net.minecraft.resources.ResourceKey;

import java.util.Optional;
import java.util.stream.Stream;

/** All registries, built-in and data-driven (reamc-compat). */
public interface RegistryAccess extends HolderLookup.Provider {
    Frozen EMPTY = new Frozen() {
        @Override public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key) { return Optional.empty(); }
        @Override public Stream<RegistryEntry<?>> registries() { return Stream.of(); }
    };

    <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key);

    default <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> key) {
        return this.<E>registry(key).orElseThrow(() -> new IllegalStateException("Missing registry: " + key));
    }

    Stream<RegistryEntry<?>> registries();

    @Override
    default Stream<ResourceKey<? extends Registry<?>>> listRegistries() { return registries().map(RegistryEntry::key); }

    @Override
    default <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        return this.<T>registry(key).map(Registry::asLookup);
    }

    default Frozen freeze() {
        RegistryAccess self = this;
        return new Frozen() {
            @Override public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key) { return self.registry(key); }
            @Override public Stream<RegistryEntry<?>> registries() { return self.registries(); }
        };
    }

    interface Frozen extends RegistryAccess { }

    record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) { }
}
