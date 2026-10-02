package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * An entity as mods see it (reamc-compat). Engine entities are wrapped; entities that mods define (built with
 * {@link EntityType}) get an engine proxy that runs their {@link #tick()}.
 */
public abstract class Entity {
    protected final mc.entity.Entity reamc$entity;
    private final EntityType<?> type;
    protected final RandomSource random = RandomSource.create();
    public int tickCount;
    private UUID uuid = UUID.randomUUID();

    /** Wraps an engine entity. */
    protected Entity(mc.entity.Entity engine) {
        this.reamc$entity = engine;
        this.type = null;
    }

    /** A mod's own entity. */
    public Entity(EntityType<?> type, Level level) {
        this.type = type;
        this.reamc$entity = mc.mod.ModEntityProxy.create(this, type, level == null ? null : level.reamc$world());
        mc.mod.Bridge.registerWrapper(reamc$entity, this);
        defineSynchedData();
    }

    protected void defineSynchedData() { }

    public mc.entity.Entity reamc$engine() { return reamc$entity; }

    public EntityType<?> getType() { return type; }

    /** Called every tick for mod entities (through the engine proxy). */
    public void tick() { baseTick(); }

    public void baseTick() { tickCount++; }

    public Level level() { return mc.mod.Bridge.level(reamc$entity.world); }
    public Level getCommandSenderWorld() { return level(); }
    public RandomSource getRandom() { return random; }
    public int getId() { return reamc$entity.id; }
    public UUID getUUID() { return uuid; }
    public void setUUID(UUID id) { uuid = id; }
    public String getStringUUID() { return uuid.toString(); }
    public double getX() { return reamc$entity.x; }
    public double getY() { return reamc$entity.y; }
    public double getZ() { return reamc$entity.z; }
    public double getX(double f) { return getX() + getBbWidth() * f; }
    public double getY(double f) { return getY() + getBbHeight() * f; }
    public double getZ(double f) { return getZ() + getBbWidth() * f; }
    public double getRandomX(double f) { return getX((2 * random.nextDouble() - 1) * f); }
    public double getRandomY() { return getY(random.nextDouble()); }
    public double getRandomZ(double f) { return getZ((2 * random.nextDouble() - 1) * f); }
    public double getEyeY() { return reamc$entity.eyeY(); }
    public float getEyeHeight() { return (float) (reamc$entity.eyeY() - reamc$entity.y); }
    public Vec3 position() { return new Vec3(getX(), getY(), getZ()); }
    public Vec3 getEyePosition() { return new Vec3(getX(), getEyeY(), getZ()); }
    public Vec3 getEyePosition(float partial) { return getEyePosition(); }
    public BlockPos blockPosition() { return BlockPos.containing(getX(), getY(), getZ()); }
    public int getBlockX() { return (int) Math.floor(getX()); }
    public int getBlockY() { return (int) Math.floor(getY()); }
    public int getBlockZ() { return (int) Math.floor(getZ()); }
    public BlockPos getOnPos() { return BlockPos.containing(getX(), getY() - 0.2, getZ()); }
    public BlockPos getBlockPosBelowThatAffectsMyMovement() { return BlockPos.containing(getX(), getY() - 0.5, getZ()); }
    public float getYRot() { return reamc$entity.yaw; }
    public float getXRot() { return reamc$entity.pitch; }
    public void setYRot(float r) { reamc$entity.yaw = r; }
    public void setXRot(float r) { reamc$entity.pitch = r; }
    public float getYHeadRot() { return getYRot(); }
    public void setYHeadRot(float r) { }
    public Vec3 getLookAngle() { return Vec3.directionFromRotation(getXRot(), getYRot()); }
    public Vec3 getViewVector(float partial) { return getLookAngle(); }
    public Direction getDirection() { return Direction.fromYRot(getYRot()); }
    public Direction getMotionDirection() { return getDirection(); }
    public Vec3 getDeltaMovement() { return new Vec3(reamc$entity.motionX, reamc$entity.motionY, reamc$entity.motionZ); }
    public void setDeltaMovement(Vec3 v) { setDeltaMovement(v.x, v.y, v.z); }
    public void setDeltaMovement(double x, double y, double z) { reamc$entity.motionX = x; reamc$entity.motionY = y; reamc$entity.motionZ = z; }
    public void addDeltaMovement(Vec3 v) { setDeltaMovement(getDeltaMovement().add(v)); }
    public void push(double x, double y, double z) { setDeltaMovement(getDeltaMovement().add(x, y, z)); }
    public AABB getBoundingBox() { mc.util.AABB b = reamc$entity.box(); return new AABB(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }
    public float getBbWidth() { return reamc$entity.width; }
    public float getBbHeight() { return reamc$entity.height; }
    public boolean isAlive() { return !reamc$entity.removed && !(reamc$entity instanceof mc.entity.LivingEntity le && le.isDead()); }
    public boolean isRemoved() { return reamc$entity.removed; }
    public void discard() { reamc$entity.remove(); }
    public void remove(RemovalReason reason) { reamc$entity.remove(); }
    public void kill() { if (this instanceof LivingEntity l) l.setHealth(0); else discard(); }
    public boolean isShiftKeyDown() { return reamc$entity instanceof mc.entity.Player p && p.sneaking; }
    public boolean isCrouching() { return isShiftKeyDown(); }
    public boolean isSteppingCarefully() { return isShiftKeyDown(); }
    public boolean isSuppressingBounce() { return isShiftKeyDown(); }
    public boolean isDescending() { return isShiftKeyDown(); }
    public boolean isSprinting() { return reamc$entity instanceof mc.entity.Player p && p.sprinting; }
    public boolean isInWater() { return reamc$entity.inWater; }
    public boolean isInLava() { return reamc$entity.inLava; }
    public boolean isUnderWater() { return reamc$entity.eyeInBlock(mc.world.Block.WATER.id); }
    public boolean isInWaterOrRain() { return isInWater() || reamc$entity.world != null && reamc$entity.world.raining; }
    public boolean isInWaterRainOrBubble() { return isInWaterOrRain(); }
    public boolean onGround() { return reamc$entity.onGround; }
    public void setOnGround(boolean g) { reamc$entity.onGround = g; }
    public boolean isNoGravity() { return false; }
    public void setNoGravity(boolean b) { }
    public void setPos(double x, double y, double z) { reamc$entity.setPos(x, y, z); }
    public void setPos(Vec3 v) { setPos(v.x, v.y, v.z); }
    public void moveTo(double x, double y, double z) { setPos(x, y, z); }
    public void moveTo(double x, double y, double z, float yRot, float xRot) { setPos(x, y, z); setYRot(yRot); setXRot(xRot); }
    public void moveTo(Vec3 v) { setPos(v); }
    public void absMoveTo(double x, double y, double z) { setPos(x, y, z); }
    public void teleportTo(double x, double y, double z) { reamc$entity.setPos(x, y, z); }
    public void setRemainingFireTicks(int t) { reamc$entity.fireTicks = t; }
    public int getRemainingFireTicks() { return reamc$entity.fireTicks; }
    public void igniteForSeconds(float s) { reamc$entity.fireTicks = Math.max(reamc$entity.fireTicks, (int) (s * 20)); }
    public void igniteForTicks(int t) { reamc$entity.fireTicks = Math.max(reamc$entity.fireTicks, t); }
    public boolean isOnFire() { return reamc$entity.fireTicks > 0; }
    public void clearFire() { reamc$entity.fireTicks = 0; }
    public boolean fireImmune() { return reamc$entity instanceof mc.entity.LivingEntity l && l.fireImmune(); }
    public float getFallDistance() { return reamc$entity.fallDistance; }
    public void resetFallDistance() { reamc$entity.fallDistance = 0; }
    public double distanceToSqr(double x, double y, double z) { return reamc$entity.distanceSq(x, y, z); }
    public double distanceToSqr(Entity e) { return distanceToSqr(e.getX(), e.getY(), e.getZ()); }
    public double distanceToSqr(Vec3 v) { return distanceToSqr(v.x, v.y, v.z); }
    public float distanceTo(Entity e) { return (float) Math.sqrt(distanceToSqr(e)); }
    public boolean closerThan(Entity e, double d) { return distanceToSqr(e) < d * d; }
    public Component getName() { return Component.literal(reamc$entity.getClass().getSimpleName().replace("Entity", "")); }
    public Component getDisplayName() { return getName(); }
    public boolean hasCustomName() { return false; }
    public DamageSources damageSources() { return level().damageSources(); }

    /** Damage through the engine's health and armour rules. */
    public boolean hurt(DamageSource source, float amount) {
        if (reamc$entity instanceof mc.entity.LivingEntity l) {
            mc.entity.Entity attacker = source.getEntity() == null ? null : source.getEntity().reamc$engine();
            return l.damage(source.reamc$engineSource(), amount, attacker);
        }
        return false;
    }

    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        int dmg = (int) Math.ceil((distance - 3) * multiplier);
        if (dmg <= 0) return false;
        return hurt(source, dmg);
    }

    public boolean isPassenger() { return reamc$entity.vehicle != null; }
    public boolean isVehicle() { return reamc$entity instanceof mc.entity.Vehicle v && v.passenger != null; }
    public Entity getVehicle() { return reamc$entity.vehicle == null ? null : mc.mod.Bridge.wrap(reamc$entity.vehicle); }
    public void stopRiding() { if (reamc$entity.vehicle != null) reamc$entity.vehicle.dismount(); }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (reamc$entity.world != null) reamc$entity.world.playSound(sound.reamc$name(), getX(), getY(), getZ(), volume, pitch);
    }

    public void playSound(SoundEvent sound) { playSound(sound, 1, 1); }

    public ItemEntity spawnAtLocation(ItemStack stack) { return spawnAtLocation(stack, 0); }

    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        if (stack.isEmpty() || reamc$entity.world == null) return null;
        mc.entity.ItemEntity e = new mc.entity.ItemEntity(stack.reamc$handle().copy());
        e.setPos(getX(), getY() + yOffset, getZ());
        reamc$entity.world.addEntity(e);
        return (ItemEntity) mc.mod.Bridge.wrap(e);
    }

    public ItemEntity spawnAtLocation(ItemLike item) { return spawnAtLocation(new ItemStack(item)); }

    public boolean isSpectator() { return false; }
    public boolean isInvisible() { return false; }
    public boolean isInvulnerable() { return false; }
    public boolean isPickable() { return false; }
    public boolean isPushable() { return false; }
    public boolean shouldBeSaved() { return true; }
    public void gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event) { }
    public void gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event, Entity cause) { }

    public <T> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, Void> capability) { return capability.reamc$get(this, null); }
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, C> capability, C context) { return capability.reamc$get(this, context); }

    @Override public boolean equals(Object o) { return o instanceof Entity e && e.reamc$entity == reamc$entity; }
    @Override public int hashCode() { return reamc$entity.id; }
    @Override public String toString() { return getClass().getSimpleName() + "[" + getId() + "]"; }

    public enum RemovalReason {
        KILLED(true, false), DISCARDED(true, false), UNLOADED_TO_CHUNK(false, true), UNLOADED_WITH_PLAYER(false, false), CHANGED_DIMENSION(false, false);

        private final boolean destroy, save;

        RemovalReason(boolean destroy, boolean save) { this.destroy = destroy; this.save = save; }

        public boolean shouldDestroy() { return destroy; }
        public boolean shouldSave() { return save; }
    }
}
