package mc.render;

import mc.world.Block;
import mc.world.Chunk;
import mc.world.Shapes;

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
        public final byte[] meta = new byte[REGION];
        public final int[] grass = new int[256], foliage = new int[256];
        public int height;
        /** Only compute light (dedicated server: no geometry). */
        public boolean lightOnly;

        public Job(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private static final boolean[] OPAQUE = new boolean[256];
    private static final int[] FILTER = new int[256];
    private static final int[] EMIT = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
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
    /** Per-vertex flag bits (bits 0-2 hold the face index, 6 = no fixed normal). */
    public static final int F_LEAVES = 8, F_PLANT = 16, F_WATER = 32, F_EMISSIVE = 64;
    private static final float[] AO_CURVE = {0.42f, 0.6f, 0.8f, 1.0f};

    private final byte[] sky = new byte[REGION];
    private final byte[] blk = new byte[REGION];
    private final int[] queue = new int[1 << 20];
    private final int[] colTop = new int[AREA];

    private byte[] b, m;
    private int h;

    // Per-face scratch
    private final int[] vSky = new int[4], vBlk = new int[4], vAo = new int[4];

    private static int idx(int x, int y, int z) { return y * AREA + z * W + x; }

    private int block(int x, int y, int z) {
        if (y < 0) return Block.BEDROCK.id;
        if (y >= h) return 0;
        return b[idx(x, y, z)] & 255;
    }

    /** Neighbour access for shape connections (fences, panes, walls). */
    private final Shapes.Getter getter = new Shapes.Getter() {
        public int getBlock(int x, int y, int z) {
            if (x < 0 || z < 0 || x >= W || z >= W) return 0;
            return block(x, y, z);
        }

        public int getMeta(int x, int y, int z) {
            if (x < 0 || z < 0 || x >= W || z >= W) return 0;
            return metaAt(x, y, z);
        }
    };

    private int b0Meta(int x, int y, int z) { return metaAt(x, y, z) & 15; }

    /** Redstone wire colour for power 0-15 (dark red to bright red), like Minecraft. */
    public static int wireColor(int power) {
        float f = power / 15f;
        int r = (int) ((f * 0.6f + (power > 0 ? 0.4f : 0.3f)) * 255);
        int g = (int) (Math.max(0, f * f * 0.7f - 0.5f) * 255);
        int b = (int) (Math.max(0, f * f * 0.6f - 0.7f) * 255);
        return 0xFF000000 | Math.min(255, r) << 16 | Math.max(0, g) << 8 | Math.max(0, b);
    }

    private int metaAt(int x, int y, int z) {
        if (y < 0 || y >= h) return 0;
        return m[idx(x, y, z)];
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
        this.m = job.meta;
        this.h = Math.min(Chunk.HEIGHT, job.height + 2);
        computeLight();
        if (job.lightOnly) {
            MeshData data = new MeshData(job.cx, job.cz, null, 0, null, 0);
            exportLight(data);
            return data;
        }

        VertexBuilder solid = new VertexBuilder(4096);
        VertexBuilder translucent = new VertexBuilder(512);
        for (int y = 0; y < Math.min(h, Chunk.HEIGHT); y++) {
            for (int z = 16; z < 32; z++) {
                for (int x = 16; x < 32; x++) {
                    int id = b[idx(x, y, z)] & 255;
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
                    if (block == Block.REDSTONE_WIRE) tint = wireColor(b0Meta(x, y, z));
                    switch (block.model) {
                        case CUBE -> cube(block, x, y, z, tint, block.layer == Block.Layer.TRANSLUCENT ? translucent : solid);
                        case LIQUID -> liquid(block, x, y, z, block == Block.WATER ? translucent : solid);
                        case CROSS -> cross(block, x, y, z, tint, solid);
                        case TORCH -> torch(block, x, y, z, solid);
                        case SHAPE -> shaped(block, x, y, z, tint, block.layer == Block.Layer.TRANSLUCENT ? translucent : solid);
                        default -> { }
                    }
                }
            }
        }
        MeshData data = new MeshData(job.cx, job.cz,
                solid.isEmpty() ? null : solid.finish(), solid.quads(),
                translucent.isEmpty() ? null : translucent.finish(), translucent.quads());
        exportLight(data);
        solid.free();
        translucent.free();
        return data;
    }

    /** Copies the centre chunk's light out for gameplay (spawning, entity brightness). */
    private void exportLight(MeshData data) {
        int lh = Math.min(h, Chunk.HEIGHT);
        byte[] light = new byte[lh * 256];
        for (int y = 0; y < lh; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    int i = idx(x + 16, y, z + 16);
                    light[(y << 8) | (z << 4) | x] = (byte) (sky[i] << 4 | blk[i]);
                }
        data.light = light;
        data.lightHeight = lh;
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
                    int id = b[i] & 255;
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
            int e = EMIT[b[i] & 255];
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
                int id = b[ni] & 255;
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
        if (self == n && (self.layer == Block.Layer.TRANSLUCENT || self == Block.GLASS || self.shape == Block.Shape.PANE)) return false;
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
                      int tex, int tint, boolean ao, int uOff, int vOff, int flags) {
        int lx = (x - 16) * 16, ly = y * 16, lz = (z - 16) * 16;
        int tu = (tex & 15) * 16 * 16, tv = (tex >> 4) * 16 * 16;
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
            int shade = (int) (255 * (ao ? AO_CURVE[vAo[v]] : 1f));
            if (c[1] == 1) { px += shearX; pz += shearZ; }
            px += offX; py += offY; pz += offZ;
            out.vertex(lx + px, ly + py, lz + pz, tu + inset(u * 16), tv + inset(w * 16), vSky[v], vBlk[v], shade, tint, flags | face);
        }
    }

    /** Vertex offsets applied by emit (wall torches); top vertices are additionally sheared. */
    private int offX, offY, offZ, shearX, shearZ;

    /** Keeps UVs 1/16 texel inside the tile so mipmapped sampling never bleeds into the neighbouring tile. */
    private static int inset(int t) {
        return t < 1 ? 1 : Math.min(t, 255);
    }

    private void cube(Block block, int x, int y, int z, int tint, VertexBuilder out) {
        for (int f = 0; f < 6; f++) {
            if (!faceVisible(block, f, x, y, z)) continue;
            smoothLight(f, x, y, z);
            int faceTint = tint;
            int flags = block.tint == Block.Tint.FOLIAGE || block.tint == Block.Tint.BIRCH || block.tint == Block.Tint.SPRUCE ? F_LEAVES : 0;
            if (block.lightEmission > 0) flags |= F_EMISSIVE;
            emit(out, f, x, y, z, 0, 0, 0, 16, 16, 16, block.textureForFace(f, metaAt(x, y, z)), faceTint, true, 0, 0, flags);
        }
    }

    /** Surface height (in 1/16 block) of a liquid column at a block, or -1 if not this liquid. */
    private float liquidLevel(int x, int y, int z, int id) {
        if (block(x, y, z) != id) return -1;
        if (block(x, y + 1, z) == id) return 16;
        int meta = metaAt(x, y, z);
        if ((meta & 8) != 0) return 14.5f;
        return 16f * (8 - (meta & 7)) / 9f;
    }

    /** Minecraft-style corner height: weighted average of the four columns sharing the corner. */
    private int cornerHeight(int x, int y, int z, int cx, int cz, int id) {
        float sum = 0;
        int weight = 0;
        for (int dx = cx - 1; dx <= cx; dx++)
            for (int dz = cz - 1; dz <= cz; dz++) {
                int px = x + dx, pz = z + dz;
                if (block(px, y + 1, pz) == id) return 16;
                float lv = liquidLevel(px, y, pz, id);
                if (lv >= 0) {
                    int w = (metaAt(px, y, pz) & 15) == 0 ? 10 : 1;
                    sum += lv * w;
                    weight += w;
                } else if (!Block.get(block(px, y, pz)).solid) {
                    weight++;
                }
            }
        return weight == 0 ? 14 : Math.max(1, Math.round(sum / weight));
    }

    private void liquid(Block block, int x, int y, int z, VertexBuilder out) {
        int id = block.id;
        boolean sameAbove = block(x, y + 1, z) == id;
        int h00, h10, h11, h01;
        if (sameAbove) h00 = h10 = h11 = h01 = 16;
        else {
            h00 = cornerHeight(x, y, z, 0, 0, id);
            h10 = cornerHeight(x, y, z, 1, 0, id);
            h11 = cornerHeight(x, y, z, 1, 1, id);
            h01 = cornerHeight(x, y, z, 0, 1, id);
        }
        int flag = block == Block.WATER ? F_WATER : F_EMISSIVE;
        VertexBuilder o = out;
        int tex = block.texTop;
        int tu = (tex & 15) * 256, tv = (tex >> 4) * 256;
        int lx = (x - 16) * 16, ly = y * 16, lz = (z - 16) * 16;
        for (int f = 0; f < 6; f++) {
            int[] d = DIR[f];
            int nid = block(x + d[0], y + d[1], z + d[2]);
            if (nid == id) continue;
            Block n = Block.get(nid);
            if (f != 0 && n.opaque) continue;
            if (f == 0 && sameAbove) continue;
            if (block == Block.WATER && nid == Block.ICE.id) continue;
            smoothLight(f, x, y, z);
            int[] hs;
            switch (f) {
                case 0 -> hs = new int[]{h00, h01, h11, h10};
                case 1 -> hs = new int[]{0, 0, 0, 0};
                case 2 -> hs = new int[]{h10, 0, 0, h00};
                case 3 -> hs = new int[]{h01, 0, 0, h11};
                case 4 -> hs = new int[]{h00, 0, 0, h01};
                default -> hs = new int[]{h11, 0, 0, h10};
            }
            for (int k = 0; k < 4; k++) {
                int[] c = CORNER[f][k];
                int px = c[0] * 16, pz = c[2] * 16;
                int py = f == 1 ? 0 : (c[1] == 1 ? hs[k] : 0);
                int u, w;
                if (f <= 1) { u = px; w = pz; }
                else {
                    u = (f == 2 || f == 5) ? 16 - (f == 2 ? px : pz) : (f == 3 ? px : pz);
                    w = 16 - py;
                }
                int ao = f == 0 ? 255 : (int) (255 * AO_CURVE[Math.max(vAo[k], 2)]);
                o.vertex(lx + px, ly + py, lz + pz, tu + inset(u * 16), tv + inset(w * 16), vSky[k], vBlk[k], ao, 0xFFFFFFFF, flag | f);
            }
        }
    }

    private void cross(Block block, int x, int y, int z, int tint, VertexBuilder out) {
        flatLight(x, y, z);
        int lx = (x - 16) * 16, ly = y * 16, lz = (z - 16) * 16;
        int tex = block.textureForFace(2, metaAt(x, y, z));
        int tu = (tex & 15) * 256 + 1, tv = (tex >> 4) * 256 + 1;
        int s = vSky[0], bl = vBlk[0];
        int shade = 255;
        int wave = block == Block.SUGAR_CANE || block == Block.FIRE ? 0 : F_PLANT;
        int fl = 6 | (block.lightEmission > 0 ? F_EMISSIVE : 0);
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
            out.vertex(x0, ly + 16, z0, tu, tv, s, bl, shade, tint, fl | wave);
            out.vertex(x0, ly, z0, tu, tv + 254, s, bl, shade, tint, fl);
            out.vertex(x1, ly, z1, tu + 254, tv + 254, s, bl, shade, tint, fl);
            out.vertex(x1, ly + 16, z1, tu + 254, tv, s, bl, shade, tint, fl | wave);
            // back
            out.vertex(x1, ly + 16, z1, tu + 254, tv, s, bl, shade, tint, fl | wave);
            out.vertex(x1, ly, z1, tu + 254, tv + 254, s, bl, shade, tint, fl);
            out.vertex(x0, ly, z0, tu, tv + 254, s, bl, shade, tint, fl);
            out.vertex(x0, ly + 16, z0, tu, tv, s, bl, shade, tint, fl | wave);
        }
    }

    private void torch(Block block, int x, int y, int z, VertexBuilder out) {
        flatLight(x, y, z);
        boolean lit = block.lightEmission > 0;
        if (lit) for (int v = 0; v < 4; v++) vBlk[v] = 15 * 16;
        int tex = block.texSide;
        int meta = metaAt(x, y, z);
        if (meta >= 1 && meta <= 4) {
            int wall = meta - 1;
            offX = Shapes.DX[wall] * 6; offZ = Shapes.DZ[wall] * 6; offY = 3;
            shearX = -Shapes.DX[wall] * 4; shearZ = -Shapes.DZ[wall] * 4;
        }
        for (int f = 0; f < 6; f++) {
            if (f == 0) emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, -1, lit ? F_EMISSIVE : 0);
            else if (f == 1) emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, 7, 0);
            else emit(out, f, x, y, z, 7, 0, 7, 9, 10, 9, tex, 0xFFFFFF, false, 0, 0, 0);
        }
        offX = offY = offZ = shearX = shearZ = 0;
    }

    /** Multi-part shapes whose parts use different textures (repeater torches, lever handle, piston arm). */
    public static int boxTexture(Block block, int face, int meta, int boxIndex) {
        if (block.shape == Block.Shape.REPEATER && boxIndex > 0)
            return block == Block.POWERED_REPEATER ? Block.Tex.REDSTONE_TORCH_ON : Block.Tex.REDSTONE_TORCH_OFF;
        if (block == Block.BREWING_STAND) return boxIndex == 0 ? Block.Tex.BREWING_ROD : Block.Tex.BREWING_BASE;
        if (block == Block.LEVER) return boxIndex == 0 ? Block.Tex.COBBLE : Block.Tex.LOG_SIDE;
        if ((block.shape == Block.Shape.PISTON && boxIndex == 1) || (block == Block.PISTON_HEAD && boxIndex == 1)) return Block.Tex.PLANKS;
        return block.textureForFace(face, meta);
    }

    private static boolean onBoundary(int face, int[] bx) {
        return switch (face) {
            case 0 -> bx[4] == 16;
            case 1 -> bx[1] == 0;
            case 2 -> bx[2] == 0;
            case 3 -> bx[5] == 16;
            case 4 -> bx[0] == 0;
            default -> bx[3] == 16;
        };
    }

    /** Slabs, stairs, fences, doors...: every box of the shape, with faces on the cell boundary culled like cubes. */
    private void shaped(Block block, int x, int y, int z, int tint, VertexBuilder out) {
        int meta = metaAt(x, y, z);
        int boxIndex = -1;
        for (int[] bx : Shapes.boxes(block, meta, getter, x, y, z, Shapes.Mode.RENDER)) {
            boxIndex++;
            for (int f = 0; f < 6; f++) {
                boolean boundary = onBoundary(f, bx);
                if (boundary) {
                    if (!faceVisible(block, f, x, y, z)) continue;
                    smoothLight(f, x, y, z);
                } else {
                    flatLight(x, y, z);
                }
                emit(out, f, x, y, z, bx[0], bx[1], bx[2], bx[3], bx[4], bx[5], boxTexture(block, f, meta, boxIndex), tint, boundary, 0, 0,
                        block.lightEmission > 0 ? F_EMISSIVE : 0);
            }
        }
    }
}
