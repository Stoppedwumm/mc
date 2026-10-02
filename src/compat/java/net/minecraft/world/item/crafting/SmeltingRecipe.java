package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public class SmeltingRecipe extends AbstractCookingRecipe {
    public SmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(RecipeType.SMELTING, group, category, ingredient, result, experience, time);
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SMELTING_RECIPE; }
}
