package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Insertion-ordered registry (reamc-compat). */
public class MappedRegistry<T> implements Registry<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final Map<ResourceLocation, T> byId = new LinkedHashMap<>();
    private final Map<T, ResourceLocation> ids = new java.util.IdentityHashMap<>();
    /** Called for every new entry (reamc binds blocks and items to the engine here). */
    public java.util.function.BiConsumer<ResourceLocation, T> onRegister;

    public MappedRegistry(ResourceKey<? extends Registry<T>> key) { this.key = key; }

    public void register(ResourceLocation id, T value) {
        if (byId.containsKey(id)) throw new IllegalStateException("Duplicate registration " + id + " in " + key.location());
        byId.put(id, value);
        ids.put(value, id);
        if (onRegister != null) onRegister.accept(id, value);
    }

    @Override public ResourceKey<? extends Registry<T>> key() { return key; }
    @Override public T get(ResourceLocation id) { return byId.get(id); }
    @Override public ResourceLocation getKey(T value) { return ids.get(value); }
    @Override public boolean containsKey(ResourceLocation id) { return byId.containsKey(id); }
    @Override public Set<ResourceLocation> keySet() { return byId.keySet(); }
    @Override public Holder<T> wrapAsHolder(T value) { return Holder.direct(value); }
    @Override public Iterator<T> iterator() { return byId.values().iterator(); }
    public Map<ResourceLocation, T> entries() { return byId; }
}
