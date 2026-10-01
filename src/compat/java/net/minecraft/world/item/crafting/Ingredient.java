package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** What a recipe slot accepts (reamc-compat). */
public class Ingredient implements Predicate<ItemStack> {
    public static final Ingredient EMPTY = new Ingredient(List.of());
    private final List<ItemLike> items;

    private Ingredient(List<ItemLike> items) { this.items = items; }

    public static Ingredient of() { return EMPTY; }

    public static Ingredient of(ItemLike... items) { return new Ingredient(List.of(items)); }

    public static Ingredient of(ItemStack... stacks) {
        List<ItemLike> l = new ArrayList<>();
        for (ItemStack s : stacks) if (!s.isEmpty()) l.add(s.getItem());
        return new Ingredient(l);
    }

    public boolean isEmpty() { return items.isEmpty(); }

    public List<ItemLike> reamc$items() { return items; }

    @Override
    public boolean test(ItemStack s) {
        for (ItemLike i : items) if (s.getItem() == i.asItem()) return true;
        return false;
    }
}
