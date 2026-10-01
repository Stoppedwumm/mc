package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** An entity as mods see it, backed by an engine entity (reamc-compat). */
public abstract class Entity {
    protected final mc.entity.Entity reamc$entity;

    protected Entity(mc.entity.Entity engine) { this.reamc$entity = engine; }

    public mc.entity.Entity reamc$engine() { return reamc$entity; }

    public Level level() { return mc.mod.Bridge.level(reamc$entity.world); }
    public int getId() { return reamc$entity.id; }
    public double getX() { return reamc$entity.x; }
    public double getY() { return reamc$entity.y; }
    public double getZ() { return reamc$entity.z; }
    public BlockPos blockPosition() { return BlockPos.containing(getX(), getY(), getZ()); }
    public BlockPos getOnPos() { return BlockPos.containing(getX(), getY() - 0.2, getZ()); }
    public float getYRot() { return reamc$entity.yaw; }
    public float getXRot() { return reamc$entity.pitch; }
    public boolean isAlive() { return !reamc$entity.removed && !(reamc$entity instanceof mc.entity.LivingEntity le && le.isDead()); }
    public boolean isRemoved() { return reamc$entity.removed; }
    public void discard() { reamc$entity.remove(); }
    public boolean isShiftKeyDown() { return reamc$entity instanceof mc.entity.Player p && p.sneaking; }
    public boolean isInWater() { return reamc$entity.inWater; }
    public boolean onGround() { return reamc$entity.onGround; }
    public void setPos(double x, double y, double z) { reamc$entity.setPos(x, y, z); }
    public void teleportTo(double x, double y, double z) { reamc$entity.setPos(x, y, z); }
    public void setRemainingFireTicks(int t) { reamc$entity.fireTicks = t; }
    public int getRemainingFireTicks() { return reamc$entity.fireTicks; }
    public double distanceToSqr(double x, double y, double z) { return reamc$entity.distanceSq(x, y, z); }
    public double distanceToSqr(Entity e) { return distanceToSqr(e.getX(), e.getY(), e.getZ()); }
    public float distanceTo(Entity e) { return (float) Math.sqrt(distanceToSqr(e)); }
    public Component getName() { return Component.literal(reamc$entity.getClass().getSimpleName()); }
    public Component getDisplayName() { return getName(); }
    public <T> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, Void> capability) { return null; }
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, C> capability, C context) { return null; }
}
