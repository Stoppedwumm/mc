package net.minecraft.tags;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** A named tag of a registry; membership comes from the loaded tag files (reamc-compat, see mc.mod.Tags). */
public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
    public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) { return new TagKey<>(registry, location); }

    public static <T> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        return ResourceLocation.CODEC.xmap(id -> create(registry, id), TagKey::location);
    }

    /** "#namespace:path" form. */
    public static <T> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> registry) {
        return Codec.STRING.comapFlatMap(s -> s.startsWith("#")
                ? ResourceLocation.read(s.substring(1)).map(id -> create(registry, id))
                : DataResult.error(() -> "Not a tag id"), t -> "#" + t.location);
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> other) { return registry.location().equals(other.location()); }

    @SuppressWarnings("unchecked")
    public <E> Optional<TagKey<E>> cast(ResourceKey<? extends Registry<E>> other) { return isFor(other) ? Optional.of((TagKey<E>) this) : Optional.empty(); }

    @Override public String toString() { return "TagKey[" + registry.location() + " / " + location + "]"; }
}
