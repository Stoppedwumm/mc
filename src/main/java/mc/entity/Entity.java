package mc.entity;

import mc.util.AABB;
import mc.world.Block;
import mc.world.Shapes;
import mc.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Base class for everything that moves: physics, collision and fluid state. */
public abstract class Entity {
    private static int nextId = 1;
    public final int id = nextId++;
    public World world;
    public double x, y, z, prevX, prevY, prevZ;
    public double motionX, motionY, motionZ;
    public float yaw, pitch, prevYaw, prevPitch;
    public float width = 0.6f, height = 1.8f;
    public float stepHeight = 0.6f;
    public boolean onGround, horizontalCollision, verticalCollision, removed, inWater, inLava;
    public int age;
    public float fallDistance;
    public int fireTicks;
    protected final Random random = new Random();

    // ------------------------------------------------------------------ network copies

    /** The server's id for this entity (on a client connected to a server). */
    public int netId;
    /** Latest state from the server; the entity glides there over a few ticks. */
    public double netX, netY, netZ;
    public float netYaw, netPitch, netHeadYaw;
    public int netSteps;
    private boolean netPlaced;

    /** Moves towards a position received from the server. */
    public void setNetTarget(double nx, double ny, double nz, float nyaw, float npitch, float nheadYaw) {
        netX = nx; netY = ny; netZ = nz;
        netYaw = nyaw; netPitch = npitch; netHeadYaw = nheadYaw;
        if (!netPlaced || distanceSq(nx, ny, nz) > 64) {
            setPos(nx, ny, nz);
            yaw = prevYaw = nyaw;
            pitch = prevPitch = npitch;
            netPlaced = true;
            netSteps = 0;
            netTargetReached();
            return;
        }
        netSteps = 3;
    }

    protected void netTargetReached() { }

    static float wrapDegrees(float d) {
        d %= 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    /** Client-side tick of a server-controlled entity: interpolation only, no physics or AI. */
    public void netTick() {
        prevX = x; prevY = y; prevZ = z;
        prevYaw = yaw; prevPitch = pitch;
        age++;
        if (netSteps > 0) {
            x += (netX - x) / netSteps;
            y += (netY - y) / netSteps;
            z += (netZ - z) / netSteps;
            yaw += wrapDegrees(netYaw - yaw) / netSteps;
            pitch += (netPitch - pitch) / netSteps;
            netSteps--;
        }
        if (world != null) {
            inWater = touching(Block.WATER.id);
            inLava = touching(Block.LAVA.id);
        }
        if (fireTicks > 0) fireTicks--;
    }

    public AABB box() {
        return new AABB(x - width / 2, y, z - width / 2, x + width / 2, y + height, z + width / 2);
    }

    public void setPos(double x, double y, double z) {
        this.x = prevX = x;
        this.y = prevY = y;
        this.z = prevZ = z;
    }

    public void tick() {
        prevX = x; prevY = y; prevZ = z;
        prevYaw = yaw; prevPitch = pitch;
        age++;
        inWater = touching(Block.WATER.id);
        inLava = touching(Block.LAVA.id);
        if (inWater) { fireTicks = 0; fallDistance = 0; }
        if (y < -64) remove();
    }

    public void remove() { removed = true; }

    public double eyeY() { return y + height * 0.85; }

    public double distanceTo(Entity e) {
        double dx = e.x - x, dy = e.y - y, dz = e.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceSq(double px, double py, double pz) {
        double dx = px - x, dy = py - y, dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    protected boolean touching(int id) {
        AABB b = box();
        for (int bx = (int) Math.floor(b.minX + 0.001); bx <= (int) Math.floor(b.maxX - 0.001); bx++)
            for (int by = (int) Math.floor(b.minY + 0.001); by <= (int) Math.floor(b.maxY - 0.4); by++)
                for (int bz = (int) Math.floor(b.minZ + 0.001); bz <= (int) Math.floor(b.maxZ - 0.001); bz++)
                    if (world.getBlock(bx, by, bz) == id) return true;
        return false;
    }

    public boolean eyeInBlock(int id) {
        double ey = eyeY();
        int b = world.getBlock((int) Math.floor(x), (int) Math.floor(ey), (int) Math.floor(z));
        return b == id && (id != Block.WATER.id || ey - Math.floor(ey) < 0.9);
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
                    // Unloaded terrain is treated as solid so nothing falls out of the world
                    if (!loaded) { list.add(new AABB(bx, by, bz, bx + 1, by + 1, bz + 1)); continue; }
                    Block b = Block.get(world.getBlock(bx, by, bz));
                    if (b.model == Block.Model.SHAPE) {
                        for (int[] s : Shapes.boxes(b, world.getMeta(bx, by, bz), world, bx, by, bz, Shapes.Mode.COLLISION))
                            list.add(new AABB(bx + s[0] / 16.0, by + s[1] / 16.0, bz + s[2] / 16.0, bx + s[3] / 16.0, by + s[4] / 16.0, bz + s[5] / 16.0));
                    } else if (b.solid) list.add(new AABB(bx, by, bz, bx + 1, by + 1, bz + 1));
                }
            }
        return list;
    }

    protected boolean fits(AABB b) {
        for (AABB o : collisionBoxes(world, b)) if (o.intersects(b)) return false;
        return true;
    }

    private boolean groundBelow(AABB b, double dx, double dz) {
        AABB t = b.offset(dx, -1.0, dz);
        for (AABB o : collisionBoxes(world, t)) if (o.intersects(t)) return true;
        return false;
    }

    /** Whether movement should stop at ledges (sneaking players). */
    protected boolean avoidsEdges() { return false; }

    /** Moves with collision, stepping up small ledges; updates onGround and collision flags. */
    public void move(double dx, double dy, double dz) {
        AABB bb = box();
        if (avoidsEdges() && onGround) {
            double step = 0.05;
            while (dx != 0 && !groundBelow(bb, dx, 0)) dx = Math.abs(dx) < step ? 0 : dx - Math.signum(dx) * step;
            while (dz != 0 && !groundBelow(bb, 0, dz)) dz = Math.abs(dz) < step ? 0 : dz - Math.signum(dz) * step;
            while (dx != 0 && dz != 0 && !groundBelow(bb, dx, dz)) {
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
        if (stepHeight > 0 && (ground || onGround) && (ox != dx || oz != dz)) {
            double sx = ox, sy = stepHeight, sz = oz;
            List<AABB> sb = collisionBoxes(world, bb.expandTowards(ox, stepHeight, oz));
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
        verticalCollision = oy != dy;
        boolean wasOnGround = onGround;
        onGround = oy != dy && oy < 0;
        if (onGround) {
            if (fallDistance > 0) onLanded(fallDistance);
            fallDistance = 0;
        } else if (dy < 0) {
            fallDistance -= (float) dy;
        }
        if (ox != dx) motionX = 0;
        if (oz != dz) motionZ = 0;
        if (oy != dy) motionY = 0;
    }

    protected void onLanded(float distance) { }

    public double interpX(float pt) { return prevX + (x - prevX) * pt; }
    public double interpY(float pt) { return prevY + (y - prevY) * pt; }
    public double interpZ(float pt) { return prevZ + (z - prevZ) * pt; }
}
