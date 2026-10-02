package net.neoforged.neoforge.registries;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** A registry entry that is filled in when its registry's event fires (reamc-compat). */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    protected final ResourceKey<R> key;
    private T value;
    private Holder<R> holder;

    protected DeferredHolder(ResourceKey<R> key) { this.key = key; }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) { return new DeferredHolder<>(key); }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registry, ResourceLocation id) { return create(ResourceKey.create(registry, id)); }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceLocation registry, ResourceLocation id) { return create(ResourceKey.create(ResourceKey.createRegistryKey(registry), id)); }

    @SuppressWarnings("unchecked")
    private Holder<R> holder() {
        if (holder == null) {
            Registry<R> r = (Registry<R>) BuiltInRegistries.REGISTRY.get(key.registry());
            if (r != null) holder = r.getHolder(key).orElse(null);
            if (holder == null) throw new IllegalStateException("Registry entry not present: " + key.location());
        }
        return holder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null) value = (T) holder().value();
        return value;
    }

    @Override public R value() { return get(); }

    @Override
    public boolean isBound() {
        if (value != null) return true;
        try { return holder().isBound(); } catch (IllegalStateException e) { return false; }
    }

    public ResourceLocation getId() { return key.location(); }
    public ResourceKey<R> getKey() { return key; }
    public Optional<T> asOptional() { return isBound() ? Optional.of(get()) : Optional.empty(); }
    public Holder<R> getDelegate() { return holder(); }

    @SuppressWarnings("unchecked")
    void bind(Object v) { value = (T) v; }

    @Override public boolean is(ResourceLocation id) { return id.equals(key.location()); }
    @Override public boolean is(ResourceKey<R> k) { return k == key || k.equals(key); }
    @Override public boolean is(Predicate<ResourceKey<R>> p) { return p.test(key); }
    @Override public boolean is(TagKey<R> tag) { return isBound() && holder().is(tag); }
    @Override public boolean is(Holder<R> other) { return other.is(key); }
    @Override public Stream<TagKey<R>> tags() { return isBound() ? holder().tags() : Stream.empty(); }
    @Override public Either<ResourceKey<R>, R> unwrap() { return Either.left(key); }
    @Override public Optional<ResourceKey<R>> unwrapKey() { return Optional.of(key); }
    @Override public Kind kind() { return Kind.REFERENCE; }
    @Override public boolean canSerializeIn(HolderOwner<R> owner) { return isBound() && holder().canSerializeIn(owner); }
    @Override public String getRegisteredName() { return key.location().toString(); }

    @Override public boolean equals(Object o) { return this == o || o instanceof Holder<?> h && h.unwrapKey().map(key::equals).orElse(false); }
    @Override public int hashCode() { return key.hashCode(); }
    @Override public String toString() { return "DeferredHolder{" + key.location() + "}"; }
}
