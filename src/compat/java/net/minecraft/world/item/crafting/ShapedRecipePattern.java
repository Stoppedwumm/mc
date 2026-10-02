package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A shaped recipe's grid (reamc-compat). */
public final class ShapedRecipePattern {
    public static final MapCodec<ShapedRecipePattern> MAP_CODEC = RecordCodecBuilder.<Data>mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(Data::key),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(Data::pattern)
    ).apply(i, Data::new)).flatXmap(ShapedRecipePattern::unpack, p -> p.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe")));

    private final int width, height;
    private final NonNullList<Ingredient> ingredients;
    private final Optional<Data> data;

    public ShapedRecipePattern(int width, int height, NonNullList<Ingredient> ingredients, Optional<Data> data) {
        this.width = width; this.height = height; this.ingredients = ingredients; this.data = data;
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, String... pattern) { return of(key, List.of(pattern)); }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, List<String> pattern) {
        Map<String, Ingredient> k = new HashMap<>();
        key.forEach((c, i) -> k.put(String.valueOf(c), i));
        return unpack(new Data(k, pattern)).getOrThrow();
    }

    private static DataResult<ShapedRecipePattern> unpack(Data d) {
        int h = d.pattern.size(), w = 0;
        for (String row : d.pattern) w = Math.max(w, row.length());
        NonNullList<Ingredient> l = NonNullList.withSize(w * h, Ingredient.EMPTY);
        for (int y = 0; y < h; y++) {
            String row = d.pattern.get(y);
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == ' ') continue;
                Ingredient i = d.key.get(String.valueOf(c));
                if (i == null) return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
                l.set(x + y * w, i);
            }
        }
        return DataResult.success(new ShapedRecipePattern(w, h, l, Optional.of(d)));
    }

    public boolean matches(CraftingInput input) {
        if (input.ingredientCount() != (int) ingredients.stream().filter(i -> !i.isEmpty()).count()) return false;
        if (input.width() == width && input.height() == height) {
            if (matches(input, false)) return true;
            return matches(input, true);
        }
        return false;
    }

    private boolean matches(CraftingInput input, boolean mirrored) {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                Ingredient i = ingredients.get((mirrored ? width - 1 - x : x) + y * width);
                ItemStack s = input.getItem(x, y);
                if (!i.test(s)) return false;
            }
        return true;
    }

    public int width() { return width; }
    public int height() { return height; }
    public NonNullList<Ingredient> ingredients() { return ingredients; }

    public record Data(Map<String, Ingredient> key, List<String> pattern) { }
}
