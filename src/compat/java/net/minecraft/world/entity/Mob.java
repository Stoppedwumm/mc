package net.minecraft.world.entity;

import net.minecraft.world.level.Level;

/** A mob with AI (reamc-compat: reamc's mobs run their own AI). */
public abstract class Mob extends LivingEntity {
    protected Mob(mc.entity.Mob engine) { super(engine); }

    protected Mob(EntityType<? extends Mob> type, Level level) { super(type, level); }

    public mc.entity.Mob reamc$mob() { return (mc.entity.Mob) reamc$entity; }

    public boolean isPersistenceRequired() { return false; }
    public void setPersistenceRequired() { }
    public LivingEntity getTarget() { return null; }
    public void setTarget(LivingEntity target) { }
    public boolean isNoAi() { return false; }
}
