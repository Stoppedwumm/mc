package mc.entity;

import mc.util.AABB;
import mc.world.Block;
import mc.world.World;

import java.util.ArrayList;
import java.util.List;

/** The local player, simulated at 20 ticks per second with Minecraft's movement constants. */
public final class Player {
    public static final double WIDTH = 0.6, HEIGHT = 1.8, EYE = 1.62, SNEAK_EYE = 1.27;

    public double x, y, z, prevX, prevY, prevZ;
    public double motionX, motionY, motionZ;
    public float yaw, pitch;
    public boolean onGround, flying, sneaking, sprinting, inWater, inLava, horizontalCollision;
    public boolean creative = false;
    public float walkDist, prevWalkDist, bob, prevBob, tilt, prevTilt;
    public float eyeHeight = (float) EYE, prevEyeHeight = (float) EYE;
    public double fallStart;
    /** Set when a footstep should play this tick. */
    public boolean stepThisTick;
    public boolean landedThisTick, splashThisTick;
    private float nextStep = 1;
    private int jumpCooldown;
    public float health = 20;

    public AABB box() {
        return new AABB(x - WIDTH / 2, y, z - WIDTH / 2, x + WIDTH / 2, y + HEIGHT, z + WIDTH / 2);
    }

    public void setPos(double x, double y, double z) {
        this.x = prevX = x;
        this.y = prevY = y;
        this.z = prevZ = z;
    }

    public void tick(World world, float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
        prevX = x; prevY = y; prevZ = z;
        prevWalkDist = walkDist;
        prevBob = bob;
        prevTilt = tilt;
        prevEyeHeight = eyeHeight;
        stepThisTick = landedThisTick = splashThisTick = false;
        if (jumpCooldown > 0) jumpCooldown--;

        boolean wasInWater = inWater;
        inWater = touching(world, Block.WATER.id);
        inLava = touching(world, Block.LAVA.id);
        if (inWater && !wasInWater && motionY < -0.2) splashThisTick = true;

        sneaking = sneak && !flying;
        if (forward <= 0 || sneaking || horizontalCollision) sprinting = false;
        else if (sprint) sprinting = true;
        if (sneaking) { forward *= 0.3f; strafe *= 0.3f; }
        forward *= 0.98f;
        strafe *= 0.98f;

        float targetEye = (float) (sneaking ? SNEAK_EYE + 0.27 : EYE);
        eyeHeight += (targetEye - eyeHeight) * 0.5f;

        double startY = y;
        if (flying) {
            if (jump) motionY += 0.15;
            if (sneak) motionY -= 0.15;
            moveRelative(strafe, forward, sprinting ? 0.1f : 0.05f);
            move(world, motionX, motionY, motionZ);
            motionX *= 0.91; motionZ *= 0.91; motionY *= 0.6;
            if (onGround && !creative) flying = false;
            if (onGround && creative && sneak) flying = false;
        } else if (inWater || inLava) {
            moveRelative(strafe, forward, 0.02f);
            move(world, motionX, motionY, motionZ);
            double drag = inWater ? 0.8 : 0.5;
            motionX *= drag; motionY *= drag; motionZ *= drag;
            motionY -= 0.02;
            if (jump) motionY += 0.04;
            // Climb out onto a ledge
            if (horizontalCollision && fits(world, box().offset(motionX, motionY + 0.6 - y + startY, motionZ))) motionY = 0.3;
        } else {
            if (jump && onGround && jumpCooldown == 0) {
                motionY = 0.42;
                if (sprinting) {
                    double r = Math.toRadians(yaw);
                    motionX -= Math.sin(r) * 0.2;
                    motionZ += Math.cos(r) * 0.2;
                }
                jumpCooldown = 10;
            }
            float slip = onGround ? slipperiness(world) * 0.91f : 0.91f;
            float accel = onGround ? 0.1f * 0.16277136f / (slip * slip * slip) : 0.02f;
            if (sprinting) accel *= 1.3f;
            moveRelative(strafe, forward, accel);
            move(world, motionX, motionY, motionZ);
            motionY -= 0.08;
            motionY *= 0.98;
            motionX *= slip;
            motionZ *= slip;
        }

        double dx = x - prevX, dz = z - prevZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        walkDist += dist * 0.6f;
        float targetBob = onGround && !flying ? Math.min(0.1f, dist) : 0;
        bob += (targetBob - bob) * 0.4f;
        float targetTilt = onGround && !flying ? (float) Math.atan(-motionY * 0.2) * 15 : 0;
        tilt += (targetTilt - tilt) * 0.8f;
        if (onGround && !flying && walkDist > nextStep && !inWater) {
            nextStep = walkDist + 1;
            stepThisTick = !sneaking;
        }
    }

    private float slipperiness(World world) {
        int id = world.getBlock((int) Math.floor(x), (int) Math.floor(y - 0.5), (int) Math.floor(z));
        return id == Block.ICE.id ? 0.98f : 0.6f;
    }

