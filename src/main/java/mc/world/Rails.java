package mc.world;

/**
 * Rail shapes and connection logic, following Minecraft's rail metadata:
 * 0 north-south, 1 east-west, 2-5 ascending east/west/north/south, 6-9 curves south-east, south-west, north-west,
 * north-east. Powered and detector rails cannot curve; they use bit 8 for "powered" / "cart on top".
 */
public final class Rails {
    private Rails() { }

    public static final int ACTIVE = 8;

    /** Exit offsets {dx, dy, dz} of each shape (dy -1 marks the lower end of a slope). */
    public static final int[][][] EXITS = {
            {{0, 0, -1}, {0, 0, 1}},
            {{-1, 0, 0}, {1, 0, 0}},
            {{-1, -1, 0}, {1, 0, 0}},
            {{-1, 0, 0}, {1, -1, 0}},
            {{0, 0, -1}, {0, -1, 1}},
            {{0, -1, -1}, {0, 0, 1}},
            {{0, 0, 1}, {1, 0, 0}},
            {{0, 0, 1}, {-1, 0, 0}},
            {{0, 0, -1}, {-1, 0, 0}},
            {{0, 0, -1}, {1, 0, 0}},
    };

    /** Horizontal directions: north (-z), south (+z), west (-x), east (+x). */
    static final int N = 0, S = 1, W = 2, E = 3;
    static final int[] HX = {0, 0, -1, 1}, HZ = {-1, 1, 0, 0};

    public static boolean isRail(int id) {
        return id == Block.RAIL.id || id == Block.POWERED_RAIL.id || id == Block.DETECTOR_RAIL.id;
    }

    public static boolean canCurve(int id) { return id == Block.RAIL.id; }

    public static int shape(int id, int meta) {
        return canCurve(id) ? meta & 15 : meta & 7;
    }

    public static boolean ascending(int shape) { return shape >= 2 && shape <= 5; }

    private static int dirOf(int dx, int dz) {
        return dz < 0 ? N : dz > 0 ? S : dx < 0 ? W : E;
    }

    /** Whether a shape has an exit in horizontal direction d. */
    static boolean exitsTo(int shape, int d) {
        for (int[] e : EXITS[shape]) if (dirOf(e[0], e[2]) == d) return true;
        return false;
    }

    private static int straight(int d) { return d == N || d == S ? 0 : 1; }

    /** Shape joining two horizontal directions (or -1 if they are the same). */
    static int join(int a, int b) {
        if (a == b) return -1;
        if ((a < 2) == (b < 2)) return straight(a);
        int ns = a < 2 ? a : b, ew = a < 2 ? b : a;
        if (ns == S) return ew == E ? 6 : 7;
        return ew == W ? 8 : 9;
    }

    /** Height of a rail neighbour in direction d: 0 same level, 1 one up, -1 one down, or MIN if none. */
    private static int neighbourDy(Shapes.Getter w, int x, int y, int z, int d) {
        int nx = x + HX[d], nz = z + HZ[d];
        if (isRail(w.getBlock(nx, y, nz))) return 0;
        if (isRail(w.getBlock(nx, y + 1, nz))) return 1;
        if (isRail(w.getBlock(nx, y - 1, nz))) return -1;
        return Integer.MIN_VALUE;
    }

    /** Number of this rail's exits that actually lead to a rail pointing back at it. */
    private static int linkCount(Shapes.Getter w, int x, int y, int z) {
        int id = w.getBlock(x, y, z);
        int s = shape(id, w.getMeta(x, y, z));
        int n = 0;
        for (int[] e : EXITS[s]) if (linkedTo(w, x, y, z, dirOf(e[0], e[2]))) n++;
        return n;
    }

