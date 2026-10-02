package net.neoforged.neoforge.common.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The features a biome places, by step; reamc runs them after its own terrain decoration (reamc-compat). */
public class BiomeGenerationSettingsBuilder {
    private final Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> features = new EnumMap<>(GenerationStep.Decoration.class);

    public BiomeGenerationSettingsBuilder addFeature(GenerationStep.Decoration step, Holder<PlacedFeature> feature) {
        features.computeIfAbsent(step, s -> new ArrayList<>()).add(feature);
        return this;
    }

    public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration step) { return features.computeIfAbsent(step, s -> new ArrayList<>()); }

    public Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> reamc$features() { return features; }
}
