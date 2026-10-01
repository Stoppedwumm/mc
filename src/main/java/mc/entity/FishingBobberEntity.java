package mc.entity;

import mc.item.Enchantment;
import mc.item.Item;
import mc.item.ItemStack;
import mc.world.Block;

import java.util.Map;

/**
 * A cast fishing line's bobber (Minecraft 1.8 fishing): it floats on water, a fish swims up after a while (a wake of
 * bubbles heads for the bobber), bites with a splash, and reeling in during the bite catches something. Entities
 * it touches get hooked and are pulled towards the angler.
 */
public final class FishingBobberEntity extends Entity {
    /** The angler (null on a client copy, which only knows the name). */
    public Player owner;
    public String ownerName = "";
    public Entity hooked;
    private boolean inGround;
    /** Ticks until a fish starts approaching, ticks of the approach, and ticks the bite lasts. */
    private int waitTicks, approachTicks, biteTicks;
    private float approachAngle;

    public FishingBobberEntity(Player owner) {
        this.owner = owner;
        if (owner != null) ownerName = owner.name;
        width = height = 0.25f;
        stepHeight = 0;
    }

    /** Throws the bobber from the owner's eyes in the direction they look. */
    public void cast() {
        double ry = Math.toRadians(owner.yaw), rp = Math.toRadians(owner.pitch);
        setPos(owner.x - Math.cos(ry) * 0.16, owner.eyeY() - 0.1, owner.z - Math.sin(ry) * 0.16);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        // Minecraft normalises the throw to 1.5 blocks per tick with a little spread
        double speed = 1.5;
        motionX = dx * speed + random.nextGaussian() * 0.0075 * speed;
        motionY = dy * speed + random.nextGaussian() * 0.0075 * speed;
        motionZ = dz * speed + random.nextGaussian() * 0.0075 * speed;
        yaw = owner.yaw;
    }

    /** Whether a fish is on the line right now. */
    public boolean biting() { return biteTicks > 0; }

    private boolean ownerHoldsRod() {
        ItemStack h = owner.inventory.held();
        return !ItemStack.isEmpty(h) && h.item == Item.FISHING_ROD;
    }

    @Override
    public void tick() {
        super.tick();
        if (owner == null || owner.removed || owner.isDead() || !ownerHoldsRod() || distanceSq(owner.x, owner.y, owner.z) > 32 * 32
                || owner.world != world) {
            remove();
            return;
        }
        if (hooked != null) {
            if (hooked.removed || (hooked instanceof LivingEntity le && le.isDead())) hooked = null;
            else {
                x = prevX = hooked.x;
                y = hooked.y + hooked.height * 0.8;
                z = hooked.z;
                return;
            }
        }
        if (inGround) {
            if (world.getBlock((int) Math.floor(x), (int) Math.floor(y - 0.05), (int) Math.floor(z)) == 0) inGround = false;
            else return;
        }
        // Hook the first living thing we fly into
        if (motionX * motionX + motionY * motionY + motionZ * motionZ > 0.01) {
            var area = box();
            for (Entity e : world.entities()) {
                if (e == this || e.removed || !(e instanceof LivingEntity le) || le.isDead() || (age < 5 && e == owner)) continue;
                if (e.box().intersects(area)) { hooked = e; return; }
            }
        }
        double water = submerged();
        if (water > 0) tickFishing(water);
        if (water > 0) {
            motionY += 0.04 * (water * 2 - 1) * 0.5;
            motionX *= 0.9;
            motionY *= 0.8;
            motionZ *= 0.9;
        } else motionY -= 0.03;
        move(motionX, motionY, motionZ);
        if (water == 0) {
            motionX *= 0.92;
            motionY *= 0.92;
            motionZ *= 0.92;
        }
        if (onGround || horizontalCollision) {
            // Landed on something solid: it stays there
            if (onGround && water == 0) {
                inGround = true;
                motionX = motionY = motionZ = 0;
            }
        }
        double h = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (h > 1e-3) yaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
    }

    /** Fraction of the bobber below the water surface. */
    private double submerged() {
        double sum = 0;
        for (int i = 0; i < 5; i++) {
            double sample = y + height * (i + 0.5) / 5.0;
            int bx = (int) Math.floor(x), by = (int) Math.floor(sample), bz = (int) Math.floor(z);
            if (world.getBlock(bx, by, bz) != Block.WATER.id) continue;
            int meta = world.getMeta(bx, by, bz);
            double surface = world.getBlock(bx, by + 1, bz) == Block.WATER.id ? by + 1 : by + ((meta & 8) != 0 ? 0.9 : (8 - (meta & 7)) / 9.0);
            if (sample < surface) sum += 0.2;
        }
        return sum;
    }