    /** Whether the rail at (x,y,z) and its neighbour in direction d point at each other. */
    static boolean linkedTo(Shapes.Getter w, int x, int y, int z, int d) {
        int dy = neighbourDy(w, x, y, z, d);
        if (dy == Integer.MIN_VALUE) return false;
        int nx = x + HX[d], ny = y + dy, nz = z + HZ[d];
        int self = shape(w.getBlock(x, y, z), w.getMeta(x, y, z));
        int other = shape(w.getBlock(nx, ny, nz), w.getMeta(nx, ny, nz));
        // A step up or down only links through a slope rising towards the higher rail
        if (dy == 1 && ascendsTo(self) != d) return false;
        if (dy == -1 && ascendsTo(other) != (d ^ 1)) return false;
        return exitsTo(self, d) && exitsTo(other, d ^ 1);
    }

    /** Direction a slope rises towards, or -1 for flat shapes. */
    static int ascendsTo(int shape) {
        return switch (shape) { case 2 -> E; case 3 -> W; case 4 -> N; case 5 -> S; default -> -1; };
    }

    /** Whether the neighbour rail in direction d would accept a new connection from us. */
    private static boolean accepts(Shapes.Getter w, int x, int y, int z, int d) {
        int dy = neighbourDy(w, x, y, z, d);
        if (dy == Integer.MIN_VALUE) return false;
        int nx = x + HX[d], ny = y + dy, nz = z + HZ[d];
        int other = shape(w.getBlock(nx, ny, nz), w.getMeta(nx, ny, nz));
        if (exitsTo(other, d ^ 1)) return true;
        return linkCount(w, nx, ny, nz) < 2;
    }

    /** Straight shape along d's axis, ascending towards a rail one block higher at either end. */
    private static int straightFor(Shapes.Getter w, int x, int y, int z, int d) {
        int axis = straight(d);
        int[] ends = axis == 0 ? new int[]{N, S} : new int[]{W, E};
        for (int end : ends) {
            if (neighbourDy(w, x, y, z, end) == 1) return end == E ? 2 : end == W ? 3 : end == N ? 4 : 5;
        }
        return axis;
    }

    /** Shape a rail placed at (x, y, z) should take given its neighbours; facing is the placer's direction. */
    public static int placementShape(Shapes.Getter w, int x, int y, int z, int id, int facingDir) {
        int[] found = new int[4];
        int n = 0;
        // Minecraft's preference order: south, east, north, west
        for (int d : new int[]{S, E, N, W}) if (accepts(w, x, y, z, d)) found[n++] = d;
        if (n == 0) return straightFor(w, x, y, z, facingDir);
        if (n >= 2) {
            // Prefer a straight line through when possible
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (found[i] == (found[j] ^ 1)) return straightFor(w, x, y, z, found[i]);
            if (canCurve(id)) return join(found[0], found[1]);
        }
        return straightFor(w, x, y, z, found[0]);
    }

    /** Horizontal facing (Shapes convention: 0 south, 1 west, 2 north, 3 east) to a rail direction. */
    public static int fromFacing(int facing) {
        return switch (facing & 3) { case 0 -> S; case 1 -> W; case 2 -> N; default -> E; };
    }

    /** After a rail is placed, bends and tilts neighbouring rails with a free end towards it. */
    public static void connectNeighbours(World w, int x, int y, int z) {
        int self = shape(w.getBlock(x, y, z), w.getMeta(x, y, z));
        for (int d = 0; d < 4; d++) {
            if (!exitsTo(self, d)) continue;
            int dy = neighbourDy(w, x, y, z, d);
            if (dy == Integer.MIN_VALUE) continue;
            int nx = x + HX[d], ny = y + dy, nz = z + HZ[d];
            int nid = w.getBlock(nx, ny, nz), nmeta = w.getMeta(nx, ny, nz);
            int back = d ^ 1;
            int ns = shape(nid, nmeta);
            int want;
            // Keep the neighbour's one existing link (if any) and add us
            int keep = -1;
            for (int[] e : EXITS[ns]) {
                int ed = dirOf(e[0], e[2]);
                if (ed != back && linkedTo(w, nx, ny, nz, ed)) keep = ed;
            }
            if (keep >= 0 && linkedTo(w, nx, ny, nz, back)) continue;
            if (keep >= 0 && linkCount(w, nx, ny, nz) >= 2) continue;
            if (keep >= 0 && keep != (back ^ 1) && canCurve(nid)) want = join(keep, back);
            else want = straightFor(w, nx, ny, nz, back);
            if (keep >= 0 && keep != (back ^ 1) && !canCurve(nid)) continue;
            if (want != ns) w.setBlock(nx, ny, nz, nid, canCurve(nid) ? want : want | (nmeta & ACTIVE), false);
        }
    }

