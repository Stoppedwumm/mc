package net.minecraft.world.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The player's own inventory screen (reamc-compat: reamc draws it; this holds the slots for mods). */
public class InventoryMenu extends AbstractContainerMenu {
    public static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    public static final int CONTAINER_ID = 0;
    private final Player owner;

    public InventoryMenu(Inventory inventory, boolean active, Player owner) {
        super(null, CONTAINER_ID);
        this.owner = owner;
        for (int i = 0; i < 36; i++) addSlot(new Slot(inventory, i, 0, 0));
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) { return true; }
}
