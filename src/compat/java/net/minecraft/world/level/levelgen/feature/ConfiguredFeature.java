package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
public record ConfiguredFeature<FC extends FeatureConfiguration, F extends Feature<FC>>(F feature, FC config) {
    public static final Codec<ConfiguredFeature<?, ?>> DIRECT_CODEC = ((Codec<Feature<?>>) (Codec) BuiltInRegistries.FEATURE.byNameCodec())
            .dispatch(c -> ((ConfiguredFeature) c).feature, f -> (MapCodec) f.configuredCodec());
    public static final Codec<Holder<ConfiguredFeature<?, ?>>> CODEC = mc.mod.DataCodecs.holder((net.minecraft.resources.ResourceKey) Registries.CONFIGURED_FEATURE, DIRECT_CODEC);
    public static final Codec<net.minecraft.core.HolderSet<ConfiguredFeature<?, ?>>> LIST_CODEC = mc.mod.DataCodecs.holderSet((net.minecraft.resources.ResourceKey) Registries.CONFIGURED_FEATURE);

    public boolean place(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos origin) {
        return feature.place(config, level, gen, random, origin);
    }
}
