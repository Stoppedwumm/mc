package net.minecraft.world.item.crafting;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** A kind of recipe (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface RecipeType<T extends Recipe<?>> {
    RecipeType<CraftingRecipe> CRAFTING = register("crafting");
    RecipeType<SmeltingRecipe> SMELTING = register("smelting");
    RecipeType<BlastingRecipe> BLASTING = register("blasting");
    RecipeType<SmokingRecipe> SMOKING = register("smoking");
    RecipeType<CampfireCookingRecipe> CAMPFIRE_COOKING = register("campfire_cooking");
    RecipeType<Recipe<?>> STONECUTTING = register("stonecutting");
    RecipeType<Recipe<?>> SMITHING = register("smithing");

    static <T extends Recipe<?>> RecipeType<T> register(String id) {
        return (RecipeType<T>) Registry.register((Registry) BuiltInRegistries.RECIPE_TYPE, ResourceLocation.withDefaultNamespace(id), simple(ResourceLocation.withDefaultNamespace(id)));
    }

    static <T extends Recipe<?>> RecipeType<T> simple(ResourceLocation id) {
        return new RecipeType<>() {
            @Override public String toString() { return id.toString(); }
        };
    }
}
