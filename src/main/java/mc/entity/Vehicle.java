package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.util.AABB;

import java.util.ArrayList;
import java.util.List;

/** Something an entity can ride (minecarts and boats): one passenger, breaks after a few hits like Minecraft's. */
public abstract class Vehicle extends Entity {
    public Entity passenger;
    /** Accumulated damage (decays every tick); the vehicle breaks above 40. */
    public float damageTaken;
    public int hurtTicks, hurtDir = 1;
    /** The rider's movement input this tick (-1..1), set by the rider. */
    public float riderForward, riderStrafe;

    /** Item the vehicle drops when broken. */
    public abstract Item item();

    /** Height of the rider's feet above the vehicle's base (riders sit with their legs folded). */
    public double riderOffset() { return -0.35; }

    public boolean mount(Entity e) {
        if (passenger != null || e.vehicle != null || removed || e == this) return false;
        passenger = e;
        e.vehicle = this;
        e.motionX = e.motionY = e.motionZ = 0;
        positionRider();
        return true;
    }

    /** Lets the passenger off on top of the vehicle, or beside it if that is blocked. */
    public void dismount() {
        Entity p = passenger;
        if (p == null) return;
        passenger = null;
        p.vehicle = null;
        riderForward = riderStrafe = 0;
        double[][] spots = {{0, height + 0.01, 0}, {1.2, 0, 0}, {-1.2, 0, 0}, {0, 0, 1.2}, {0, 0, -1.2}, {0, 1.2, 0}};
        double px = x, py = y + height + 0.01, pz = z;
        for (double[] s : spots) {
            p.x = x + s[0]; p.y = y + s[1]; p.z = z + s[2];
            if (p.world == null || p.fits(p.box())) { px = p.x; py = p.y; pz = p.z; break; }
        }
        p.setPos(px, py, pz);
        p.motionX = p.motionY = p.motionZ = 0;
        p.fallDistance = 0;
    }

    /** Places the passenger in the seat (both the current and previous positions, so it moves smoothly with us). */
    public void positionRider() {
        Entity p = passenger;
        if (p == null) return;
        if (p.removed || p.vehicle != this || (p instanceof LivingEntity le && le.isDead())) {
            if (p.vehicle == this) p.vehicle = null;
            passenger = null;
            return;
        }
        // Players sit; mobs stand inside
        double oy = p instanceof Player ? riderOffset() : 0.15;
        p.prevX = prevX; p.prevY = prevY + oy; p.prevZ = prevZ;
        p.x = x; p.y = y + oy; p.z = z;
        p.fallDistance = 0;
    }

    /** A hit from a player: the vehicle wobbles and breaks after a few hits (at once in creative). Returns true if it broke. */
    public boolean hit(Player by, float amount) {
        if (removed) return false;
        hurtDir = -hurtDir;
        hurtTicks = 10;
        damageTaken += amount * 10;
        boolean creative = by != null && by.creative;
        if (creative || damageTaken > 40) {
            if (passenger != null) dismount();
            if (!creative && world != null) dropItems();
            remove();
            return true;
        }
        return false;
    }

    protected void dropItems() {
        world.spawnItem(x, y + 0.3, z, new ItemStack(item(), 1));
    }

    @Override
    public void tick() {
        super.tick();
        if (hurtTicks > 0) hurtTicks--;
        if (damageTaken > 0) damageTaken = Math.max(0, damageTaken - 1);
    }

    @Override
    public void netTick() {
        super.netTick();
        if (hurtTicks > 0) hurtTicks--;
        if (damageTaken > 0) damageTaken = Math.max(0, damageTaken - 1);
        positionRider();
    }

    /** Living things and other vehicles we bump into. */
    protected List<Entity> touchingEntities(double grow) {
        AABB area = box();
        area = new AABB(area.minX - grow, area.minY, area.minZ - grow, area.maxX + grow, area.maxY, area.maxZ + grow);
        List<Entity> out = new ArrayList<>();
        List<Entity> all = new ArrayList<>(world.entities());
        all.addAll(world.players());
        for (Entity o : all) {
            if (o == this || o == passenger || o.removed || o.vehicle != null) continue;
            if (!(o instanceof LivingEntity || o instanceof Vehicle)) continue;
            if (o instanceof LivingEntity le && le.isDead()) continue;
            if (o.box().intersects(area)) out.add(o);
        }
        return out;
    }

    /** Pushes us and the other entity apart (Minecraft's entity collision). */
    protected void pushApart(Entity o, double strength) {
        double dx = o.x - x, dz = o.z - z;
        double d = Math.max(Math.abs(dx), Math.abs(dz));
        if (d < 0.01) return;
        d = Math.sqrt(d);
        dx /= d;
        dz /= d;
        double f = Math.min(1, 1 / d);
        dx *= f * strength;
        dz *= f * strength;
        motionX -= dx;
        motionZ -= dz;
        if (o instanceof Vehicle) {
            o.motionX += dx;
            o.motionZ += dz;
        } else {
            o.motionX += dx / 4;
            o.motionZ += dz / 4;
        }
    }
}
