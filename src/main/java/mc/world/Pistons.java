package mc.world;

import mc.entity.Entity;
import mc.entity.ItemEntity;
import mc.entity.LivingEntity;
import mc.util.AABB;

import java.util.ArrayList;
import java.util.List;

/** Pistons push up to 12 blocks; sticky pistons pull one block back when they retract. */
public final class Pistons {
    public static final int PUSH_LIMIT = 12;

    private Pistons() { }

    private static boolean movable(Block b, int meta) {
        if (b == Block.BEDROCK || b == Block.OBSIDIAN || b == Block.NETHER_PORTAL || b == Block.PISTON_HEAD || b.hardness < 0) return false;
        if (b == Block.CHEST || b == Block.FURNACE || b == Block.LIT_FURNACE || b == Block.SPAWNER) return false;
        if ((b == Block.PISTON || b == Block.STICKY_PISTON) && (meta & 8) != 0) return false;
        return !b.isLiquid();
    }

    /** Blocks in the way that are simply destroyed (and dropped) instead of pushed. */
    private static boolean breaksWhenPushed(Block b) {
        return b.replaceable || b.washable || b.model == Block.Model.CROSS || b.isLiquid() || b.shape == Block.Shape.DOOR || b == Block.CACTUS
                || b == Block.CAKE || b.shape == Block.Shape.BED;
    }

    public static boolean extend(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z), meta = w.getMeta(x, y, z);
        int f = meta & 7;
        int[] d = Shapes.DIR6[f];
        List<int[]> line = new ArrayList<>();
        int cx = x + d[0], cy = y + d[1], cz = z + d[2];
        while (true) {
            if (cy < 0 || cy >= Chunk.HEIGHT) return false;
            Block b = Block.get(w.getBlock(cx, cy, cz));
            if (b == Block.AIR || breaksWhenPushed(b)) break;
            if (!movable(b, w.getMeta(cx, cy, cz)) || line.size() >= PUSH_LIMIT) return false;
            line.add(new int[]{cx, cy, cz, b.id, w.getMeta(cx, cy, cz)});
            cx += d[0]; cy += d[1]; cz += d[2];
        }
        // Whatever fragile block is at the end is destroyed
        if (w.getBlock(cx, cy, cz) != 0) w.breakBlock(cx, cy, cz, null, !Block.get(w.getBlock(cx, cy, cz)).isLiquid());
        for (int i = line.size() - 1; i >= 0; i--) {
            int[] b = line.get(i);
            w.setBlock(b[0] + d[0], b[1] + d[1], b[2] + d[2], b[3], b[4], false);
        }
        w.setBlock(x + d[0], y + d[1], z + d[2], Block.PISTON_HEAD.id, f | (id == Block.STICKY_PISTON.id ? 8 : 0), false);
        w.setBlock(x, y, z, id, meta | 8, false);
        pushEntities(w, x, y, z, d, line.size() + 1);
        w.playSound("piston_out", x + 0.5, y + 0.5, z + 0.5, 0.5f, 0.7f + w.random().nextFloat() * 0.2f);
        for (int i = 0; i <= line.size() + 1; i++) w.notifyAround(x + d[0] * i, y + d[1] * i, z + d[2] * i);
        return true;
    }

    public static void retract(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z), meta = w.getMeta(x, y, z);
        int f = meta & 7;
        int[] d = Shapes.DIR6[f];
        int hx = x + d[0], hy = y + d[1], hz = z + d[2];
        w.setBlock(x, y, z, id, meta & ~8, false);
        if (w.getBlock(hx, hy, hz) == Block.PISTON_HEAD.id) w.setBlock(hx, hy, hz, 0, 0, false);
        if (id == Block.STICKY_PISTON.id) {
            int px = hx + d[0], py = hy + d[1], pz = hz + d[2];
            Block pulled = Block.get(w.getBlock(px, py, pz));
            int pm = w.getMeta(px, py, pz);
            if (pulled != Block.AIR && movable(pulled, pm) && !breaksWhenPushed(pulled)) {
                w.setBlock(px, py, pz, 0, 0, false);
                w.setBlock(hx, hy, hz, pulled.id, pm, false);
            }
        }
        w.playSound("piston_in", x + 0.5, y + 0.5, z + 0.5, 0.5f, 0.6f + w.random().nextFloat() * 0.15f);
        for (int i = 0; i <= 2; i++) w.notifyAround(x + d[0] * i, y + d[1] * i, z + d[2] * i);
    }

    /** Entities in the cells the push moved into get shoved along. */
    private static void pushEntities(World w, int x, int y, int z, int[] d, int length) {
        List<Entity> all = new ArrayList<>(w.entities());
        all.addAll(w.players());
        for (int i = 1; i <= length + 1; i++) {
            int bx = x + d[0] * i, by = y + d[1] * i, bz = z + d[2] * i;
            AABB cell = new AABB(bx, by, bz, bx + 1, by + 1, bz + 1);
            for (Entity e : all) {
                if (!e.box().intersects(cell)) continue;
                e.setPos(e.x + d[0], e.y + d[1] + (d[1] > 0 ? 0.01 : 0), e.z + d[2]);
            }
        }
    }

    /** Whether something is standing on a pressure plate (stone plates only react to living things). */
    public static boolean entityOn(World w, int x, int y, int z, boolean livingOnly) {
        AABB plate = new AABB(x + 0.0625, y, z + 0.0625, x + 0.9375, y + 0.25, z + 0.9375);
        for (mc.entity.Player p : w.players()) if (!p.isDead() && p.box().intersects(plate)) return true;
        for (Entity e : w.entities()) {
            if (e.removed || !e.box().intersects(plate)) continue;
            if (e instanceof LivingEntity || (!livingOnly && e instanceof ItemEntity)) return true;
        }
        return false;
    }
}
