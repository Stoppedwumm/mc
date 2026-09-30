package mc.render;

import mc.world.Block;
import mc.world.Chunk;

/**
 * Builds the mesh for one chunk from a snapshot of the 3x3 chunks around it. Light (sky + block) is
 * flood-filled over the whole 48x48 snapshot first, which captures every light source within 15 blocks
 * of the centre chunk. Then faces are emitted with Minecraft-style smooth lighting and ambient occlusion.
 */
public final class ChunkMesher {
    public static final int W = 48;
    public static final int AREA = W * W;
    public static final int REGION = AREA * Chunk.HEIGHT;

    /** Snapshot handed to a worker. */
    public static final class Job {
        public final int cx, cz;
        public final byte[] region = new byte[REGION];
        public final int[] grass = new int[256], foliage = new int[256];
        public int height;

        public Job(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private static final boolean[] OPAQUE = new boolean[128];
    private static final int[] FILTER = new int[128];
    private static final int[] EMIT = new int[128];

    static {
        for (int i = 0; i < 128; i++) {
            Block b = Block.get(i);
            OPAQUE[i] = b.opaque;
            FILTER[i] = b.opaque ? 15 : b.lightFilter;
            EMIT[i] = b.lightEmission;
        }
    }

    private static final int[][] DIR = {{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    private static final int[][][] CORNER = {
            {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
            {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
            {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
            {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
            {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}},
    };
    private static final float[] FACE_SHADE = {1.0f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f};
    private static final float[] AO_CURVE = {0.42f, 0.6f, 0.8f, 1.0f};

    private final byte[] sky = new byte[REGION];
    private final byte[] blk = new byte[REGION];
    private final int[] queue = new int[1 << 20];
    private final int[] colTop = new int[AREA];

    private byte[] b;
    private int h;

    // Per-face scratch
    private final int[] vSky = new int[4], vBlk = new int[4], vAo = new int[4];

    private static int idx(int x, int y, int z) { return y * AREA + z * W + x; }

    private int block(int x, int y, int z) {
        if (y < 0) return Block.BEDROCK.id;
        if (y >= h) return 0;
        return b[idx(x, y, z)];
    }

    private int skyAt(int x, int y, int z) {
        if (y >= h) return 15;
        if (y < 0) return 0;
        return sky[idx(x, y, z)];
    }

    private int blkAt(int x, int y, int z) {
        if (y >= h || y < 0) return 0;
        return blk[idx(x, y, z)];
    }

    private boolean opaqueAt(int x, int y, int z) {
        return OPAQUE[block(x, y, z)];
    }

    public MeshData build(Job job) {
        this.b = job.region;
        this.h = Math.min(Chunk.HEIGHT, job.height + 2);
        computeLight();

        VertexBuilder solid = new VertexBuilder(4096);
        VertexBuilder translucent = new VertexBuilder(512);
        for (int y = 0; y < Math.min(h, Chunk.HEIGHT); y++) {
            for (int z = 16; z < 32; z++) {
                for (int x = 16; x < 32; x++) {
                    int id = b[idx(x, y, z)];
                    if (id == 0) continue;
                    Block block = Block.get(id);
                    int col = (z - 16) * 16 + (x - 16);
                    // Colour alpha selects the tint mode: 255 tints every pixel, 0 only pixels whose
                    // texture alpha marks them as tintable (grass block overlay)
                    int tint = switch (block.tint) {
                        case GRASS -> block == Block.GRASS ? job.grass[col] : job.grass[col] | 0xFF000000;
                        case FOLIAGE -> job.foliage[col] | 0xFF000000;
                        case BIRCH -> 0xFF80a755;
                        case SPRUCE -> 0xFF619961;
                        default -> 0xFFFFFFFF;
                    };
                    switch (block.model) {
                        case CUBE -> cube(block, x, y, z, tint, block.layer == Block.Layer.TRANSLUCENT ? translucent : solid);
                        case LIQUID -> liquid(block, x, y, z, block == Block.WATER ? translucent : solid);
                        case CROSS -> cross(block, x, y, z, tint, solid);
                        case TORCH -> torch(block, x, y, z, solid);
                        default -> { }
                    }
                }
            }
        }
        MeshData data = new MeshData(job.cx, job.cz,
                solid.isEmpty() ? null : solid.finish(), solid.quads(),
                translucent.isEmpty() ? null : translucent.finish(), translucent.quads());
        solid.free();
        translucent.free();
        return data;
    }

    // ------------------------------------------------------------------ light

    private void computeLight() {
        int H = h;
        int n = H * AREA;
        java.util.Arrays.fill(blk, 0, n, (byte) 0);
        // Sunlight falls straight down until something filters it
        for (int z = 0; z < W; z++) {
            for (int x = 0; x < W; x++) {
                int light = 15;
                int top = -1;
                for (int y = H - 1; y >= 0; y--) {
                    int i = idx(x, y, z);
                    int id = b[i];
                    if (light > 0) {
                        if (OPAQUE[id]) light = 0;
                        else light = Math.max(0, light - FILTER[id]);
                    }
                    sky[i] = (byte) light;
                    if (light < 15 && top < 0) top = y;
                }
                colTop[z * W + x] = top;
            }
        }
        int head = 0, tail = 0;
        int mask = queue.length - 1;
        for (int z = 0; z < W; z++) {
            for (int x = 0; x < W; x++) {
                int maxN = colTop[z * W + x];
                if (x > 0) maxN = Math.max(maxN, colTop[z * W + x - 1]);
                if (x < W - 1) maxN = Math.max(maxN, colTop[z * W + x + 1]);
                if (z > 0) maxN = Math.max(maxN, colTop[(z - 1) * W + x]);
                if (z < W - 1) maxN = Math.max(maxN, colTop[(z + 1) * W + x]);
                for (int y = Math.min(maxN + 1, H - 1); y >= 0; y--) {
                    int i = idx(x, y, z);
                    if (sky[i] > 1) { queue[tail++ & mask] = i; }
                }
            }
        }
        tail = propagate(sky, head, tail);

        head = tail = 0;
        for (int i = 0; i < n; i++) {
            int e = EMIT[b[i]];
            if (e > 0) { blk[i] = (byte) e; queue[tail++ & mask] = i; }
        }
        propagate(blk, head, tail);
    }

    private int propagate(byte[] light, int head, int tail) {
        int mask = queue.length - 1;
        int H = h;
        while (head != tail) {
            int i = queue[head++ & mask];
            int l = light[i];
            if (l <= 1) continue;
            int y = i / AREA, rem = i - y * AREA, z = rem / W, x = rem - z * W;
            for (int d = 0; d < 6; d++) {
                int nx = x, ny = y, nz = z;
                switch (d) {
                    case 0 -> nx--; case 1 -> nx++; case 2 -> nz--; case 3 -> nz++; case 4 -> ny--; default -> ny++;
                }
                if (nx < 0 || nx >= W || nz < 0 || nz >= W || ny < 0 || ny >= H) continue;
                int ni = idx(nx, ny, nz);
                int id = b[ni];
                if (OPAQUE[id]) continue;
                int nl = l - 1 - FILTER[id];
                if (nl > light[ni]) {
                    light[ni] = (byte) nl;
                    queue[tail++ & mask] = ni;
                }
            }
        }
        return tail;
    }

    // ------------------------------------------------------------------ geometry

    private boolean faceVisible(Block self, int face, int x, int y, int z) {
        int[] d = DIR[face];
        int nid = block(x + d[0], y + d[1], z + d[2]);
        if (nid == 0) return true;
        Block n = Block.get(nid);
        if (n.opaque) return false;
        if (self == n && (self.layer == Block.Layer.TRANSLUCENT || self == Block.GLASS)) return false;
        return true;
    }

    /** Smooth light + AO for the four corners of a full face. */
    private void smoothLight(int face, int x, int y, int z) {
        int[] d = DIR[face];
        int bx = x + d[0], by = y + d[1], bz = z + d[2];
        int axis = d[0] != 0 ? 0 : d[1] != 0 ? 1 : 2;
        for (int v = 0; v < 4; v++) {
            int[] c = CORNER[face][v];
            int s1x = 0, s1y = 0, s1z = 0, s2x = 0, s2y = 0, s2z = 0;
            if (axis == 0) { s1y = c[1] == 1 ? 1 : -1; s2z = c[2] == 1 ? 1 : -1; }
            else if (axis == 1) { s1x = c[0] == 1 ? 1 : -1; s2z = c[2] == 1 ? 1 : -1; }
            else { s1x = c[0] == 1 ? 1 : -1; s2y = c[1] == 1 ? 1 : -1; }
            boolean o1 = opaqueAt(bx + s1x, by + s1y, bz + s1z);
            boolean o2 = opaqueAt(bx + s2x, by + s2y, bz + s2z);
            boolean oc = opaqueAt(bx + s1x + s2x, by + s1y + s2y, bz + s1z + s2z);
            vAo[v] = (o1 && o2) ? 0 : 3 - ((o1 ? 1 : 0) + (o2 ? 1 : 0) + (oc ? 1 : 0));

            int sSum = skyAt(bx, by, bz), bSum = blkAt(bx, by, bz), cnt = 1;
            if (!o1) { sSum += skyAt(bx + s1x, by + s1y, bz + s1z); bSum += blkAt(bx + s1x, by + s1y, bz + s1z); cnt++; }
            if (!o2) { sSum += skyAt(bx + s2x, by + s2y, bz + s2z); bSum += blkAt(bx + s2x, by + s2y, bz + s2z); cnt++; }
            if (!oc && !(o1 && o2)) { sSum += skyAt(bx + s1x + s2x, by + s1y + s2y, bz + s1z + s2z); bSum += blkAt(bx + s1x + s2x, by + s1y + s2y, bz + s1z + s2z); cnt++; }
            vSky[v] = sSum * 16 / cnt;
            vBlk[v] = bSum * 16 / cnt;
        }
    }

    private void flatLight(int x, int y, int z) {
        int s = skyAt(x, y, z) * 16, bl = blkAt(x, y, z) * 16;
        for (int v = 0; v < 4; v++) { vSky[v] = s; vBlk[v] = bl; vAo[v] = 3; }
    }

    /**
     * Emits one face of the box [x0..x1]x[y0..y1]x[z0..z1] (in 1/16 block units, relative to the block).
     * UVs are derived from box coordinates like Minecraft's block models.
     */
    private void emit(VertexBuilder out, int face, int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1,
                      int tex, int tint, boolean ao, int uOff, int vOff) {
        int lx = (x - 16) * 16, ly = y * 16, lz = (z - 16) * 16;
        int tu = (tex & 15) * 16 * 16, tv = (tex >> 4) * 16 * 16;
        float fs = FACE_SHADE[face];
        boolean flip = ao && vAo[0] + vAo[2] < vAo[1] + vAo[3];
        for (int k = 0; k < 4; k++) {
            int v = flip ? (k + 1) & 3 : k;
            int[] c = CORNER[face][v];
            int px = c[0] == 1 ? x1 : x0, py = c[1] == 1 ? y1 : y0, pz = c[2] == 1 ? z1 : z0;
            int u, w;
            switch (face) {
                case 0, 1 -> { u = px; w = pz; }
                case 2 -> { u = 16 - px; w = 16 - py; }
                case 3 -> { u = px; w = 16 - py; }
                case 4 -> { u = pz; w = 16 - py; }
                default -> { u = 16 - pz; w = 16 - py; }
            }
            u += uOff; w += vOff;
            int shade = (int) (255 * fs * (ao ? AO_CURVE[vAo[v]] : 1f));
            out.vertex(lx + px, ly + py, lz + pz, tu + inset(u * 16), tv + inset(w * 16), vSky[v], vBlk[v], shade, tint);
        }
    }

    /** Keeps UVs 1/16 texel inside the tile so mipmapped sampling never bleeds into the neighbouring tile. */
    private static int inset(int t) {
        return t < 1 ? 1 : Math.min(t, 255);
    }

    private void cube(Block block, int x, int y, int z, int tint, VertexBuilder out) {
        for (int f = 0; f < 6; f++) {
            if (!faceVisible(block, f, x, y, z)) continue;
            smoothLight(f, x, y, z);
            int faceTint = tint;
            emit(out, f, x, y, z, 0, 0, 0, 16, 16, 16, block.textureForFace(f), faceTint, true, 0, 0);
        }
    }

    private void liquid(Block block, int x, int y, int z, VertexBuilder out) {
        boolean sameAbove = block(x, y + 1, z) == block.id;
        int top = sameAbove ? 16 : 14;
        for (int f = 0; f < 6; f++) {
            int[] d = DIR[f];
            int nid = block(x + d[0], y + d[1], z + d[2]);
            if (nid == block.id) continue;
            Block n = Block.get(nid);
            if (f != 0 && n.opaque) continue;
            if (f == 0 && sameAbove) continue;
            if (block == Block.WATER && nid == Block.ICE.id) continue;
            if (f == 0) {
                // The lowered surface is lit from the cell it sits in (or above)
                smoothLight(f, x, y, z);
                for (int v = 0; v < 4; v++) vAo[v] = 3;
            } else {
                smoothLight(f, x, y, z);
                for (int v = 0; v < 4; v++) vAo[v] = Math.max(vAo[v], 2);
            }
            emit(out, f, x, y, z, 0, 0, 0, 16, top, 16, block.texTop, 0xFFFFFF, f != 0, 0, 0);
        }
    }

    private void cross(Block block, int x, int y, int z, int tint, VertexBuilder out) {
        flatLight(x, y, z);
        int lx = (x - 16) * 16, ly = y * 16, lz = (z - 16) * 16;
        int tex = block.texSide;
        int tu = (tex & 15) * 256 + 1, tv = (tex >> 4) * 256 + 1;
        int s = vSky[0], bl = vBlk[0];
        int shade = 235;
        // Slight random offset for grass so fields look less regular
        int ox = 0, oz = 0;
        if (block == Block.TALL_GRASS || block == Block.FERN) {
            int hsh = (x + 31 * z) * 1103515245 + y * 12345;
            ox = ((hsh >> 8) & 3) - 1;
            oz = ((hsh >> 12) & 3) - 1;
        }
        int a = 1, bb = 15;
        int[][] planes = {{a, a, bb, bb}, {a, bb, bb, a}};
        for (int[] p : planes) {
            int x0 = lx + p[0] + ox, z0 = lz + p[1] + oz, x1 = lx + p[2] + ox, z1 = lz + p[3] + oz;
            // front
            out.vertex(x0, ly + 16, z0, tu, tv, s, bl, shade, tint);
            out.vertex(x0, ly, z0, tu, tv + 254, s, bl, shade, tint);
            out.vertex(x1, ly, z1, tu + 254, tv + 254, s, bl, shade, tint);
            out.vertex(x1, ly + 16, z1, tu + 254, tv, s, bl, shade, tint);
            // back
            out.vertex(x1, ly + 16, z1, tu + 254, tv, s, bl, shade, tint);
            out.vertex(x1, ly, z1, tu + 254, tv + 254, s, bl, shade, tint);
            out.vertex(x0, ly, z0, tu, tv + 254, s, bl, shade, tint);
            out.vertex(x0, ly + 16, z0, tu, tv, s, bl, shade, tint);
        }
    }

    private void torch(Block block, int x, int y, int z, VertexBuilder out) {
        flatLight(x, y, z);
        for (int v = 0; v < 4; v++) vBlk[v] = 15 * 16;
        int tex = block.texSide;
        for (int f = 0; f < 6; f++) {
            if (f == 0) emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, -1);
            else if (f == 1) emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, 7);
            else emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, 0);
        }
    }
}
