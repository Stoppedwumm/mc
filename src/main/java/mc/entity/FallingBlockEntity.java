package mc.entity;

import mc.item.Item;
import mc.item.ItemStack;
import mc.world.Block;

/** Sand or gravel falling as an entity; becomes a block again when it lands. */
public final class FallingBlockEntity extends Entity {
    public final int blockId;

    public FallingBlockEntity(int blockId) {
        this.blockId = blockId;
        width = height = 0.98f;
        stepHeight = 0;
    }

    @Override
    public void tick() {
        super.tick();
        motionY -= 0.04;
        move(motionX, motionY, motionZ);
        motionX *= 0.98;
        motionY *= 0.98;
        motionZ *= 0.98;
        if (onGround || age > 600) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y + 0.5), bz = (int) Math.floor(z);
            Block here = Block.get(world.getBlock(bx, by, bz));
            if (here.replaceable) {
                world.setBlock(bx, by, bz, blockId);
                world.checkFalling(bx, by, bz);
            } else {
                world.spawnItem(x, y + 0.5, z, new ItemStack(Item.get(blockId), 1));
            }
            remove();
        }
    }
}
