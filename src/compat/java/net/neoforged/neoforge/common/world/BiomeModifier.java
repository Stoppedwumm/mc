package net.neoforged.neoforge.common.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Changes biomes when the world loads, in phases (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface BiomeModifier {
    Codec<BiomeModifier> DIRECT_CODEC = Codec.lazyInitialized(() -> ((Codec<MapCodec<? extends BiomeModifier>>) (Codec) NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS.byNameCodec())
            .dispatch(BiomeModifier::codec, c -> (MapCodec) c));
    Codec<Holder<BiomeModifier>> REFERENCE_CODEC = Codec.lazyInitialized(() -> mc.mod.DataCodecs.holder(NeoForgeRegistries.Keys.BIOME_MODIFIERS, DIRECT_CODEC));

    void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder);

    MapCodec<? extends BiomeModifier> codec();

    enum Phase { BEFORE_EVERYTHING, ADD, REMOVE, MODIFY, AFTER_EVERYTHING }
}
