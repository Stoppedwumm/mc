package net.neoforged.neoforge.common.world;

/** A biome's settings as biome modifiers see them (reamc-compat: only generation settings). */
public class ModifiableBiomeInfo {
    private final BiomeInfo info;

    public ModifiableBiomeInfo(BiomeInfo info) { this.info = info; }

    public BiomeInfo get() { return info; }

    public record BiomeInfo(BiomeGenerationSettingsBuilder generationSettings) {
        public static class Builder {
            private final BiomeGenerationSettingsBuilder generation;

            public Builder(BiomeGenerationSettingsBuilder generation) { this.generation = generation; }

            public BiomeGenerationSettingsBuilder getGenerationSettings() { return generation; }

            public BiomeInfo build() { return new BiomeInfo(generation); }
        }
    }
}
