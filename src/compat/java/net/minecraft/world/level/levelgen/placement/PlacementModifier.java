package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

import java.util.stream.Stream;

/** Turns a position into positions to place at (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class PlacementModifier {
    public static final Codec<PlacementModifier> CODEC = ((Codec<PlacementModifierType<?>>) (Codec) BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.byNameCodec())
            .dispatch(PlacementModifier::type, t -> (MapCodec) t.codec());

    public abstract Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos);

    public abstract PlacementModifierType<?> type();
}
