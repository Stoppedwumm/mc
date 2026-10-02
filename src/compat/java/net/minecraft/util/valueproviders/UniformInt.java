package net.minecraft.util.valueproviders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;

/** A random number in a range, both ends included (reamc-compat). */
public class UniformInt extends IntProvider {
    private final int min, max;

    public static UniformInt of(int min, int max) { return new UniformInt(min, max); }

    private UniformInt(int min, int max) { this.min = min; this.max = max; }

    @Override public int sample(RandomSource random) { return min + random.nextInt(max - min + 1); }
    @Override public int getMinValue() { return min; }
    @Override public int getMaxValue() { return max; }

    @Override
    JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:uniform");
        o.addProperty("min_inclusive", min);
        o.addProperty("max_inclusive", max);
        return o;
    }

    @Override public String toString() { return "[" + min + "-" + max + "]"; }
}
