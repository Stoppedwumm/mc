package mc.world;

/** Nether portals: lighting obsidian frames, keeping them intact, and linking between dimensions. */
public final class Portal {
    private Portal() { }

    private static int ax(int axis) { return axis == 0 ? 1 : 0; }

    private static int az(int axis) { return axis == 0 ? 0 : 1; }

    private static boolean empty(World w, int x, int y, int z) {
        int id = w.getBlock(x, y, z);
        return id == 0 || id == Block.FIRE.id;
    }

    private static boolean obsidian(World w, int x, int y, int z) {
        return w.getBlock(x, y, z) == Block.OBSIDIAN.id;
    }

    /**
     * Tries to light a portal whose interior contains (x, y, z). Frames must be obsidian with an interior of 2-21
     * blocks wide and 3-21 tall, like Minecraft. Returns true if a portal was created.
     */
    public static boolean tryLight(World w, int x, int y, int z) {
        for (int axis = 0; axis < 2; axis++) {
            int dx = ax(axis), dz = az(axis);
            if (!empty(w, x, y, z)) return false;
            // Bottom of the interior
            int by = y;
            while (by > 0 && empty(w, x, by - 1, z) && y - by < 21) by--;
            if (!obsidian(w, x, by - 1, z)) continue;
            // Left and right edges along the axis
            int left = 0, right = 0;
            while (left < 21 && empty(w, x - dx * (left + 1), by, z - dz * (left + 1))) left++;
            while (right < 21 && empty(w, x + dx * (right + 1), by, z + dz * (right + 1))) right++;
            int width = left + right + 1;
            if (width < 2 || width > 21) continue;
            int sx = x - dx * left, sz = z - dz * left;
            if (!obsidian(w, sx - dx, by, sz - dz) || !obsidian(w, sx + dx * width, by, sz + dz * width)) continue;
            // Height: rise until the whole row is capped by obsidian
            int height = 0;
            boolean ok = true;
            while (height < 22) {
                boolean rowEmpty = true, rowCapped = true;
                for (int i = 0; i < width; i++) {
                    int px = sx + dx * i, pz = sz + dz * i;
                    if (!empty(w, px, by + height, pz)) rowEmpty = false;
                    if (!obsidian(w, px, by + height, pz)) rowCapped = false;
                }
                if (rowCapped && height >= 3) break;
                if (!rowEmpty || !obsidian(w, sx - dx, by + height, sz - dz) || !obsidian(w, sx + dx * width, by + height, sz + dz * width)) { ok = false; break; }
                height++;
            }
            if (!ok || height < 3 || height > 21) continue;
            for (int i = 0; i < width; i++)
                if (!obsidian(w, sx + dx * i, by - 1, sz + dz * i)) { ok = false; break; }
            if (!ok) continue;
            for (int i = 0; i < width; i++)
                for (int h = 0; h < height; h++) w.setBlock(sx + dx * i, by + h, sz + dz * i, Block.NETHER_PORTAL.id, axis, false);
            w.playSound("portal_light", x + 0.5, y + 0.5, z + 0.5, 1, 1);
            return true;
        }
        return false;
    }

    /** A portal block survives only while its in-plane neighbours are portal or obsidian. */
    public static boolean intact(World w, int x, int y, int z) {
        int axis = w.getMeta(x, y, z) & 1;
        int dx = ax(axis), dz = az(axis);
        int[][] n = {{0, 1, 0}, {0, -1, 0}, {dx, 0, dz}, {-dx, 0, -dz}};
        for (int[] d : n) {
            int id = w.getBlock(x + d[0], y + d[1], z + d[2]);
            if (id != Block.NETHER_PORTAL.id && id != Block.OBSIDIAN.id) return false;
        }
        return true;
    }

    /**
     * Finds a portal near (x, z) in the destination world, or builds one. Returns the feet position
     * {x, y, z} of a player standing inside it. The area must already be loaded.
     */
    public static double[] findOrCreate(World w, double x, double y, double z, int maxY) {
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        double best = Double.MAX_VALUE;
        int[] found = null;
        for (int dx = -16; dx <= 16; dx++)
            for (int dz = -16; dz <= 16; dz++)
                for (int py = 1; py < maxY; py++) {
                    if (w.getBlock(cx + dx, py, cz + dz) != Block.NETHER_PORTAL.id || w.getBlock(cx + dx, py - 1, cz + dz) == Block.NETHER_PORTAL.id) continue;
                    double d = dx * dx + dz * dz + (py - y) * (py - y) * 0.1;
                    if (d < best) { best = d; found = new int[]{cx + dx, py, cz + dz}; }
                }
        if (found != null) return new double[]{found[0] + 0.5, found[1], found[2] + 0.5};
        int[] spot = findSpot(w, cx, (int) y, cz, maxY);
        int px = spot[0], py = spot[1], pz = spot[2], axis = spot[3];
        int dx = ax(axis), dz = az(axis);
        // Frame is 4 wide (2 wide interior) and 5 tall; clear the plane and the spaces in front and behind
        for (int i = -1; i <= 2; i++)
            for (int h = -1; h <= 3; h++)
                for (int side = -1; side <= 1; side++) {
                    int bx = px + dx * i + dz * side, bz = pz + dz * i + dx * side;
                    boolean frame = i == -1 || i == 2 || h == -1 || h == 3;
                    if (side == 0) w.setBlock(bx, py + h, bz, frame ? Block.OBSIDIAN.id : Block.NETHER_PORTAL.id, frame ? 0 : axis, false);
                    else if (h == -1) { if (!Block.get(w.getBlock(bx, py - 1, bz)).solid) w.setBlock(bx, py - 1, bz, Block.OBSIDIAN.id, 0, false); }
                    else if (h <= 2) w.setBlock(bx, py + h, bz, 0, 0, false);
                }
        return new double[]{px + 0.5 + dx * 0.5, py, pz + 0.5 + dz * 0.5};
    }

    /** A place with solid ground and room for a portal, preferring the target height; forced if none exists. */
    private static int[] findSpot(World w, int x, int y, int z, int maxY) {
        for (int r = 0; r <= 16; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int k = 0; k < maxY - 8; k++) {
                        // Search outwards from the preferred height
                        int py = y + ((k & 1) == 0 ? k / 2 : -(k / 2 + 1));
                        if (py < 6 || py > maxY - 6) continue;
                        for (int axis = 0; axis < 2; axis++)
                            if (fits(w, x + dx, py, z + dz, axis)) return new int[]{x + dx, py, z + dz, axis};
                    }
                }
        int py = Math.max(40, Math.min(maxY - 10, y));
        return new int[]{x, py, z, 0};
    }

    private static boolean fits(World w, int x, int y, int z, int axis) {
        int dx = ax(axis), dz = az(axis);
        for (int i = -1; i <= 2; i++) {
            Block below = Block.get(w.getBlock(x + dx * i, y - 1, z + dz * i));
            if (!below.solid || below.isLiquid()) return false;
            for (int h = 0; h <= 3; h++)
                for (int side = -1; side <= 1; side++)
                    if (w.getBlock(x + dx * i + dz * side, y + h, z + dz * i + dx * side) != 0) return false;
        }
        return true;
    }
}
