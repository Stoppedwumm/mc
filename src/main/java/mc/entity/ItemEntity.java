package mc.entity;

import mc.item.ItemStack;

/** A dropped item stack that bobs, spins, merges with neighbours and can be picked up. */
public final class ItemEntity extends Entity {
    public final ItemStack stack;
    public int pickupDelay = 10;
    public final float spin = (float) (Math.random() * Math.PI * 2);

    public ItemEntity(ItemStack stack) {
        this.stack = stack;
        width = height = 0.25f;
        stepHeight = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (pickupDelay > 0) pickupDelay--;
        if (inWater) {
            motionY += 0.02;
            motionY *= 0.8;
        } else {
            motionY -= 0.04;
        }
        move(motionX, motionY, motionZ);
        double friction = onGround ? 0.588 : 0.98;
        motionX *= friction;
        motionZ *= friction;
        motionY *= 0.98;
        if (onGround) motionY *= -0.5;
        if (inLava) remove();
        if (age >= 6000 || stack.count <= 0) remove();
        if (age % 20 == 0) merge();
    }

    private void merge() {
        for (Entity e : world.entities()) {
            if (e == this || e.removed || !(e instanceof ItemEntity o)) continue;
            if (!o.stack.canMerge(stack) || distanceSq(o.x, o.y, o.z) > 1.0) continue;
            int n = Math.min(o.stack.count, stack.item.maxStack - stack.count);
            if (n <= 0) continue;
            stack.count += n;
            o.stack.count -= n;
            if (o.stack.count <= 0) o.remove();
        }
    }
}
