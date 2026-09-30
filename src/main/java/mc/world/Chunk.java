package mc.world;

import mc.render.ChunkMesh;

/** A 16x256x16 column of blocks. */
public final class Chunk {
    public static final int SIZE = 16, HEIGHT = 256, VOLUME = SIZE * SIZE * HEIGHT;

    /** Generation stages. */
    public static final int STATE_TERRAIN = 1, STATE_DECORATED = 2;

    public final int cx, cz;
    public final byte[] blocks;
    /** Per-block metadata: liquid level, crop age, facing... */
    public final byte[] meta;
    /** Per-column biome tint colours (0xRRGGBB). */
    public final int[] grassColor = new int[SIZE * SIZE];
    public final int[] foliageColor = new int[SIZE * SIZE];
    public final byte[] biome = new byte[SIZE * SIZE];

    public volatile int state;
    /** Modified since it was generated or loaded; must be written to disk on unload. */
    public boolean touched;
    /** Contains player edits; used to prioritise saves. */
    public boolean needsMesh = true;
    public boolean meshInFlight;
    public boolean urgentMesh;
    public ChunkMesh mesh;
    /** Light computed by the last mesh build: (sky << 4 | block), lightHeight layers; above that is open sky. */
    public byte[] light;
    public int lightHeight;
    /** Highest non-air y + 1 in this chunk. */
    public int maxY;
    /** Saved entities read by the loader thread, spawned on the main thread. */
    public volatile String entityJson;
    /** Whether an entity file exists on disk for this chunk (so an empty save must clear it). */
    public boolean hasEntityFile;

    public Chunk(int cx, int cz) {
        this(cx, cz, new byte[VOLUME]);
    }

    public Chunk(int cx, int cz, byte[] blocks) {
        this(cx, cz, blocks, new byte[VOLUME]);
    }

    public Chunk(int cx, int cz, byte[] blocks, byte[] meta) {
        this.cx = cx;
        this.cz = cz;
        this.blocks = blocks;
        this.meta = meta;
    }

    public static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public int get(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) return 0;
        return blocks[(y << 8) | (z << 4) | x] & 255;
    }

    public void set(int x, int y, int z, int id) {
        set(x, y, z, id, 0);
    }

    public void set(int x, int y, int z, int id, int m) {
        if (y < 0 || y >= HEIGHT) return;
        int i = (y << 8) | (z << 4) | x;
        blocks[i] = (byte) id;
        meta[i] = (byte) m;
        if (id != 0 && y + 1 > maxY) maxY = y + 1;
    }

    public int getMeta(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) return 0;
        return meta[(y << 8) | (z << 4) | x];
    }

    public void recomputeMaxY() {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            int base = y << 8;
            for (int i = 0; i < 256; i++) {
                if (blocks[base + i] != 0) { maxY = y + 1; return; }
            }
        }
        maxY = 0;
    }

    /** y of the highest block that is neither air nor a non-solid plant, or -1. */
    public int topSolid(int x, int z) {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            int id = blocks[(y << 8) | (z << 4) | x] & 255;
            if (id != 0 && Block.get(id).solid) return y;
        }
        return -1;
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    public long key() { return key(cx, cz); }
}
