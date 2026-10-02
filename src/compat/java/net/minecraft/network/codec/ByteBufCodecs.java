package net.minecraft.network.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Stream codecs for basic values (reamc-compat). Values described by a data codec travel as JSON text, which is
 * enough within one reamc process (singleplayer and LAN host).
 */
public interface ByteBufCodecs {
    int MAX_INITIAL_COLLECTION_SIZE = 65536;

    StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((b, v) -> b.writeByte(v), ByteBuf::readByte);
    StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((b, v) -> b.writeShort(v), ByteBuf::readShort);
    StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = StreamCodec.of((b, v) -> b.writeShort(v), ByteBuf::readUnsignedShort);
    StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(ByteBufCodecs::writeVarInt, ByteBufCodecs::readVarInt);
    StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(ByteBufCodecs::writeVarLong, ByteBufCodecs::readVarLong);
    StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = byteArray(Integer.MAX_VALUE);
    StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    StreamCodec<ByteBuf, Tag> TAG = tagCodec(() -> null);
    StreamCodec<ByteBuf, Tag> TRUSTED_TAG = TAG;
    StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(() -> null);
    StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = COMPOUND_TAG;
    StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = optional(COMPOUND_TAG);
    StreamCodec<ByteBuf, org.joml.Vector3f> VECTOR3F = StreamCodec.composite(FLOAT, org.joml.Vector3f::x, FLOAT, org.joml.Vector3f::y, FLOAT, org.joml.Vector3f::z, org.joml.Vector3f::new);
    StreamCodec<ByteBuf, org.joml.Quaternionf> QUATERNIONF = StreamCodec.composite(FLOAT, org.joml.Quaternionf::x, FLOAT, org.joml.Quaternionf::y, FLOAT, org.joml.Quaternionf::z, FLOAT, org.joml.Quaternionf::w, org.joml.Quaternionf::new);

    static void writeVarInt(ByteBuf b, int v) {
        while ((v & ~0x7F) != 0) {
            b.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        b.writeByte(v);
    }

    static int readVarInt(ByteBuf b) {
        int v = 0, shift = 0;
        byte x;
        do {
            x = b.readByte();
            v |= (x & 0x7F) << shift;
            shift += 7;
        } while ((x & 0x80) != 0);
        return v;
    }

    static void writeVarLong(ByteBuf b, long v) {
        while ((v & ~0x7FL) != 0) {
            b.writeByte((int) (v & 0x7F) | 0x80);
            v >>>= 7;
        }
        b.writeByte((int) v);
    }

    static long readVarLong(ByteBuf b) {
        long v = 0;
        int shift = 0;
        byte x;
        do {
            x = b.readByte();
            v |= (long) (x & 0x7F) << shift;
            shift += 7;
        } while ((x & 0x80) != 0);
        return v;
    }

    static StreamCodec<ByteBuf, byte[]> byteArray(int max) {
        return StreamCodec.of((b, v) -> { writeVarInt(b, v.length); b.writeBytes(v); }, b -> {
            byte[] v = new byte[readCount(b, max)];
            b.readBytes(v);
            return v;
        });
    }

    static StreamCodec<ByteBuf, String> stringUtf8(int max) {
        return StreamCodec.of((b, v) -> {
            byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
            writeVarInt(b, bytes.length);
            b.writeBytes(bytes);
        }, b -> {
            byte[] bytes = new byte[readVarInt(b)];
            b.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        });
    }

    static StreamCodec<ByteBuf, Tag> tagCodec(Supplier<?> accounter) {
        return STRING_UTF8.map(s -> s.isEmpty() ? null : CompoundTag.reamc$fromJson(s), t -> t == null ? "" : CompoundTag.reamc$toJson(t));
    }

    static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<?> accounter) {
        return tagCodec(accounter).map(t -> (CompoundTag) t, t -> t);
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return STRING_UTF8.map(s -> codec.parse(JsonOps.INSTANCE, JsonParser.parseString(s)).getOrThrow(),
                v -> codec.encodeStart(JsonOps.INSTANCE, v).getOrThrow().toString());
    }

