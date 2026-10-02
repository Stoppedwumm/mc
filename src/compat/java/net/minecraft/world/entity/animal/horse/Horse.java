package net.minecraft.world.entity.animal.horse;

import net.minecraft.world.entity.animal.Animal;

/** Horses (reamc-compat: reamc has none, so this only exists for type checks). */
public class Horse extends Animal {
    public Horse(mc.entity.Mob engine) { super(engine); }

    public boolean isTamed() { return false; }
}
