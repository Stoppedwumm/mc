package mc.util;

public final class AABB {
    public double minX, minY, minZ, maxX, maxY, maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    public AABB copy() { return new AABB(minX, minY, minZ, maxX, maxY, maxZ); }

    public AABB offset(double x, double y, double z) {
        return new AABB(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public void move(double x, double y, double z) {
        minX += x; minY += y; minZ += z; maxX += x; maxY += y; maxZ += z;
    }

    /** Expands in the direction of motion (for broad-phase collision). */
    public AABB expandTowards(double x, double y, double z) {
        AABB b = copy();
        if (x < 0) b.minX += x; else b.maxX += x;
        if (y < 0) b.minY += y; else b.maxY += y;
        if (z < 0) b.minZ += z; else b.maxZ += z;
        return b;
    }

    public boolean intersects(AABB o) {
        return o.maxX > minX && o.minX < maxX && o.maxY > minY && o.minY < maxY && o.maxZ > minZ && o.minZ < maxZ;
    }

    public double clipX(AABB o, double dx) {
        if (o.maxY <= minY || o.minY >= maxY || o.maxZ <= minZ || o.minZ >= maxZ) return dx;
        if (dx > 0 && o.maxX <= minX) dx = Math.min(dx, minX - o.maxX);
        else if (dx < 0 && o.minX >= maxX) dx = Math.max(dx, maxX - o.minX);
        return dx;
    }

    public double clipY(AABB o, double dy) {
        if (o.maxX <= minX || o.minX >= maxX || o.maxZ <= minZ || o.minZ >= maxZ) return dy;
        if (dy > 0 && o.maxY <= minY) dy = Math.min(dy, minY - o.maxY);
        else if (dy < 0 && o.minY >= maxY) dy = Math.max(dy, maxY - o.minY);
        return dy;
    }

    public double clipZ(AABB o, double dz) {
        if (o.maxX <= minX || o.minX >= maxX || o.maxY <= minY || o.minY >= maxY) return dz;
        if (dz > 0 && o.maxZ <= minZ) dz = Math.min(dz, minZ - o.maxZ);
        else if (dz < 0 && o.minZ >= maxZ) dz = Math.max(dz, maxZ - o.minZ);
        return dz;
    }
}
