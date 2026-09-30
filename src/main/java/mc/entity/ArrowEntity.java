package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.util.AABB;
import mc.util.RayCast;

/** Projectile shot by bows and skeletons. Sticks into blocks and can be picked back up. */
public final class ArrowEntity extends Entity {
    public final Entity shooter;
    public boolean inGround, pickup;
    private int groundTicks;
    public float damage = 2;

    public ArrowEntity(Entity shooter) {
        this.shooter = shooter;
        width = height = 0.5f;
        stepHeight = 0;
    }

    public void shoot(double dx, double dy, double dz, double velocity, double inaccuracy) {
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx = dx / d + random.nextGaussian() * 0.0075 * inaccuracy;
        dy = dy / d + random.nextGaussian() * 0.0075 * inaccuracy;
        dz = dz / d + random.nextGaussian() * 0.0075 * inaccuracy;
        motionX = dx * velocity;
        motionY = dy * velocity;
        motionZ = dz * velocity;
        updateRotation();
    }

    private void updateRotation() {
        double h = Math.sqrt(motionX * motionX + motionZ * motionZ);
        yaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
        pitch = (float) -Math.toDegrees(Math.atan2(motionY, h));
    }

    @Override
    public void tick() {
        super.tick();
        if (inGround) {
            if (++groundTicks > 1200) remove();
            return;
        }
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        RayCast.Hit hit = speed > 0 ? RayCast.cast(world, x, y, z, motionX / speed, motionY / speed, motionZ / speed, speed) : null;
        double travel = hit != null ? hit.distance : speed;

        // Entity hits along the path
        Entity target = null;
        double best = travel;
        for (LivingEntity e : world.livingEntities()) {
            if (e.removed || e.isDead()) continue;
            if (e == shooter && age < 5) continue;
            if (e == shooter && age < 5) continue;
            AABB b = e.box();
            b.minX -= 0.3; b.minY -= 0.3; b.minZ -= 0.3; b.maxX += 0.3; b.maxY += 0.3; b.maxZ += 0.3;
            double t = rayBox(b, speed);
            if (t >= 0 && t <= best) { best = t; target = e; }
        }
        if (target != null) {
            float dmg = (float) Math.ceil(speed * damage);
            if (((LivingEntity) target).damage(DamageSource.ARROW, dmg, shooter != null ? shooter : this)) {
                world.playSound("hit", x, y, z, 1, 1);
            }
            remove();
            return;
        }
        if (hit != null) {
            x += motionX / speed * hit.distance;
            y += motionY / speed * hit.distance;
            z += motionZ / speed * hit.distance;
            inGround = true;
            world.playSound("arrow_hit", x, y, z, 0.6f, 1.2f);
            return;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        double drag = inWater ? 0.6 : 0.99;
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        motionY -= 0.05;
        updateRotation();
    }

    private double rayBox(AABB b, double speed) {
        double tmin = 0, tmax = 1;
        double[] o = {x, y, z}, d = {motionX, motionY, motionZ}, mn = {b.minX, b.minY, b.minZ}, mx = {b.maxX, b.maxY, b.maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < mn[i] || o[i] > mx[i]) return -1;
            } else {
                double t1 = (mn[i] - o[i]) / d[i], t2 = (mx[i] - o[i]) / d[i];
                if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1;
            }
        }
        return tmin * speed;
    }

    /** Lets the player collect stuck arrows they fired. */
    public boolean tryPickup(Player p) {
        if (!inGround || !pickup) return false;
        if (p.distanceSq(x, y, z) > 2.5) return false;
        ItemStack left = p.inventory.add(new ItemStack(Item.ARROW, 1));
        if (left.count == 0) {
            remove();
            return true;
        }
        return false;
    }
}
