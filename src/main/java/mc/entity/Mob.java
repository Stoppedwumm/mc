package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.util.RayCast;
import mc.world.Block;

/** Animals and monsters with simple goal-based AI. */
public final class Mob extends LivingEntity {
    public final MobType type;
    private double targetX, targetZ;
    private int goalTimer, panicTicks, attackCooldown, jumpCooldown;
    private boolean moving;
    public int fuse, prevFuse;
    public int sheepColor;
    private int eggTimer;
    public boolean aggressive;
    public final ItemStack[] armor = new ItemStack[4];
    public boolean sheared, tamed;
    /** Negative while a baby (counts up to 0), positive during the breeding cooldown. */
    public int growingAge;

    public void setGrowingAge(int age) { growingAge = age; }

    public boolean isBaby() { return growingAge < 0; }

    public Mob(MobType type) {
        this.type = type;
        maxHealth = health = type.maxHealth;
        width = type.width;
        height = type.height;
        stepHeight = 0.6f;
        eggTimer = 6000 + random.nextInt(6000);
        if ((type == MobType.ZOMBIE || type == MobType.SKELETON) && random.nextFloat() < 0.1f) {
            // Like Minecraft: a random material, pieces from the boots upwards
            float r = random.nextFloat();
            int mat = r < 0.37f ? 0 : r < 0.86f ? 3 : r < 0.96f ? 1 : r < 0.99f ? 2 : 4;
            for (int slot = 3; slot >= 0; slot--) {
                armor[slot] = new ItemStack(Item.armor(mat, slot), 1);
                if (random.nextFloat() < 0.25f) break;
            }
        }
        if (type == MobType.SHEEP) {
            int r = random.nextInt(100);
            sheepColor = r < 82 ? 0 : r < 87 ? 1 : r < 92 ? 2 : r < 97 ? 3 : 4;
        }
    }

    @Override
    public double eyeY() { return y + height * 0.85; }

    @Override
    public ItemStack[] armorSlots() { return armor; }

    @Override
    protected double gravity() {
        return type == MobType.CHICKEN && motionY < 0 ? 0.03 : 0.08;
    }

    @Override
    protected boolean canDrown() { return type != MobType.SKELETON; }

    @Override
    protected void onLanded(float distance) {
        if (type != MobType.CHICKEN) super.onLanded(distance);
    }

    @Override
    protected void onHurt(DamageSource source, float amount) {
        world.playSound(type.name().toLowerCase() + "_hurt", x, y + 1, z, 1, pitch());
        if (!type.hostile) panicTicks = 60;
        else if (source == DamageSource.ATTACK || source == DamageSource.ARROW) aggressive = true;
    }

    private float pitch() {
        return switch (type) {
            case CHICKEN -> 1.6f; case PIG -> 1.1f; case COW -> 0.7f; case SPIDER -> 0.8f; default -> 1f;
        } + random.nextFloat() * 0.2f;
    }

    @Override
    protected void onDeath(DamageSource source) {
        world.playSound(type.name().toLowerCase() + "_death", x, y + 1, z, 1, pitch());
        boolean byPlayer = lastAttacker instanceof Player || (lastAttacker instanceof ArrowEntity a && a.shooter instanceof Player);
        dropLoot(fireTicks > 0, byPlayer);
        if (byPlayer) XpOrbEntity.spawn(world, x, y + 0.5, z, type.hostile ? 5 + armorPieces() * (1 + random.nextInt(3)) : 1 + random.nextInt(3));
    }

    private int armorPieces() {
        int n = 0;
        for (ItemStack s : armor) if (s != null) n++;
        return n;
    }

    private void drop(Item item, int min, int max) {
        int n = min + random.nextInt(max - min + 1);
        if (n > 0) world.spawnItem(x, y + 0.5, z, new ItemStack(item, n));
    }

    private void dropLoot(boolean burning, boolean byPlayer) {
        switch (type) {
            case PIG -> drop(burning ? Item.COOKED_PORKCHOP : Item.RAW_PORKCHOP, 1, 3);
            case COW -> { drop(burning ? Item.STEAK : Item.RAW_BEEF, 1, 3); drop(Item.LEATHER, 0, 2); }
            case SHEEP -> drop(Item.of(new Block[]{Block.WHITE_WOOL, Block.BLACK_WOOL, Block.YELLOW_WOOL, Block.RED_WOOL, Block.BLUE_WOOL}[sheepColor]), 1, 1);
            case CHICKEN -> { drop(burning ? Item.COOKED_CHICKEN : Item.RAW_CHICKEN, 1, 1); drop(Item.FEATHER, 0, 2); }
            case ZOMBIE -> drop(Item.ROTTEN_FLESH, 0, 2);
            case SKELETON -> { drop(Item.BONE, 0, 2); drop(Item.ARROW, 0, 2); }
            case CREEPER -> drop(Item.GUNPOWDER, 0, 2);
            case SPIDER -> drop(Item.STRING, 0, 2);
        }
        if (type.hostile && byPlayer && random.nextInt(40) == 0) drop(Item.IRON_INGOT, 1, 1);
        for (ItemStack a : armor) {
            if (a != null && byPlayer && random.nextFloat() < 0.085f) {
                ItemStack d = a.copy();
                d.damage = random.nextInt(Math.max(1, a.item.maxDamage * 3 / 4));
                world.spawnItem(x, y + 0.5, z, d);
            }
        }
    }

