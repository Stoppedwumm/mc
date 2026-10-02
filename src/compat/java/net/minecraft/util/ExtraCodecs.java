package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Extra codecs (reamc-compat: the common ones). */
public class ExtraCodecs {
    public static final Codec<Integer> NON_NEGATIVE_INT = intRange(0, Integer.MAX_VALUE, "Value must be non-negative");
    public static final Codec<Integer> POSITIVE_INT = intRange(1, Integer.MAX_VALUE, "Value must be positive");
    public static final Codec<Float> POSITIVE_FLOAT = Codec.FLOAT.validate(f -> f > 0 ? DataResult.success(f) : DataResult.error(() -> "Value must be positive: " + f));
    public static final Codec<Float> NON_NEGATIVE_FLOAT = Codec.FLOAT.validate(f -> f >= 0 ? DataResult.success(f) : DataResult.error(() -> "Value must be non-negative: " + f));
    public static final Codec<String> NON_EMPTY_STRING = Codec.STRING.validate(s -> s.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(s));
    public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.INT;

    private static Codec<Integer> intRange(int min, int max, String msg) {
        return Codec.INT.validate(v -> v >= min && v <= max ? DataResult.success(v) : DataResult.error(() -> msg + ": " + v));
    }

    public static Codec<Integer> intRange(int min, int max) { return intRange(min, max, "Value outside of range [" + min + ":" + max + "]"); }

    public static <T> Codec<java.util.List<T>> nonEmptyList(Codec<java.util.List<T>> c) { return c.validate(l -> l.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(l)); }
}
