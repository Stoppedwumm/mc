package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShapedRecipe implements CraftingRecipe {
    final ShapedRecipePattern pattern;
    final ItemStack result;
    final String group;
    final CraftingBookCategory category;
    final boolean showNotification;

    public ShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        this.group = group; this.category = category; this.pattern = pattern; this.result = result; this.showNotification = showNotification;
    }

    public ShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) { this(group, category, pattern, result, true); }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SHAPED_RECIPE; }
    @Override public String getGroup() { return group; }
    @Override public CraftingBookCategory category() { return category; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public NonNullList<Ingredient> getIngredients() { return pattern.ingredients(); }
    @Override public boolean showNotification() { return showNotification; }
    @Override public boolean canCraftInDimensions(int w, int h) { return w >= pattern.width() && h >= pattern.height(); }
    @Override public boolean matches(CraftingInput input, Level level) { return pattern.matches(input); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return result.copy(); }
    public int getWidth() { return pattern.width(); }
    public int getHeight() { return pattern.height(); }
    public ShapedRecipePattern reamc$pattern() { return pattern; }

    public static class Serializer implements RecipeSerializer<ShapedRecipe> {
        public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(r -> r.showNotification)
        ).apply(i, ShapedRecipe::new));

        @Override public MapCodec<ShapedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() { return RecipeSerializer.reamc$stream(CODEC); }
    }
}
