package mc.entity;

import mc.item.Item;
import mc.world.Block;
import mc.world.Rails;

/**
 * A rideable minecart following Minecraft's rail physics: speed is kept along the track's direction, slopes
 * accelerate, powered rails boost (or brake when unpowered), and riders can nudge a stopped cart forward.
 */
public final class MinecartEntity extends Vehicle {
    public static final double MAX_SPEED = 0.4;

    public MinecartEntity() {
        width = 0.98f;
        height = 0.7f;
        stepHeight = 0;
    }

    @Override
    public Item item() { return Item.MINECART; }

    @Override
    public void tick() {
        super.tick();
        motionY -= 0.04;
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        if (Rails.isRail(world.getBlock(bx, by - 1, bz))) by--;
        int id = world.getBlock(bx, by, bz);
        if (Rails.isRail(id)) {
            moveAlongTrack(bx, by, bz, id);
            if (id == Block.DETECTOR_RAIL.id) Rails.cartOnDetector(world, bx, by, bz);
        } else moveOffRail();
        orient();
        for (Entity o : touchingEntities(0.2)) {
            if (o instanceof Mob m && passenger == null && motionX * motionX + motionZ * motionZ > 0.01 && canCarry(m)) {
                mount(m);
                continue;
            }
            pushApart(o, 0.05);
        }
        positionRider();
    }

    /** Mobs that walk into a moving empty cart get in (small enough ones, like Minecraft). */
    private static boolean canCarry(Mob m) {
        return m.width < 1.0f && m.type != MobType.GHAST;
    }

    /** Faces along the direction of travel (either end, the cart is symmetric) and tilts on slopes. */
    private void orient() {
        double dx = x - prevX, dz = z - prevZ, dy = y - prevY;
        double h = dx * dx + dz * dz;
        if (h > 1e-5) {
            float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float diff = wrapDegrees(target - yaw);
            if (diff > 90) target -= 180;
            else if (diff < -90) target += 180;
            yaw = yaw + wrapDegrees(target - yaw);
            double r = Math.toRadians(yaw);
            double along = -Math.sin(r) * dx + Math.cos(r) * dz;
            pitch = Math.abs(along) > 1e-4 ? (float) -Math.toDegrees(Math.atan2(dy, along)) : pitch;
            if (Math.abs(pitch) < 1) pitch = 0;
        }
    }

    private void moveOffRail() {
        motionX = clamp(motionX, MAX_SPEED);
        motionZ = clamp(motionZ, MAX_SPEED);
        if (onGround) {
            motionX *= 0.5;
            motionY *= 0.5;
            motionZ *= 0.5;
        }
        move(motionX, motionY, motionZ);
        if (!onGround) {
            motionX *= 0.95;
            motionY *= 0.95;
            motionZ *= 0.95;
        }
        pitch *= 0.8f;
    }

    private static double clamp(double v, double max) { return Math.max(-max, Math.min(max, v)); }