    /** Rails need a solid top below them; ascending rails also pop off without it. */
    public static boolean canStay(World w, int x, int y, int z) {
        return w.sturdyTop(x, y - 1, z);
    }

    // ------------------------------------------------------------------ powered rails

    /** A powered rail conducts power along a line of up to 8 connected powered rails. */
    public static void updatePowered(World w, int x, int y, int z) {
        if (w.getBlock(x, y, z) != Block.POWERED_RAIL.id) return;
        // Collect the line of powered rails through this one
        java.util.List<int[]> line = new java.util.ArrayList<>();
        line.add(new int[]{x, y, z});
        int s = shape(Block.POWERED_RAIL.id, w.getMeta(x, y, z));
        for (int[] e : EXITS[s]) {
            int d = dirOf(e[0], e[2]);
            int cx = x, cy = y, cz = z;
            for (int i = 0; i < 16; i++) {
                int dy = neighbourDy(w, cx, cy, cz, d);
                if (dy == Integer.MIN_VALUE) break;
                int nx = cx + HX[d], ny = cy + dy, nz = cz + HZ[d];
                if (w.getBlock(nx, ny, nz) != Block.POWERED_RAIL.id || !linkedTo(w, cx, cy, cz, d)) break;
                line.add(new int[]{nx, ny, nz});
                cx = nx; cy = ny; cz = nz;
            }
        }
        boolean[] direct = new boolean[line.size()];
        for (int i = 0; i < line.size(); i++) {
            int[] p = line.get(i);
            direct[i] = Redstone.powered(w, p[0], p[1], p[2]) || Redstone.powered(w, p[0], p[1] - 1, p[2]);
        }
        for (int i = 0; i < line.size(); i++) {
            int[] p = line.get(i);
            boolean on = false;
            for (int j = 0; j < line.size() && !on; j++) {
                if (!direct[j]) continue;
                int[] q = line.get(j);
                on = Math.abs(p[0] - q[0]) + Math.abs(p[2] - q[2]) <= 8;
            }
            int meta = w.getMeta(p[0], p[1], p[2]);
            boolean was = (meta & ACTIVE) != 0;
            if (on != was) w.setBlock(p[0], p[1], p[2], Block.POWERED_RAIL.id, on ? meta | ACTIVE : meta & ~ACTIVE, false);
        }
    }

    /** Whether a minecart is on the rail block. */
    public static boolean cartOn(World w, int x, int y, int z) {
        mc.util.AABB area = new mc.util.AABB(x + 0.125, y, z + 0.125, x + 0.875, y + 0.875, z + 0.875);
        for (mc.entity.Entity e : w.entities())
            if (e instanceof mc.entity.MinecartEntity && !e.removed && e.box().intersects(area)) return true;
        return false;
    }

    /** A minecart rolled onto a detector rail. */
    public static void cartOnDetector(World w, int x, int y, int z) {
        int meta = w.getMeta(x, y, z);
        if ((meta & ACTIVE) != 0) return;
        w.setBlock(x, y, z, Block.DETECTOR_RAIL.id, meta | ACTIVE, false);
        w.scheduleTick(x, y, z, 20);
        Redstone.changedAround(w, x, y, z, new int[]{x, y - 1, z});
    }
}
