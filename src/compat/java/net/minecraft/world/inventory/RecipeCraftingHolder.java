package net.minecraft.world.inventory;

import net.minecraft.world.item.crafting.RecipeHolder;

/** Something that remembers the last recipe it made (reamc-compat). */
public interface RecipeCraftingHolder {
    void setRecipeUsed(RecipeHolder<?> recipe);

    RecipeHolder<?> getRecipeUsed();

    default void awardUsedRecipes(net.minecraft.world.entity.player.Player player, java.util.List<net.minecraft.world.item.ItemStack> items) { }

    default boolean setRecipeUsed(net.minecraft.world.level.Level level, net.minecraft.server.level.ServerPlayer player, RecipeHolder<?> recipe) {
        setRecipeUsed(recipe);
        return true;
    }
}
