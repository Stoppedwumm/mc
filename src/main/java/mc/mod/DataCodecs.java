package mc.mod;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;

/** Codecs for references into data registries: an id, a "#tag", or the value written inline. */
public final class DataCodecs {
    private DataCodecs() { }

    /** A block state as {"Name": id, "Properties": {name: value}}. */
    public static final Codec<net.minecraft.world.level.block.state.BlockState> BLOCK_STATE = Codec.PASSTHROUGH.comapFlatMap(d -> {
        try {
            com.google.gson.JsonObject o = d.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(o.get("Name").getAsString()));
            var state = block.defaultBlockState();
            if (o.has("Properties")) for (var e : o.getAsJsonObject("Properties").entrySet()) state = withValue(state, e.getKey(), e.getValue().getAsString());
            return DataResult.success(state);
        } catch (RuntimeException e) {
            return DataResult.error(() -> "Bad block state: " + e.getMessage());
        }
    }, s -> {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("Name", String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock())));
        com.google.gson.JsonObject props = new com.google.gson.JsonObject();
        for (var p : s.getBlock().getStateDefinition().getProperties()) props.addProperty(p.getName(), valueName(s, p));
        if (props.size() > 0) o.add("Properties", props);
        return new Dynamic<>(JsonOps.INSTANCE, o);
    });

    private static <V extends Comparable<V>> net.minecraft.world.level.block.state.BlockState withValue(net.minecraft.world.level.block.state.BlockState s, String name, String value) {
        @SuppressWarnings("unchecked")
        var p = (net.minecraft.world.level.block.state.properties.Property<V>) s.getBlock().getStateDefinition().getProperty(name);
        if (p == null) return s;
        return p.getValue(value).map(v -> s.setValue(p, v)).orElse(s);
    }

    private static <V extends Comparable<V>> String valueName(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.block.state.properties.Property<V> p) { return p.getName(s.getValue(p)); }

    /** An id naming a registry entry, or the entry written out in place. */
    public static <E> Codec<Holder<E>> holder(ResourceKey<? extends Registry<E>> registry, Codec<E> direct) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input) {
                var asString = ops.getStringValue(input);
                if (asString.result().isPresent()) {
                    ResourceLocation id = ResourceLocation.parse(asString.result().get());
                    MappedRegistry<E> r = Bridge.dynamicRegistry(registry);
                    var h = r.getHolder(id);
                    if (h.isEmpty()) return DataResult.error(() -> "Unknown " + registry.location() + " entry " + id);
                    return DataResult.success(Pair.of(h.get(), input));
                }
                return direct.decode(ops, input).map(p -> p.mapFirst(Holder::direct));
            }

            @Override
            public <T> DataResult<T> encode(Holder<E> h, DynamicOps<T> ops, T prefix) {
                if (h.unwrapKey().isPresent()) return DataResult.success(ops.createString(h.unwrapKey().get().location().toString()));
                return direct.encode(h.value(), ops, prefix);
            }
        };
    }

    /** "#tag", one id, or a list of ids. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> Codec<HolderSet<E>> holderSet(ResourceKey<? extends Registry<E>> registry) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> ops, T input) {
                try {
                    JsonElement j = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
                    MappedRegistry<E> r = Bridge.dynamicRegistry(registry);
                    if (j.isJsonPrimitive() && j.getAsString().startsWith("#"))
                        return DataResult.success(Pair.of(r.getOrCreateTag(TagKey.create((ResourceKey) registry, ResourceLocation.parse(j.getAsString().substring(1)))), input));
                    List<Holder<E>> l = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    if (j.isJsonArray()) for (JsonElement e : j.getAsJsonArray()) ids.add(e.getAsString());
                    else ids.add(j.getAsString());
                    for (String id : ids) r.getHolder(ResourceLocation.parse(id)).ifPresent(l::add);
                    return DataResult.success(Pair.of(HolderSet.direct(l), input));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Bad holder set: " + e.getMessage());
                }
            }

            @Override
            public <T> DataResult<T> encode(HolderSet<E> s, DynamicOps<T> ops, T prefix) {
                if (s.unwrapKey().isPresent()) return DataResult.success(ops.createString("#" + s.unwrapKey().get().location()));
                return DataResult.success(ops.createList(s.stream().map(h -> ops.createString(h.getRegisteredName()))));
            }
        };
    }
}
