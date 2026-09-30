package mc.world;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Redstone power, modelled on Minecraft's rules:
 * <ul>
 * <li>Sources (levers, buttons, pressure plates, redstone torches, blocks of redstone) power their neighbours and
 * strongly power the block they are attached to (a torch strongly powers the block above it).</li>
 * <li>Wire carries power 15 down to 1, losing one per block, and weakly powers the block beneath it and the blocks it
 * points into.</li>
 * <li>A strongly powered solid block powers adjacent wire and components; a weakly powered one only components.</li>
 * <li>Torches invert the power of their support block after a 1 redstone-tick delay; repeaters pass power forward
 * after 1-4 redstone ticks and strongly power the block in front.</li>
 * <li>Components (lamps, pistons, doors, trapdoors, fence gates, TNT) react to being powered.</li>
 * </ul>
 */
public final class Redstone {
    private Redstone() { }

    private static final int[][] D6 = Shapes.DIR6;

    private static boolean isTorch(int id) { return id == Block.REDSTONE_TORCH.id || id == Block.UNLIT_REDSTONE_TORCH.id; }

    private static boolean isRepeater(int id) { return id == Block.REPEATER.id || id == Block.POWERED_REPEATER.id; }

    /** Blocks that react to power or produce it (used to limit update work). */
    public static boolean relevant(int id) {
        Block b = Block.get(id);
        return id == Block.REDSTONE_WIRE.id || isTorch(id) || isRepeater(id) || id == Block.LEVER.id || b.shape == Block.Shape.BUTTON
                || b.shape == Block.Shape.PLATE || id == Block.REDSTONE_LAMP.id || id == Block.LIT_REDSTONE_LAMP.id
                || id == Block.PISTON.id || id == Block.STICKY_PISTON.id || id == Block.REDSTONE_BLOCK.id || id == Block.TNT.id
                || b.shape == Block.Shape.DOOR || b.shape == Block.Shape.TRAPDOOR || b.shape == Block.Shape.GATE;
    }

    // ------------------------------------------------------------------ what blocks emit

    /** Support block of a torch (floor or wall). */
    private static int[] torchSupport(int x, int y, int z, int meta) {
        if (meta >= 1 && meta <= 4) return new int[]{x + Shapes.DX[meta - 1], y, z + Shapes.DZ[meta - 1]};
        return new int[]{x, y - 1, z};
    }

    /** Support block of a lever or button. */
    private static int[] attachSupport(int x, int y, int z, int meta) {
        int m = meta & 7;
        if (m == 0) return new int[]{x, y - 1, z};
        if (m == 5) return new int[]{x, y + 1, z};
        return new int[]{x + Shapes.DX[m - 1], y, z + Shapes.DZ[m - 1]};
    }

    private static boolean same(int[] p, int x, int y, int z) { return p[0] == x && p[1] == y && p[2] == z; }

    /**
     * Power the block at (x, y, z) sends into its neighbour at (tx, ty, tz). strongOnly counts only power that can
     * pass through a solid block (as a wire reading a block would).
     */
    public static int emitted(Shapes.Getter w, int x, int y, int z, int tx, int ty, int tz, boolean forWire) {
        int id = w.getBlock(x, y, z);
        if (id == 0) return 0;
        int meta = w.getMeta(x, y, z);
        Block b = Block.get(id);
        if (id == Block.REDSTONE_BLOCK.id) return 15;
        if (id == Block.REDSTONE_TORCH.id) {
            return same(torchSupport(x, y, z, meta), tx, ty, tz) ? 0 : 15;
        }
        if (id == Block.LEVER.id || b.shape == Block.Shape.BUTTON) return (meta & 8) != 0 ? 15 : 0;
        if (b.shape == Block.Shape.PLATE) return (meta & 1) != 0 ? 15 : 0;
        if (id == Block.POWERED_REPEATER.id) {
            int f = meta & 3;
            return tx == x + Shapes.DX[f] && tz == z + Shapes.DZ[f] && ty == y ? 15 : 0;
        }
        if (id == Block.REDSTONE_WIRE.id) {
            if (forWire) return 0; // wire-to-wire handled by the network
            int level = meta & 15;
            if (level == 0) return 0;
            if (ty == y - 1 && tx == x && tz == z) return level;
            if (ty != y) return 0;
            for (int d = 0; d < 4; d++) {
                if (tx == x + Shapes.DX[d] && tz == z + Shapes.DZ[d]) return pointsInto(w, x, y, z, d) ? level : 0;
            }
            return 0;
        }
        if ((b.opaque && b.model == Block.Model.CUBE) || (b.isSlab() && (meta & 3) == 2)) {
            int strong = strongPower(w, x, y, z);
            if (forWire) return strong;
            return Math.max(strong, weakPower(w, x, y, z));
        }
        return 0;
    }

