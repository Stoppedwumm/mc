package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.util.RayCast;
import mc.world.Block;

/**
 * Animals and monsters with simple goal-based AI: wandering, panicking, following food, breeding, chasing and
 * attacking, plus the special behaviour of wolves, squid, endermen and slimes.
 */
public final class Mob extends LivingEntity {
    public final MobType type;
    private double targetX, targetZ;
    private int goalTimer, panicTicks, attackCooldown, jumpCooldown;
    private boolean moving;
    public int fuse, prevFuse;
    /** Sheep wool colour index (see WOOL_COLORS). */
    public int sheepColor;
    private int eggTimer;
    public boolean aggressive;
    public final ItemStack[] armor = new ItemStack[4];
    public boolean sheared, tamed, sitting;
    /** Negative while a baby (counts up to 0), positive during the breeding cooldown. */
    public int growingAge;
    public int loveTicks;
    private int breedTimer;
    /** Wild wolves and endermen attack the player while angry. */
    public int angerTicks;
    /** Target of a tamed wolf. */
    public LivingEntity attackTarget;
    public int slimeSize = 1;
    private int slimeJumpDelay;
    public float squish, prevSquish;
    /** Block an enderman is holding (0 = none). */
    public int carriedBlock;
    public int eatGrassTicks;
    public float tentacleAngle, prevTentacleAngle, squidPitch, prevSquidPitch;
    private double swimX, swimY, swimZ;
    private int teleportCooldown, dryTicks;

    public static final Block[] WOOL = {Block.WHITE_WOOL, Block.BLACK_WOOL, Block.YELLOW_WOOL, Block.RED_WOOL, Block.BLUE_WOOL};

