package net.minecraft.world.flag;

public final class FeatureFlags {
    private FeatureFlags() { }

    public static final FeatureFlag VANILLA = new FeatureFlag(), BUNDLE = new FeatureFlag(), TRADE_REBALANCE = new FeatureFlag();
    public static final FeatureFlagSet DEFAULT_FLAGS = FeatureFlagSet.of();
}
