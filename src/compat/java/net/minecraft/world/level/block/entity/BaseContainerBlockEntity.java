package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A block entity with an inventory and a menu (chests, furnaces...) (reamc-compat). */
public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {
    protected BaseContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    protected abstract Component getDefaultName();

    protected abstract NonNullList<ItemStack> getItems();

    protected abstract void setItems(NonNullList<ItemStack> items);

    protected abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory);

    @Override public Component getName() { return getDefaultName(); }
    @Override public Component getDisplayName() { return getName(); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) { return createMenu(containerId, inventory); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : getItems()) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return getItems().get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack s = ContainerHelper.removeItem(getItems(), slot, amount);
        if (!s.isEmpty()) setChanged();
        return s;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(getItems(), slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override public void clearContent() { getItems().clear(); }
}
