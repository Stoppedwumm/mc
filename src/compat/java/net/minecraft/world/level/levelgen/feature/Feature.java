package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

import java.util.Optional;

/** Something worldgen places: a patch of plants, a block... (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Feature<FC extends FeatureConfiguration> {
    public static final Feature<NoneFeatureConfiguration> NO_OP = register("no_op", new Feature<>(NoneFeatureConfiguration.CODEC) {
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) { return true; }
    });
    public static final Feature<SimpleBlockConfiguration> SIMPLE_BLOCK = register("simple_block", new Feature<>(SimpleBlockConfiguration.CODEC) {
        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            BlockState s = ctx.config().toPlace().getState(ctx.random(), ctx.origin());
            if (!s.canSurvive(ctx.level(), ctx.origin())) return false;
            ctx.level().setBlock(ctx.origin(), s, 2);
            return true;
        }
    });
    public static final Feature<RandomPatchConfiguration> RANDOM_PATCH = register("random_patch", new Feature<>(RandomPatchConfiguration.CODEC) {
        @Override
        public boolean place(FeaturePlaceContext<RandomPatchConfiguration> ctx) {
            RandomPatchConfiguration c = ctx.config();
            RandomSource r = ctx.random();
            int placed = 0, xz = c.xzSpread() + 1, y = c.ySpread() + 1;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int i = 0; i < c.tries(); i++) {
                m.setWithOffset(ctx.origin(), r.nextInt(xz) - r.nextInt(xz), r.nextInt(y) - r.nextInt(y), r.nextInt(xz) - r.nextInt(xz));
                if (c.feature().value().place(ctx.level(), ctx.chunkGenerator(), r, m)) placed++;
            }
            return placed > 0;
        }
    });

    private static <F extends Feature<?>> F register(String id, F f) {
        return (F) Registry.register((Registry) BuiltInRegistries.FEATURE, ResourceLocation.withDefaultNamespace(id), f);
    }

    private final MapCodec<ConfiguredFeature<FC, Feature<FC>>> configuredCodec;

    public Feature(Codec<FC> codec) { configuredCodec = codec.fieldOf("config").xmap(c -> new ConfiguredFeature<>(this, c), ConfiguredFeature::config); }

    public MapCodec<ConfiguredFeature<FC, Feature<FC>>> configuredCodec() { return configuredCodec; }

    protected void setBlock(LevelWriter level, BlockPos pos, BlockState state) { level.setBlock(pos, state, 3); }

    public abstract boolean place(FeaturePlaceContext<FC> context);

    public boolean place(FC config, WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos origin) {
        return level.ensureCanWrite(origin) && place(new FeaturePlaceContext<>(Optional.empty(), level, gen, random, origin, config));
    }

    public static boolean isStone(BlockState s) { return s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD); }
    public static boolean isDirt(BlockState s) { return s.is(net.minecraft.tags.BlockTags.DIRT); }
}
