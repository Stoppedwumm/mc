package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A chest's menu: rows of container slots over the player inventory (reamc-compat). */
public class ChestMenu extends AbstractContainerMenu {
    private final Container container;
    private final int containerRows;

    public ChestMenu(MenuType<?> type, int id, Inventory inv, int rows) { this(type, id, inv, new SimpleContainer(9 * rows), rows); }

    public ChestMenu(MenuType<?> type, int id, Inventory inv, Container container, int rows) {
        super(type, id);
        this.container = container;
        this.containerRows = rows;
        container.startOpen(inv.player);
        int off = (rows - 4) * 18;
        for (int r = 0; r < rows; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(container, c + r * 9, 8 + c * 18, 18 + r * 18));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + off));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 161 + off));
    }

    public static ChestMenu oneRow(int id, Inventory inv) { return new ChestMenu(MenuType.GENERIC_9x1, id, inv, 1); }
    public static ChestMenu twoRows(int id, Inventory inv) { return new ChestMenu(MenuType.GENERIC_9x2, id, inv, 2); }
    public static ChestMenu threeRows(int id, Inventory inv) { return new ChestMenu(MenuType.GENERIC_9x3, id, inv, 3); }
    public static ChestMenu threeRows(int id, Inventory inv, Container c) { return new ChestMenu(MenuType.GENERIC_9x3, id, inv, c, 3); }
    public static ChestMenu sixRows(int id, Inventory inv) { return new ChestMenu(MenuType.GENERIC_9x6, id, inv, 6); }
    public static ChestMenu sixRows(int id, Inventory inv, Container c) { return new ChestMenu(MenuType.GENERIC_9x6, id, inv, c, 6); }

    @Override public boolean stillValid(Player p) { return container.stillValid(p); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack s = slot.getItem();
            result = s.copy();
            if (index < containerRows * 9) { if (!moveItemStackTo(s, containerRows * 9, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(s, 0, containerRows * 9, false)) return ItemStack.EMPTY;
            if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override public void removed(Player p) { super.removed(p); container.stopOpen(p); }

    public Container getContainer() { return container; }

    public int getRowCount() { return containerRows; }
}
