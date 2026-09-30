package mc.entity;

import mc.item.Inventory;
import mc.world.Block;

/** The local player, simulated at 20 ticks per second with Minecraft's movement constants. */
public final class Player extends LivingEntity {
    public static final double WIDTH = 0.6, HEIGHT = 1.8, EYE = 1.62, SNEAK_EYE = 1.27;

    public boolean flying, sneaking, sprinting;
    public boolean creative = false;
    public float walkDist, prevWalkDist, bob, prevBob, tilt, prevTilt;
    public float eyeHeight = (float) EYE, prevEyeHeight = (float) EYE;
    /** Set when a footstep should play this tick. */
    public boolean stepThisTick;
    public boolean landedThisTick, splashThisTick;
    private float nextStep = 1;
    private int jumpCooldown;
    public final Inventory inventory = new Inventory();
    public int food = 20;
    public float saturation = 5, exhaustion;
    private int foodTimer;
    public double spawnX, spawnY = -1, spawnZ;
    public DamageSource deathCause;
    public int xpLevel, xpTotal;
    /** Progress towards the next level, 0-1. */
    public float xpProgress;

    public Player() {
        width = 0.6f;
        height = 1.8f;
    }

    @Override
    public double eyeY() { return y + eyeHeight; }

    /** Experience needed to go from `level` to the next one. */
    public static int xpBarCap(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    /** Adds experience points; returns true if a level was gained. */
    public boolean addXp(int amount) {
        int before = xpLevel;
        xpTotal += amount;
        xpProgress += (float) amount / xpBarCap(xpLevel);
        while (xpProgress >= 1) {
            xpProgress = (xpProgress - 1) * xpBarCap(xpLevel);
            xpLevel++;
            xpProgress /= xpBarCap(xpLevel);
        }
        return xpLevel > before;
    }

    /** Spends levels (enchanting). */
    public void removeLevels(int levels) {
        xpLevel = Math.max(0, xpLevel - levels);
        if (xpLevel == 0 && levels > 0) xpProgress = 0;
    }

    @Override
    protected boolean avoidsEdges() { return sneaking; }

    @Override
    protected boolean holdsOnLadder() { return sneaking; }

    @Override
    public mc.item.ItemStack[] armorSlots() { return inventory.armor; }

    @Override
    protected float applyArmor(DamageSource source, float amount) {
        return creative ? amount : super.applyArmor(source, amount);
    }

    @Override
    protected boolean isInvulnerable() { return creative; }

    @Override
    public boolean damage(DamageSource source, float amount, Entity attacker) {
        if (creative && source != DamageSource.VOID) return false;
        boolean ok = super.damage(source, amount, attacker);
        if (ok) exhaustion += 0.1f;
        return ok;
    }

    @Override
    protected void onDeath(DamageSource source) {
        deathCause = source;
    }

    @Override
    protected void onLanded(float distance) {
        if (distance > 1.2f) landedThisTick = true;
        if (!flying) super.onLanded(distance);
    }

    public void addExhaustion(float f) {
        if (!creative) exhaustion += f;
    }

    public void eat(int f, float sat) {
        food = Math.min(20, food + f);
        saturation = Math.min(food, saturation + sat);
    }

    public void respawn() {
        health = maxHealth;
        food = 20;
        saturation = 5;
        exhaustion = 0;
        air = 300;
        fireTicks = 0;
        deathTime = 0;
        hurtTime = 0;
        removed = false;
        motionX = motionY = motionZ = 0;
        fallDistance = 0;
        setPos(spawnX, spawnY, spawnZ);
    }

    /** Legacy entry point used by tests: sets the world and ticks. */
    public void tick(mc.world.World world, float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
        this.world = world;
        tick(forward, strafe, jump, sneak, sprint);
    }

    public void tick(float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
        prevWalkDist = walkDist;
        prevBob = bob;
        prevTilt = tilt;
        prevEyeHeight = eyeHeight;
        stepThisTick = landedThisTick = splashThisTick = false;
        boolean wasInWater = inWater;
        super.tick();
        if (isDead()) return;
        if (inWater && !wasInWater && motionY < -0.2) splashThisTick = true;
        if (jumpCooldown > 0) jumpCooldown--;

        sneaking = sneak && !flying;
        if (forward <= 0 || sneaking || horizontalCollision || (food <= 6 && !creative)) sprinting = false;
        else if (sprint) sprinting = true;
        if (sneaking) { forward *= 0.3f; strafe *= 0.3f; }
        forward *= 0.98f;
        strafe *= 0.98f;

        float targetEye = (float) (sneaking ? 1.54 : EYE);
        eyeHeight += (targetEye - eyeHeight) * 0.5f;

        if (flying) {
            if (jump) motionY += 0.15;
            if (sneak) motionY -= 0.15;
            moveRelative(strafe, forward, sprinting ? 0.1f : 0.05f);
            move(motionX, motionY, motionZ);
            motionX *= 0.91; motionZ *= 0.91; motionY *= 0.6;
            fallDistance = 0;
            if (onGround && (!creative || sneak)) flying = false;
        } else {
            if (jump && onGround && jumpCooldown == 0 && !inWater && !inLava) {
                jumpFromGround();
                if (sprinting) {
                    double r = Math.toRadians(yaw);
                    motionX -= Math.sin(r) * 0.2;
                    motionZ += Math.cos(r) * 0.2;
                    addExhaustion(0.2f);
                } else addExhaustion(0.05f);
                jumpCooldown = 10;
            }
            travel(strafe, forward, sprinting ? 0.13f : 0.1f, jump);
        }

        double dx = x - prevX, dz = z - prevZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        walkDist += dist * 0.6f;
        if (sprinting) addExhaustion(dist * 0.1f);
        else if (inWater) addExhaustion(dist * 0.01f);
        float targetBob = onGround && !flying ? Math.min(0.1f, dist) : 0;
        bob += (targetBob - bob) * 0.4f;
        float targetTilt = onGround && !flying ? (float) Math.atan(-motionY * 0.2) * 15 : 0;
        tilt += (targetTilt - tilt) * 0.8f;
        if (onGround && !flying && walkDist > nextStep && !inWater) {
            nextStep = walkDist + 1;
            stepThisTick = !sneaking;
        }
        tickFood();
    }

    private void tickFood() {
        if (creative) return;
        if (exhaustion > 4) {
            exhaustion -= 4;
            if (saturation > 0) saturation = Math.max(0, saturation - 1);
            else food = Math.max(0, food - 1);
        }
        foodTimer++;
        if (food >= 20 && saturation > 0 && health < maxHealth) {
            if (foodTimer >= 10) {
                float amount = Math.min(saturation, 6);
                heal(amount / 6);
                exhaustion += amount;
                foodTimer = 0;
            }
        } else if (food >= 18 && health < maxHealth) {
            if (foodTimer >= 80) {
                heal(1);
                exhaustion += 6;
                foodTimer = 0;
            }
        } else if (food <= 0) {
            if (foodTimer >= 80) {
                if (health > 1) damage(DamageSource.STARVE, 1, null);
                foodTimer = 0;
            }
        } else {
            foodTimer = 0;
        }
    }

    public boolean isOnLadderOrWater() {
        return world.getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)) == Block.WATER.id;
    }
}
