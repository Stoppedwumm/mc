package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A value with a stable name, for codecs and block state properties (reamc-compat). */
public interface StringRepresentable {
    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnum(Supplier<E[]> values) {
        return fromEnumWithMapping(values, s -> s);
    }

    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnumWithMapping(Supplier<E[]> values, Function<String, String> keyMapper) {
        E[] vals = values.get();
        Map<String, E> byName = Arrays.stream(vals).collect(Collectors.toMap(e -> keyMapper.apply(e.getSerializedName()), e -> e, (a, b) -> a));
        return new EnumCodec<>(vals, byName::get);
    }

    static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> values) {
        T[] vals = values.get();
        Map<String, T> byName = Arrays.stream(vals).collect(Collectors.toMap(StringRepresentable::getSerializedName, e -> e, (a, b) -> a));
        return Codec.STRING.comapFlatMap(s -> {
            T v = byName.get(s);
            return v != null ? DataResult.success(v) : DataResult.error(() -> "Unknown element name: " + s);
        }, StringRepresentable::getSerializedName);
    }

    static Keyable keys(StringRepresentable[] values) {
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) { return Arrays.stream(values).map(StringRepresentable::getSerializedName).map(ops::createString); }
        };
    }

    /** Codec for an enum by serialized name (reamc-compat). */
    class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {
        private final Codec<E> codec;
        private final Function<String, E> resolver;

        public EnumCodec(E[] values, Function<String, E> resolver) {
            this.resolver = resolver;
            this.codec = Codec.STRING.comapFlatMap(s -> {
                E v = resolver.apply(s);
                return v != null ? DataResult.success(v) : DataResult.error(() -> "Unknown element name: " + s);
            }, StringRepresentable::getSerializedName);
        }

        @Override
        public <T> DataResult<com.mojang.datafixers.util.Pair<E, T>> decode(DynamicOps<T> ops, T input) { return codec.decode(ops, input); }

        @Override
        public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) { return codec.encode(input, ops, prefix); }

        public E byName(String name) { return name == null ? null : resolver.apply(name); }

        public E byName(String name, E fallback) { E v = byName(name); return v != null ? v : fallback; }

        public E byName(String name, Supplier<? extends E> fallback) { E v = byName(name); return v != null ? v : fallback.get(); }
    }
}
