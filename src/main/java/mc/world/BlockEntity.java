package mc.world;

import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;

/** Blocks with extra state: chests and furnaces. */
public abstract class BlockEntity {
    public final int x, y, z;
    public final ItemStack[] slots;

    protected BlockEntity(int x, int y, int z, int size) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.slots = new ItemStack[size];
    }

    public void tick(World world) { }

    public static final class Chest extends BlockEntity {
        public Chest(int x, int y, int z) {
            super(x, y, z, 27);
        }
    }

    /** Slot 0 input, 1 fuel, 2 output. Smelts one item per 200 ticks like Minecraft. */
    public static final class Furnace extends BlockEntity {
        public int burnTime, burnTotal, cookTime;
        public static final int COOK_TOTAL = 200;

        public Furnace(int x, int y, int z) {
            super(x, y, z, 3);
        }

        private boolean canSmelt() {
            if (ItemStack.isEmpty(slots[0])) return false;
            ItemStack result = Recipes.smelting(slots[0].item);
            if (result == null) return false;
            ItemStack out = slots[2];
            if (ItemStack.isEmpty(out)) return true;
            return out.canMerge(result) && out.count + result.count <= out.item.maxStack;
        }

        @Override
        public void tick(World world) {
            boolean wasBurning = burnTime > 0;
            if (burnTime > 0) burnTime--;
            if (burnTime == 0 && canSmelt() && !ItemStack.isEmpty(slots[1]) && slots[1].item.fuelTicks > 0) {
                burnTotal = burnTime = slots[1].item.fuelTicks;
                if (slots[1].item == Item.LAVA_BUCKET) slots[1] = new ItemStack(Item.BUCKET, 1);
                else if (--slots[1].count <= 0) slots[1] = null;
            }
            if (burnTime > 0 && canSmelt()) {
                if (++cookTime >= COOK_TOTAL) {
                    cookTime = 0;
                    ItemStack result = Recipes.smelting(slots[0].item);
                    if (ItemStack.isEmpty(slots[2])) slots[2] = result;
                    else slots[2].count += result.count;
                    if (--slots[0].count <= 0) slots[0] = null;
                }
            } else if (cookTime > 0) {
                cookTime = Math.max(0, cookTime - 2);
            }
            boolean burning = burnTime > 0;
            if (burning != wasBurning) {
                int meta = world.getMeta(x, y, z);
                world.setBlock(x, y, z, burning ? Block.LIT_FURNACE.id : Block.FURNACE.id, meta, false);
            }
        }
    }
}
