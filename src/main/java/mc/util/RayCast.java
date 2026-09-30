package mc.util;

import mc.world.Block;
import mc.world.World;

/** Voxel traversal (Amanatides & Woo) for block picking. */
public final class RayCast {
    public static final class Hit {
        public int x, y, z;
        /** Face normal of the side that was hit. */
        public int nx, ny, nz;
        public double distance;
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
                if (!b.isLiquid() && hitsShape(b, ox + dx * t, oy + dy * t, oz + dz * t, dx, dy, dz, x, y, z)) {
                    Hit h = new Hit();
                    h.x = x; h.y = y; h.z = z;
                    h.nx = nx; h.ny = ny; h.nz = nz;
                    h.distance = t;
                    return h;
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

    /** Plants and torches only occupy the middle of their cell. */
    private static boolean hitsShape(Block b, double px, double py, double pz, double dx, double dy, double dz, int x, int y, int z) {
        if (b.model == Block.Model.CUBE) return true;
        double[] box = shape(b);
        return rayBox(px - x, py - y, pz - z, dx, dy, dz, box);
    }

    public static double[] shape(Block b) {
        if (b.model == Block.Model.TORCH) return new double[]{0.375, 0, 0.375, 0.625, 0.625, 0.625};
        if (b.model == Block.Model.CROSS) return new double[]{0.15, 0, 0.15, 0.85, 0.8, 0.85};
        return new double[]{0, 0, 0, 1, 1, 1};
    }

    private static boolean rayBox(double ox, double oy, double oz, double dx, double dy, double dz, double[] b) {
        double tmin = 0, tmax = 2;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < b[i] || o[i] > b[i + 3]) return false;
            } else {
                double t1 = (b[i] - o[i]) / d[i], t2 = (b[i + 3] - o[i]) / d[i];
                if (t1 > t2) { double tt = t1; t1 = t2; t2 = tt; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return false;
            }
        }
        return true;
    }
}
