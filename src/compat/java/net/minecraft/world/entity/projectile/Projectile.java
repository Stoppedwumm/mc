package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Something thrown or shot (reamc-compat). */
public abstract class Projectile extends Entity {
    private Entity owner;

    protected Projectile(EntityType<? extends Projectile> type, Level level) { super(type, level); }

    public void setOwner(Entity owner) { this.owner = owner; }

    public Entity getOwner() { return owner; }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3 v = new Vec3(x, y, z).normalize().add(random.triangle(0, 0.0172275 * inaccuracy), random.triangle(0, 0.0172275 * inaccuracy), random.triangle(0, 0.0172275 * inaccuracy)).scale(velocity);
        setDeltaMovement(v);
        setYRot((float) (Math.atan2(v.x, v.z) * 180 / Math.PI));
        setXRot((float) (Math.atan2(v.y, v.horizontalDistance()) * 180 / Math.PI));
    }

    public void shootFromRotation(Entity shooter, float xRot, float yRot, float zOffset, float velocity, float inaccuracy) {
        float x = -(float) Math.sin(Math.toRadians(yRot)) * (float) Math.cos(Math.toRadians(xRot));
        float y = -(float) Math.sin(Math.toRadians(xRot + zOffset));
        float z = (float) Math.cos(Math.toRadians(yRot)) * (float) Math.cos(Math.toRadians(xRot));
        shoot(x, y, z, velocity, inaccuracy);
        Vec3 m = shooter.getDeltaMovement();
        setDeltaMovement(getDeltaMovement().add(m.x, shooter.onGround() ? 0 : m.y, m.z));
    }

    protected boolean canHitEntity(Entity e) {
        if (e == owner || !e.isAlive() || e.isSpectator()) return false;
        return tickCount > 2 || owner == null || e != owner;
    }

    protected void onHit(HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY) onHitEntity((EntityHitResult) hit);
        else if (hit.getType() == HitResult.Type.BLOCK) onHitBlock((BlockHitResult) hit);
    }

    protected void onHitEntity(EntityHitResult hit) { }

    protected void onHitBlock(BlockHitResult hit) { }

    protected void updateRotation() {
        Vec3 v = getDeltaMovement();
        setYRot((float) (Math.atan2(v.x, v.z) * 180 / Math.PI));
        setXRot((float) (Math.atan2(v.y, v.horizontalDistance()) * 180 / Math.PI));
    }

    /** The first block or entity in the way of this tick's movement, or null. */
    protected HitResult reamc$findHit() {
        mc.entity.Entity me = reamc$engine();
        Vec3 v = getDeltaMovement();
        double len = v.length();
        if (len < 1e-6 || me.world == null) return null;
        mc.util.RayCast.Hit block = mc.util.RayCast.cast(me.world, me.x, me.y, me.z, v.x / len, v.y / len, v.z / len, len);
        double reach = block != null ? block.distance : len;
        HitResult best = null;
        double bestD = reach;
        Vec3 from = position(), to = from.add(v.scale(reach / len));
        for (mc.entity.Entity e : me.world.entities()) {
            if (e == me || e.removed || !(e instanceof mc.entity.LivingEntity)) continue;
            Entity w = mc.mod.Bridge.wrap(e);
            if (w == null || !canHitEntity(w)) continue;
            var box = w.getBoundingBox().inflate(0.3);
            var p = box.clip(from, to);
            if (p.isPresent() && p.get().distanceTo(from) < bestD) {
                bestD = p.get().distanceTo(from);
                best = new EntityHitResult(w, p.get());
            }
        }
        if (best != null) return best;
        if (block == null) return null;
        net.minecraft.core.Direction face = net.minecraft.core.Direction.fromDelta(block.nx, block.ny, block.nz);
        return new BlockHitResult(new Vec3(block.px, block.py, block.pz), face == null ? net.minecraft.core.Direction.UP : face, new BlockPos(block.x, block.y, block.z), false);
    }
}
