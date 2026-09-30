package mc.entity;

import mc.util.AABB;
import mc.util.RayCast;

/** Ghast fireball: flies straight, explodes on impact and sets fires. */
public final class FireballEntity extends Entity {
    public final Entity shooter;
    private double ax, ay, az;

    public FireballEntity(Entity shooter) {
        this.shooter = shooter;
        width = height = 1;
        stepHeight = 0;
    }

    /** Sets the acceleration direction (Minecraft fireballs accelerate towards their target). */
    public void aim(double dx, double dy, double dz) {
        dx += random.nextGaussian() * 0.4;
        dy += random.nextGaussian() * 0.4;
        dz += random.nextGaussian() * 0.4;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        ax = dx / d * 0.1;
        ay = dy / d * 0.1;
        az = dz / d * 0.1;
    }

    @Override
    public void tick() {
        super.tick();
        if (age > 400) { remove(); return; }
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        RayCast.Hit hit = speed > 0 ? RayCast.cast(world, x, y, z, motionX / speed, motionY / speed, motionZ / speed, speed) : null;
        boolean hitEntity = false;
        for (LivingEntity e : world.livingEntities()) {
            if (e == shooter || e.isDead()) continue;
            AABB b = e.box();
            if (ArrowPath.rayBox(x, y, z, motionX, motionY, motionZ, b.minX - 0.5, b.minY - 0.5, b.minZ - 0.5, b.maxX + 0.5, b.maxY + 0.5, b.maxZ + 0.5) >= 0) {
                e.damage(DamageSource.EXPLOSION, 6, shooter);
                e.fireTicks = Math.max(e.fireTicks, 100);
                hitEntity = true;
                break;
            }
        }
        if (hit != null || hitEntity) {
            if (hit != null) { x = hit.px; y = hit.py; z = hit.pz; }
            remove();
            world.explode(x, y, z, 1, this, true);
            return;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        motionX = (motionX + ax) * 0.95;
        motionY = (motionY + ay) * 0.95;
        motionZ = (motionZ + az) * 0.95;
        world.addParticle("smoke", x, y + 0.5, z);
    }
}
