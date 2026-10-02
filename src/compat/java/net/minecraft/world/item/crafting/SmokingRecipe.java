package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public class SmokingRecipe extends AbstractCookingRecipe {
    public SmokingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(RecipeType.SMOKING, group, category, ingredient, result, experience, time);
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SMOKING_RECIPE; }
}
