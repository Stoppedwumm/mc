package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A recipe of some type (reamc-compat). */
public interface Recipe<T extends RecipeInput> {
    boolean matches(T input, Level level);

    ItemStack assemble(T input, HolderLookup.Provider registries);

    boolean canCraftInDimensions(int width, int height);

    ItemStack getResultItem(HolderLookup.Provider registries);

    default NonNullList<ItemStack> getRemainingItems(T input) {
        NonNullList<ItemStack> l = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < l.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.hasCraftingRemainingItem()) l.set(i, s.getCraftingRemainingItem());
        }
        return l;
    }

    default NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }

    default boolean isSpecial() { return false; }

    default boolean showNotification() { return true; }

    default String getGroup() { return ""; }

    default ItemStack getToastSymbol() { return ItemStack.EMPTY; }

    RecipeSerializer<?> getSerializer();

    RecipeType<?> getType();

    default boolean isIncomplete() {
        NonNullList<Ingredient> l = getIngredients();
        return l.isEmpty() || l.stream().anyMatch(Ingredient::hasNoItems);
    }
}