    /** Power a solid block receives from sources attached to it, torches below and repeaters facing it. */
    public static int strongPower(Shapes.Getter w, int x, int y, int z) {
        int best = 0;
        for (int[] d : D6) {
            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
            int id = w.getBlock(nx, ny, nz);
            if (id == 0) continue;
            int meta = w.getMeta(nx, ny, nz);
            Block b = Block.get(id);
            int p = 0;
            if (id == Block.REDSTONE_TORCH.id && d[1] == -1) p = 15; // torch below
            else if ((id == Block.LEVER.id || b.shape == Block.Shape.BUTTON) && (meta & 8) != 0 && same(attachSupport(nx, ny, nz, meta), x, y, z)) p = 15;
            else if (b.shape == Block.Shape.PLATE && (meta & 1) != 0 && d[1] == 1) p = 15;
            else if (id == Block.POWERED_REPEATER.id) {
                int f = meta & 3;
                if (nx + Shapes.DX[f] == x && nz + Shapes.DZ[f] == z && ny == y) p = 15;
            }
            best = Math.max(best, p);
        }
        return best;
    }

    /** Power a solid block gets from wire on top of it or pointing into it. */
    private static int weakPower(Shapes.Getter w, int x, int y, int z) {
        int best = 0;
        if (w.getBlock(x, y + 1, z) == Block.REDSTONE_WIRE.id) best = w.getMeta(x, y + 1, z) & 15;
        for (int d = 0; d < 4; d++) {
            int nx = x + Shapes.DX[d], nz = z + Shapes.DZ[d];
            if (w.getBlock(nx, y, nz) != Block.REDSTONE_WIRE.id) continue;
            if (pointsInto(w, nx, y, nz, Shapes.opposite(d))) best = Math.max(best, w.getMeta(nx, y, nz) & 15);
        }
        return best;
    }

