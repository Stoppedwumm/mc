package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Spills a container's contents into the world (reamc-compat). */
public final class Containers {
    private Containers() { }

    public static void dropContents(Level level, BlockPos pos, Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), container.getItem(i));
    }

    public static void dropContents(Level level, BlockPos pos, net.minecraft.core.NonNullList<ItemStack> items) {
        for (ItemStack s : items) dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
    }

    public static void dropItemStack(Level level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) return;
        level.reamc$world().spawnItem(x + 0.5, y + 0.5, z + 0.5, stack.reamc$handle().copy());
        stack.setCount(0);
    }
}
