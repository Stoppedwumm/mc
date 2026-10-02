package net.minecraft.world.entity.player;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Item counts used by the recipe book (reamc-compat). */
public class StackedContents {
    public final List<ItemStack> reamc$stacks = new ArrayList<>();

    public void accountSimpleStack(ItemStack stack) { if (!stack.isEmpty()) reamc$stacks.add(stack.copy()); }
    public void accountStack(ItemStack stack) { accountSimpleStack(stack); }
    public void accountStack(ItemStack stack, int max) { accountSimpleStack(stack); }
    public void clear() { reamc$stacks.clear(); }
}
