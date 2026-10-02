package net.minecraft.world.level.levelgen.feature.configurations;

import java.util.stream.Stream;

/** A feature's settings (reamc-compat). */
public interface FeatureConfiguration {
    NoneFeatureConfiguration NONE = NoneFeatureConfiguration.INSTANCE;

    default Stream<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getFeatures() { return Stream.empty(); }
}
