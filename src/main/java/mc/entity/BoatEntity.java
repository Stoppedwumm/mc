package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.world.Block;

/**
 * A rowing boat with Minecraft 1.8's handling: it floats at the surface, the rider pushes it towards where they
 * look, top speed builds up gradually, and ramming something at speed smashes it into planks and sticks.
 */
public final class BoatEntity extends Vehicle {
    private double speedMultiplier = 0.07;
    /** Paddle animation for the renderer. */
    public float paddle, prevPaddle;

    public BoatEntity() {
        width = 1.5f;
        height = 0.6f;
        stepHeight = 0;
    }

    @Override
    public Item item() { return Item.BOAT; }

    @Override
    public double riderOffset() { return -0.3; }

    /** Fraction (0-1) of the hull below the water surface, in five slices. */
    private double submerged() {
        var b = box();
        double frac = 0;
        for (int i = 0; i < 5; i++) {
            double y0 = b.minY + (b.maxY - b.minY) * i / 5 - 0.125, y1 = b.minY + (b.maxY - b.minY) * (i + 1) / 5 - 0.125;
            if (waterIn(b.minX, y0, b.minZ, b.maxX, y1, b.maxZ)) frac += 0.2;
        }
        return frac;
    }

    private boolean waterIn(double x0, double y0, double z0, double x1, double y1, double z1) {
        for (int bx = (int) Math.floor(x0); bx <= (int) Math.floor(x1); bx++)
            for (int by = (int) Math.floor(y0); by <= (int) Math.floor(y1); by++)
                for (int bz = (int) Math.floor(z0); bz <= (int) Math.floor(z1); bz++) {
                    if (world.getBlock(bx, by, bz) != Block.WATER.id) continue;
                    int meta = world.getMeta(bx, by, bz);
                    double surface = world.getBlock(bx, by + 1, bz) == Block.WATER.id ? by + 1
                            : by + ((meta & 8) != 0 ? 0.9 : (8 - (meta & 7)) / 9.0);
                    if (y1 >= by && y0 < surface) return true;
                }
        return false;
    }

    /** Whether the boat has room where it stands (used when placing it). */
    public boolean fitsAnywhere() { return world == null || fits(box()); }

    @Override
    public void netTick() {
        prevPaddle = paddle;
        super.netTick();
        double dx = x - prevX, dz = z - prevZ;
        if (passenger != null && dx * dx + dz * dz > 1e-4) paddle += 0.4f;
    }

    @Override
    public void tick() {
        super.tick();
        prevPaddle = paddle;
        double frac = submerged();
        double before = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (before > 0.2625 && world.random().nextInt(3) == 0) {
            double r = Math.toRadians(yaw);
            for (int i = 0; i < 2; i++)
                world.addParticle("splash", x - Math.sin(r) * 0.8 + (random.nextDouble() - 0.5), y + 0.4, z + Math.cos(r) * 0.8 + (random.nextDouble() - 0.5));
        }
        // Float: sink when out of the water, bob up when under it
        if (frac < 1) motionY += 0.04 * (frac * 2 - 1);
        else {
            if (motionY < 0) motionY /= 2;
            motionY += 0.007;
        }
        if (passenger instanceof LivingEntity) {
            double r = Math.toRadians(passenger.yaw - riderStrafe * 90);
            motionX += -Math.sin(r) * speedMultiplier * riderForward * 0.05;
            motionZ += Math.cos(r) * speedMultiplier * riderForward * 0.05;
            if (riderForward != 0) paddle += 0.4f;
        }
        double speed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed > 0.35) {
            motionX *= 0.35 / speed;
            motionZ *= 0.35 / speed;
            speed = 0.35;
        }
        if (speed > before && speedMultiplier < 0.35) speedMultiplier = Math.min(0.35, speedMultiplier + (0.35 - speedMultiplier) / 35);
        else speedMultiplier = Math.max(0.07, speedMultiplier - (speedMultiplier - 0.07) / 35);
        // Lily pads and snow layers in the way are broken
        for (int i = 0; i < 4; i++) {
            int bx = (int) Math.floor(x + ((i % 2) - 0.5) * 0.8), bz = (int) Math.floor(z + ((i / 2) - 0.5) * 0.8);
            for (int j = 0; j < 2; j++) {
                int by = (int) Math.floor(y) + j;
                if (Block.get(world.getBlock(bx, by, bz)).shape == Block.Shape.SNOW_LAYER) world.breakBlock(bx, by, bz, null, true);
            }
        }
        if (onGround) {
            motionX *= 0.5;
            motionY *= 0.5;
            motionZ *= 0.5;
        }
        move(motionX, motionY, motionZ);
        if (horizontalCollision && before > 0.2) {
            // Crashing at speed breaks the boat
            if (passenger != null) dismount();
            for (int i = 0; i < 3; i++) world.spawnItem(x, y + 0.3, z, new ItemStack(Item.of(Block.PLANKS), 1));
            for (int i = 0; i < 2; i++) world.spawnItem(x, y + 0.3, z, new ItemStack(Item.STICK, 1));
            world.playSound("hit", x, y, z, 1, 0.6f);
            remove();
            return;
        }
        motionX *= 0.99;
        motionY *= 0.95;
        motionZ *= 0.99;
        pitch = 0;
        double dx = x - prevX, dz = z - prevZ;
        if (dx * dx + dz * dz > 0.001) {
            float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float d = wrapDegrees(target - yaw);
            yaw += Math.max(-20, Math.min(20, d));
        }
        for (Entity o : touchingEntities(0.2)) pushApart(o, 0.05);
        positionRider();
    }
}
