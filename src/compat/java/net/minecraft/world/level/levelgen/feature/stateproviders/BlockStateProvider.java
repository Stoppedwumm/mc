package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Chooses the block a feature places (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BlockStateProvider {
    static {
        Registry.register((Registry) BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, ResourceLocation.withDefaultNamespace("simple_state_provider"), BlockStateProviderType.SIMPLE);
        Registry.register((Registry) BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, ResourceLocation.withDefaultNamespace("weighted_state_provider"), BlockStateProviderType.WEIGHTED);
    }

    public static final Codec<BlockStateProvider> CODEC = ((Codec<BlockStateProviderType<?>>) (Codec) BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE.byNameCodec())
            .dispatch(BlockStateProvider::type, t -> (MapCodec) t.codec());

    public static SimpleStateProvider simple(BlockState s) { return new SimpleStateProvider(s); }

    public static SimpleStateProvider simple(Block b) { return simple(b.defaultBlockState()); }

    protected abstract BlockStateProviderType<?> type();

    public abstract BlockState getState(RandomSource random, BlockPos pos);
}
