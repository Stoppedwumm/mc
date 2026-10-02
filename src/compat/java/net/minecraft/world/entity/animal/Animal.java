package net.minecraft.world.entity.animal;

import net.minecraft.world.entity.Mob;

/** An animal (reamc-compat). */
public abstract class Animal extends Mob {
    protected Animal(mc.entity.Mob engine) { super(engine); }

    public boolean isFood(net.minecraft.world.item.ItemStack stack) { return false; }
    public boolean isInLove() { return false; }
}
