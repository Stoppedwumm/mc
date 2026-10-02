package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

/** A number an enchantment changes (reamc-compat: kept for mods' enchantment data). */
public interface EnchantmentValueEffect {
    Codec<EnchantmentValueEffect> CODEC = Codec.unit(() -> (level, random, value) -> value);

    float process(int level, RandomSource random, float value);

    default MapCodec<? extends EnchantmentValueEffect> codec() { return MapCodec.unit(this); }
}
