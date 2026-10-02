package net.minecraft.world.entity.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A dropped item (reamc-compat: reamc's item entity). */
public class ItemEntity extends Entity {
    public ItemEntity(mc.entity.ItemEntity engine) {
        super(engine);
        mc.mod.Bridge.registerWrapper(engine, this);
    }

    public ItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(new mc.entity.ItemEntity(stack.reamc$handle() == null ? new mc.item.ItemStack(mc.item.Item.STICK, 0) : stack.reamc$handle().copy()));
        reamc$entity.world = level.reamc$world();
        setPos(x, y, z);
        setDeltaMovement(random.nextDouble() * 0.2 - 0.1, 0.2, random.nextDouble() * 0.2 - 0.1);
    }

    public ItemEntity(Level level, double x, double y, double z, ItemStack stack, double dx, double dy, double dz) {
        this(level, x, y, z, stack);
        setDeltaMovement(dx, dy, dz);
    }

    private mc.entity.ItemEntity item() { return (mc.entity.ItemEntity) reamc$entity; }

    public ItemStack getItem() { return ItemStack.reamc$wrap(item().stack); }

    public void setItem(ItemStack stack) { if (!stack.isEmpty()) item().stack = stack.reamc$handle(); else discard(); }

    public void setPickUpDelay(int ticks) { item().pickupDelay = ticks; }
    public void setDefaultPickUpDelay() { setPickUpDelay(10); }
    public void setNoPickUpDelay() { setPickUpDelay(0); }
    public void setNeverPickUp() { setPickUpDelay(Short.MAX_VALUE); }
    public boolean hasPickUpDelay() { return item().pickupDelay > 0; }
    public void setExtendedLifetime() { }
    public void setUnlimitedLifetime() { }
}