    /** Whether the position is powered, for components like lamps, pistons and doors. */
    public static boolean powered(Shapes.Getter w, int x, int y, int z) {
        for (int[] d : D6) {
            if (emitted(w, x + d[0], y + d[1], z + d[2], x, y, z, false) > 0) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ wire connections

    private static boolean connectsTo(int id, int meta, int dir) {
        if (id == Block.REDSTONE_WIRE.id || isTorch(id) || id == Block.LEVER.id || id == Block.REDSTONE_BLOCK.id) return true;
        Block b = Block.get(id);
        if (b.shape == Block.Shape.BUTTON || b.shape == Block.Shape.PLATE) return true;
        if (isRepeater(id)) return (meta & 1) == (dir & 1);
        return false;
    }

    /** Whether wire at (x, y, z) connects in horizontal direction d (same level, one up or one down). */
    public static boolean wireConnects(Shapes.Getter w, int x, int y, int z, int d) {
        int nx = x + Shapes.DX[d], nz = z + Shapes.DZ[d];
        int id = w.getBlock(nx, y, nz);
        if (connectsTo(id, w.getMeta(nx, y, nz), d)) return true;
        Block n = Block.get(id);
        if (!n.opaque && w.getBlock(nx, y - 1, nz) == Block.REDSTONE_WIRE.id) return true;
        return n.opaque && !Block.get(w.getBlock(x, y + 1, z)).opaque && w.getBlock(nx, y + 1, nz) == Block.REDSTONE_WIRE.id;
    }

    /** Wire points into a block when it connects that way, or when it is a straight line or lone dot through it. */
    private static boolean pointsInto(Shapes.Getter w, int x, int y, int z, int d) {
        if (wireConnects(w, x, y, z, d)) return true;
        boolean any = false;
        for (int k = 0; k < 4; k++) if (wireConnects(w, x, y, z, k)) { any = true; break; }
        if (!any) return true;
        // Straight line: continues into the block ahead
        int side1 = (d + 1) & 3, side2 = (d + 3) & 3;
        return wireConnects(w, x, y, z, Shapes.opposite(d)) && !wireConnects(w, x, y, z, side1) && !wireConnects(w, x, y, z, side2);
    }

    /** Wire neighbours of a wire cell, including steps up and down. */
    private static List<int[]> wireNeighbours(World w, int x, int y, int z) {
        List<int[]> out = new ArrayList<>(4);
        boolean coveredAbove = Block.get(w.getBlock(x, y + 1, z)).opaque;
        for (int d = 0; d < 4; d++) {
            int nx = x + Shapes.DX[d], nz = z + Shapes.DZ[d];
            int id = w.getBlock(nx, y, nz);
            if (id == Block.REDSTONE_WIRE.id) out.add(new int[]{nx, y, nz});
            else if (!Block.get(id).opaque) {
                if (w.getBlock(nx, y - 1, nz) == Block.REDSTONE_WIRE.id) out.add(new int[]{nx, y - 1, nz});
            } else if (!coveredAbove && w.getBlock(nx, y + 1, nz) == Block.REDSTONE_WIRE.id) out.add(new int[]{nx, y + 1, nz});
        }
        return out;
    }

    // ------------------------------------------------------------------ updates

    private static boolean busy;
    private static final LongArrayFIFOQueue QUEUE = new LongArrayFIFOQueue();

    /** Something changed at (x, y, z): recompute wire and update components nearby. */
    public static void update(World w, int x, int y, int z) {
        if (w.remote != null) return; // the server computes power
        QUEUE.enqueue(World.posKey(x, y, z));
        if (busy) return;
        busy = true;
        try {
            int guard = 0;
            while (!QUEUE.isEmpty() && guard++ < 4096) {
                long k = QUEUE.dequeueLong();
                int[] p = unpack(k);
                process(w, p[0], p[1], p[2]);
            }
            QUEUE.clear();
        } finally {
            busy = false;
        }
    }

    private static int[] unpack(long key) {
        int x = (int) (key >> 38), z = (int) ((key >> 12) & 0x3FFFFFF), y = (int) (key & 0xFFF);
        if (x >= 1 << 25) x -= 1 << 26;
        if (z >= 1 << 25) z -= 1 << 26;
        return new int[]{x, y, z};
    }

    private static void process(World w, int x, int y, int z) {
        // Wire networks touching this area
        LongOpenHashSet seen = new LongOpenHashSet();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    int px = x + dx, py = y + dy, pz = z + dz;
                    if (w.getBlock(px, py, pz) == Block.REDSTONE_WIRE.id && !seen.contains(World.posKey(px, py, pz))) recomputeNetwork(w, px, py, pz, seen);
                }
        // Components within reach of this change
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 3) continue;
                    evaluate(w, x + dx, y + dy, z + dz);
                }
    }

    /** Flood-fills a wire network, then spreads power from its inputs, losing one level per block. */
    private static void recomputeNetwork(World w, int sx, int sy, int sz, LongOpenHashSet seen) {
        List<int[]> cells = new ArrayList<>();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        long start = World.posKey(sx, sy, sz);
        seen.add(start);
        cells.add(new int[]{sx, sy, sz});
        for (int i = 0; i < cells.size() && cells.size() < 1024; i++) {
            int[] c = cells.get(i);
            for (int[] n : wireNeighbours(w, c[0], c[1], c[2])) {
                if (seen.add(World.posKey(n[0], n[1], n[2]))) cells.add(n);
            }
        }
        Long2IntOpenHashMap level = new Long2IntOpenHashMap();
        // Direct input from non-wire neighbours
        for (int[] c : cells) {
            int best = 0;
            for (int[] d : D6) {
                int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                if (w.getBlock(nx, ny, nz) == Block.REDSTONE_WIRE.id) continue;
                best = Math.max(best, emitted(w, nx, ny, nz, c[0], c[1], c[2], true));
            }
            long k = World.posKey(c[0], c[1], c[2]);
            level.put(k, best);
            if (best > 0) q.enqueue(k);
        }
        while (!q.isEmpty()) {
            long k = q.dequeueLong();
            int[] c = unpack(k);
            int l = level.get(k);
            if (l <= 1) continue;
            for (int[] n : wireNeighbours(w, c[0], c[1], c[2])) {
                long nk = World.posKey(n[0], n[1], n[2]);
                if (level.getOrDefault(nk, 0) < l - 1) {
                    level.put(nk, l - 1);
                    q.enqueue(nk);
                }
            }
        }
        for (int[] c : cells) {
            int l = level.get(World.posKey(c[0], c[1], c[2]));
            if ((w.getMeta(c[0], c[1], c[2]) & 15) != l) {
                w.setBlock(c[0], c[1], c[2], Block.REDSTONE_WIRE.id, l, false);
                // Things around the changed wire may need to react
                for (int[] d : D6) {
                    int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                    if (relevant(w.getBlock(nx, ny, nz))) QUEUE.enqueue(World.posKey(nx, ny, nz));
                    if (Block.get(w.getBlock(nx, ny, nz)).opaque) for (int[] e : D6) {
                        if (relevant(w.getBlock(nx + e[0], ny + e[1], nz + e[2]))) QUEUE.enqueue(World.posKey(nx + e[0], ny + e[1], nz + e[2]));
                    }
                }
            }
        }
    }

    /** Brings a component in line with its power, directly or via a scheduled tick for delayed ones. */
    private static void evaluate(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z);
        if (id == 0 || id == Block.REDSTONE_WIRE.id) return;
        int meta = w.getMeta(x, y, z);
        Block b = Block.get(id);
        if (isTorch(id)) {
            int[] s = torchSupport(x, y, z, meta);
            boolean supportPowered = supportPowered(w, s[0], s[1], s[2], x, y, z);
            if ((id == Block.REDSTONE_TORCH.id) == supportPowered) w.scheduleTick(x, y, z, 2);
        } else if (isRepeater(id)) {
            boolean in = repeaterInput(w, x, y, z, meta);
            if (in != (id == Block.POWERED_REPEATER.id)) w.scheduleTick(x, y, z, 2 * (((meta >> 2) & 3) + 1));
        } else if (id == Block.REDSTONE_LAMP.id || id == Block.LIT_REDSTONE_LAMP.id) {
            boolean p = powered(w, x, y, z);
            if (p && id == Block.REDSTONE_LAMP.id) w.setBlock(x, y, z, Block.LIT_REDSTONE_LAMP.id, 0, false);
            else if (!p && id == Block.LIT_REDSTONE_LAMP.id) w.scheduleTick(x, y, z, 4);
        } else if (id == Block.PISTON.id || id == Block.STICKY_PISTON.id) {
            boolean p = powered(w, x, y, z);
            if (p != ((meta & 8) != 0)) w.scheduleTick(x, y, z, 1);
        } else if (id == Block.TNT.id) {
            if (powered(w, x, y, z)) {
                w.setBlock(x, y, z, 0);
                mc.entity.TntEntity t = new mc.entity.TntEntity(80);
                t.setPos(x + 0.5, y, z + 0.5);
                w.addEntity(t);
                w.playSound("fuse", x + 0.5, y + 0.5, z + 0.5, 1, 1);
            }
        } else if (b.shape == Block.Shape.DOOR) {
            int lower = (meta & 8) != 0 ? y - 1 : y;
            boolean p = powered(w, x, lower, z) || powered(w, x, lower + 1, z);
            int lm = w.getMeta(x, lower, z);
            boolean open = (lm & 4) != 0;
            if (p != open && w.getBlock(x, lower, z) == id) {
                w.setBlock(x, lower, z, id, lm ^ 4, false);
                if (w.getBlock(x, lower + 1, z) == id) w.setBlock(x, lower + 1, z, id, w.getMeta(x, lower + 1, z) ^ 4, false);
                w.playSound(p ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 1, 1);
            }
        } else if (b.shape == Block.Shape.TRAPDOOR || b.shape == Block.Shape.GATE) {
            boolean p = powered(w, x, y, z);
            if (p != ((meta & 4) != 0)) {
                w.setBlock(x, y, z, id, meta ^ 4, false);
                w.playSound(p ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 1, 1.1f);
            }
        }
    }

    /** A torch's support block counts as powered by anything except the torch itself. */
    private static boolean supportPowered(World w, int sx, int sy, int sz, int tx, int ty, int tz) {
        Block s = Block.get(w.getBlock(sx, sy, sz));
        if (!s.opaque) return false;
        for (int[] d : D6) {
            int nx = sx + d[0], ny = sy + d[1], nz = sz + d[2];
            if (nx == tx && ny == ty && nz == tz) continue;
            int id = w.getBlock(nx, ny, nz);
            int meta = w.getMeta(nx, ny, nz);
            if (id == Block.REDSTONE_WIRE.id) {
                if ((meta & 15) > 0 && (d[1] == 1 || (d[1] == 0 && pointsInto(w, nx, ny, nz, dirIndex(-d[0], -d[2]))))) return true;
                continue;
            }
            if (emitted(w, nx, ny, nz, sx, sy, sz, false) > 0) return true;
        }
        return false;
    }

    private static int dirIndex(int dx, int dz) { return Shapes.facingOf(dx, dz); }

    private static boolean repeaterInput(World w, int x, int y, int z, int meta) {
        int back = Shapes.opposite(meta & 3);
        int bx = x + Shapes.DX[back], bz = z + Shapes.DZ[back];
        return emitted(w, bx, y, bz, x, y, z, false) > 0;
    }

    /** Delayed reactions: torches, repeaters, lamps switching off, buttons releasing, plates, pistons. */
    public static void scheduledTick(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z);
        int meta = w.getMeta(x, y, z);
        Block b = Block.get(id);
        if (isTorch(id)) {
            int[] s = torchSupport(x, y, z, meta);
            boolean lit = !supportPowered(w, s[0], s[1], s[2], x, y, z);
            int want = lit ? Block.REDSTONE_TORCH.id : Block.UNLIT_REDSTONE_TORCH.id;
            if (want != id) {
                w.setBlock(x, y, z, want, meta, false);
                changed(w, x, y, z);
            }
        } else if (isRepeater(id)) {
            boolean in = repeaterInput(w, x, y, z, meta);
            int want = in ? Block.POWERED_REPEATER.id : Block.REPEATER.id;
            if (want != id) {
                w.setBlock(x, y, z, want, meta, false);
                int f = meta & 3;
                changed(w, x, y, z);
                changed(w, x + Shapes.DX[f], y, z + Shapes.DZ[f]);
            }
        } else if (id == Block.LIT_REDSTONE_LAMP.id) {
            if (!powered(w, x, y, z)) w.setBlock(x, y, z, Block.REDSTONE_LAMP.id, 0, false);
        } else if (b.shape == Block.Shape.BUTTON && (meta & 8) != 0) {
            w.setBlock(x, y, z, id, meta & ~8, false);
            w.playSound("click", x + 0.5, y + 0.5, z + 0.5, 0.3f, 0.5f);
            changedAround(w, x, y, z, attachSupport(x, y, z, meta));
        } else if (b.shape == Block.Shape.PLATE && (meta & 1) != 0) {
            if (!Pistons.entityOn(w, x, y, z, b == Block.STONE_PRESSURE_PLATE)) {
                w.setBlock(x, y, z, id, 0, false);
                w.playSound("click", x + 0.5, y + 0.1, z + 0.5, 0.3f, 0.5f);
                changedAround(w, x, y, z, new int[]{x, y - 1, z});
            } else w.scheduleTick(x, y, z, 20);
        } else if (id == Block.PISTON.id || id == Block.STICKY_PISTON.id) {
            boolean p = powered(w, x, y, z);
            if (p && (meta & 8) == 0) Pistons.extend(w, x, y, z);
            else if (!p && (meta & 8) != 0) Pistons.retract(w, x, y, z);
        }
    }

    /** Update the block and its neighbours after a power change. */
    public static void changed(World w, int x, int y, int z) {
        // Solid neighbours relay strong power, so their surroundings need a look too
        for (int[] d : D6) {
            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
            if (Block.get(w.getBlock(nx, ny, nz)).opaque) QUEUE.enqueue(World.posKey(nx, ny, nz));
        }
        update(w, x, y, z);
    }

    /** A source toggled: update around it and around its support block. */
    public static void changedAround(World w, int x, int y, int z, int[] support) {
        changed(w, x, y, z);
        changed(w, support[0], support[1], support[2]);
    }

    /** Toggles a lever. */
    public static void toggleLever(World w, int x, int y, int z) {
        int meta = w.getMeta(x, y, z);
        w.setBlock(x, y, z, Block.LEVER.id, meta ^ 8, false);
        w.playSound("click", x + 0.5, y + 0.5, z + 0.5, 0.3f, (meta & 8) == 0 ? 0.6f : 0.5f);
        changedAround(w, x, y, z, attachSupport(x, y, z, meta));
    }

    /** Presses a button for 1 (stone) or 1.5 (wood) seconds. */
    public static void pressButton(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z), meta = w.getMeta(x, y, z);
        if ((meta & 8) != 0) return;
        w.setBlock(x, y, z, id, meta | 8, false);
        w.playSound("click", x + 0.5, y + 0.5, z + 0.5, 0.3f, 0.6f);
        w.scheduleTick(x, y, z, id == Block.OAK_BUTTON.id ? 30 : 20);
        changedAround(w, x, y, z, attachSupport(x, y, z, meta));
    }

    /** Called when an entity stands on a pressure plate. */
    public static void stepOnPlate(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z), meta = w.getMeta(x, y, z);
        if ((meta & 1) != 0) return;
        w.setBlock(x, y, z, id, 1, false);
        w.playSound("click", x + 0.5, y + 0.1, z + 0.5, 0.3f, 0.6f);
        w.scheduleTick(x, y, z, 20);
        changedAround(w, x, y, z, new int[]{x, y - 1, z});
    }
}
