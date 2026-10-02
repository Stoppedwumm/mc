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

/** Furnace, smoker, blast furnace and campfire recipes (reamc-compat). */
public abstract class AbstractCookingRecipe implements Recipe<SingleRecipeInput> {
    protected final RecipeType<?> type;
    protected final CookingBookCategory category;
    protected final String group;
    protected final Ingredient ingredient;
    protected final ItemStack result;
    protected final float experience;
    protected final int cookingTime;

    public AbstractCookingRecipe(RecipeType<?> type, String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        this.type = type; this.group = group; this.category = category; this.ingredient = ingredient; this.result = result; this.experience = experience; this.cookingTime = cookingTime;
    }

    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { NonNullList<Ingredient> l = NonNullList.create(); l.add(ingredient); return l; }
    public float getExperience() { return experience; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public String getGroup() { return group; }
    public int getCookingTime() { return cookingTime; }
    @Override public RecipeType<?> getType() { return type; }
    public CookingBookCategory category() { return category; }

    @FunctionalInterface
    public interface Factory<T extends AbstractCookingRecipe> {
        T create(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time);
    }

    public static class Serializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {
        private final MapCodec<T> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        public Serializer(Factory<T> factory, int defaultTime) {
            codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                    CookingBookCategory.CODEC.optionalFieldOf("category", CookingBookCategory.MISC).forGetter(r -> r.category),
                    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                    ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                    Codec.FLOAT.optionalFieldOf("experience", 0f).forGetter(r -> r.experience),
                    Codec.INT.optionalFieldOf("cookingtime", defaultTime).forGetter(r -> r.cookingTime)
            ).apply(i, factory::create));
            streamCodec = RecipeSerializer.reamc$stream(codec);
        }

        @Override public MapCodec<T> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() { return streamCodec; }
    }
}
