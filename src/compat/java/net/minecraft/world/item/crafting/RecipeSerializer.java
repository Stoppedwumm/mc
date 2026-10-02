package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Reads and writes a kind of recipe (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface RecipeSerializer<T extends Recipe<?>> {
    RecipeSerializer<ShapedRecipe> SHAPED_RECIPE = register("crafting_shaped", new ShapedRecipe.Serializer());
    RecipeSerializer<ShapelessRecipe> SHAPELESS_RECIPE = register("crafting_shapeless", new ShapelessRecipe.Serializer());
    RecipeSerializer<SmeltingRecipe> SMELTING_RECIPE = register("smelting", new AbstractCookingRecipe.Serializer<>(SmeltingRecipe::new, 200));
    RecipeSerializer<BlastingRecipe> BLASTING_RECIPE = register("blasting", new AbstractCookingRecipe.Serializer<>(BlastingRecipe::new, 100));
    RecipeSerializer<SmokingRecipe> SMOKING_RECIPE = register("smoking", new AbstractCookingRecipe.Serializer<>(SmokingRecipe::new, 100));
    RecipeSerializer<CampfireCookingRecipe> CAMPFIRE_COOKING_RECIPE = register("campfire_cooking", new AbstractCookingRecipe.Serializer<>(CampfireCookingRecipe::new, 100));

    MapCodec<T> codec();

    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

    static <S extends RecipeSerializer<T>, T extends Recipe<?>> S register(String id, S serializer) {
        return (S) Registry.register((Registry) BuiltInRegistries.RECIPE_SERIALIZER, ResourceLocation.withDefaultNamespace(id), serializer);
    }

    /** A stream codec from a map codec (recipes travel as JSON text in reamc). */
    static <T> StreamCodec<RegistryFriendlyByteBuf, T> reamc$stream(MapCodec<T> codec) { return ByteBufCodecs.fromCodecWithRegistries(codec.codec()); }
}
