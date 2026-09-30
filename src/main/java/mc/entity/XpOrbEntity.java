package mc.entity;

/** Experience orb: drifts towards a nearby player and is absorbed on contact. */
public final class XpOrbEntity extends Entity {
    public int value;
    public int pickupDelay = 10;
    public final float phase = (float) (Math.random() * 100);

    public XpOrbEntity(int value) {
        this.value = value;
        width = height = 0.5f;
        stepHeight = 0;
    }

    /** Minecraft splits experience into orbs of these sizes. */
    public static int orbSize(int xp) {
        int[] sizes = {2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1};
        for (int s : sizes) if (xp >= s) return s;
        return 1;
    }

    /** Spawns orbs worth `xp` in total around a position. */
    public static void spawn(mc.world.World world, double x, double y, double z, int xp) {
        while (xp > 0) {
            int v = orbSize(xp);
            xp -= v;
            XpOrbEntity o = new XpOrbEntity(v);
            o.setPos(x, y, z);
            o.motionX = (Math.random() - 0.5) * 0.2;
            o.motionY = Math.random() * 0.2 + 0.1;
            o.motionZ = (Math.random() - 0.5) * 0.2;
            world.addEntity(o);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pickupDelay > 0) pickupDelay--;
        motionY -= 0.03;
        if (inLava) { motionY = 0.2; motionX = (random.nextDouble() - 0.5) * 0.2; motionZ = (random.nextDouble() - 0.5) * 0.2; }
        Player p = world.nearestPlayer(x, y, z);
        if (p != null && !p.isDead()) {
            double dx = p.x - x, dy = p.y + p.eyeHeight / 2 - y, dz = p.z - z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < 64) {
                double d = Math.sqrt(d2);
                double pull = 1 - d / 8;
                pull *= pull;
                motionX += dx / d * pull * 0.1;
                motionY += dy / d * pull * 0.1;
                motionZ += dz / d * pull * 0.1;
            }
        }
        move(motionX, motionY, motionZ);
        double friction = onGround ? 0.588 : 0.98;
        motionX *= friction;
        motionY *= 0.98;
        motionZ *= friction;
        if (onGround) motionY *= -0.9;
        if (age >= 6000) remove();
    }
}
