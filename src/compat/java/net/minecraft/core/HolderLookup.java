package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.stream.Stream;

/** Registry lookups passed to saving and loading code (reamc-compat: backed by the game's registries). */
public interface HolderLookup<T> extends HolderGetter<T> {
    Stream<Holder.Reference<T>> listElements();

    Stream<HolderSet.Named<T>> listTags();

    default Stream<ResourceKey<T>> listElementIds() { return listElements().map(Holder.Reference::key); }

    default Stream<TagKey<T>> listTagIds() { return listTags().map(HolderSet.Named::key); }

    interface Provider {
        Provider EMPTY = new Provider() { };

        default Stream<ResourceKey<? extends Registry<?>>> listRegistries() { return mc.mod.Bridge.registryAccess().listRegistries(); }

        @SuppressWarnings({"unchecked", "rawtypes"})
        default <T> Optional<RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
            return mc.mod.Bridge.registryAccess().registry((ResourceKey) key).map(r -> ((Registry<T>) r).asLookup());
        }

        default <T> RegistryLookup<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
            return this.<T>lookup(key).orElseThrow(() -> new IllegalStateException("Registry " + key.location() + " not found"));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        default <T> Optional<Holder.Reference<T>> holder(ResourceKey<T> key) {
            Optional<RegistryLookup<T>> l = lookup((ResourceKey) ResourceKey.createRegistryKey(key.registry()));
            return l.flatMap(r -> r.get(key));
        }

        default HolderGetter.Provider asGetterLookup() {
            Provider self = this;
            return new HolderGetter.Provider() {
                @Override
                public <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) { return self.<T>lookup(key).map(l -> l); }
            };
        }

        default <V> com.mojang.serialization.DynamicOps<V> createSerializationContext(com.mojang.serialization.DynamicOps<V> ops) { return ops; }
    }

    interface RegistryLookup<T> extends HolderLookup<T>, HolderOwner<T> {
        ResourceKey<? extends Registry<? extends T>> key();

        @Override
        default boolean canSerializeIn(HolderOwner<T> owner) { return true; }
    }
}
