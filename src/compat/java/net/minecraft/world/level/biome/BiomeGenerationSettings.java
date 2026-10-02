package net.minecraft.world.level.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;

/** Features a biome places (reamc-compat: mods' additions, placed after reamc's own decoration). */
public class BiomeGenerationSettings {
    public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(List.of());
    private final List<HolderSet<PlacedFeature>> features;

    BiomeGenerationSettings(List<HolderSet<PlacedFeature>> features) { this.features = features; }

    public List<HolderSet<PlacedFeature>> features() { return features; }

    public static class PlainBuilder {
        protected final List<List<Holder<PlacedFeature>>> features = new ArrayList<>();

        public PlainBuilder addFeature(GenerationStep.Decoration step, Holder<PlacedFeature> feature) { return addFeature(step.ordinal(), feature); }

        public PlainBuilder addFeature(int step, Holder<PlacedFeature> feature) {
            while (features.size() <= step) features.add(new ArrayList<>());
            features.get(step).add(feature);
            return this;
        }

        public BiomeGenerationSettings build() {
            List<HolderSet<PlacedFeature>> l = new ArrayList<>();
            for (List<Holder<PlacedFeature>> s : features) l.add(HolderSet.direct(s));
            return new BiomeGenerationSettings(l);
        }
    }
}
