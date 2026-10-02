package net.minecraft.world.phys.shapes;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Building and combining block shapes (reamc-compat: combinations other than OR work on a 1/16 grid). */
public final class Shapes {
    private Shapes() { }

    public static final double EPSILON = 1.0E-7, BIG_EPSILON = 1.0E-6;
    private static final VoxelShape BLOCK = new VoxelShape(List.of(new AABB(0, 0, 0, 1, 1, 1)));
    public static final VoxelShape INFINITY = new VoxelShape(List.of(new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)));
    private static final VoxelShape EMPTY = new VoxelShape(List.of());

    public static VoxelShape empty() { return EMPTY; }

    public static VoxelShape block() { return BLOCK; }

    public static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        if (x0 > x1 || y0 > y1 || z0 > z1) throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        return create(x0, y0, z0, x1, y1, z1);
    }

    public static VoxelShape create(double x0, double y0, double z0, double x1, double y1, double z1) {
        if (x1 - x0 < EPSILON || y1 - y0 < EPSILON || z1 - z0 < EPSILON) return EMPTY;
        return new VoxelShape(List.of(new AABB(x0, y0, z0, x1, y1, z1)));
    }

    public static VoxelShape create(AABB b) { return create(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }

    public static VoxelShape or(VoxelShape a, VoxelShape b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        List<AABB> l = new ArrayList<>(a.boxes);
        l.addAll(b.boxes);
        return new VoxelShape(l);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... rest) {
        VoxelShape s = first;
        for (VoxelShape r : rest) s = or(s, r);
        return s;
    }

    public static VoxelShape join(VoxelShape a, VoxelShape b, BooleanOp op) { return joinUnoptimized(a, b, op); }

    public static VoxelShape joinUnoptimized(VoxelShape a, VoxelShape b, BooleanOp op) {
        if (op == BooleanOp.OR) return or(a, b);
        if (op.apply(false, false)) throw new IllegalArgumentException("Can't join shapes outside the block");
        boolean[] ga = grid(a), gb = grid(b), r = new boolean[16 * 16 * 16];
        for (int i = 0; i < r.length; i++) r[i] = op.apply(ga[i], gb[i]);
        return fromGrid(r);
    }

    public static boolean joinIsNotEmpty(VoxelShape a, VoxelShape b, BooleanOp op) {
        boolean[] ga = grid(a), gb = grid(b);
        for (int i = 0; i < ga.length; i++) if (op.apply(ga[i], gb[i])) return true;
        return false;
    }

    public static boolean blockOccudes(VoxelShape a, VoxelShape b, Direction d) { return !a.isEmpty() && a.getFaceShape(d).boxes.stream().anyMatch(x -> x.getXsize() * x.getZsize() >= 1 || x.getXsize() * x.getYsize() >= 1 || x.getYsize() * x.getZsize() >= 1); }

    public static boolean faceShapeOccludes(VoxelShape a, VoxelShape b) { return a == BLOCK || b == BLOCK; }

    public static VoxelShape rotate(VoxelShape s, Direction to) { return s; }

    private static boolean[] grid(VoxelShape s) {
        boolean[] g = new boolean[16 * 16 * 16];
        for (AABB b : s.boxes) {
            int x0 = cell(b.minX), y0 = cell(b.minY), z0 = cell(b.minZ), x1 = cell(b.maxX), y1 = cell(b.maxY), z1 = cell(b.maxZ);
            for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) g[(x * 16 + y) * 16 + z] = true;
        }
        return g;
    }

    private static int cell(double v) { return (int) Math.max(0, Math.min(16, Math.round(v * 16))); }

    /** Greedy merge of filled cells back into boxes. */
    private static VoxelShape fromGrid(boolean[] g) {
        boolean[] used = new boolean[g.length];
        List<AABB> out = new ArrayList<>();
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++) {
                    int i = (x * 16 + y) * 16 + z;
                    if (!g[i] || used[i]) continue;
                    int z1 = z;
                    while (z1 + 1 < 16 && g[(x * 16 + y) * 16 + z1 + 1] && !used[(x * 16 + y) * 16 + z1 + 1]) z1++;
                    int y1 = y;
                    outer:
                    while (y1 + 1 < 16) {
                        for (int zz = z; zz <= z1; zz++) { int j = (x * 16 + y1 + 1) * 16 + zz; if (!g[j] || used[j]) break outer; }
                        y1++;
                    }
                    int x1 = x;
                    outer2:
                    while (x1 + 1 < 16) {
                        for (int yy = y; yy <= y1; yy++) for (int zz = z; zz <= z1; zz++) { int j = ((x1 + 1) * 16 + yy) * 16 + zz; if (!g[j] || used[j]) break outer2; }
                        x1++;
                    }
                    for (int xx = x; xx <= x1; xx++) for (int yy = y; yy <= y1; yy++) for (int zz = z; zz <= z1; zz++) used[(xx * 16 + yy) * 16 + zz] = true;
                    out.add(new AABB(x / 16.0, y / 16.0, z / 16.0, (x1 + 1) / 16.0, (y1 + 1) / 16.0, (z1 + 1) / 16.0));
                }
        return new VoxelShape(out);
    }

    @FunctionalInterface
    public interface DoubleLineConsumer {
        void consume(double x0, double y0, double z0, double x1, double y1, double z1);
    }
}
