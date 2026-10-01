package mc.entity;

import mc.item.ItemStack;
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
    /** Active status effects. */
    public final java.util.EnumMap<Effect, Effect.Instance> effects = new java.util.EnumMap<>(Effect.class);
    /** Extra hearts from Absorption (golden apples). */
    public float absorption;
    public int lastHurtAge = -1000;

    protected LivingEntity() {
        health = maxHealth;
    }

    public boolean isDead() { return health <= 0; }

    public boolean hasEffect(Effect e) { return effects.containsKey(e); }

    /** Amplifier of an effect, or -1 if it isn't active. */
    public int amplifier(Effect e) {
        Effect.Instance i = effects.get(e);
        return i == null ? -1 : i.amplifier;
    }

    /** Adds or strengthens an effect; instant effects apply immediately. */
    public void addEffect(Effect e, int amplifier, int duration) {
        if (e == Effect.INSTANT_HEALTH) { heal(4 << amplifier); return; }
        if (e == Effect.INSTANT_DAMAGE) { damage(DamageSource.MAGIC, 6 << amplifier, null); return; }
        Effect.Instance cur = effects.get(e);
        if (cur == null || amplifier > cur.amplifier || (amplifier == cur.amplifier && duration > cur.duration)) {
            effects.put(e, new Effect.Instance(e, amplifier, duration));
            if (e == Effect.ABSORPTION) absorption = Math.max(absorption, 4 * (amplifier + 1));
        }
    }

    public void clearEffects() {
        effects.clear();
        absorption = 0;
    }

    /** Mixed colour of the active effects (for particles), or -1. */
    public int effectColor() {
        if (effects.isEmpty()) return -1;
        int r = 0, g = 0, b = 0, n = 0;
        for (Effect.Instance i : effects.values()) {
            if (i.effect == Effect.INVISIBILITY) continue;
            for (int k = 0; k <= i.amplifier; k++) {
                r += i.effect.color >> 16 & 255; g += i.effect.color >> 8 & 255; b += i.effect.color & 255; n++;
            }
        }
        return n == 0 ? -1 : (r / n) << 16 | (g / n) << 8 | (b / n);
    }

    public void tickEffects() {
        if (effects.isEmpty()) return;
        java.util.Iterator<Effect.Instance> it = effects.values().iterator();
        while (it.hasNext()) {
            Effect.Instance i = it.next();
            switch (i.effect) {
                case REGENERATION -> { int period = Math.max(1, 50 >> i.amplifier); if (i.duration % period == 0) heal(1); }
                case POISON -> { int period = Math.max(1, 25 >> i.amplifier); if (i.duration % period == 0 && health > 1) damage(DamageSource.MAGIC, 1, null); }
                default -> { }
            }
            if (--i.duration <= 0) {
                if (i.effect == Effect.ABSORPTION) absorption = 0;
                it.remove();
            }
        }
        int color = effectColor();
        if (color >= 0 && world != null && random.nextInt(3) == 0)
            world.addParticle("effect:" + Integer.toHexString(color), x + (random.nextDouble() - 0.5) * width, y + random.nextDouble() * height, z + (random.nextDouble() - 0.5) * width);
    }

    /** Movement speed multiplier from Speed and Slowness. */
    public float speedFactor() {
        float f = 1;
        int s = amplifier(Effect.SPEED), sl = amplifier(Effect.SLOWNESS);
        if (s >= 0) f *= 1 + 0.2f * (s + 1);
        if (sl >= 0) f *= Math.max(0, 1 - 0.15f * (sl + 1));
        return f;
    }

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
        if (hasEffect(Effect.FIRE_RESISTANCE) && (source == DamageSource.FIRE || source == DamageSource.LAVA)) return false;
        amount = applyArmor(source, amount);
        amount = applyEnchantProtection(source, amount);
        int res = amplifier(Effect.RESISTANCE);
        if (res >= 0 && source != DamageSource.VOID) amount *= Math.max(0, 1 - 0.2f * (res + 1));
        if (absorption > 0) {
            float absorbed = Math.min(absorption, amount);
            absorption -= absorbed;
            amount -= absorbed;
        }
        if (attacker instanceof LivingEntity le && armorSlots() != null) {
            int thorns = 0;
            for (ItemStack s : armorSlots()) thorns = Math.max(thorns, ItemStack.level(s, mc.item.Enchantment.THORNS));
            if (thorns > 0 && random.nextFloat() < 0.15f * thorns) le.damage(DamageSource.GENERIC, 1 + random.nextInt(4), this);
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

    /** Worn armor (helmet, chestplate, leggings, boots) or null. */
    public ItemStack[] armorSlots() { return null; }

    public int armorValue() {
        ItemStack[] a = armorSlots();
        int v = 0;
        if (a != null) for (ItemStack s : a) if (!ItemStack.isEmpty(s)) v += s.item.armorPoints;
        return v;
    }

    /** Minecraft's armor formula; worn pieces lose durability. */
    protected float applyArmor(DamageSource source, float amount) {
        ItemStack[] a = armorSlots();
        if (a == null || source.bypassesArmor) return amount;
        int def = 0;
        float tough = 0;
        for (ItemStack s : a) if (!ItemStack.isEmpty(s)) { def += s.item.armorPoints; tough += s.item.toughness; }
        if (def == 0) return amount;
        int wear = Math.max(1, (int) (amount / 4));
        for (int i = 0; i < 4; i++) {
            if (!ItemStack.isEmpty(a[i]) && a[i].damageTool(wear)) {
                a[i] = null;
                if (world != null) world.playSound("hit", x, y + 1, z, 1, 0.5f);
            }
        }
        float reduction = Math.min(20, Math.max(def / 5f, def - amount / (2 + tough / 4))) / 25f;
        return amount * (1 - reduction);
    }

    /** Protection enchantments: Minecraft's enchantment protection factor, capped at 20 (80%). */
    protected float applyEnchantProtection(DamageSource source, float amount) {
        ItemStack[] a = armorSlots();
        if (a == null || source == DamageSource.VOID || source == DamageSource.STARVE) return amount;
        int epf = 0;
        for (ItemStack s : a) {
            if (s == null) continue;
            epf += s.level(mc.item.Enchantment.PROTECTION);
            if (source == DamageSource.FIRE || source == DamageSource.LAVA) epf += 2 * s.level(mc.item.Enchantment.FIRE_PROTECTION);
            if (source == DamageSource.EXPLOSION) epf += 2 * s.level(mc.item.Enchantment.BLAST_PROTECTION);
            if (source == DamageSource.ARROW) epf += 2 * s.level(mc.item.Enchantment.PROJECTILE_PROTECTION);
            if (source == DamageSource.FALL) epf += 3 * s.level(mc.item.Enchantment.FEATHER_FALLING);
        }
        epf = Math.min(20, Math.max(0, epf));
        return amount * (1 - epf / 25f);
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
        int dmg = (int) Math.ceil(distance - 3 - (amplifier(Effect.JUMP_BOOST) + 1));
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
        tickEffects();
        if (isDead()) return;
        // Drowning
        if (eyeInBlock(Block.WATER.id) && canDrown() && !hasEffect(Effect.WATER_BREATHING)) {
            ItemStack[] worn = armorSlots();
            int resp = worn == null ? 0 : ItemStack.level(worn[0], mc.item.Enchantment.RESPIRATION);
            if (resp == 0 || random.nextInt(resp + 1) == 0) air--;
            if (air <= -20) {
                air = 0;
                damage(DamageSource.DROWN, 2, null);
            }
        } else {
            air = Math.min(300, air + 5);
        }
        if (fireImmune() || hasEffect(Effect.FIRE_RESISTANCE)) { if (fireImmune()) fireTicks = 0; else if (fireTicks > 0) fireTicks--; }
        else {
            if (inLava) {
                fireTicks = 300;
                damage(DamageSource.LAVA, 4, null);
            }
            if (touching(Block.FIRE.id)) {
                fireTicks = Math.max(fireTicks, 160);
                damage(DamageSource.FIRE, 1, null);
            }
            if (fireTicks > 0) {
                fireTicks--;
                if (fireTicks % 20 == 0 && !inLava) damage(DamageSource.FIRE, 1, null);
            }
        }
        if (touchingCactus()) damage(DamageSource.CACTUS, 1, null);
        if (y < -64) damage(DamageSource.VOID, 4, null);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        prevLimbSwingAmount = limbSwingAmount;
        prevBodyYaw = bodyYaw;
        prevHeadYaw = headYaw;
        limbSwingAmount *= 0.6f;
        if (hurtTime > 0) hurtTime--;
        if (invulnerableTime > 0) invulnerableTime--;
        if (vehicle != null) bodyYaw += wrapDegrees(vehicle.yaw - bodyYaw) * 0.3f;
    }

    @Override
    public void netTick() {
        prevLimbSwingAmount = limbSwingAmount;
        prevBodyYaw = bodyYaw;
        prevHeadYaw = headYaw;
        float oldYaw = yaw;
        super.netTick();
        if (hurtTime > 0) hurtTime--;
        if (isDead()) deathTime++;
        headYaw += wrapDegrees(netHeadYaw - headYaw) * 0.5f;
        // Bodies turn towards where they walk, like the server's
        double dx = x - prevX, dz = z - prevZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.02f) bodyYaw += wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - bodyYaw) * 0.4f;
        else bodyYaw += wrapDegrees(yaw - bodyYaw) * 0.3f;
        if (Math.abs(wrapDegrees(headYaw - bodyYaw)) > 75) bodyYaw = headYaw - Math.signum(wrapDegrees(headYaw - bodyYaw)) * 75;
        float swing = Math.min(1, dist * 4);
        limbSwingAmount += (swing - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
        int color = effectColor();
        if (color >= 0 && world != null && random.nextInt(3) == 0)
            world.addParticle("effect:" + Integer.toHexString(color), x + (random.nextDouble() - 0.5) * width, y + random.nextDouble() * height, z + (random.nextDouble() - 0.5) * width);
    }

    @Override
    protected void netTargetReached() {
        bodyYaw = prevBodyYaw = headYaw = prevHeadYaw = netHeadYaw;
    }

    protected boolean canDrown() { return true; }

    /** Nether mobs don't burn. */
    public boolean fireImmune() { return false; }

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
            speed *= speedFactor();
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
        motionY = 0.42 + 0.1 * (amplifier(Effect.JUMP_BOOST) + 1);
    }
}