    private void tickFishing(double water) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y) + 1, bz = (int) Math.floor(z);
        boolean openSky = world.getSkyLight(bx, by, bz) >= 14;
        if (biteTicks > 0) {
            biteTicks--;
            if (biteTicks == 0) waitTicks = 0;
            return;
        }
        if (approachTicks > 0) {
            approachTicks--;
            // The fish's wake: bubbles and splashes moving towards the bobber
            approachAngle += (float) (random.nextGaussian() * 4);
            double dist = approachTicks * 0.1, a = Math.toRadians(approachAngle);
            double wx = x + Math.sin(a) * dist, wz = z + Math.cos(a) * dist;
            int wy = (int) Math.floor(y);
            if (world.getBlock((int) Math.floor(wx), wy, (int) Math.floor(wz)) == Block.WATER.id) {
                world.addParticle("bubble", wx, wy + 1.0, wz);
                if (random.nextInt(2) == 0) world.addParticle("splash", wx, wy + 1.0, wz);
            }
            if (approachTicks == 0) {
                // Bite: the bobber dips under with a splash
                motionY -= 0.2;
                world.playSound("splash", x, y, z, 0.25f, 1 + (random.nextFloat() - random.nextFloat()) * 0.4f);
                for (int i = 0; i < 12; i++) world.addParticle("splash", x + (random.nextDouble() - 0.5) * 0.5, y + 0.3, z + (random.nextDouble() - 0.5) * 0.5);
                biteTicks = 20 + random.nextInt(21);
            }
            return;
        }
        if (waitTicks == 0) {
            // Lure shortens the wait; rain and covered water lengthen or shorten it like Minecraft
            int lure = owner != null ? ItemStack.level(owner.inventory.held(), Enchantment.LURE) : 0;
            waitTicks = Math.max(20, 100 + random.nextInt(500) - lure * 100);
            if (!openSky) waitTicks *= 2;
            else if (world.raining) waitTicks = Math.max(20, waitTicks * 4 / 5);
            return;
        }
        if (--waitTicks == 0) {
            approachTicks = 20 + random.nextInt(61);
            approachAngle = random.nextFloat() * 360;
        }
    }

    /**
     * Reels in: pulls a hooked entity, lands a catch (which flies to the angler), or just retrieves the line.
     * Returns the rod damage (1 normally, 2 when pulled out of the ground, 3 for a hooked entity, 0 if nothing).
     */
    public int reel() {
        int damage = 0;
        if (hooked != null) {
            double dx = owner.x - x, dy = owner.y - y, dz = owner.z - z;
            hooked.motionX += dx * 0.1;
            hooked.motionY += dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08;
            hooked.motionZ += dz * 0.1;
            damage = 3;
        } else if (biteTicks > 0) {
            ItemStack loot = catchLoot();
            ItemEntity item = new ItemEntity(loot);
            item.setPos(x, y, z);
            double dx = owner.x - x, dy = owner.y - y, dz = owner.z - z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            item.motionX = dx * 0.1;
            item.motionY = dy * 0.1 + Math.sqrt(d) * 0.08;
            item.motionZ = dz * 0.1;
            item.pickupDelay = 0;
            world.addEntity(item);
            XpOrbEntity orb = new XpOrbEntity(1 + random.nextInt(6));
            orb.setPos(owner.x, owner.y + 0.5, owner.z + 0.5);
            world.addEntity(orb);
            damage = 1;
        } else if (inGround) damage = 2;
        remove();
        return damage;
    }

    /** Minecraft 1.8's catch table: mostly fish, some junk, a little treasure (Luck of the Sea shifts the odds). */
    ItemStack catchLoot() {
        int luck = owner != null ? ItemStack.level(owner.inventory.held(), Enchantment.LUCK_OF_THE_SEA) : 0;
        float junk = 0.1f - luck * 0.025f, treasure = 0.05f + luck * 0.01f;
        float roll = random.nextFloat();
        if (roll < junk) {
            Item[] junkItems = {Item.LEATHER, Item.BONE, Item.STRING, Item.BOWL, Item.STICK, Item.INK_SAC, Item.ROTTEN_FLESH, Item.GLASS_BOTTLE};
            return new ItemStack(junkItems[random.nextInt(junkItems.length)], 1);
        }
        if (roll < junk + treasure) {
            Item[] treasureItems = {Item.BOW, Item.FISHING_ROD, Item.ENCHANTED_BOOK};
            Item t = treasureItems[random.nextInt(treasureItems.length)];
            ItemStack s = new ItemStack(t, 1);
            Map<Enchantment, Integer> ench = Enchantment.roll(t == Item.ENCHANTED_BOOK ? Item.BOOK : t, 30, random);
            for (Map.Entry<Enchantment, Integer> en : ench.entrySet()) s.enchant(en.getKey(), en.getValue());
            return s;
        }
        float f = random.nextFloat() * 100;
        Item fish = f < 60 ? Item.RAW_FISH : f < 85 ? Item.RAW_SALMON : f < 87 ? Item.CLOWNFISH : Item.PUFFERFISH;
        return new ItemStack(fish, 1);
    }

    @Override
    public void netTick() {
        super.netTick();
    }
}
