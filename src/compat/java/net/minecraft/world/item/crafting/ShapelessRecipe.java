package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipe implements CraftingRecipe {
    final String group;
    final CraftingBookCategory category;
    final ItemStack result;
    final NonNullList<Ingredient> ingredients;

    public ShapelessRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        this.group = group; this.category = category; this.result = result; this.ingredients = ingredients;
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SHAPELESS_RECIPE; }
    @Override public String getGroup() { return group; }
    @Override public CraftingBookCategory category() { return category; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public NonNullList<Ingredient> getIngredients() { return ingredients; }
    @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= ingredients.size(); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) return false;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : input.items()) if (!s.isEmpty()) items.add(s);
        return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(items, ingredients) != null;
    }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return result.copy(); }

    public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
        public static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.category),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(l -> l.isEmpty() ? DataResult.error(() -> "No ingredients for shapeless recipe")
                        : DataResult.success(Ingredient.reamc$list(l)), DataResult::success).forGetter(r -> r.ingredients)
        ).apply(i, ShapelessRecipe::new));

        @Override public MapCodec<ShapelessRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> streamCodec() { return RecipeSerializer.reamc$stream(CODEC); }
    }
}
