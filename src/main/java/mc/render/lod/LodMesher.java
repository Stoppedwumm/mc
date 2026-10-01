package mc.render.lod;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Builds the mesh of one LOD tile: a 32 x 32 grid of cells, each cell (1 << level) blocks wide, drawn as a column
 * (top face plus walls down to lower neighbours) like Distant Horizons' column LODs. Runs on worker threads.
 */
final class LodMesher {
    static final int N = LodRenderer.CELLS;
    /** Bytes per vertex: position (3 floats) and colour (rgb + packed face/water depth). */
    static final int STRIDE = 16;

    /** Mesh data ready for upload. */
    static final class Built {
        final long key;
        final int generation;
        int tx, tz;
        ByteBuffer data;
        int opaqueQuads, waterQuads;
        float minY = 1e9f, maxY = -1e9f;
        boolean anyKnown;

        Built(long key, int generation) {
            this.key = key;
            this.generation = generation;
        }
    }

    private ByteBuffer buf;
    private int quads;
    private float minY, maxY;

    private void ensure(int more) {
        if (buf.remaining() < more * 4 * STRIDE) {
            ByteBuffer bigger = MemoryUtil.memAlloc(buf.capacity() * 2 + more * 4 * STRIDE);
            buf.flip();
            bigger.put(buf);
            MemoryUtil.memFree(buf);
            buf = bigger;
        }
    }

