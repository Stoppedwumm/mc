package mc.render.lod;

import mc.world.Block;
import mc.world.Chunk;
import mc.world.gen.Biome;
import mc.world.gen.TerrainGenerator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Surface columns for distant terrain. Like Voxy, every chunk that has been loaded is summarised (top block,
 * height, water) and kept on disk, so places you have seen look right from far away; like Distant Horizons'
 * fast generator mode, columns nobody has visited are estimated straight from the terrain noise, including
 * approximate tree canopies. Safe to read from worker threads.
 */
public final class LodData {
    /** Values stored per column: height | water << 9 | ground under a canopy << 18, top, side and ground colours. */
    static final int STRIDE = 4;
    private static final int FILE_VERSION = 2;
    /** Per chunk: 256 columns x STRIDE ints. */
    private final Map<Long, int[]> chunks = new ConcurrentHashMap<>();
    private final Set<Long> loadedRegions = ConcurrentHashMap.newKeySet();
    private final Set<Long> dirtyRegions = ConcurrentHashMap.newKeySet();
    private final TerrainGenerator gen;
    private final LodPalette palette;
    private final Path dir;
    private final boolean estimate;

    /** dir may be null (multiplayer, panorama): nothing is saved. estimate=false only uses real chunks. */
    public LodData(TerrainGenerator gen, LodPalette palette, Path dir, boolean estimate) {
        this.gen = gen;
        this.palette = palette;
        this.dir = dir;
        this.estimate = estimate;
    }

