package net.minecraft.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Insertion-ordered registry (reamc-compat). */
public class MappedRegistry<T> implements Registry<T>, HolderOwner<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final Map<ResourceLocation, Holder.Reference<T>> byId = new LinkedHashMap<>();
    private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>();
    private final List<Holder.Reference<T>> byIndex = new ArrayList<>();
    private final Map<T, Integer> indexOf = new IdentityHashMap<>();
    private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
    /** Called for every new entry. */
    public java.util.function.BiConsumer<ResourceLocation, T> onRegister;

    public MappedRegistry(ResourceKey<? extends Registry<T>> key) { this.key = key; }

    public MappedRegistry(ResourceKey<? extends Registry<T>> key, com.mojang.serialization.Lifecycle lifecycle) { this(key); }

    @SuppressWarnings("unchecked")
    public Holder.Reference<T> register(ResourceLocation id, T value) {
        if (byId.containsKey(id)) throw new IllegalStateException("Duplicate registration " + id + " in " + key.location());
        ResourceKey<T> k = ResourceKey.create((ResourceKey<? extends Registry<T>>) key, id);
        Holder.Reference<T> h = Holder.Reference.createStandAlone(this, k);
        h.bindValue(value);
        byId.put(id, h);
        byValue.put(value, h);
        indexOf.put(value, byIndex.size());
        byIndex.add(h);
        // Values that carry their own holder (blocks, items) learn their key
        if (value instanceof net.minecraft.world.level.block.Block b) b.reamc$bindHolder((Holder.Reference) h);
        if (value instanceof net.minecraft.world.item.Item i) i.reamc$bindHolder((Holder.Reference) h);
        if (onRegister != null) onRegister.accept(id, value);
        return h;
    }

    public Holder.Reference<T> register(ResourceKey<T> k, T value, Object registrationInfo) { return register(k.location(), value); }

    public void addAlias(ResourceLocation from, ResourceLocation to) { aliases.put(from, to); }

    private Holder.Reference<T> ref(ResourceLocation id) {
        if (id == null) return null;
        Holder.Reference<T> h = byId.get(id);
        if (h == null && aliases.containsKey(id)) h = byId.get(aliases.get(id));
        return h;
    }

    @Override public ResourceKey<? extends Registry<T>> key() { return key; }
    @Override public T get(ResourceLocation id) { Holder.Reference<T> h = ref(id); return h == null ? null : h.value(); }
    @Override public T get(ResourceKey<T> k) { return k == null ? null : get(k.location()); }
    @Override public ResourceLocation getKey(T value) { Holder.Reference<T> h = byValue.get(value); return h == null ? null : h.key().location(); }
    @Override public Optional<ResourceKey<T>> getResourceKey(T value) { return Optional.ofNullable(byValue.get(value)).map(Holder.Reference::key); }
    @Override public int getId(T value) { Integer i = indexOf.get(value); return i == null ? -1 : i; }
    @Override public T byId(int id) { return id >= 0 && id < byIndex.size() ? byIndex.get(id).value() : null; }
    @Override public int size() { return byId.size(); }
    @Override public boolean containsKey(ResourceLocation id) { return ref(id) != null; }
    @Override public boolean containsKey(ResourceKey<T> k) { return containsKey(k.location()); }
    @Override public Set<ResourceLocation> keySet() { return byId.keySet(); }
    @Override public Set<ResourceKey<T>> registryKeySet() { return byId.values().stream().map(Holder.Reference::key).collect(Collectors.toCollection(LinkedHashSet::new)); }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        Set<Map.Entry<ResourceKey<T>, T>> s = new LinkedHashSet<>();
        for (Holder.Reference<T> h : byId.values()) s.add(new AbstractMap.SimpleImmutableEntry<>(h.key(), h.value()));
        return s;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource random) {
        return byIndex.isEmpty() ? Optional.empty() : Optional.of(byIndex.get(random.nextInt(byIndex.size())));
    }

    @Override
    public Holder<T> wrapAsHolder(T value) {
        Holder.Reference<T> h = byValue.get(value);
        return h != null ? h : Holder.direct(value);
    }

    @Override public Optional<Holder.Reference<T>> getHolder(int id) { return id >= 0 && id < byIndex.size() ? Optional.of(byIndex.get(id)) : Optional.empty(); }
    @Override public Optional<Holder.Reference<T>> getHolder(ResourceLocation id) { return Optional.ofNullable(ref(id)); }
    @Override public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> k) { return Optional.ofNullable(ref(k.location())); }
    @Override public Stream<Holder.Reference<T>> holders() { return byIndex.stream(); }
    @Override public Optional<HolderSet.Named<T>> getTag(TagKey<T> tag) { return Optional.of(getOrCreateTag(tag)); }
    @Override public HolderSet.Named<T> getOrCreateTag(TagKey<T> tag) { return HolderSet.emptyNamed(this, tag); }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<TagKey<T>> getTagNames() { return mc.mod.Tags.tagNames(key).stream().map(id -> TagKey.create((ResourceKey<? extends Registry<T>>) key, id)); }

    @Override public Iterator<T> iterator() { return byIndex.stream().map(Holder.Reference::value).iterator(); }

    /** Holder lookup view of this registry. */
    @Override
    public HolderLookup.RegistryLookup<T> asLookup() {
        MappedRegistry<T> self = this;
        return new HolderLookup.RegistryLookup<>() {
            @Override public ResourceKey<? extends Registry<? extends T>> key() { return self.key; }
            @Override public Stream<Holder.Reference<T>> listElements() { return self.holders(); }
            @Override public Stream<HolderSet.Named<T>> listTags() { return self.getTagNames().map(self::getOrCreateTag); }
            @Override public Optional<Holder.Reference<T>> get(ResourceKey<T> k) { return self.getHolder(k); }
            @Override public Optional<HolderSet.Named<T>> get(TagKey<T> tag) { return self.getTag(tag); }
        };
    }

    public Map<ResourceLocation, T> entries() {
        Map<ResourceLocation, T> m = new LinkedHashMap<>();
        byId.forEach((id, h) -> m.put(id, h.value()));
        return m;
    }

    @Override public String toString() { return "Registry[" + key.location() + " (" + byId.size() + ")]"; }
}
