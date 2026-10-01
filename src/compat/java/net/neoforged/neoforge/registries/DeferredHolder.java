package net.neoforged.neoforge.registries;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** A registry entry that is filled in when its registry's event fires (reamc-compat). */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    protected final ResourceKey<R> key;
    private T value;

    protected DeferredHolder(ResourceKey<R> key) { this.key = key; }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) { return new DeferredHolder<>(key); }

    @Override
    public T get() {
        if (value == null) throw new IllegalStateException("Registry entry not present: " + key.location());
        return value;
    }

    @Override public R value() { return get(); }
    @Override public boolean isBound() { return value != null; }

    public ResourceLocation getId() { return key.location(); }
    public ResourceKey<R> getKey() { return key; }

    @SuppressWarnings("unchecked")
    void bind(Object v) { value = (T) v; }

    @Override public String toString() { return "DeferredHolder{" + key.location() + "}"; }
}