    private static long regionKey(int rx, int rz) {
        return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------ real chunks

    /** Summarises a loaded chunk's surface (called on the main thread whenever its mesh is rebuilt). */
    public void capture(Chunk c) {
        int[] d = new int[256 * STRIDE];
        char[] blocks = c.blocks;
        int top0 = Math.min(Chunk.HEIGHT - 1, Math.max(c.maxY, 1));
        for (int z = 0; z < 16; z++)
            for (int x = 0; x < 16; x++) {
                int col = z * 16 + x;
                int water = 0, h = 0, id = 0;
                for (int y = top0; y >= 0; y--) {
                    int b = blocks[Chunk.index(x, y, z)];
                    if (b == 0) continue;
                    if (b == Block.WATER.id) {
                        if (water == 0) water = y + 1;
                        continue;
                    }
                    Block bl = Block.get(b);
                    // Flowers, grass, torches and other small things don't show from afar
                    if (bl.model == Block.Model.CROSS || bl.model == Block.Model.TORCH || bl.model == Block.Model.NONE) continue;
                    if (bl.shape == Block.Shape.WIRE || bl.shape == Block.Shape.RAIL || bl.shape == Block.Shape.PLATE
                            || bl.shape == Block.Shape.BUTTON || bl.shape == Block.Shape.LEVER) continue;
                    h = y + 1;
                    id = b;
                    break;
                }
                // Under tree crowns, also remember the ground (distant trees float above it instead of being pillars)
                int ground = 0, groundId = 0;
                if (isFoliage(id)) {
                    for (int y = h - 2; y >= 0; y--) {
                        int b = blocks[Chunk.index(x, y, z)];
                        if (b == 0 || isFoliage(b) || isLog(b)) continue;
                        Block bl = Block.get(b);
                        if (bl.model == Block.Model.CROSS || bl.model == Block.Model.TORCH) continue;
                        ground = y + 1;
                        groundId = b;
                        break;
                    }
                }
                int grass = c.grassColor[col], foliage = c.foliageColor[col];
                d[col * STRIDE] = h | water << 9 | ground << 18;
                d[col * STRIDE + 1] = id == 0 ? 0 : palette.top(id, grass, foliage);
                d[col * STRIDE + 2] = id == 0 ? 0 : palette.side(id, grass, foliage);
                d[col * STRIDE + 3] = groundId == 0 ? 0 : palette.top(groundId, grass, foliage);
            }
        chunks.put(Chunk.key(c.cx, c.cz), d);
        int rx = c.cx >> 5, rz = c.cz >> 5;
        ensureRegion(rx, rz);
        dirtyRegions.add(regionKey(rx, rz));
    }

    private static boolean isFoliage(int id) {
        return id == Block.OAK_LEAVES.id || id == Block.BIRCH_LEAVES.id || id == Block.SPRUCE_LEAVES.id;
    }

    private static boolean isLog(int id) {
        return id == Block.OAK_LOG.id || id == Block.BIRCH_LOG.id || id == Block.SPRUCE_LOG.id;
    }

    public boolean hasChunk(int cx, int cz) {
        ensureRegion(cx >> 5, cz >> 5);
        return chunks.containsKey(Chunk.key(cx, cz));
    }

    private void ensureRegion(int rx, int rz) {
        long k = regionKey(rx, rz);
        if (dir == null || loadedRegions.contains(k)) return;
        synchronized (this) {
            if (!loadedRegions.add(k)) return;
            Path f = dir.resolve("r." + rx + "." + rz + ".lod");
            if (!Files.exists(f)) return;
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(f)))) {
                if (in.readInt() != FILE_VERSION) return;
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    int cx = in.readInt(), cz = in.readInt();
                    int[] d = new int[256 * STRIDE];
                    for (int j = 0; j < d.length; j++) d[j] = in.readInt();
                    chunks.putIfAbsent(Chunk.key(cx, cz), d);
                }
            } catch (IOException e) {
                System.err.println("Could not read LOD region " + f.getFileName() + ": " + e);
            }
        }
    }

    /** Writes regions with new chunk summaries. */
    public void save() {
        if (dir == null || dirtyRegions.isEmpty()) return;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            return;
        }
        for (Long k : dirtyRegions.toArray(new Long[0])) {
            dirtyRegions.remove(k);
            int rx = (int) (k >> 32), rz = (int) (long) k;
            Path f = dir.resolve("r." + rx + "." + rz + ".lod");
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(f)))) {
                int n = 0;
                for (int cx = rx * 32; cx < rx * 32 + 32; cx++)
                    for (int cz = rz * 32; cz < rz * 32 + 32; cz++) if (chunks.containsKey(Chunk.key(cx, cz))) n++;
                out.writeInt(FILE_VERSION);
                out.writeInt(n);
                for (int cx = rx * 32; cx < rx * 32 + 32; cx++)
                    for (int cz = rz * 32; cz < rz * 32 + 32; cz++) {
                        int[] d = chunks.get(Chunk.key(cx, cz));
                        if (d == null) continue;
                        out.writeInt(cx);
                        out.writeInt(cz);
                        for (int v : d) out.writeInt(v);
                    }
            } catch (IOException e) {
                System.err.println("Could not save LOD region " + f.getFileName() + ": " + e);
            }
        }
    }

    // ------------------------------------------------------------------ sampling

    /**
     * Surface at a block column: out[0] surface height (top block y + 1, 0 = nothing), out[1] water surface
     * height (0 = dry), out[2] top colour, out[3] side colour, out[4] ground height under a tree crown (0 = none),
     * out[5] ground colour. Returns false if nothing is known there.
     */
    public boolean sample(int x, int z, int[] out, double[] tmp) {
        return sample(x, z, 1, out, tmp);
    }

    /** As sample(), for a cell `cellSize` blocks wide (big cells blend tree canopies instead of single trees). */
    public boolean sample(int x, int z, int cellSize, int[] out, double[] tmp) {
        int cx = x >> 4, cz = z >> 4;
        ensureRegion(cx >> 5, cz >> 5);
        int[] d = chunks.get(Chunk.key(cx, cz));
        if (d != null) {
            int col = ((z & 15) * 16 + (x & 15)) * STRIDE;
            out[0] = d[col] & 511;
            out[1] = d[col] >> 9 & 511;
            out[2] = d[col + 1];
            out[3] = d[col + 2];
            out[4] = d[col] >> 18 & 511;
            out[5] = d[col + 3];
            return true;
        }
        if (!estimate) return false;
        estimate(x, z, cellSize, out, tmp);
        return true;
    }

    /** Biome, surface block and height straight from the noise, without generating the chunk. */
    void estimate(int x, int z, int cellSize, int[] out, double[] p) {
        gen.columnParams(x, z, p);
        int h = (int) p[0];
        double temp = gen.temperature01(x, z), hum = gen.humidity01(x, z);
        double tAdj = Math.max(0, Math.min(1, temp - Math.max(0, h - 90) * 0.006));
        Biome biome = gen.biomeAt(tAdj, hum, h, p[2], p[3]);
        int grass = TerrainGenerator.grassColor(tAdj, hum, biome), foliage = TerrainGenerator.foliageColor(tAdj, hum, biome);
        int sea = TerrainGenerator.SEA_LEVEL;
        boolean underwater = h < sea;
        out[4] = 0;
        Block top, side;
        switch (biome) {
            case DESERT, BEACH -> { top = Block.SAND; side = Block.SAND; }
            case BADLANDS -> { top = Block.TERRACOTTA; side = Block.TERRACOTTA; }
            case SNOWY_TAIGA -> { top = Block.SNOWY_GRASS; side = Block.DIRT; }
            case SNOWY_PEAKS -> { top = Block.SNOW; side = Block.STONE; }
            case MOUNTAINS -> {
                boolean rocky = p[2] > 0.55;
                top = h > 150 ? Block.SNOW : rocky ? Block.STONE : Block.GRASS;
                side = Block.STONE;
            }
            default -> { top = Block.GRASS; side = Block.DIRT; }
        }
        if (underwater) {
            top = h > sea - 6 ? Block.SAND : Block.GRAVEL;
            side = top;
        }
        int topCol = palette.top(top.id, grass, foliage), sideCol = palette.side(side.id, grass, foliage);
        if (top == Block.GRASS) sideCol = palette.side(Block.DIRT.id, grass, foliage);
        int water = 0;
        if (underwater) water = sea + 1;
        // Trees on land
        if (!underwater && h >= sea) {
            int leaves = biome == Biome.BIRCH_FOREST ? Block.BIRCH_LEAVES.id
                    : biome == Biome.TAIGA || biome == Biome.SNOWY_TAIGA ? Block.SPRUCE_LEAVES.id : Block.OAK_LEAVES.id;
            int leafCol = palette.top(leaves, grass, foliage);
            if (biome == Biome.SNOWY_TAIGA) leafCol = LodPalette.mix(leafCol, 0xf0f4f8, 0.55f);
            out[4] = 0;
            if (cellSize < 4) {
                int canopy = canopy(x, z, biome);
                if (canopy > 0) {
                    out[4] = h + 1;
                    out[5] = topCol;
                    h += canopy;
                    topCol = leafCol;
                    sideCol = LodPalette.mix(leafCol, 0x000000, 0.15f);
                }
            } else {
                // Big cells: a forest is a raised green layer covering part of the ground
                float cover = coverage(biome);
                if (cover > 0) {
                    h += Math.round(cover * 5);
                    topCol = LodPalette.mix(topCol, leafCol, Math.min(1f, cover * 1.15f));
                    if (cover > 0.5f) sideCol = LodPalette.mix(leafCol, 0x000000, 0.2f);
                }
            }
        }
        if (biome == Biome.FROZEN_OCEAN && water > 0) {
            h = water;
            water = 0;
            topCol = palette.top(Block.ICE.id, grass, foliage);
            sideCol = topCol;
        }
        out[0] = h + 1;
        out[1] = water;
        out[2] = topCol;
        out[3] = sideCol;
    }

    /** Fraction of the ground covered by tree crowns in a biome. */
    static float coverage(Biome biome) {
        return switch (biome) {
            case FOREST, BIRCH_FOREST -> 0.75f;
            case TAIGA -> 0.65f;
            case SNOWY_TAIGA -> 0.45f;
            case PLAINS -> 0.04f;
            case MOUNTAINS -> 0.15f;
            default -> 0f;
        };
    }

    /** Height of the tree canopy above the ground at a column (0 if no tree), from a jittered grid per biome. */
    static int canopy(int x, int z, Biome biome) {
        int spacing;
        float chance;
        switch (biome) {
            case FOREST, BIRCH_FOREST -> { spacing = 5; chance = 0.85f; }
            case TAIGA -> { spacing = 5; chance = 0.8f; }
            case SNOWY_TAIGA -> { spacing = 6; chance = 0.6f; }
            case PLAINS -> { spacing = 14; chance = 0.25f; }
            case MOUNTAINS -> { spacing = 10; chance = 0.35f; }
            default -> { return 0; }
        }
        boolean conifer = biome == Biome.TAIGA || biome == Biome.SNOWY_TAIGA;
        int gx = Math.floorDiv(x, spacing), gz = Math.floorDiv(z, spacing);
        int best = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int cx = gx + dx, cz = gz + dz;
                long hsh = (cx * 341873128712L + cz * 132897987541L) * 0x9E3779B97F4A7C15L;
                hsh ^= hsh >>> 29;
                if (((hsh >>> 8) & 1023) / 1024f > chance) continue;
                double tx = cx * spacing + ((hsh >>> 20) & 255) / 256.0 * spacing;
                double tz = cz * spacing + ((hsh >>> 28) & 255) / 256.0 * spacing;
                int trunk = 4 + (int) ((hsh >>> 36) & 3);
                double d = Math.hypot(x + 0.5 - tx, z + 0.5 - tz);
                double radius = conifer ? 2.2 : 2.6;
                if (d > radius) continue;
                int top = conifer ? trunk + 3 - (int) (d * 1.4) : trunk + 2 - (d > 1.8 ? 1 : 0);
                best = Math.max(best, top);
            }
        return best;
    }
}
