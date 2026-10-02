package net.minecraft.world.item.crafting;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A crafting grid's contents (reamc-compat). */
public class CraftingInput implements RecipeInput {
    public static final CraftingInput EMPTY = new CraftingInput(0, 0, List.of());
    private final int width, height;
    private final List<ItemStack> items;
    private final int ingredientCount;

    private CraftingInput(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = items;
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        ingredientCount = n;
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) { return ofPositioned(width, height, items).input(); }

    /** Trims empty rows and columns around the items. */
    public static Positioned ofPositioned(int width, int height, List<ItemStack> items) {
        if (width == 0 || height == 0) return new Positioned(EMPTY, 0, 0);
        int x0 = width, x1 = -1, y0 = height, y1 = -1;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (!items.get(x + y * width).isEmpty()) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
        if (x1 < 0) return new Positioned(EMPTY, 0, 0);
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        List<ItemStack> l = new ArrayList<>();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) l.add(items.get(x0 + x + (y0 + y) * width));
        return new Positioned(new CraftingInput(w, h, l), x0, y0);
    }

    @Override public ItemStack getItem(int index) { return items.get(index); }
    public ItemStack getItem(int x, int y) { return items.get(x + y * width); }
    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return ingredientCount == 0; }
    public int width() { return width; }
    public int height() { return height; }
    public List<ItemStack> items() { return items; }
    public int ingredientCount() { return ingredientCount; }
    public StackedContents stackedContents() { StackedContents c = new StackedContents(); for (ItemStack s : items) c.accountSimpleStack(s); return c; }

    public record Positioned(CraftingInput input, int left, int top) { }
}
