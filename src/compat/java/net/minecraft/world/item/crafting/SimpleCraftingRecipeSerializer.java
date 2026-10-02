package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SimpleCraftingRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public SimpleCraftingRecipeSerializer(Factory<T> factory) {
        codec = RecordCodecBuilder.mapCodec(i -> i.group(CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CraftingRecipe::category)).apply(i, factory::create));
        streamCodec = CraftingBookCategory.STREAM_CODEC.map(factory::create, CraftingRecipe::category).cast();
    }

    @Override public MapCodec<T> codec() { return codec; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() { return streamCodec; }

    @FunctionalInterface
    public interface Factory<T extends CraftingRecipe> {
        T create(CraftingBookCategory category);
    }
}
