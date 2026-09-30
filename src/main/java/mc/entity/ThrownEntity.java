package mc.entity;

import mc.item.Item;
import mc.util.AABB;
import mc.util.RayCast;

/** Thrown snowballs, eggs and ender pearls. */
public final class ThrownEntity extends Entity {
    public final Item item;
    public final Entity thrower;
    /** Potion type for splash potions. */
    public int potionMeta;

    public ThrownEntity(Item item, Entity thrower) {
        this.item = item;
        this.thrower = thrower;
        width = height = 0.25f;
        stepHeight = 0;
    }

    public void shoot(double dx, double dy, double dz, double velocity) {
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        motionX = dx / d * velocity;
        motionY = dy / d * velocity;
        motionZ = dz / d * velocity;
    }

    @Override
    public void tick() {
        super.tick();
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        RayCast.Hit hit = speed > 0 ? RayCast.cast(world, x, y, z, motionX / speed, motionY / speed, motionZ / speed, speed) : null;
        double travel = hit != null ? hit.distance : speed;
        LivingEntity target = null;
        double best = travel;
        for (LivingEntity e : world.livingEntities()) {
            if (e.isDead() || (e == thrower && age < 5)) continue;
            AABB b = e.box();
            double t = ArrowPath.rayBox(x, y, z, motionX, motionY, motionZ, b.minX - 0.3, b.minY - 0.3, b.minZ - 0.3, b.maxX + 0.3, b.maxY + 0.3, b.maxZ + 0.3) * speed;
            if (t >= 0 && t <= best) { best = t; target = e; }
        }
        if (target != null || hit != null) {
            double t = target != null ? best : hit.distance;
            x += motionX / speed * t;
            y += motionY / speed * t;
            z += motionZ / speed * t;
            impact(target);
            remove();
            return;
        }
        x += motionX;
        y += motionY;
        z += motionZ;
        double drag = inWater ? 0.8 : 0.99;
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        motionY -= 0.03;
    }

    public void impact(LivingEntity target) {
        if (target != null && item != Item.SPLASH_POTION) target.damage(DamageSource.ATTACK, 0.01f, thrower);
        if (item == Item.SNOWBALL) {
            for (int i = 0; i < 8; i++) world.addParticle("poof", x, y, z);
        } else if (item == Item.EGG) {
            for (int i = 0; i < 8; i++) world.addParticle("poof", x, y, z);
            if (random.nextInt(8) == 0) {
                int n = random.nextInt(32) == 0 ? 4 : 1;
                for (int i = 0; i < n; i++) {
                    Mob chick = new Mob(MobType.CHICKEN);
                    chick.setGrowingAge(-24000);
                    chick.setPos(x, y, z);
                    chick.yaw = random.nextFloat() * 360;
                    world.addEntity(chick);
                }
            }
        } else if (item == Item.SPLASH_POTION) {
            int color = mc.item.Potions.color(potionMeta);
            for (int i = 0; i < 30; i++) world.addParticle("effect:" + Integer.toHexString(color), x + random.nextGaussian() * 0.8, y + random.nextDouble(), z + random.nextGaussian() * 0.8);
            world.playSound("glass", x, y, z, 1, 1);
            for (LivingEntity e : world.livingEntities()) {
                double d = Math.sqrt(e.distanceSq(x, y, z));
                if (d < 4) mc.item.Potions.apply(e, potionMeta, e == target ? 1 : 1 - d / 4);
            }
        } else if (item == Item.EXPERIENCE_BOTTLE) {
            for (int i = 0; i < 12; i++) world.addParticle("happy", x + random.nextGaussian() * 0.3, y + random.nextDouble(), z + random.nextGaussian() * 0.3);
            world.playSound("glass", x, y, z, 1, 1);
            XpOrbEntity.spawn(world, x, y, z, 3 + random.nextInt(5) + random.nextInt(5));
        } else if (item == Item.ENDER_PEARL) {
            for (int i = 0; i < 32; i++) world.addParticle("portal", x + random.nextGaussian() * 0.5, y + random.nextDouble() * 2, z + random.nextGaussian() * 0.5);
            if (thrower instanceof LivingEntity le && !le.isDead()) {
                le.setPos(x, y, z);
                le.fallDistance = 0;
                le.damage(DamageSource.FALL, 5, null);
                world.playSound("teleport", x, y, z, 1, 1);
            }
        }
    }
}
