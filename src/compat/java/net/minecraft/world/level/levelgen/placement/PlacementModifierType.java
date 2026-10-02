package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;

/** A kind of placement modifier (reamc-compat). */
@FunctionalInterface
public interface PlacementModifierType<P extends PlacementModifier> {
    MapCodec<P> codec();
}