    private boolean canSee(Entity e) {
        double dx = e.x - x, dy = e.eyeY() - eyeY(), dz = e.z - z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1e-3) return true;
        RayCast.Hit h = RayCast.cast(world, x, eyeY(), z, dx / d, dy / d, dz / d, d);
        return h == null || !Block.get(world.getBlock(h.x, h.y, h.z)).opaque;
    }

    @Override
    public void tick() {
        super.tick();
        prevFuse = fuse;
        if (isDead()) return;
        if (attackCooldown > 0) attackCooldown--;
        if (jumpCooldown > 0) jumpCooldown--;

        // Undead burn in daylight
        if ((type == MobType.ZOMBIE || type == MobType.SKELETON) && world.isDaytime() && !inWater && armor[0] == null
                && world.getSkyLight((int) Math.floor(x), (int) Math.floor(eyeY()), (int) Math.floor(z)) >= 15 && random.nextInt(20) == 0) {
            fireTicks = Math.max(fireTicks, 160);
        }
        if (type == MobType.CHICKEN && --eggTimer <= 0) {
            world.spawnItem(x, y + 0.3, z, new ItemStack(Item.EGG, 1));
            world.playSound("pop", x, y, z, 0.5f, 1.4f);
            eggTimer = 6000 + random.nextInt(6000);
        }

        float forward = 0;
        float speed = type.walkSpeed;
        boolean jump = false;
        Player player = world.player();
        boolean hostileNow = type.hostile && (type != MobType.SPIDER || aggressive || !world.isDaytime());

        if (hostileNow && player != null && !player.isDead() && !player.creative && distanceTo(player) < 24 && (distanceTo(player) < 8 || canSee(player))) {
            double dist = distanceTo(player);
            faceTowards(player.x, player.z, 30);
            headYaw = yaw;
            speed = type.chaseSpeed;
            switch (type) {
                case SKELETON -> {
                    forward = dist > 10 ? 1 : dist < 5 ? -0.6f : 0;
                    if (attackCooldown == 0 && dist < 15 && canSee(player)) {
                        shootAt(player);
                        attackCooldown = 40 + random.nextInt(20);
                    }
                }
                case CREEPER -> {
                    if (dist < 3.2 && canSee(player)) {
                        if (fuse == 0) world.playSound("fuse", x, y, z, 1, 0.5f);
                        fuse++;
                    } else if (dist > 7) fuse = Math.max(0, fuse - 1);
                    forward = fuse > 0 ? 0 : 1;
                    if (fuse >= 30) {
                        remove();
                        world.explode(x, y + 0.5, z, 3, this);
                        return;
                    }
                }
                case SPIDER -> {
                    forward = 1;
                    if (dist > 2 && dist < 6 && onGround && random.nextInt(10) == 0) {
                        double dx = player.x - x, dz = player.z - z;
                        motionX += dx / dist * 0.4;
                        motionZ += dz / dist * 0.4;
                        motionY = 0.4;
                    }
                    meleeIfClose(player, dist, 2);
                }
                default -> {
                    forward = 1;
                    meleeIfClose(player, dist, 3);
                }
            }
        } else {
            if (fuse > 0) fuse--;
            // Wander or panic
            if (panicTicks > 0) {
                panicTicks--;
                speed = type.chaseSpeed;
                if (goalTimer <= 0 || !moving) pickWanderTarget(8);
                moving = true;
            } else if (--goalTimer <= 0) {
                if (random.nextInt(3) == 0) pickWanderTarget(10);
                else { moving = false; goalTimer = 60 + random.nextInt(120); }
            }
            if (moving) {
                double dx = targetX - x, dz = targetZ - z;
                if (dx * dx + dz * dz < 1) moving = false;
                else {
                    faceTowards(targetX, targetZ, 20);
                    forward = 1;
                }
            }
            if (random.nextInt(40) == 0) headYaw = yaw + (random.nextFloat() - 0.5f) * 80;
            if (random.nextInt(600) == 0) world.playSound(type.name().toLowerCase() + "_say", x, y + 1, z, 0.8f, pitch());
        }

        // Jump over single blocks; spiders climb walls
        if (horizontalCollision && forward != 0) {
            if (type == MobType.SPIDER) motionY = 0.2;
            else if (onGround && jumpCooldown == 0) { jump = true; jumpCooldown = 10; }
        }
        if (jump) jumpFromGround();
        if (inWater && random.nextFloat() < 0.8f) jump = true;
        travel(0, forward, speed, jump);
        bodyYaw += wrap(yaw - bodyYaw) * 0.3f;
        headYaw = bodyYaw + Math.max(-60, Math.min(60, wrap(headYaw - bodyYaw)));
    }

    private void meleeIfClose(Player player, double dist, float damage) {
        if (dist < 1.6 + width / 2 && attackCooldown == 0 && Math.abs(player.y - y) < 1.5) {
            player.damage(DamageSource.ATTACK, damage, this);
            attackCooldown = 20;
        }
    }

    private void shootAt(Player player) {
        ArrowEntity arrow = new ArrowEntity(this);
        arrow.setPos(x, eyeY() - 0.1, z);
        double dx = player.x - x, dz = player.z - z;
        double dy = player.eyeY() - 0.3 - arrow.y;
        double h = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + h * 0.2, dz, 1.6, 10);
        world.addEntity(arrow);
        world.playSound("bow", x, y + 1, z, 1, 0.9f + random.nextFloat() * 0.3f);
    }

    private void pickWanderTarget(int range) {
        targetX = x + random.nextInt(range * 2 + 1) - range;
        targetZ = z + random.nextInt(range * 2 + 1) - range;
        goalTimer = 100 + random.nextInt(100);
        moving = true;
    }

    private void faceTowards(double tx, double tz, float maxTurn) {
        float target = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        yaw += Math.max(-maxTurn, Math.min(maxTurn, wrap(target - yaw)));
    }

    private static float wrap(float a) {
        a %= 360;
        if (a >= 180) a -= 360;
        if (a < -180) a += 360;
        return a;
    }
}