    private void moveRelative(float strafe, float forward, float accel) {
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

    private boolean touching(World world, int id) {
        AABB b = box();
        for (int bx = (int) Math.floor(b.minX + 0.001); bx <= (int) Math.floor(b.maxX - 0.001); bx++)
            for (int by = (int) Math.floor(b.minY + 0.001); by <= (int) Math.floor(b.maxY - 0.4); by++)
                for (int bz = (int) Math.floor(b.minZ + 0.001); bz <= (int) Math.floor(b.maxZ - 0.001); bz++)
                    if (world.getBlock(bx, by, bz) == id) return true;
        return false;
    }

    public static List<AABB> collisionBoxes(World world, AABB area) {
        List<AABB> list = new ArrayList<>();
        int x0 = (int) Math.floor(area.minX), x1 = (int) Math.floor(area.maxX);
        int y0 = (int) Math.floor(area.minY) - 1, y1 = (int) Math.floor(area.maxY);
        int z0 = (int) Math.floor(area.minZ), z1 = (int) Math.floor(area.maxZ);
        for (int bx = x0; bx <= x1; bx++)
            for (int bz = z0; bz <= z1; bz++) {
                boolean loaded = world.isLoaded(bx, bz);
                for (int by = y0; by <= y1; by++) {
                    // Unloaded terrain is treated as solid so the player can't fall out of the world
                    if (!loaded || Block.get(world.getBlock(bx, by, bz)).solid)
                        list.add(new AABB(bx, by, bz, bx + 1, by + 1, bz + 1));
                }
            }
        return list;
    }

    private boolean fits(World world, AABB b) {
        for (AABB o : collisionBoxes(world, b)) if (o.intersects(b)) return false;
        return true;
    }

    private boolean groundBelow(World world, AABB b, double dx, double dz) {
        AABB t = b.offset(dx, -1.0, dz);
        for (AABB o : collisionBoxes(world, t)) if (o.intersects(t)) return true;
        return false;
    }

    public void move(World world, double dx, double dy, double dz) {
        AABB bb = box();
        // Sneaking stops you walking off edges
        if (sneaking && onGround) {
            double step = 0.05;
            while (dx != 0 && !groundBelow(world, bb, dx, 0)) dx = Math.abs(dx) < step ? 0 : dx - Math.signum(dx) * step;
            while (dz != 0 && !groundBelow(world, bb, 0, dz)) dz = Math.abs(dz) < step ? 0 : dz - Math.signum(dz) * step;
            while (dx != 0 && dz != 0 && !groundBelow(world, bb, dx, dz)) {
                dx = Math.abs(dx) < step ? 0 : dx - Math.signum(dx) * step;
                dz = Math.abs(dz) < step ? 0 : dz - Math.signum(dz) * step;
            }
        }
        double ox = dx, oy = dy, oz = dz;
        List<AABB> boxes = collisionBoxes(world, bb.expandTowards(dx, dy, dz));
        AABB moved = bb.copy();
        for (AABB o : boxes) dy = o.clipY(moved, dy);
        moved.move(0, dy, 0);
        for (AABB o : boxes) dx = o.clipX(moved, dx);
        moved.move(dx, 0, 0);
        for (AABB o : boxes) dz = o.clipZ(moved, dz);
        moved.move(0, 0, dz);

        boolean ground = oy != dy && oy < 0;
        // Step up onto blocks up to 0.6 high
        if ((ground || onGround) && (ox != dx || oz != dz)) {
            double sx = ox, sy = 0.6, sz = oz;
            List<AABB> sb = collisionBoxes(world, bb.expandTowards(ox, 0.6, oz));
            AABB s = bb.copy();
            for (AABB o : sb) sy = o.clipY(s, sy);
            s.move(0, sy, 0);
            for (AABB o : sb) sx = o.clipX(s, sx);
            s.move(sx, 0, 0);
            for (AABB o : sb) sz = o.clipZ(s, sz);
            s.move(0, 0, sz);
            double down = -sy;
            for (AABB o : sb) down = o.clipY(s, down);
            s.move(0, down, 0);
            if (sx * sx + sz * sz > dx * dx + dz * dz + 1e-7) {
                moved = s;
                dx = sx; dz = sz; dy = sy + down;
            }
        }

        x = (moved.minX + moved.maxX) / 2;
        y = moved.minY;
        z = (moved.minZ + moved.maxZ) / 2;

        horizontalCollision = ox != dx || oz != dz;
        boolean wasOnGround = onGround;
        onGround = oy != dy && oy < 0;
        if (onGround && !wasOnGround && fallStart - y > 1.2) landedThisTick = true;
        if (onGround || inWater || flying) fallStart = y;
        else if (y > fallStart) fallStart = y;
        if (ox != dx) motionX = 0;
        if (oz != dz) motionZ = 0;
        if (oy != dy) motionY = 0;
    }

    public double interpX(float pt) { return prevX + (x - prevX) * pt; }
    public double interpY(float pt) { return prevY + (y - prevY) * pt; }
    public double interpZ(float pt) { return prevZ + (z - prevZ) * pt; }
}
