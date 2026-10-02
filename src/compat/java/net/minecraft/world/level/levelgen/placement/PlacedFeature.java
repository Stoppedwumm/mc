package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** A configured feature plus where it goes (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public record PlacedFeature(Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
    public static final Codec<PlacedFeature> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ConfiguredFeature.CODEC.fieldOf("feature").forGetter(PlacedFeature::feature),
            PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter(PlacedFeature::placement)
    ).apply(i, PlacedFeature::new));
    public static final Codec<Holder<PlacedFeature>> CODEC = mc.mod.DataCodecs.holder((ResourceKey) Registries.PLACED_FEATURE, DIRECT_CODEC);
    public static final Codec<HolderSet<PlacedFeature>> LIST_CODEC = mc.mod.DataCodecs.holderSet((ResourceKey) Registries.PLACED_FEATURE);
    public static final Codec<List<HolderSet<PlacedFeature>>> LIST_OF_LISTS_CODEC = LIST_CODEC.listOf();

    public boolean place(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos pos) {
        return placeWithContext(new PlacementContext(level, gen, Optional.empty()), random, pos);
    }

    public boolean placeWithBiomeCheck(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos pos) {
        return placeWithContext(new PlacementContext(level, gen, Optional.of(this)), random, pos);
    }

    private boolean placeWithContext(PlacementContext ctx, RandomSource random, BlockPos origin) {
        Stream<BlockPos> positions = Stream.of(origin);
        for (PlacementModifier m : placement) positions = positions.flatMap(p -> m.getPositions(ctx, random, p));
        ConfiguredFeature<?, ?> f = feature.value();
        boolean[] any = {false};
        positions.forEach(p -> { if (f.place(ctx.getLevel(), ctx.generator(), random, p)) any[0] = true; });
        return any[0];
    }

    public Stream<ConfiguredFeature<?, ?>> getFeatures() { return Stream.of(feature.value()); }
}
