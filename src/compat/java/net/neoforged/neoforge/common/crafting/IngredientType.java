package net.neoforged.neoforge.common.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** How a custom ingredient is read and written (reamc-compat). */
public record IngredientType<T extends ICustomIngredient>(MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
    public IngredientType(MapCodec<T> codec) { this(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec())); }
}
