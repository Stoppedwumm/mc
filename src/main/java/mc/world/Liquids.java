package mc.world;

/**
 * Flowing water and lava. Meta bits 0-2 hold the level (0 = source, 1-7 = further from the source) and
 * bit 3 marks liquid falling from above. Water spreads 7 blocks, lava 3, both prefer the direction of
 * the nearest drop like Minecraft.
 */
public final class Liquids {
    public static final int WATER_DELAY = 5, LAVA_DELAY = 30;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static int delay(int id) {
        return id == Block.LAVA.id ? LAVA_DELAY : WATER_DELAY;
    }

    /** Effective level for spreading: sources and falling liquid count as 0. */
    private static int level(int meta) {
        return (meta & 8) != 0 ? 0 : meta & 7;
    }

    private static boolean isSource(int meta) {
        return (meta & 15) == 0;
    }

    /** Whether liquid `id` may flow into the block at the position. */
    private static boolean canFlowInto(World w, int x, int y, int z, int id) {
        int b = w.getBlock(x, y, z);
        if (b == 0) return true;
        Block bl = Block.get(b);
        if (b == id) return false;
        if (bl.isLiquid()) return false;
        return bl.replaceable || bl.model == Block.Model.CROSS || bl.washable;
    }

    public static void tick(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z);
        Block block = Block.get(id);
        if (!block.isLiquid()) return;
        boolean lava = block == Block.LAVA;
        int drop = lava ? 2 : 1;
        int meta = w.getMeta(x, y, z);

        if (lava && touchesWater(w, x, y, z)) {
            w.setBlock(x, y, z, isSource(meta) ? Block.OBSIDIAN.id : Block.COBBLESTONE.id);
            w.playSound("fizz", x + 0.5, y + 0.5, z + 0.5, 0.5f, 2.6f);
            for (int i = 0; i < 6; i++) w.addParticle("smoke", x + Math.random(), y + 1.1, z + Math.random());
            return;
        }

        if (!isSource(meta)) {
            int newMeta;
            if (w.getBlock(x, y + 1, z) == id) {
                newMeta = 8;
            } else {
                int best = 99, sources = 0;
                for (int[] d : DIRS) {
                    if (w.getBlock(x + d[0], y, z + d[1]) != id) continue;
                    int nm = w.getMeta(x + d[0], y, z + d[1]);
                    best = Math.min(best, level(nm) + drop);
                    if (isSource(nm)) sources++;
                }
                int below = w.getBlock(x, y - 1, z);
                boolean solidBelow = Block.get(below).solid || (below == id && isSource(w.getMeta(x, y - 1, z)));
                if (!lava && sources >= 2 && solidBelow) best = 0;
                if (best > 7) {
                    w.setBlock(x, y, z, 0);
                    return;
                }
                newMeta = best;
            }
            if (newMeta != meta) {
                w.setBlock(x, y, z, id, newMeta, true);
                meta = newMeta;
            }
        }

        // Flow downwards first
        if (canFlowInto(w, x, y - 1, z, id)) {
            breakReplaced(w, x, y - 1, z);
            w.setBlock(x, y - 1, z, id, 8, true);
            if (!isSource(meta)) return;
        } else if (w.getBlock(x, y - 1, z) == id) {
            return;
        } else if (Block.get(w.getBlock(x, y - 1, z)).isLiquid()) {
            // Lava flowing onto water turns it into stone
            if (lava) w.setBlock(x, y - 1, z, Block.STONE.id);
            return;
        }

        int next = level(meta) + drop;
        if (next > 7) return;
        boolean[] dirs = flowDirections(w, x, y, z, id, lava ? 2 : 4);
        for (int i = 0; i < 4; i++) {
            if (!dirs[i]) continue;
            int nx = x + DIRS[i][0], nz = z + DIRS[i][1];
            if (canFlowInto(w, nx, y, nz, id)) {
                breakReplaced(w, nx, y, nz);
                w.setBlock(nx, y, nz, id, next, true);
            } else if (w.getBlock(nx, y, nz) == id) {
                int nm = w.getMeta(nx, y, nz);
                if (!isSource(nm) && (nm & 8) == 0 && (nm & 7) > next) w.setBlock(nx, y, nz, id, next, true);
            }
        }
    }

    private static boolean touchesWater(World w, int x, int y, int z) {
        int wid = Block.WATER.id;
        return w.getBlock(x + 1, y, z) == wid || w.getBlock(x - 1, y, z) == wid || w.getBlock(x, y, z + 1) == wid
                || w.getBlock(x, y, z - 1) == wid || w.getBlock(x, y + 1, z) == wid;
    }

    private static void breakReplaced(World w, int x, int y, int z) {
        int b = w.getBlock(x, y, z);
        if (b != 0 && !Block.get(b).isLiquid()) w.breakBlock(x, y, z, null, true);
    }

    /** Directions towards the nearest place where the liquid can fall, or all open directions. */
    private static boolean[] flowDirections(World w, int x, int y, int z, int id, int maxDist) {
        int[] dist = new int[4];
        int min = 1000;
        for (int i = 0; i < 4; i++) {
            int nx = x + DIRS[i][0], nz = z + DIRS[i][1];
            if (!canFlowInto(w, nx, y, nz, id) && w.getBlock(nx, y, nz) != id) { dist[i] = 1000; continue; }
            dist[i] = holeDistance(w, nx, y, nz, id, 1, maxDist, i ^ 1);
            min = Math.min(min, dist[i]);
        }
        boolean[] out = new boolean[4];
        for (int i = 0; i < 4; i++) out[i] = dist[i] == min && min < 1000;
        if (min >= 1000) {
            // No drop nearby: spread evenly
            for (int i = 0; i < 4; i++) out[i] = true;
        }
        return out;
    }

    private static int holeDistance(World w, int x, int y, int z, int id, int depth, int maxDist, int from) {
        if (canFlowInto(w, x, y - 1, z, id) || w.getBlock(x, y - 1, z) == id) return depth;
        if (depth >= maxDist) return 1000;
        int best = 1000;
        for (int i = 0; i < 4; i++) {
            if (i == from) continue;
            int nx = x + DIRS[i][0], nz = z + DIRS[i][1];
            if (!canFlowInto(w, nx, y, nz, id) && w.getBlock(nx, y, nz) != id) continue;
            best = Math.min(best, holeDistance(w, nx, y, nz, id, depth + 1, maxDist, i ^ 1));
        }
        return best;
    }
}