    public Mob(MobType type) {
        this.type = type;
        maxHealth = health = type.maxHealth;
        width = type.width;
        height = type.height;
        stepHeight = 0.6f;
        eggTimer = 6000 + random.nextInt(6000);
        if (type.isUndead() && random.nextFloat() < 0.1f) {
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
        if (type == MobType.SLIME) setSlimeSize(1 << random.nextInt(3));
        if (type == MobType.SQUID) stepHeight = 0;
    }

    public void setGrowingAge(int age) {
        growingAge = age;
        updateSize();
    }

    public boolean isBaby() { return growingAge < 0; }

    public void setSlimeSize(int size) {
        slimeSize = size;
        maxHealth = health = size * size;
        updateSize();
    }

    private void updateSize() {
        if (type == MobType.SLIME) {
            width = height = 0.51f * slimeSize;
            return;
        }
        float s = isBaby() ? 0.5f : 1;
        width = type.width * s;
        height = type.height * s;
    }

    @Override
    public double eyeY() { return y + height * 0.85; }

    @Override
    public ItemStack[] armorSlots() { return armor; }

    @Override
    protected double gravity() {
        if (type == MobType.CHICKEN && motionY < 0) return 0.03;
        return 0.08;
    }

    @Override
    protected boolean canDrown() { return type != MobType.SKELETON && type != MobType.SQUID; }

    @Override
    protected void onLanded(float distance) {
        if (type != MobType.CHICKEN && type != MobType.SLIME) super.onLanded(distance);
        if (type == MobType.SLIME) {
            squish = -0.5f;
            for (int i = 0; i < slimeSize * 8; i++) world.addParticle("slime", x + (random.nextDouble() - 0.5) * width, y + 0.1, z + (random.nextDouble() - 0.5) * width);
        }
    }

    private String soundName() {
        return type.name().toLowerCase();
    }

    @Override
    public boolean damage(DamageSource source, float amount, Entity attacker) {
        // Endermen dodge projectiles by teleporting
        if (type == MobType.ENDERMAN && source == DamageSource.ARROW) {
            for (int i = 0; i < 16 && !teleportRandomly(); i++) { }
            return false;
        }
        if (type == MobType.WOLF && tamed && sitting) sitting = false;
        return super.damage(source, amount, attacker);
    }

    @Override
    protected void onHurt(DamageSource source, float amount) {
        world.playSound(soundName() + "_hurt", x, y + 1, z, 1, pitch());
        boolean byPlayer = lastAttacker instanceof Player || (lastAttacker instanceof ArrowEntity a && a.shooter instanceof Player);
        if (type == MobType.WOLF) {
            if (!tamed && byPlayer) angerNearbyWolves();
            if (tamed && lastAttacker instanceof LivingEntity le && !(le instanceof Player)) attackTarget = le;
        } else if (type == MobType.ENDERMAN) {
            if (byPlayer) angerTicks = 600;
            if (random.nextInt(3) == 0) teleportRandomly();
        } else if (!type.hostile) {
            panicTicks = 60;
        } else if (source == DamageSource.ATTACK || source == DamageSource.ARROW) {
            aggressive = true;
        }
    }

    /** A wild wolf that is hit calls its pack. */
    private void angerNearbyWolves() {
        for (Entity e : world.entities())
            if (e instanceof Mob m && m.type == MobType.WOLF && !m.tamed && m.distanceTo(this) < 16) m.angerTicks = 400 + random.nextInt(400);
    }

    private float pitch() {
        float base = switch (type) {
            case CHICKEN -> 1.6f; case PIG -> 1.1f; case COW -> 0.7f; case SPIDER -> 0.8f; case SLIME -> 2f / slimeSize + 0.4f; default -> 1f;
        };
        if (isBaby()) base *= 1.4f;
        return base + random.nextFloat() * 0.2f;
    }

    @Override
    protected void onDeath(DamageSource source) {
        world.playSound(soundName() + "_death", x, y + 1, z, 1, pitch());
        boolean byPlayer = lastAttacker instanceof Player || (lastAttacker instanceof ArrowEntity a && a.shooter instanceof Player)
                || (lastAttacker instanceof Mob m && m.type == MobType.WOLF && m.tamed);
        if (!isBaby()) dropLoot(fireTicks > 0, byPlayer);
        if (byPlayer && !isBaby()) {
            int xp = type == MobType.SLIME ? slimeSize : type.hostile ? 5 + armorPieces() * (1 + random.nextInt(3)) : 1 + random.nextInt(3);
            XpOrbEntity.spawn(world, x, y + 0.5, z, xp);
        }
        if (type == MobType.SLIME && slimeSize > 1) {
            // Split into 2-4 smaller slimes
            int n = 2 + random.nextInt(3);
            for (int i = 0; i < n; i++) {
                Mob s = new Mob(MobType.SLIME);
                s.setSlimeSize(slimeSize / 2);
                s.setPos(x + (i % 2 - 0.5) * slimeSize / 4.0, y + 0.5, z + (i / 2 - 0.5) * slimeSize / 4.0);
                s.yaw = random.nextFloat() * 360;
                world.addEntity(s);
            }
        }
        if (type == MobType.ENDERMAN && carriedBlock != 0) world.spawnItem(x, y + 1, z, new ItemStack(Item.get(carriedBlock), 1));
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
            case SHEEP -> { if (!sheared) drop(Item.of(WOOL[sheepColor]), 1, 1); }
            case CHICKEN -> { drop(burning ? Item.COOKED_CHICKEN : Item.RAW_CHICKEN, 1, 1); drop(Item.FEATHER, 0, 2); }
            case ZOMBIE -> {
                drop(Item.ROTTEN_FLESH, 0, 2);
                if (byPlayer && random.nextInt(40) == 0) drop(random.nextBoolean() ? Item.CARROT : Item.POTATO, 1, 1);
            }
            case SKELETON -> { drop(Item.BONE, 0, 2); drop(Item.ARROW, 0, 2); }
            case CREEPER -> drop(Item.GUNPOWDER, 0, 2);
            case SPIDER -> drop(Item.STRING, 0, 2);
            case SQUID -> drop(Item.INK_SAC, 1, 3);
            case ENDERMAN -> drop(Item.ENDER_PEARL, 0, 1);
            case SLIME -> { if (slimeSize == 1) drop(Item.SLIMEBALL, 0, 2); }
            default -> { }
        }
        if (type.isUndead() && byPlayer && random.nextInt(40) == 0) drop(Item.IRON_INGOT, 1, 1);
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

    // ------------------------------------------------------------------ player interaction

    /** Whether the item makes this animal follow the player and breed. */
    public boolean isBreedingItem(Item item) {
        if (item == null) return false;
        return switch (type) {
            case COW, SHEEP -> item == Item.WHEAT;
            case PIG -> item == Item.CARROT || item == Item.POTATO;
            case CHICKEN -> item == Item.SEEDS;
            case WOLF -> tamed && (item == Item.RAW_BEEF || item == Item.STEAK || item == Item.RAW_PORKCHOP || item == Item.COOKED_PORKCHOP
                    || item == Item.RAW_CHICKEN || item == Item.COOKED_CHICKEN || item == Item.ROTTEN_FLESH);
            default -> false;
        };
    }

    private void consumeHeld(Player p) {
        if (p.creative) return;
        ItemStack h = p.inventory.held();
        if (h != null) h.count--;
        p.inventory.cleanup();
    }

    /** Right click by the player; returns true if something happened. */
    public boolean interact(Player p) {
        if (isDead()) return false;
        ItemStack h = p.inventory.held();
        Item item = ItemStack.isEmpty(h) ? null : h.item;
        if (type == MobType.COW && item == Item.BUCKET && !isBaby()) {
            if (!p.creative) {
                if (h.count == 1) p.inventory.setHeld(new ItemStack(Item.MILK_BUCKET, 1));
                else {
                    h.count--;
                    ItemStack left = p.inventory.add(new ItemStack(Item.MILK_BUCKET, 1));
                    if (left.count > 0) world.spawnItem(p.x, p.y + 1, p.z, left);
                }
            }
            world.playSound("bucket", x, y + 1, z, 1, 1.2f);
            return true;
        }
        if (type == MobType.SHEEP && item == Item.SHEARS && !sheared && !isBaby()) {
            sheared = true;
            int n = 1 + random.nextInt(3);
            for (int i = 0; i < n; i++) {
                world.spawnItem(x, y + 1, z, new ItemStack(Item.of(WOOL[sheepColor]), 1));
            }
            world.playSound("shear", x, y + 1, z, 1, 1);
            if (!p.creative && h.damageTool(1)) p.inventory.cleanup();
            return true;
        }
        if (type == MobType.WOLF) {
            if (!tamed && item == Item.BONE && angerTicks == 0) {
                consumeHeld(p);
                if (random.nextInt(3) == 0) {
                    tamed = true;
                    sitting = true;
                    maxHealth = health = 20;
                    attackTarget = null;
                    for (int i = 0; i < 7; i++) world.addParticle("heart", x + random.nextGaussian() * 0.3, y + height + 0.3, z + random.nextGaussian() * 0.3);
                } else {
                    for (int i = 0; i < 7; i++) world.addParticle("smoke", x + random.nextGaussian() * 0.3, y + height + 0.3, z + random.nextGaussian() * 0.3);
                }
                return true;
            }
            if (tamed) {
                if (isBreedingItem(item) && health < maxHealth) {
                    consumeHeld(p);
                    heal(item.food);
                    for (int i = 0; i < 3; i++) world.addParticle("heart", x, y + height + 0.3, z);
                    return true;
                }
                if (!isBreedingItem(item) || growingAge != 0 || loveTicks > 0) {
                    sitting = !sitting;
                    attackTarget = null;
                    motionX = motionZ = 0;
                    return true;
                }
            }
        }
        if (type.isAnimal() && isBreedingItem(item)) {
            if (isBaby()) {
                consumeHeld(p);
                setGrowingAge(growingAge + (-growingAge) / 10);
                for (int i = 0; i < 4; i++) world.addParticle("happy", x + random.nextGaussian() * 0.3, y + height, z + random.nextGaussian() * 0.3);
                return true;
            }
            if (growingAge == 0 && loveTicks == 0) {
                consumeHeld(p);
                loveTicks = 600;
                for (int i = 0; i < 7; i++) world.addParticle("heart", x + random.nextGaussian() * 0.4, y + height + 0.2, z + random.nextGaussian() * 0.4);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        prevFuse = fuse;
        prevSquish = squish;
        prevTentacleAngle = tentacleAngle;
        prevSquidPitch = squidPitch;
        if (isDead()) return;
        if (attackCooldown > 0) attackCooldown--;
        if (jumpCooldown > 0) jumpCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (growingAge < 0) { if (++growingAge == 0) updateSize(); }
        else if (growingAge > 0) growingAge--;
        if (loveTicks > 0) {
            loveTicks--;
            if (loveTicks % 10 == 0) world.addParticle("heart", x + random.nextGaussian() * 0.3, y + height + 0.2, z + random.nextGaussian() * 0.3);
        }
        if (angerTicks > 0) angerTicks--;
        if (eatGrassTicks > 0) eatGrassTicks--;
        squish *= 0.6f;

        // Undead burn in daylight unless wearing a helmet
        if (type.isUndead() && world.isDaytime() && !inWater && armor[0] == null
                && world.getSkyLight((int) Math.floor(x), (int) Math.floor(eyeY()), (int) Math.floor(z)) >= 15 && random.nextInt(20) == 0) {
            fireTicks = Math.max(fireTicks, 160);
        }
        if (type == MobType.ENDERMAN) tickEnderman();
        if (type == MobType.CHICKEN && !isBaby() && --eggTimer <= 0) {
            world.spawnItem(x, y + 0.3, z, new ItemStack(Item.EGG, 1));
            world.playSound("pop", x, y, z, 0.5f, 1.4f);
            eggTimer = 6000 + random.nextInt(6000);
        }
        if (type == MobType.SHEEP && eatGrassTicks == 0 && random.nextInt(isBaby() ? 50 : 1000) == 0) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y - 0.5), bz = (int) Math.floor(z);
            if (world.getBlock(bx, by, bz) == Block.GRASS.id) {
                eatGrassTicks = 40;
                world.setBlock(bx, by, bz, Block.DIRT.id);
                sheared = false;
                if (isBaby()) setGrowingAge(Math.min(0, growingAge + 60));
            }
        }
        if (type == MobType.SQUID) {
            tickSquid();
            return;
        }

        float forward = 0;
        float speed = type.walkSpeed;
        boolean jump = false;
        Player player = world.player();
        LivingEntity target = findTarget(player);

        if (target != null) {
            double dist = distanceTo(target);
            faceTowards(target.x, target.z, 30);
            headYaw = yaw;
            speed = type.chaseSpeed;
            switch (type) {
                case SKELETON -> {
                    forward = dist > 10 ? 1 : dist < 5 ? -0.6f : 0;
                    if (attackCooldown == 0 && dist < 15 && canSee(target)) {
                        shootAt(target);
                        attackCooldown = 40 + random.nextInt(20);
                    }
                }
                case CREEPER -> {
                    if (dist < 3.2 && canSee(target)) {
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
                        double dx = target.x - x, dz = target.z - z;
                        motionX += dx / dist * 0.4;
                        motionZ += dz / dist * 0.4;
                        motionY = 0.4;
                    }
                    meleeIfClose(target, dist, 2);
                }
                case ENDERMAN -> {
                    forward = 1;
                    if (dist > 12 && teleportCooldown == 0 && random.nextInt(40) == 0) teleportTowards(target);
                    meleeIfClose(target, dist, 7);
                }
                case WOLF -> {
                    forward = 1;
                    if (dist < 4 && dist > 1.5 && onGround && random.nextInt(15) == 0) motionY = 0.4;
                    meleeIfClose(target, dist, tamed ? 4 : 2);
                }
                case SLIME -> {
                    if (slimeSize > 1) meleeIfClose(target, dist, slimeSize == 4 ? 4 : 2);
                }
                default -> {
                    forward = 1;
                    meleeIfClose(target, dist, 3);
                }
            }
        } else {
            if (fuse > 0) fuse--;
            float[] f = passiveMovement(player);
            forward = f[0];
            speed = f[1];
        }

        if (type == MobType.SLIME) {
            // Slimes hop: jump from the ground, drift forward while airborne
            if (onGround) {
                if (slimeJumpDelay-- <= 0) {
                    slimeJumpDelay = (random.nextInt(20) + 10) / (target != null ? 3 : 1);
                    if (target != null || moving) {
                        jump = true;
                        squish = 1;
                        world.playSound("slime_say", x, y, z, 0.3f * slimeSize, pitch());
                    }
                }
                forward = 0;
            } else forward = 1;
            speed = 0.05f + slimeSize * 0.01f;
        }

        // Jump over single blocks; spiders climb walls
        if (horizontalCollision && forward != 0) {
            if (type == MobType.SPIDER) motionY = 0.2;
            else if (onGround && jumpCooldown == 0) { jump = true; jumpCooldown = 10; }
        }
        if (jump && onGround) jumpFromGround();
        if (inWater && random.nextFloat() < 0.8f) jump = true;
        if (sitting) forward = 0;
        travel(0, forward, speed, jump);
        bodyYaw += wrap(yaw - bodyYaw) * 0.3f;
        headYaw = bodyYaw + Math.max(-60, Math.min(60, wrap(headYaw - bodyYaw)));
    }

    /** Who this mob is currently fighting, or null. */
    private LivingEntity findTarget(Player player) {
        boolean playerValid = player != null && !player.isDead() && !player.creative;
        switch (type) {
            case WOLF -> {
                if (tamed) {
                    if (attackTarget != null && (attackTarget.isDead() || attackTarget.removed || attackTarget.distanceTo(this) > 24)) attackTarget = null;
                    return sitting ? null : attackTarget;
                }
                return angerTicks > 0 && playerValid && distanceTo(player) < 24 ? player : null;
            }
            case ENDERMAN -> {
                return angerTicks > 0 && playerValid && distanceTo(player) < 64 ? player : null;
            }
            default -> {
                if (!type.hostile || !playerValid) return null;
                if (type == MobType.SPIDER && !aggressive && world.isDaytime()) return null;
                double d = distanceTo(player);
                return d < (type == MobType.SLIME ? 16 : 24) && (d < 8 || canSee(player)) ? player : null;
            }
        }
    }

    /** Wandering, panicking, following food, breeding and wolves following their owner: {forward, speed}. */
    private float[] passiveMovement(Player player) {
        float forward = 0, speed = type.walkSpeed;
        if (type == MobType.WOLF && tamed && player != null && !player.isDead()) {
            if (sitting) return new float[]{0, speed};
            double d = distanceTo(player);
            if (d > 12 && onGround) teleportNear(player);
            else if (d > 3) {
                faceTowards(player.x, player.z, 30);
                return new float[]{1, type.chaseSpeed};
            }
        }
        // Follow a player holding this animal's food
        if (type.isAnimal() && player != null && !player.isDead() && panicTicks == 0 && loveTicks == 0) {
            ItemStack h = player.inventory.held();
            if (!ItemStack.isEmpty(h) && isBreedingItem(h.item) && distanceTo(player) < 10) {
                faceTowards(player.x, player.z, 30);
                headYaw = yaw;
                return new float[]{distanceTo(player) > 2.5 ? 1 : 0, type.walkSpeed * 1.4f};
            }
        }
        if (loveTicks > 0) {
            Mob partner = null;
            double best = 8;
            for (Entity e : world.entities()) {
                if (e == this || !(e instanceof Mob m) || m.type != type || m.loveTicks <= 0 || m.isBaby() || m.isDead()) continue;
                double d = distanceTo(m);
                if (d < best) { best = d; partner = m; }
            }
            if (partner != null) {
                faceTowards(partner.x, partner.z, 30);
                if (best > 1.5) forward = 1;
                if (best < 3 && ++breedTimer >= 60) breedWith(partner);
                return new float[]{forward, type.walkSpeed * 1.2f};
            }
        }
        if (panicTicks > 0) {
            panicTicks--;
            speed = type.chaseSpeed;
            if (goalTimer <= 0 || !moving) pickWanderTarget(8);
            moving = true;
        } else if (--goalTimer <= 0) {
            if (random.nextInt(3) == 0 && !sitting) pickWanderTarget(10);
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
        if (random.nextInt(600) == 0) world.playSound(soundName() + "_say", x, y + 1, z, 0.8f, pitch());
        return new float[]{forward, speed};
    }

    private void breedWith(Mob partner) {
        breedTimer = 0;
        loveTicks = partner.loveTicks = 0;
        setGrowingAge(6000);
        partner.setGrowingAge(6000);
        Mob baby = new Mob(type);
        baby.setGrowingAge(-24000);
        baby.setPos((x + partner.x) / 2, y, (z + partner.z) / 2);
        baby.yaw = baby.bodyYaw = baby.headYaw = random.nextFloat() * 360;
        if (type == MobType.SHEEP) baby.sheepColor = random.nextBoolean() ? sheepColor : partner.sheepColor;
        if (type == MobType.WOLF) { baby.tamed = true; baby.maxHealth = baby.health = 20; }
        java.util.Arrays.fill(baby.armor, null);
        world.addEntity(baby);
        for (int i = 0; i < 7; i++) world.addParticle("heart", x + random.nextGaussian() * 0.5, y + height, z + random.nextGaussian() * 0.5);
        XpOrbEntity.spawn(world, x, y + 0.5, z, 1 + random.nextInt(7));
    }

    // ------------------------------------------------------------------ squid

    private void tickSquid() {
        tentacleAngle = (float) Math.sin(age * 0.2) * 0.3f + 0.35f;
        if (inWater) {
            dryTicks = 0;
            if (random.nextInt(50) == 0 || (swimX == 0 && swimY == 0 && swimZ == 0)) {
                double a = random.nextDouble() * Math.PI * 2;
                swimX = Math.cos(a) * 0.2;
                swimY = (random.nextDouble() - 0.5) * 0.1;
                swimZ = Math.sin(a) * 0.2;
                if (panicTicks > 0) { swimX *= 2; swimZ *= 2; }
            }
            if (panicTicks > 0) panicTicks--;
            motionX += (swimX - motionX) * 0.1;
            motionY += (swimY - motionY) * 0.1;
            motionZ += (swimZ - motionZ) * 0.1;
            // Keep under the surface
            if (world.getBlock((int) Math.floor(x), (int) Math.floor(y + height + 0.2), (int) Math.floor(z)) != Block.WATER.id && motionY > 0) motionY = -0.02;
            move(motionX, motionY, motionZ);
            double h = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (h > 0.01) yaw += wrap((float) Math.toDegrees(Math.atan2(-motionX, motionZ)) - yaw) * 0.1f;
            squidPitch += ((float) -Math.toDegrees(Math.atan2(h, motionY)) + 90 - squidPitch) * 0.1f;
        } else {
            tentacleAngle = Math.abs((float) Math.sin(age * 0.5)) * 0.8f;
            // Suffocates out of water (the base class refills air, so count separately)
            if (++dryTicks > 300 && dryTicks % 20 == 0) damage(DamageSource.DROWN, 2, null);
            motionY -= 0.08;
            move(motionX, motionY, motionZ);
            motionX *= 0.8;
            motionZ *= 0.8;
            motionY *= 0.98;
            squidPitch += (90 - squidPitch) * 0.1f;
        }
        bodyYaw = headYaw = yaw;
    }

    // ------------------------------------------------------------------ endermen

    private static final Block[] CARRIABLE = {Block.GRASS, Block.DIRT, Block.SAND, Block.GRAVEL, Block.DANDELION, Block.POPPY,
            Block.CACTUS, Block.CLAY, Block.PUMPKIN, Block.MELON, Block.TNT};

    private void tickEnderman() {
        Player p = world.player();
        // Looking at an enderman's head angers it
        if (p != null && !p.creative && !p.isDead() && angerTicks == 0) {
            double dx = x - p.x, dy = y + height * 0.9 - p.eyeY(), dz = z - p.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 64 && d > 0.1) {
                double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
                double lx = -Math.sin(ry) * Math.cos(rp), ly = -Math.sin(rp), lz = Math.cos(ry) * Math.cos(rp);
                double dot = (lx * dx + ly * dy + lz * dz) / d;
                if (dot > 1 - 0.025 / d && canSee(p)) {
                    angerTicks = 600;
                    world.playSound("enderman_stare", x, y + 2, z, 1, 1);
                }
            }
        }
        // Water and rain hurt endermen
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        boolean wet = inWater || (world.raining && world.getSkyLight(bx, (int) Math.floor(y + 1), bz) >= 15);
        if (wet && random.nextInt(10) == 0) {
            damage(DamageSource.DROWN, 1, null);
            teleportRandomly();
        }
        if (angerTicks > 0 && random.nextInt(4) == 0) world.addParticle("portal", x + (random.nextDouble() - 0.5) * width, y + random.nextDouble() * height, z + (random.nextDouble() - 0.5) * width);
        // Pick up and put down blocks
        if (carriedBlock == 0 && random.nextInt(400) == 0) {
            int tx = bx + random.nextInt(5) - 2, ty = (int) Math.floor(y) + random.nextInt(3) - 1, tz = bz + random.nextInt(5) - 2;
            Block b = Block.get(world.getBlock(tx, ty, tz));
            for (Block c : CARRIABLE) if (c == b) {
                carriedBlock = b.id;
                world.setBlock(tx, ty, tz, 0);
                break;
            }
        } else if (carriedBlock != 0 && random.nextInt(2000) == 0) {
            int tx = bx + random.nextInt(3) - 1, ty = (int) Math.floor(y) + random.nextInt(2), tz = bz + random.nextInt(3) - 1;
            if (world.getBlock(tx, ty, tz) == 0 && Block.get(world.getBlock(tx, ty - 1, tz)).opaque) {
                world.setBlock(tx, ty, tz, carriedBlock);
                carriedBlock = 0;
            }
        }
    }

    private boolean teleportTo(double tx, double ty, double tz) {
        int bx = (int) Math.floor(tx), bz = (int) Math.floor(tz);
        int by = (int) Math.floor(ty);
        if (!world.isLoaded(bx, bz)) return false;
        // Drop to the ground
        while (by > 1 && !Block.get(world.getBlock(bx, by - 1, bz)).solid) by--;
        if (by <= 1) return false;
        double ox = x, oy = y, oz = z;
        setPos(bx + 0.5, by, bz + 0.5);
        if (!fits(box()) || touching(Block.WATER.id)) {
            setPos(ox, oy, oz);
            return false;
        }
        for (int i = 0; i < 24; i++) {
            double f = i / 23.0;
            world.addParticle("portal", ox + (x - ox) * f + random.nextGaussian() * 0.3, oy + (y - oy) * f + random.nextDouble() * 2.5, oz + (z - oz) * f + random.nextGaussian() * 0.3);
        }
        world.playSound("teleport", x, y, z, 1, 1);
        teleportCooldown = 40;
        return true;
    }

    private boolean teleportRandomly() {
        return teleportTo(x + (random.nextDouble() - 0.5) * 64, y + random.nextInt(64) - 32, z + (random.nextDouble() - 0.5) * 64);
    }

    private void teleportTowards(Entity e) {
        double dx = x - e.x, dz = z - e.z;
        double d = Math.sqrt(dx * dx + dz * dz);
        for (int i = 0; i < 16; i++) {
            double tx = x + (random.nextDouble() - 0.5) * 8 - dx / d * 16, tz = z + (random.nextDouble() - 0.5) * 8 - dz / d * 16;
            if (teleportTo(tx, y + random.nextInt(16) - 8, tz)) return;
        }
    }

    /** Tamed wolves catch up with their owner. */
    private void teleportNear(Entity owner) {
        for (int i = 0; i < 10; i++) {
            int tx = (int) Math.floor(owner.x) + random.nextInt(5) - 2, tz = (int) Math.floor(owner.z) + random.nextInt(5) - 2;
            int ty = (int) Math.floor(owner.y);
            if (Block.get(world.getBlock(tx, ty - 1, tz)).solid && world.getBlock(tx, ty, tz) == 0 && world.getBlock(tx, ty + 1, tz) == 0) {
                setPos(tx + 0.5, ty, tz + 0.5);
                motionX = motionY = motionZ = 0;
                return;
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void meleeIfClose(LivingEntity target, double dist, float damage) {
        if (dist < 1.6 + width / 2 && attackCooldown == 0 && Math.abs(target.y - y) < 1.5 + height / 2) {
            target.damage(DamageSource.ATTACK, damage, this);
            attackCooldown = 20;
        }
    }

    private void shootAt(LivingEntity target) {
        ArrowEntity arrow = new ArrowEntity(this);
        arrow.setPos(x, eyeY() - 0.1, z);
        double dx = target.x - x, dz = target.z - z;
        double dy = target.eyeY() - 0.3 - arrow.y;
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
