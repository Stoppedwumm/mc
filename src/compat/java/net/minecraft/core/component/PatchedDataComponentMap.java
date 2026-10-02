package net.minecraft.core.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An item's default components plus a stack's own changes (reamc-compat). The changes are what an engine stack
 * carries and saves (as JSON through each component's codec).
 */
public final class PatchedDataComponentMap implements DataComponentMap, mc.item.ItemComponents {
    private final DataComponentMap prototype;
    private final Map<DataComponentType<?>, Optional<?>> patch = new IdentityHashMap<>();

    public PatchedDataComponentMap(DataComponentMap prototype) { this.prototype = prototype; }

    static {
        mc.item.ItemComponents.PARSER[0] = PatchedDataComponentMap::reamc$parse;
    }

    public static PatchedDataComponentMap fromPatch(DataComponentMap prototype, DataComponentPatch p) {
        PatchedDataComponentMap m = new PatchedDataComponentMap(prototype);
        m.applyPatch(p);
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        Optional<?> o = patch.get(type);
        if (o != null) return (T) o.orElse(null);
        return prototype.get(type);
    }

    public <T> T set(DataComponentType<? super T> type, T value) {
        @SuppressWarnings("unchecked") T old = (T) get((DataComponentType) type);
        if (value == null) { remove(type); return old; }
        if (java.util.Objects.equals(value, prototype.get(type))) patch.remove(type);
        else patch.put(type, Optional.of(value));
        return old;
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(DataComponentType<? extends T> type) {
        T old = get(type);
        if (prototype.has(type)) patch.put(type, Optional.empty());
        else patch.remove(type);
        return old;
    }

    public void applyPatch(DataComponentPatch p) { p.changes.forEach(patch::put); }

    public void restorePatch(DataComponentPatch p) { patch.clear(); applyPatch(p); }

    public void setAll(DataComponentMap m) { for (TypedDataComponent<?> t : m) setTyped(t); }

    private <T> void setTyped(TypedDataComponent<T> t) { set(t.type(), t.value()); }

    @Override
    public Set<DataComponentType<?>> keySet() {
        Set<DataComponentType<?>> s = new LinkedHashSet<>(prototype.keySet());
        patch.forEach((t, v) -> { if (v.isPresent()) s.add(t); else s.remove(t); });
        return s;
    }

    public DataComponentPatch asPatch() { return new DataComponentPatch(new IdentityHashMap<>(patch)); }

    @Override
    public PatchedDataComponentMap copy() {
        PatchedDataComponentMap m = new PatchedDataComponentMap(prototype);
        m.patch.putAll(patch);
        return m;
    }

    @Override public boolean isEmpty() { return patch.isEmpty(); }

    @Override public boolean equals(Object o) { return o instanceof PatchedDataComponentMap m && m.patch.equals(patch); }
    @Override public int hashCode() { return patch.hashCode(); }
    @Override public String toString() { return "Components" + patch; }

    // ------------------------------------------------------------------ saving with engine stacks

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public String save() {
        JsonObject o = new JsonObject();
        patch.forEach((type, value) -> {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id == null || type.isTransient()) return;
            if (value.isEmpty()) { o.add("!" + id, new JsonObject()); return; }
            ((Codec) type.codec()).encodeStart(JsonOps.INSTANCE, value.get()).result().ifPresent(j -> o.add(id.toString(), (JsonElement) j));
        });
        return o.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static PatchedDataComponentMap reamc$parse(String json) {
        PatchedDataComponentMap m = new PatchedDataComponentMap(DataComponentMap.EMPTY);
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            for (var e : o.entrySet()) {
                boolean removed = e.getKey().startsWith("!");
                DataComponentType<?> type = (DataComponentType<?>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(removed ? e.getKey().substring(1) : e.getKey()));
                if (type == null || type.isTransient()) continue;
                if (removed) { m.patch.put(type, Optional.empty()); continue; }
                ((Codec) type.codec()).parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(v -> m.patch.put(type, Optional.of(v)));
            }
        } catch (RuntimeException ex) {
            System.err.println("[mods] Bad item components " + json + ": " + ex);
        }
        return m;
    }

    /** The same changes over an item's real defaults (stacks are parsed before their item is known). */
    public PatchedDataComponentMap reamc$withPrototype(DataComponentMap proto) {
        if (proto == prototype) return this;
        PatchedDataComponentMap m = new PatchedDataComponentMap(proto);
        m.patch.putAll(patch);
        return m;
    }
}