    private void moveAlongTrack(int bx, int by, int bz, int id) {
        fallDistance = 0;
        double[] before = posOnTrack(x, y, z);
        y = by;
        int meta = world.getMeta(bx, by, bz);
        int shape = Rails.shape(id, meta);
        boolean powered = false, braking = false;
        if (id == Block.POWERED_RAIL.id) {
            powered = (meta & Rails.ACTIVE) != 0;
            braking = !powered;
        }
        double slope = 0.0078125;
        switch (shape) {
            case 2 -> { motionX -= slope; y++; }
            case 3 -> { motionX += slope; y++; }
            case 4 -> { motionZ += slope; y++; }
            case 5 -> { motionZ -= slope; y++; }
            default -> { }
        }
        int[][] e = Rails.EXITS[shape];
        double ex = e[1][0] - e[0][0], ez = e[1][2] - e[0][2];
        double len = Math.sqrt(ex * ex + ez * ez);
        // Line up with the track (either way round)
        float along = (float) Math.toDegrees(Math.atan2(-ex, ez));
        float off = wrapDegrees(along - yaw);
        if (off > 90) off -= 180;
        else if (off < -90) off += 180;
        yaw += off * 0.5f;
        if (motionX * ex + motionZ * ez < 0) { ex = -ex; ez = -ez; }
        double speed = Math.min(2, Math.sqrt(motionX * motionX + motionZ * motionZ));
        motionX = speed * ex / len;
        motionZ = speed * ez / len;
        // A rider pressing forward gives a stopped cart a push in the direction they look
        if (passenger instanceof LivingEntity && riderForward > 0 && motionX * motionX + motionZ * motionZ < 0.01) {
            double r = Math.toRadians(passenger.yaw);
            motionX += -Math.sin(r) * 0.1;
            motionZ += Math.cos(r) * 0.1;
            braking = false;
        }
        if (braking) {
            double s = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (s < 0.03) motionX = motionY = motionZ = 0;
            else {
                motionX *= 0.5;
                motionY = 0;
                motionZ *= 0.5;
            }
        }
        // Snap onto the line through the rail's two exits
        double x0 = bx + 0.5 + e[0][0] * 0.5, z0 = bz + 0.5 + e[0][2] * 0.5;
        double x1 = bx + 0.5 + e[1][0] * 0.5, z1 = bz + 0.5 + e[1][2] * 0.5;
        double dx = x1 - x0, dz = z1 - z0, t;
        if (dx == 0) { x = bx + 0.5; t = z - bz; }
        else if (dz == 0) { z = bz + 0.5; t = x - bx; }
        else t = ((x - x0) * dx + (z - z0) * dz) * 2;
        x = x0 + dx * t;
        z = z0 + dz * t;

        double mx = motionX, mz = motionZ;
        if (passenger != null) { mx *= 0.75; mz *= 0.75; }
        move(clamp(mx, MAX_SPEED), 0, clamp(mz, MAX_SPEED));
        if (e[0][1] != 0 && (int) Math.floor(x) - bx == e[0][0] && (int) Math.floor(z) - bz == e[0][2]) y += e[0][1];
        else if (e[1][1] != 0 && (int) Math.floor(x) - bx == e[1][0] && (int) Math.floor(z) - bz == e[1][2]) y += e[1][1];
        // Rolling resistance (less with a rider, as in Minecraft)
        double drag = passenger != null ? 0.997 : 0.96;
        motionX *= drag;
        motionY = 0;
        motionZ *= drag;
        double[] after = posOnTrack(x, y, z);
        if (after != null && before != null) {
            // Going downhill speeds up, uphill slows down
            double boost = (before[1] - after[1]) * 0.05;
            double s = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (s > 0) {
                motionX = motionX / s * (s + boost);
                motionZ = motionZ / s * (s + boost);
            }
            y = after[1];
        }
        int nbx = (int) Math.floor(x), nbz = (int) Math.floor(z);
        if (nbx != bx || nbz != bz) {
            double s = Math.sqrt(motionX * motionX + motionZ * motionZ);
            motionX = s * (nbx - bx);
            motionZ = s * (nbz - bz);
        }
        if (powered) {
            double s = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (s > 0.01) {
                motionX += motionX / s * 0.06;
                motionZ += motionZ / s * 0.06;
            } else if (shape == 1) {
                // A stopped cart is launched away from a block at one end
                if (world.sturdy(bx - 1, by, bz)) motionX = 0.02;
                else if (world.sturdy(bx + 1, by, bz)) motionX = -0.02;
            } else if (shape == 0) {
                if (world.sturdy(bx, by, bz - 1)) motionZ = 0.02;
                else if (world.sturdy(bx, by, bz + 1)) motionZ = -0.02;
            }
        }
    }

    /** The point on the track nearest to (x, z), with the track's height there; null if not on a rail. */
    double[] posOnTrack(double px, double py, double pz) {
        int i = (int) Math.floor(px), j = (int) Math.floor(py), k = (int) Math.floor(pz);
        if (Rails.isRail(world.getBlock(i, j - 1, k))) j--;
        int id = world.getBlock(i, j, k);
        if (!Rails.isRail(id)) return null;
        int[][] e = Rails.EXITS[Rails.shape(id, world.getMeta(i, j, k))];
        double x0 = i + 0.5 + e[0][0] * 0.5, y0 = j + 0.0625 + e[0][1] * 0.5, z0 = k + 0.5 + e[0][2] * 0.5;
        double x1 = i + 0.5 + e[1][0] * 0.5, y1 = j + 0.0625 + e[1][1] * 0.5, z1 = k + 0.5 + e[1][2] * 0.5;
        double dx = x1 - x0, dy = (y1 - y0) * 2, dz = z1 - z0, t;
        if (dx == 0) { px = i + 0.5; t = pz - k; }
        else if (dz == 0) { pz = k + 0.5; t = px - i; }
        else t = ((px - x0) * dx + (pz - z0) * dz) * 2;
        px = x0 + dx * t;
        py = y0 + dy * t;
        pz = z0 + dz * t;
        if (dy < 0) py += 1;
        if (dy > 0) py += 0.5;
        return new double[]{px, py, pz};
    }
}
