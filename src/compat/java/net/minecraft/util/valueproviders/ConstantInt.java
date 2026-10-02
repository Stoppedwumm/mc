package net.minecraft.util.valueproviders;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.RandomSource;

/** Always the same number (reamc-compat). */
public class ConstantInt extends IntProvider {
    public static final ConstantInt ZERO = new ConstantInt(0);
    private final int value;

    public static ConstantInt of(int value) { return value == 0 ? ZERO : new ConstantInt(value); }

    private ConstantInt(int value) { this.value = value; }

    public int getValue() { return value; }
    @Override public int sample(RandomSource random) { return value; }
    @Override public int getMinValue() { return value; }
    @Override public int getMaxValue() { return value; }
    @Override JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public String toString() { return Integer.toString(value); }
}
