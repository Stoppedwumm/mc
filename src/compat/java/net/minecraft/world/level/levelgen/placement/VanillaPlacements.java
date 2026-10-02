package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Minecraft's placement modifiers (reamc-compat; registered when placement types are first needed). */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class VanillaPlacements {
    private VanillaPlacements() { }

    private static boolean done;

    public static synchronized void register() {
        if (done) return;
        done = true;
        reg("rarity_filter", Codec.INT.fieldOf("chance").xmap(c -> filter("rarity_filter", (ctx, r, p) -> r.nextFloat() < 1f / c), m -> 1));
        reg("in_square", MapCodec.unit(() -> modifier("in_square", (ctx, r, p) -> Stream.of(p.offset(r.nextInt(16), 0, r.nextInt(16))))));
        reg("heightmap", Heightmap.Types.CODEC.fieldOf("heightmap").xmap(t -> modifier("heightmap", (ctx, r, p) -> {
            int y = ctx.getHeight(t, p.getX(), p.getZ());
            return y > ctx.getMinBuildHeight() ? Stream.of(new BlockPos(p.getX(), y, p.getZ())) : Stream.empty();
        }), m -> Heightmap.Types.MOTION_BLOCKING));
        reg("biome", MapCodec.unit(() -> filter("biome", (ctx, r, p) -> true)));
        reg("count", Codec.INT.fieldOf("count").orElse(1).xmap(c -> modifier("count", (ctx, r, p) -> IntStream.range(0, c).mapToObj(i -> p)), m -> 1));
        reg("count_on_every_layer", Codec.INT.fieldOf("count").orElse(1).xmap(c -> modifier("count_on_every_layer", (ctx, r, p) -> IntStream.range(0, c).mapToObj(i -> {
            BlockPos q = p.offset(r.nextInt(16), 0, r.nextInt(16));
            return new BlockPos(q.getX(), ctx.getHeight(Heightmap.Types.MOTION_BLOCKING, q.getX(), q.getZ()), q.getZ());
        })), m -> 1));
        reg("block_predicate_filter", BlockPredicate.CODEC.fieldOf("predicate").xmap(pr -> filter("block_predicate_filter", (ctx, r, p) -> pr.test(ctx.getLevel(), p)), m -> BlockPredicate.alwaysTrue()));
        reg("random_offset", RecordCodecBuilder.<int[]>mapCodec(i -> i.group(
                Codec.INT.fieldOf("xz_spread").orElse(0).forGetter(a -> a[0]), Codec.INT.fieldOf("y_spread").orElse(0).forGetter(a -> a[1])
        ).apply(i, (a, b) -> new int[]{a, b})).xmap(a -> modifier("random_offset", (ctx, r, p) -> Stream.of(p.offset(a[0] == 0 ? 0 : r.nextInt(a[0] * 2 + 1) - a[0], a[1] == 0 ? 0 : r.nextInt(a[1] * 2 + 1) - a[1], a[0] == 0 ? 0 : r.nextInt(a[0] * 2 + 1) - a[0]))), m -> new int[2]));
        reg("height_range", Codec.PASSTHROUGH.fieldOf("height").xmap(h -> modifier("height_range", (ctx, r, p) -> Stream.of(new BlockPos(p.getX(), 1 + r.nextInt(64), p.getZ()))), m -> null));
        reg("surface_relative_threshold_filter", Codec.PASSTHROUGH.fieldOf("heightmap").xmap(h -> filter("surface_relative_threshold_filter", (ctx, r, p) -> true), m -> null));
        reg("noise_threshold_count", Codec.INT.fieldOf("above_noise").orElse(1).xmap(c -> modifier("noise_threshold_count", (ctx, r, p) -> IntStream.range(0, c).mapToObj(i -> p)), m -> 1));
        reg("noise_based_count", Codec.INT.fieldOf("noise_to_count_ratio").orElse(1).xmap(c -> modifier("noise_based_count", (ctx, r, p) -> IntStream.range(0, 1 + r.nextInt(3)).mapToObj(i -> p)), m -> 1));
        reg("environment_scan", MapCodec.unit(() -> filter("environment_scan", (ctx, r, p) -> true)));
        reg("carving_mask", MapCodec.unit(() -> filter("carving_mask", (ctx, r, p) -> false)));
    }

    private static void reg(String id, MapCodec<? extends PlacementModifier> codec) {
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(id);
        if (!BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.containsKey(rl)) Registry.register((Registry) BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, rl, (PlacementModifierType) () -> codec);
    }

    interface Positions { Stream<BlockPos> get(PlacementContext ctx, RandomSource r, BlockPos p); }

    interface Test { boolean test(PlacementContext ctx, RandomSource r, BlockPos p); }

    private static PlacementModifier modifier(String id, Positions f) {
        return new PlacementModifier() {
            @Override public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource r, BlockPos p) { return f.get(ctx, r, p); }
            @Override public PlacementModifierType<?> type() { return (PlacementModifierType<?>) BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.get(ResourceLocation.withDefaultNamespace(id)); }
        };
    }

    private static PlacementModifier filter(String id, Test t) {
        return new PlacementFilter() {
            @Override protected boolean shouldPlace(PlacementContext ctx, RandomSource r, BlockPos p) { return t.test(ctx, r, p); }
            @Override public PlacementModifierType<?> type() { return (PlacementModifierType<?>) BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.get(ResourceLocation.withDefaultNamespace(id)); }
        };
    }
}
