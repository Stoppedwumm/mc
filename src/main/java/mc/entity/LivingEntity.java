package mc.entity;

import mc.world.Block;

/** Entities with health: damage, knockback, drowning, burning, fall damage and movement physics. */
public abstract class LivingEntity extends Entity {
    public float health, maxHealth = 20;
    public int hurtTime, deathTime, invulnerableTime;
    public int air = 300;
    public float lastDamage;
    public float limbSwing, limbSwingAmount, prevLimbSwingAmount;
    public float bodyYaw, prevBodyYaw, headYaw, prevHeadYaw;
    public Entity lastAttacker;
    public int lastHurtAge = -1000;

    protected LivingEntity() {
        health = maxHealth;
    }

    public boolean isDead() { return health <= 0; }

    protected boolean isInvulnerable() { return false; }

    /** Applies damage; returns true if it took effect. */
    public boolean damage(DamageSource source, float amount, Entity attacker) {
        if (isDead() || isInvulnerable()) return false;
        if (invulnerableTime > 10) {
            if (amount <= lastDamage) return false;
            float extra = amount - lastDamage;
            lastDamage = amount;
            amount = extra;
        } else {
            lastDamage = amount;
            invulnerableTime = 20;
            hurtTime = 10;
        }
        health -= amount;
        lastHurtAge = age;
        lastAttacker = attacker;
        if (attacker != null) {
            double dx = attacker.x - x, dz = attacker.z - z;
            knockback(0.4, dx, dz);
        }
        onHurt(source, amount);
        if (health <= 0) {
            health = 0;
            onDeath(source);
        }
        return true;
    }

    public void knockback(double strength, double dx, double dz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1e-4) return;
        motionX = motionX / 2 - dx / d * strength;
        motionZ = motionZ / 2 - dz / d * strength;
        motionY = Math.min(0.4, motionY / 2 + strength);
    }

    protected void onHurt(DamageSource source, float amount) { }

    protected void onDeath(DamageSource source) { }

    public void heal(float amount) {
        if (!isDead()) health = Math.min(maxHealth, health + amount);
    }

    @Override
    protected void onLanded(float distance) {
        if (inWater) return;
        int dmg = (int) Math.ceil(distance - 3);
        if (dmg > 0) damage(DamageSource.FALL, dmg, null);
    }

    @Override
    public void tick() {
        super.tick();
        prevLimbSwingAmount = limbSwingAmount;
        prevBodyYaw = bodyYaw;
        prevHeadYaw = headYaw;
        if (hurtTime > 0) hurtTime--;
        if (invulnerableTime > 0) invulnerableTime--;
        if (isDead()) {
            deathTime++;
            if (deathTime >= 20) remove();
            return;
        }
        // Drowning
        if (eyeInBlock(Block.WATER.id) && canDrown()) {
            air--;
            if (air <= -20) {
                air = 0;
                damage(DamageSource.DROWN, 2, null);
            }
        } else {
            air = Math.min(300, air + 5);
        }
        if (inLava) {
            fireTicks = 300;
            damage(DamageSource.LAVA, 4, null);
        }
        if (fireTicks > 0) {
            fireTicks--;
            if (fireTicks % 20 == 0 && !inLava) damage(DamageSource.FIRE, 1, null);
        }
        if (touchingCactus()) damage(DamageSource.CACTUS, 1, null);
        if (y < -64) damage(DamageSource.VOID, 4, null);
    }

    protected boolean canDrown() { return true; }

    private boolean touchingCactus() {
        var b = box();
        for (int bx = (int) Math.floor(b.minX - 0.01); bx <= (int) Math.floor(b.maxX + 0.01); bx++)
            for (int by = (int) Math.floor(b.minY); by <= (int) Math.floor(b.maxY); by++)
                for (int bz = (int) Math.floor(b.minZ - 0.01); bz <= (int) Math.floor(b.maxZ + 0.01); bz++)
                    if (world.getBlock(bx, by, bz) == Block.CACTUS.id) return true;
        return false;
    }

    protected float slipperiness() {
        int id = world.getBlock((int) Math.floor(x), (int) Math.floor(y - 0.5), (int) Math.floor(z));
        return id == Block.ICE.id ? 0.98f : 0.6f;
    }

    protected void moveRelative(float strafe, float forward, float accel) {
        float f = strafe * strafe + forward * forward;
        if (f < 1e-4f) return;
        f = (float) Math.sqrt(f);
        if (f < 1) f = 1;
        f = accel / f;
        strafe *= f;
        forward *= f;
        double r = Math.toRadians(yaw);
        double sin = Math.sin(r), cos = Math.cos(r);
        motionX += strafe * cos - forward * sin;
        motionZ += forward * cos + strafe * sin;
    }

    /** Minecraft's living-entity travel: ground friction, air control, swimming and gravity. */
    protected void travel(float strafe, float forward, float speed, boolean jump) {
        double startY = y;
        if (inWater || inLava) {
            moveRelative(strafe, forward, 0.02f);
            move(motionX, motionY, motionZ);
            double drag = inWater ? 0.8 : 0.5;
            motionX *= drag; motionY *= drag; motionZ *= drag;
            motionY -= 0.02;
            if (jump) motionY += 0.04;
            if (horizontalCollision && fits(box().offset(motionX, motionY + 0.6 - y + startY, motionZ))) motionY = 0.3;
        } else {
            float slip = onGround ? slipperiness() * 0.91f : 0.91f;
            float accel = onGround ? speed * 0.16277136f / (slip * slip * slip) : speed * 0.2f;
            moveRelative(strafe, forward, accel);
            boolean ladder = onLadder();
            if (ladder) {
                // Climbing: limited sideways speed, slow descent (none while sneaking), push against it to go up
                motionX = Math.max(-0.15, Math.min(0.15, motionX));
                motionZ = Math.max(-0.15, Math.min(0.15, motionZ));
                motionY = Math.max(motionY, -0.15);
                if (holdsOnLadder() && motionY < 0) motionY = 0;
                fallDistance = 0;
            }
            move(motionX, motionY, motionZ);
            if (ladder && (horizontalCollision || jump)) motionY = 0.2;
            motionY -= gravity();
            motionY *= 0.98;
            motionX *= slip;
            motionZ *= slip;
        }
        double dx = x - prevX, dz = z - prevZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz) * 4;
        if (dist > 1) dist = 1;
        limbSwingAmount += (dist - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
    }

    protected double gravity() { return 0.08; }

    public boolean onLadder() {
        return Block.get(world.getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))).climbable;
    }

    /** Sneaking players stop on ladders. */
    protected boolean holdsOnLadder() { return false; }

    protected void jumpFromGround() {
        motionY = 0.42;
    }
}
