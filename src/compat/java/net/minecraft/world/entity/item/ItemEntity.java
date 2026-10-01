package net.minecraft.world.entity.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** A dropped item (reamc-compat). */
public class ItemEntity extends Entity {
    public ItemEntity(mc.entity.ItemEntity engine) { super(engine); }

    public ItemStack getItem() { return ItemStack.reamc$wrap(((mc.entity.ItemEntity) reamc$entity).stack); }
}
