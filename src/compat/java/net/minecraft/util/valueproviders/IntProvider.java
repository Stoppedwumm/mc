package net.minecraft.util.valueproviders;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.RandomSource;

/** A source of whole numbers: a constant or a random range (reamc-compat). */
public abstract class IntProvider {
    /** A number, or {"type": "uniform"/"biased_to_bottom"/"constant"/..., ...}. */
    public static final Codec<IntProvider> CODEC = Codec.PASSTHROUGH.comapFlatMap(d -> {
        try {
            return DataResult.success(fromJson(d.convert(JsonOps.INSTANCE).getValue()));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "Bad int provider: " + e.getMessage());
        }
    }, p -> new Dynamic<>(JsonOps.INSTANCE, p.toJson()));
    public static final Codec<IntProvider> NON_NEGATIVE_CODEC = CODEC;
    public static final Codec<IntProvider> POSITIVE_CODEC = CODEC;

    public static Codec<IntProvider> codec(int min, int max) { return CODEC; }

    static IntProvider fromJson(JsonElement j) {
        if (j.isJsonPrimitive()) return ConstantInt.of(j.getAsInt());
        var o = j.getAsJsonObject();
        String type = o.get("type").getAsString().replace("minecraft:", "");
        return switch (type) {
            case "constant" -> ConstantInt.of(o.get("value").getAsInt());
            case "uniform", "biased_to_bottom", "clamped", "trapezoid" -> {
                if (o.has("min_inclusive")) yield UniformInt.of(o.get("min_inclusive").getAsInt(), o.get("max_inclusive").getAsInt());
                if (o.has("source")) yield fromJson(o.get("source"));
                yield UniformInt.of(o.has("min") ? o.get("min").getAsInt() : 0, o.has("max") ? o.get("max").getAsInt() : 0);
            }
            default -> throw new IllegalArgumentException("unknown int provider " + type);
        };
    }

    abstract JsonElement toJson();

    public abstract int sample(RandomSource random);
    public abstract int getMinValue();
    public abstract int getMaxValue();
}
