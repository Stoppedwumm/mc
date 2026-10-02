package net.minecraft.world.inventory;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

/** A menu with a recipe book (reamc-compat: the book itself is not shown). */
public abstract class RecipeBookMenu<I extends RecipeInput, R extends Recipe<I>> extends AbstractContainerMenu {
    public RecipeBookMenu(MenuType<?> type, int id) { super(type, id); }

    public void handlePlacement(boolean placeAll, RecipeHolder<?> recipe, net.minecraft.server.level.ServerPlayer player) { }

    public abstract void fillCraftSlotsStackedContents(StackedContents contents);

    public abstract void clearCraftingContent();

    public abstract boolean recipeMatches(RecipeHolder<R> recipe);

    public abstract int getResultSlotIndex();

    public abstract int getGridWidth();

    public abstract int getGridHeight();

    public abstract int getSize();

    public abstract RecipeBookType getRecipeBookType();

    public abstract boolean shouldMoveToInventory(int slot);
}
