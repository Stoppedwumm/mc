package net.minecraft.world.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A thrown projectile: flies with gravity and drag, and calls onHit when it touches something (reamc-compat). */
public abstract class ThrowableProjectile extends Projectile {
    protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> type, Level level) { super(type, level); }

    protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> type, double x, double y, double z, Level level) {
        this(type, level);
        setPos(x, y, z);
    }

    protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> type, LivingEntity shooter, Level level) {
        this(type, shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(), level);
        setOwner(shooter);
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = reamc$findHit();
        if (hit != null && !level().isClientSide) onHit(hit);
        if (isRemoved()) return;
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        updateRotation();
        double drag = isInWater() ? 0.8 : 0.99;
        setDeltaMovement(v.scale(drag).add(0, -getGravity(), 0));
    }

    protected double getDefaultGravity() { return 0.03; }

    public double getGravity() { return getDefaultGravity(); }
}
