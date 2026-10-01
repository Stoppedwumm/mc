package net.minecraft.world.entity.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** A player as mods see it, backed by reamc's player (reamc-compat). */
public abstract class Player extends LivingEntity {
    private final Inventory inventory;
    /** The open menu (null = only the player's own inventory). */
    public AbstractContainerMenu containerMenu;

    protected Player(mc.entity.Player engine) {
        super(engine);
        inventory = new Inventory(this);
    }

    public mc.entity.Player reamc$player() { return (mc.entity.Player) reamc$entity; }

    public Inventory getInventory() { return inventory; }

    @Override public ItemStack getMainHandItem() { return ItemStack.reamc$wrap(reamc$player().inventory.held()); }

    public boolean isCreative() { return reamc$player().creative; }
    public boolean isSpectator() { return false; }

    @Override public Component getName() { return Component.literal(reamc$player().name); }

    public ItemEntity drop(ItemStack stack, boolean randomly) {
        if (stack.isEmpty()) return null;
        mc.entity.Player p = reamc$player();
        mc.entity.ItemEntity e = new mc.entity.ItemEntity(stack.reamc$handle().copy());
        e.setPos(p.x, p.eyeY() - 0.3, p.z);
        double r = Math.toRadians(p.yaw);
        e.motionX = -Math.sin(r) * 0.3;
        e.motionZ = Math.cos(r) * 0.3;
        e.pickupDelay = 40;
        p.world.addEntity(e);
        return new ItemEntity(e);
    }

    public void sendSystemMessage(Component message) { mc.mod.Bridge.chat(reamc$player(), message.getString()); }

    public void displayClientMessage(Component message, boolean actionBar) { mc.mod.Bridge.chat(reamc$player(), message.getString()); }

    public void giveExperiencePoints(int xp) { reamc$player().addXp(xp); }

    public boolean addItem(ItemStack stack) { return inventory.add(stack); }

    public void closeContainer() { mc.mod.Bridge.closeMenu(this); }
}
