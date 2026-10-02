package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** One of several states by weight (reamc-compat). */
public class WeightedStateProvider extends BlockStateProvider {
    record Entry(BlockState data, int weight) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(BlockState.CODEC.fieldOf("data").forGetter(Entry::data), Codec.INT.fieldOf("weight").forGetter(Entry::weight)).apply(i, Entry::new));
    }

    public static final MapCodec<WeightedStateProvider> CODEC = Entry.CODEC.listOf().fieldOf("entries").xmap(WeightedStateProvider::new, p -> p.entries);
    private final List<Entry> entries;

    WeightedStateProvider(List<Entry> entries) { this.entries = entries; }

    @Override protected BlockStateProviderType<?> type() { return BlockStateProviderType.WEIGHTED; }

    @Override
    public BlockState getState(RandomSource random, BlockPos pos) {
        int total = entries.stream().mapToInt(Entry::weight).sum();
        int r = random.nextInt(Math.max(1, total));
        for (Entry e : entries) if ((r -= e.weight) < 0) return e.data;
        return entries.get(0).data;
    }
}