    private void vertex(float x, float y, float z, int rgb, int extra) {
        buf.putFloat(x).putFloat(y).putFloat(z);
        buf.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb).put((byte) extra);
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
    }

    private void quad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, int rgb, int extra) {
        ensure(1);
        vertex(x0, y0, z0, rgb, extra);
        vertex(x1, y1, z1, rgb, extra);
        vertex(x2, y2, z2, rgb, extra);
        vertex(x3, y3, z3, rgb, extra);
        quads++;
    }

    private static int shade(int rgb, float f) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * f)), g = Math.min(255, (int) ((rgb >> 8 & 255) * f)), b = Math.min(255, (int) ((rgb & 255) * f));
        return r << 16 | g << 8 | b;
    }

    Built build(LodData data, int level, int tx, int tz, long key, int generation) {
        Built out = new Built(key, generation);
        out.tx = tx;
        out.tz = tz;
        int s = 1 << level;
        int originX = tx * N * s, originZ = tz * N * s;
        int G = N + 2;
        int[] height = new int[G * G], water = new int[G * G], top = new int[G * G], side = new int[G * G];
        // Tree crowns floating above the ground (small cells only)
        int[] canopy = new int[G * G], canopyCol = new int[G * G];
        boolean[] known = new boolean[G * G];
        int[] sample = new int[6];
        double[] tmp = new double[4];
        // Sample points inside a cell: one for small cells, a few spread out for big ones (highest wins)
        int[][] pts = s == 1 ? new int[][]{{0, 0}} : s == 2 ? new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}
                : new int[][]{{s / 2, s / 2}, {s / 4, s / 4}, {3 * s / 4, s / 4}, {s / 4, 3 * s / 4}, {3 * s / 4, 3 * s / 4}};
        for (int j = 0; j < G; j++)
            for (int i = 0; i < G; i++) {
                int cx = originX + (i - 1) * s, cz = originZ + (j - 1) * s;
                int best = -1, bw = 0, bt = 0, bs = 0, bg = 0, bgc = 0;
                long r = 0, g = 0, b = 0;
                int count = 0;
                int[] hs = new int[pts.length], cols = new int[pts.length];
                for (int k = 0; k < pts.length; k++) {
                    if (!data.sample(cx + pts[k][0], cz + pts[k][1], s, sample, tmp)) { hs[k] = -1; continue; }
                    hs[k] = sample[0];
                    cols[k] = sample[2];
                    bw = Math.max(bw, sample[1]);
                    if (sample[0] > best) { best = sample[0]; bt = sample[2]; bs = sample[3]; bg = sample[4]; bgc = sample[5]; }
                }
                int idx = j * G + i;
                if (best < 0) continue;
                // Blend the colours of samples near the top so big cells don't flicker between materials
                for (int k = 0; k < pts.length; k++) {
                    if (hs[k] < 0 || hs[k] < best - 1 - s / 4) continue;
                    r += cols[k] >> 16 & 255; g += cols[k] >> 8 & 255; b += cols[k] & 255; count++;
                }
                if (count > 1) bt = (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
                if (s >= 4) {
                    // Big cells average their samples, so single trees or spikes don't become huge pillars
                    int sum = 0, n = 0;
                    long ar = 0, ag = 0, ab = 0;
                    for (int k = 0; k < pts.length; k++) {
                        if (hs[k] < 0) continue;
                        sum += hs[k];
                        n++;
                        ar += cols[k] >> 16 & 255; ag += cols[k] >> 8 & 255; ab += cols[k] & 255;
                    }
                    best = (sum + n / 2) / n;
                    bt = (int) (ar / n) << 16 | (int) (ag / n) << 8 | (int) (ab / n);
                }
                known[idx] = true;
                if (s < 4 && bg > 0 && bg < best) {
                    canopy[idx] = best;
                    canopyCol[idx] = bt;
                    best = bg;
                    bt = bgc;
                    bs = LodPalette.mix(bgc, 0x6b4a2f, 0.6f);
                }
                height[idx] = best;
                water[idx] = bw;
                top[idx] = bt;
                side[idx] = bs;
            }

        buf = MemoryUtil.memAlloc(64 * 1024);
        quads = 0;
        minY = 1e9f;
        maxY = -1e9f;
        int[] dx = {0, 0, -1, 1}, dz = {-1, 1, 0, 0};
        int[] faces = {2, 3, 4, 5};
        // Opaque columns
        for (int j = 1; j <= N; j++)
            for (int i = 1; i <= N; i++) {
                int idx = j * G + i;
                if (!known[idx] || height[idx] <= 0) continue;
                out.anyKnown = true;
                int h = height[idx];
                int depth = water[idx] - h;
                // Deep water hides the floor completely
                if (depth >= 10) continue;
                float x0 = (i - 1) * s, z0 = (j - 1) * s, x1 = x0 + s, z1 = z0 + s;
                // Valleys and the feet of cliffs are darker (cheap ambient occlusion from the neighbours)
                int higher = 0;
                for (int d = 0; d < 4; d++) {
                    int n = (j + dz[d]) * G + i + dx[d];
                    if (known[n]) higher = Math.max(higher, height[n] - h);
                }
                float ao = 1f - Math.min(0.35f, higher / (float) (6 + 2 * s) * 0.35f);
                int tc = shade(top[idx], ao);
                quad(x0, h, z0, x0, h, z1, x1, h, z1, x1, h, z0, tc, 0);
                for (int d = 0; d < 4; d++) {
                    int ni = i + dx[d], nj = j + dz[d];
                    int n = nj * G + ni;
                    boolean border = ni < 1 || ni > N || nj < 1 || nj > N;
                    int nh = known[n] ? height[n] : 0;
                    // Tile edges get a skirt so neighbouring tiles of other sizes leave no cracks
                    int bottom = border ? Math.min(nh, h - s - 1) : nh;
                    if (bottom >= h) continue;
                    int sc = shade(side[idx], ao * 0.92f);
                    switch (d) {
                        case 0 -> quad(x0, h, z0, x1, h, z0, x1, bottom, z0, x0, bottom, z0, sc, faces[d]);
                        case 1 -> quad(x0, h, z1, x0, bottom, z1, x1, bottom, z1, x1, h, z1, sc, faces[d]);
                        case 2 -> quad(x0, h, z0, x0, bottom, z0, x0, bottom, z1, x0, h, z1, sc, faces[d]);
                        default -> quad(x1, h, z0, x1, h, z1, x1, bottom, z1, x1, bottom, z0, sc, faces[d]);
                    }
                }
            }
        // Tree crowns: a slab of leaves above the ground with an underside and sides
        for (int j = 1; j <= N; j++)
            for (int i = 1; i <= N; i++) {
                int idx = j * G + i;
                int ct = canopy[idx];
                if (ct <= 0) continue;
                int cb = Math.max(height[idx] + 1, ct - 3);
                float x0 = (i - 1) * s, z0 = (j - 1) * s, x1 = x0 + s, z1 = z0 + s;
                int lc = canopyCol[idx];
                quad(x0, ct, z0, x0, ct, z1, x1, ct, z1, x1, ct, z0, lc, 0);
                quad(x0, cb, z0, x1, cb, z0, x1, cb, z1, x0, cb, z1, shade(lc, 0.55f), 1);
                for (int d = 0; d < 4; d++) {
                    int n = (j + dz[d]) * G + i + dx[d];
                    int other = known[n] ? (canopy[n] > 0 ? canopy[n] : height[n]) : 0;
                    if (other >= ct) continue;
                    int bottom = Math.max(cb, other);
                    int sc = shade(lc, 0.82f);
                    switch (d) {
                        case 0 -> quad(x0, ct, z0, x1, ct, z0, x1, bottom, z0, x0, bottom, z0, sc, faces[d]);
                        case 1 -> quad(x0, ct, z1, x0, bottom, z1, x1, bottom, z1, x1, ct, z1, sc, faces[d]);
                        case 2 -> quad(x0, ct, z0, x0, bottom, z0, x0, bottom, z1, x0, ct, z1, sc, faces[d]);
                        default -> quad(x1, ct, z0, x1, ct, z1, x1, bottom, z1, x1, bottom, z0, sc, faces[d]);
                    }
                }
            }
        out.opaqueQuads = quads;
        // Water surfaces, merged along rows of equal level
        for (int j = 1; j <= N; j++) {
            int i = 1;
            while (i <= N) {
                int idx = j * G + i;
                int w = known[idx] ? water[idx] : 0;
                if (w <= 0 || w <= height[idx]) { i++; continue; }
                int depth = Math.min(31, w - height[idx]);
                int start = i;
                while (i + 1 <= N) {
                    int n = j * G + i + 1;
                    int nw = known[n] ? water[n] : 0;
                    if (nw != w || nw <= height[n] || Math.min(31, nw - height[n]) / 4 != depth / 4) break;
                    i++;
                }
                float x0 = (start - 1) * s, x1 = i * s, z0 = (j - 1) * s, z1 = z0 + s, y = w - 0.12f;
                int floor = top[idx];
                // Shallow water shows the colour of the bottom; deep water is dark blue
                int col = LodPalette.mix(LodPalette.multiply(floor, 0x5a8cd8), 0x14305e, Math.min(1f, depth / 12f));
                quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, col, 6 | depth << 3);
                i++;
            }
        }
        out.waterQuads = quads - out.opaqueQuads;
        buf.flip();
        out.data = buf;
        out.minY = minY;
        out.maxY = maxY;
        buf = null;
        return out;
    }
}
