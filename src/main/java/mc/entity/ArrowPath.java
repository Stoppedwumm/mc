package mc.entity;

/** Segment/box intersection shared by projectiles. */
final class ArrowPath {
    private ArrowPath() { }

    /** Fraction (0-1) along the motion vector where the box is entered, or -1. */
    static double rayBox(double ox, double oy, double oz, double dx, double dy, double dz,
                         double x0, double y0, double z0, double x1, double y1, double z1) {
        double tmin = 0, tmax = 1;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz}, mn = {x0, y0, z0}, mx = {x1, y1, z1};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < mn[i] || o[i] > mx[i]) return -1;
            } else {
                double t1 = (mn[i] - o[i]) / d[i], t2 = (mx[i] - o[i]) / d[i];
                if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1;
            }
        }
        return tmin;
    }
}