    static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) { return fromCodec(codec); }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<?> accounter) { return fromCodec(codec); }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) { return fromCodec(codec).cast(); }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> codec) { return fromCodecWithRegistries(codec); }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec, Supplier<?> accounter) { return fromCodecWithRegistries(codec); }

    static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return StreamCodec.of((b, v) -> {
            b.writeBoolean(v.isPresent());
            v.ifPresent(x -> codec.encode(b, x));
        }, b -> b.readBoolean() ? Optional.of(codec.decode(b)) : Optional.empty());
    }

    static int readCount(ByteBuf b, int max) {
        int n = readVarInt(b);
        if (n > max) throw new IllegalStateException(n + " elements exceeded max size of: " + max);
        return n;
    }

    static void writeCount(ByteBuf b, int n, int max) {
        if (n > max) throw new IllegalStateException(n + " elements exceeded max size of: " + max);
        writeVarInt(b, n);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec) {
        return collection(factory, codec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec, int max) {
        return StreamCodec.of((b, c) -> {
            writeCount(b, c.size(), max);
            for (V v : c) codec.encode(b, v);
        }, b -> {
            int n = readCount(b, max);
            C c = factory.apply(Math.min(n, MAX_INITIAL_COLLECTION_SIZE));
            for (int i = 0; i < n; i++) c.add(codec.decode(b));
            return c;
        });
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
        return c -> collection(factory, c);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return c -> collection(java.util.ArrayList::new, c);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int max) {
        return c -> collection(java.util.ArrayList::new, c, max);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory, StreamCodec<? super B, K> keys, StreamCodec<? super B, V> values) {
        return map(factory, keys, values, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory, StreamCodec<? super B, K> keys, StreamCodec<? super B, V> values, int max) {
        return StreamCodec.of((b, m) -> {
            writeCount(b, m.size(), max);
            m.forEach((k, v) -> { keys.encode(b, k); values.encode(b, v); });
        }, b -> {
            int n = readCount(b, max);
            M m = factory.apply(Math.min(n, MAX_INITIAL_COLLECTION_SIZE));
            for (int i = 0; i < n; i++) m.put(keys.decode(b), values.decode(b));
            return m;
        });
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> left, StreamCodec<? super B, R> right) {
        return StreamCodec.of((b, e) -> e.ifLeft(l -> { b.writeBoolean(true); left.encode(b, l); }).ifRight(r -> { b.writeBoolean(false); right.encode(b, r); }),
                b -> b.readBoolean() ? Either.left(left.decode(b)) : Either.right(right.decode(b)));
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return StreamCodec.of((b, v) -> writeVarInt(b, toId.applyAsInt(v)), b -> byId.apply(readVarInt(b)));
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> map) { return idMapper(map::byIdOrThrow, map::getIdOrThrow); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Registry<T> registryOf(ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) mc.mod.Bridge.registryAccess().registryOrThrow((ResourceKey) key);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> key) {
        return StreamCodec.of((b, v) -> writeVarInt(b, registryOf(key).getIdOrThrow(v)), b -> registryOf(key).byIdOrThrow(readVarInt(b)));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> key) {
        return StreamCodec.of((b, h) -> writeVarInt(b, registryOf(key).getIdOrThrow(h.value())),
                b -> registryOf(key).getHolder(readVarInt(b)).<Holder<T>>map(r -> r).orElseThrow());
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(ResourceKey<? extends Registry<T>> key, StreamCodec<? super RegistryFriendlyByteBuf, T> direct) {
        return StreamCodec.of((b, h) -> {
            if (h.kind() == Holder.Kind.REFERENCE) writeVarInt(b, registryOf(key).getIdOrThrow(h.value()) + 1);
            else {
                writeVarInt(b, 0);
                direct.encode(b, h.value());
            }
        }, b -> {
            int id = readVarInt(b);
            return id == 0 ? Holder.direct(direct.decode(b)) : registryOf(key).getHolder(id - 1).<Holder<T>>map(r -> r).orElseThrow();
        });
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(ResourceKey<? extends Registry<T>> key) {
        StreamCodec<RegistryFriendlyByteBuf, Holder<T>> one = holderRegistry(key);
        return StreamCodec.of((b, s) -> {
            writeVarInt(b, s.size());
            for (Holder<T> h : s) one.encode(b, h);
        }, b -> {
            int n = readVarInt(b);
            java.util.List<Holder<T>> l = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) l.add(one.decode(b));
            return HolderSet.direct(l);
        });
    }
}
