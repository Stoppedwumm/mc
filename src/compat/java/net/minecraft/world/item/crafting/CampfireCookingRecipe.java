package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public class CampfireCookingRecipe extends AbstractCookingRecipe {
    public CampfireCookingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(RecipeType.CAMPFIRE_COOKING, group, category, ingredient, result, experience, time);
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.CAMPFIRE_COOKING_RECIPE; }
}
