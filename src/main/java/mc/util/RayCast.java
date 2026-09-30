package mc.util;

import mc.world.Block;
import mc.world.Shapes;
import mc.world.World;

/** Voxel traversal (Amanatides & Woo) for block picking. */
public final class RayCast {
    public static final class Hit {
        public int x, y, z;
        /** Face normal of the side that was hit. */
        public int nx, ny, nz;
        public double distance;
        /** Exact hit point in world space. */
        public double px, py, pz;
    }

    public static Hit cast(World world, double ox, double oy, double oz, double dx, double dy, double dz, double maxDist) {
        int x = (int) Math.floor(ox), y = (int) Math.floor(oy), z = (int) Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double tMaxX = dx == 0 ? Double.MAX_VALUE : (dx > 0 ? (x + 1 - ox) : (ox - x)) * tDeltaX;
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (dy > 0 ? (y + 1 - oy) : (oy - y)) * tDeltaY;
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : (dz > 0 ? (z + 1 - oz) : (oz - z)) * tDeltaZ;
        int nx = 0, ny = 0, nz = 0;
        double t = 0;
        while (t <= maxDist) {
            int id = world.getBlock(x, y, z);
            if (id != 0) {
                Block b = Block.get(id);
                if (!b.isLiquid()) {
                    Hit h = null;
                    if (b.model == Block.Model.CUBE) {
                        h = new Hit();
                        h.nx = nx; h.ny = ny; h.nz = nz;
                        h.distance = t;
                    } else {
                        // Nearest box of the shape, with the face normal of the side entered
                        for (double[] box : shapes(world, b, x, y, z)) {
                            double[] r = rayBox(ox - x, oy - y, oz - z, dx, dy, dz, box);
                            if (r != null && r[0] <= maxDist && (h == null || r[0] < h.distance)) {
                                if (h == null) h = new Hit();
                                h.distance = r[0];
                                h.nx = (int) r[1]; h.ny = (int) r[2]; h.nz = (int) r[3];
                            }
                        }
                    }
                    if (h != null) {
                        h.x = x; h.y = y; h.z = z;
                        h.px = ox + dx * h.distance; h.py = oy + dy * h.distance; h.pz = oz + dz * h.distance;
                        return h;
                    }
                }
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX; t = tMaxX; tMaxX += tDeltaX; nx = -stepX; ny = 0; nz = 0;
            } else if (tMaxY < tMaxZ) {
                y += stepY; t = tMaxY; tMaxY += tDeltaY; nx = 0; ny = -stepY; nz = 0;
            } else {
                z += stepZ; t = tMaxZ; tMaxZ += tDeltaZ; nx = 0; ny = 0; nz = -stepZ;
            }
        }
        return null;
    }

    /** Pickable boxes of a non-cube block in block-local coordinates. */
    public static java.util.List<double[]> shapes(World world, Block b, int x, int y, int z) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        if (b.model == Block.Model.TORCH) {
            int m = world.getMeta(x, y, z);
            if (m >= 1 && m <= 4) {
                double cx = 0.5 + Shapes.DX[m - 1] * 0.34, cz = 0.5 + Shapes.DZ[m - 1] * 0.34;
                out.add(new double[]{cx - 0.16, 0.2, cz - 0.16, cx + 0.16, 0.8, cz + 0.16});
            } else out.add(new double[]{0.375, 0, 0.375, 0.625, 0.625, 0.625});
        } else if (b.model == Block.Model.CROSS) {
            out.add(new double[]{0.15, 0, 0.15, 0.85, 0.8, 0.85});
        } else if (b.model == Block.Model.SHAPE) {
            for (int[] s : Shapes.boxes(b, world.getMeta(x, y, z), world, x, y, z, Shapes.Mode.OUTLINE))
                out.add(new double[]{s[0] / 16.0, s[1] / 16.0, s[2] / 16.0, s[3] / 16.0, s[4] / 16.0, s[5] / 16.0});
        } else out.add(new double[]{0, 0, 0, 1, 1, 1});
        return out;
    }

    /** Union of a block's pickable boxes (for the selection outline). */
    public static double[] bounds(World world, Block b, int x, int y, int z) {
        double[] u = {1, 1, 1, 0, 0, 0};
        for (double[] s : shapes(world, b, x, y, z))
            for (int i = 0; i < 3; i++) { u[i] = Math.min(u[i], s[i]); u[i + 3] = Math.max(u[i + 3], s[i + 3]); }
        return u;
    }

    /** Entry distance and entry face normal {t, nx, ny, nz}, or null if the ray misses the box. */
    private static double[] rayBox(double ox, double oy, double oz, double dx, double dy, double dz, double[] b) {
        double tmin = 0, tmax = 1e9;
        int axis = -1;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < b[i] || o[i] > b[i + 3]) return null;
            } else {
                double t1 = (b[i] - o[i]) / d[i], t2 = (b[i + 3] - o[i]) / d[i];
                if (t1 > t2) { double tt = t1; t1 = t2; t2 = tt; }
                if (t1 > tmin) { tmin = t1; axis = i; }
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return null;
            }
        }
        double[] r = {tmin, 0, 0, 0};
        if (axis >= 0) r[1 + axis] = d[axis] > 0 ? -1 : 1;
        return r;
    }
}
