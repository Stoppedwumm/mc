package net.minecraft.world.entity.animal;

/** reamc's wolf as mods see it (reamc-compat). */
public class Wolf extends Animal {
    public Wolf(mc.entity.Mob engine) { super(engine); }

    public boolean isTame() { return reamc$mob().tamed; }
    public boolean isInSittingPose() { return reamc$mob().sitting; }
    public boolean isAngry() { return false; }
}
