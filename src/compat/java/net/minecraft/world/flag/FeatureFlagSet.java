package net.minecraft.world.flag;

/** Experimental feature flags; reamc enables everything (reamc-compat). */
public final class FeatureFlagSet {
    private static final FeatureFlagSet EMPTY = new FeatureFlagSet();

    private FeatureFlagSet() { }

    public static FeatureFlagSet of() { return EMPTY; }
    public static FeatureFlagSet of(FeatureFlag flag) { return EMPTY; }
    public static FeatureFlagSet of(FeatureFlag flag, FeatureFlag... more) { return EMPTY; }
    public boolean isEmpty() { return true; }
    public boolean contains(FeatureFlag flag) { return true; }
    public boolean isSubsetOf(FeatureFlagSet other) { return true; }
    public boolean intersects(FeatureFlagSet other) { return false; }
    public FeatureFlagSet join(FeatureFlagSet other) { return this; }
    public FeatureFlagSet subtract(FeatureFlagSet other) { return this; }
}
