package mc.entity;

/** Primed TNT: falls, flashes and explodes after its fuse runs out. */
public final class TntEntity extends Entity {
    public int fuse;

    public TntEntity(int fuse) {
        this.fuse = fuse;
        width = height = 0.98f;
        stepHeight = 0;
        motionY = 0.2;
        double a = Math.random() * Math.PI * 2;
        motionX = -Math.sin(a) * 0.02;
        motionZ = Math.cos(a) * 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        motionY -= 0.04;
        move(motionX, motionY, motionZ);
        motionX *= 0.98;
        motionY *= 0.98;
        motionZ *= 0.98;
        if (onGround) {
            motionX *= 0.7;
            motionZ *= 0.7;
            motionY *= -0.5;
        }
        if (--fuse <= 0) {
            remove();
            world.explode(x, y + 0.5, z, 4, this);
        } else if (age % 3 == 0) {
            world.addParticle("smoke", x, y + 1, z);
        }
    }
}
