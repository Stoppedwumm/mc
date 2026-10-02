package net.minecraft.world.flag;

public interface FeatureElement {
    FeatureFlagSet requiredFeatures();

    default boolean isEnabled(FeatureFlagSet enabled) { return true; }
}
