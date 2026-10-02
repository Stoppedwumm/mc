package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;

public record BlockStateProviderType<P extends BlockStateProvider>(MapCodec<P> codec) {
    public static final BlockStateProviderType<SimpleStateProvider> SIMPLE = new BlockStateProviderType<>(SimpleStateProvider.CODEC);
    public static final BlockStateProviderType<WeightedStateProvider> WEIGHTED = new BlockStateProviderType<>(WeightedStateProvider.CODEC);
}
