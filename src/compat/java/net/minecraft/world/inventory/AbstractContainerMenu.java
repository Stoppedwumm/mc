package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A container GUI's slots and click handling, like Minecraft's (reamc-compat). */
public abstract class AbstractContainerMenu {
    public final NonNullList<Slot> slots = NonNullList.create();
    public final int containerId;
    private final MenuType<?> menuType;
    private ItemStack carried = ItemStack.EMPTY;

    protected AbstractContainerMenu(MenuType<?> menuType, int containerId) {
        this.menuType = menuType;
        this.containerId = containerId;
    }

    public MenuType<?> getType() { return menuType; }

    protected Slot addSlot(Slot slot) {
        slot.index = slots.size();
        slots.add(slot);
        return slot;
    }

    public Slot getSlot(int index) { return slots.get(index); }

    public abstract ItemStack quickMoveStack(Player player, int index);

    public abstract boolean stillValid(Player player);

    public ItemStack getCarried() { return carried; }
    public void setCarried(ItemStack s) { carried = s; }

    public void broadcastChanges() { }
    public void broadcastFullState() { }
    public void slotsChanged(Container container) { }
    public boolean clickMenuButton(Player player, int id) { return false; }
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return true; }

    /** Returns the cursor stack to the player when the menu closes. */
    public void removed(Player player) {
        if (!carried.isEmpty()) {
            player.getInventory().placeItemBackInInventory(carried);
            carried = ItemStack.EMPTY;
        }
    }

    public static boolean stillValid(ContainerLevelAccess access, Player player, Block block) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).is(block)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }

    /** Synced numbers (progress bars): shared with the screen in reamc. */
    protected final java.util.List<DataSlot> dataSlots = new java.util.ArrayList<>();

    protected DataSlot addDataSlot(DataSlot slot) { dataSlots.add(slot); return slot; }

    protected void addDataSlots(ContainerData data) { for (int i = 0; i < data.getCount(); i++) addDataSlot(DataSlot.forContainer(data, i)); }

    public void setData(int id, int value) { dataSlots.get(id).set(value); }

    public boolean isValidSlotIndex(int index) { return index == -1 || index == -999 || index < slots.size(); }

    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> l = NonNullList.create();
        for (Slot s : slots) l.add(s.getItem());
        return l;
    }

    public void setItem(int slot, int stateId, ItemStack stack) { getSlot(slot).set(stack); }

    public int getStateId() { return 0; }

    public int incrementStateId() { return 0; }

    public static int getRedstoneSignalFromBlockEntity(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be instanceof Container c ? getRedstoneSignalFromContainer(c) : 0;
    }

    public static int getRedstoneSignalFromContainer(Container container) {
        if (container == null) return 0;
        float f = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) f += (float) s.getCount() / container.getMaxStackSize(s);
        }
        f /= container.getContainerSize();
        return (int) Math.floor(f * 14) + (f > 0 ? 1 : 0);
    }

    /** Minecraft's shift-click mover: merges into matching stacks first, then fills empty slots. */
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        int i = reverse ? end - 1 : start;
        if (stack.isStackable()) {
            while (!stack.isEmpty() && (reverse ? i >= start : i < end)) {
                Slot slot = slots.get(i);
                ItemStack in = slot.getItem();
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(stack, in)) {
                    int total = in.getCount() + stack.getCount(), max = slot.getMaxStackSize(in);
                    if (total <= max) {
                        stack.setCount(0);
                        in.setCount(total);
                        slot.setChanged();
                        moved = true;
                    } else if (in.getCount() < max) {
                        stack.shrink(max - in.getCount());
                        in.setCount(max);
                        slot.setChanged();
                        moved = true;
                    }
                }
                i += reverse ? -1 : 1;
            }
        }
        if (!stack.isEmpty()) {
            i = reverse ? end - 1 : start;
            while (reverse ? i >= start : i < end) {
                Slot slot = slots.get(i);
                if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
                    slot.setByPlayer(stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack))));
                    slot.setChanged();
                    moved = true;
                    break;
                }
                i += reverse ? -1 : 1;
            }
        }
        return moved;
    }

    /** Minecraft's click handling for the slot under the cursor (left/right click, shift-click, number keys, Q). */
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId < 0 || slotId >= slots.size()) {
            if (slotId == -999 && !carried.isEmpty()) {
                if (button == 0) { player.drop(carried, true); carried = ItemStack.EMPTY; }
                else player.drop(carried.split(1), true);
            }
            return;
        }
        Slot slot = slots.get(slotId);
        switch (type) {
            case QUICK_MOVE -> {
                if (!slot.mayPickup(player)) return;
                ItemStack moved = quickMoveStack(player, slotId);
                // Keep moving while it makes progress, as Minecraft does
                for (int guard = 0; guard < 64 && !moved.isEmpty() && ItemStack.isSameItem(slot.getItem(), moved); guard++)
                    moved = quickMoveStack(player, slotId);
            }
            case SWAP -> {
                ItemStack hot = player.getInventory().getItem(button), in = slot.getItem();
                if (!slot.mayPickup(player) || (!hot.isEmpty() && !slot.mayPlace(hot))) return;
                player.getInventory().setItem(button, in);
                slot.set(hot);
                slot.onTake(player, in);
            }
            case THROW -> {
                if (!carried.isEmpty() || !slot.hasItem() || !slot.mayPickup(player)) return;
                player.drop(slot.remove(button == 0 ? 1 : slot.getItem().getCount()), true);
            }
            case PICKUP -> {
                ItemStack in = slot.getItem();
                if (in.isEmpty()) {
                    if (carried.isEmpty() || !slot.mayPlace(carried)) return;
                    int n = button == 0 ? carried.getCount() : 1;
                    carried = slot.safeInsert(carried, n);
                } else if (slot.mayPickup(player)) {
                    if (carried.isEmpty()) {
                        int n = button == 0 ? in.getCount() : (in.getCount() + 1) / 2;
                        carried = slot.remove(n);
                        slot.onTake(player, carried);
                    } else if (slot.mayPlace(carried)) {
                        if (ItemStack.isSameItemSameComponents(in, carried)) {
                            carried = slot.safeInsert(carried, button == 0 ? carried.getCount() : 1);
                        } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                            slot.setByPlayer(carried);
                            carried = in;
                            slot.onTake(player, in);
                        }
                    } else if (ItemStack.isSameItemSameComponents(in, carried) && carried.getCount() + in.getCount() <= carried.getMaxStackSize()) {
                        carried.grow(in.getCount());
                        slot.set(ItemStack.EMPTY);
                        slot.onTake(player, carried);
                    }
                }
                slot.setChanged();
            }
            default -> { }
        }
        if (carried.isEmpty()) carried = ItemStack.EMPTY;
    }
}
