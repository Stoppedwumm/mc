package net.neoforged.neoforge.common.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.stream.Stream;

/** An ingredient with its own matching rules (reamc-compat). */
public interface ICustomIngredient {
    boolean test(ItemStack stack);

    Stream<ItemStack> getItems();

    boolean isSimple();

    IngredientType<?> getType();

    default Ingredient toVanilla() { return new Ingredient(this); }
}
