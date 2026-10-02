package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;

/** Looks up registry entries and tags by key (reamc-compat). */
public interface HolderGetter<T> {
    Optional<Holder.Reference<T>> get(ResourceKey<T> key);

    default Holder.Reference<T> getOrThrow(ResourceKey<T> key) { return get(key).orElseThrow(() -> new IllegalStateException("Missing element " + key)); }

    Optional<HolderSet.Named<T>> get(TagKey<T> tag);

    default HolderSet.Named<T> getOrThrow(TagKey<T> tag) { return get(tag).orElseThrow(() -> new IllegalStateException("Missing tag " + tag)); }

    interface Provider {
        <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> key);

        default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
            return this.<T>lookup(key).orElseThrow(() -> new IllegalStateException("Registry " + key.location() + " not found"));
        }
    }
}
