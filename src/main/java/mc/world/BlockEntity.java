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

    /** Monster spawner: spawns its mob type around itself while a player is within 16 blocks. */
    public static final class Spawner extends BlockEntity {
        public String mob = "ZOMBIE";
        public int delay = 20;
        /** Spin of the little mob shown inside (render only). */
        public float spin;

        public Spawner(int x, int y, int z) {
            super(x, y, z, 0);
        }

        @Override
        public void tick(World world) {
            mc.entity.Player p = world.player();
            if (p == null || p.distanceSq(x + 0.5, y + 0.5, z + 0.5) > 16 * 16) return;
            spin += 10;
            if (world.random().nextInt(4) == 0) world.addParticle("smoke", x + world.random().nextDouble(), y + world.random().nextDouble(), z + world.random().nextDouble());
            if (world.random().nextInt(8) == 0) world.addParticle("flame", x + world.random().nextDouble(), y + world.random().nextDouble(), z + world.random().nextDouble());
            if (--delay > 0) return;
            delay = 200 + world.random().nextInt(600);
            mc.entity.MobType type;
            try { type = mc.entity.MobType.valueOf(mob); } catch (IllegalArgumentException e) { return; }
            int nearby = 0;
            for (mc.entity.Entity e : world.entities())
                if (e instanceof mc.entity.Mob m && m.type == type && Math.abs(m.x - x) < 9 && Math.abs(m.y - y) < 5 && Math.abs(m.z - z) < 9) nearby++;
            for (int i = 0; i < 4 && nearby < 6; i++) {
                double sx = x + 0.5 + (world.random().nextDouble() - world.random().nextDouble()) * 4;
                double sy = y + world.random().nextInt(3) - 1;
                double sz = z + 0.5 + (world.random().nextDouble() - world.random().nextDouble()) * 4;
                int bx = (int) Math.floor(sx), by = (int) Math.floor(sy), bz = (int) Math.floor(sz);
                if (world.getBlock(bx, by, bz) != 0 || world.getBlock(bx, by + 1, bz) != 0 || !Block.get(world.getBlock(bx, by - 1, bz)).solid) continue;
                mc.entity.Mob m = new mc.entity.Mob(type);
                java.util.Arrays.fill(m.armor, null);
                m.setPos(sx, by, sz);
                m.yaw = world.random().nextFloat() * 360;
                world.addEntity(m);
                nearby++;
                for (int k = 0; k < 10; k++) world.addParticle("poof", sx, by + 0.5, sz);
            }
        }
    }

    public static final class Chest extends BlockEntity {
        public Chest(int x, int y, int z) {
            super(x, y, z, 27);
        }
    }

    /** Slot 0 input, 1 fuel, 2 output. Smelts one item per 200 ticks like Minecraft. */
    public static final class Furnace extends BlockEntity {
        public int burnTime, burnTotal, cookTime;
        public static final int COOK_TOTAL = 200;
        /** Experience earned by smelting, paid out when the output is taken. */
        public float storedXp;

        /** Whole experience points to award now (the fraction is rounded randomly like Minecraft). */
        public int takeXp() {
            int whole = (int) storedXp;
            if (Math.random() < storedXp - whole) whole++;
            storedXp = 0;
            return whole;
        }

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
                    storedXp += Recipes.smeltingXp(slots[0].item);
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
