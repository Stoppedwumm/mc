package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public class BlastingRecipe extends AbstractCookingRecipe {
    public BlastingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(RecipeType.BLASTING, group, category, ingredient, result, experience, time);
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.BLASTING_RECIPE; }
}
