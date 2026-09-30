package mc.world.gen;

import mc.world.Block;
import mc.world.Chunk;

import java.util.Random;

/**
 * Noise based terrain generator in the spirit of modern Minecraft: continentalness / erosion / peaks
 * shape a base height, a 3D density field adds cliffs and overhangs, then surface rules, carvers and ores run.
 * Everything here only touches the chunk being generated, so it is safe to run on worker threads.
 */
public final class TerrainGenerator {
    public static final int SEA_LEVEL = 62;

    private final long seed;
    private final Noise.Octaves continental, erosion, peaks, detail, density, temperature, humidity, river;
    private final Noise.Octaves cheese, surfaceNoise;
    private final Noise spaghettiA, spaghettiB, spaghettiC, spaghettiD, spaghettiWidth;

    public TerrainGenerator(long seed) {
        this.seed = seed;
        Random r = new Random(seed);
        continental = new Noise.Octaves(r, 6);
        erosion = new Noise.Octaves(r, 5);
        peaks = new Noise.Octaves(r, 5);
        detail = new Noise.Octaves(r, 4);
        density = new Noise.Octaves(r, 4);
        temperature = new Noise.Octaves(r, 4);
        humidity = new Noise.Octaves(r, 4);
        river = new Noise.Octaves(r, 4);
        cheese = new Noise.Octaves(r, 2);
        surfaceNoise = new Noise.Octaves(r, 3);
        spaghettiA = new Noise(r);
        spaghettiB = new Noise(r);
        spaghettiC = new Noise(r);
        spaghettiD = new Noise(r);
        spaghettiWidth = new Noise(r);
    }

    public long seed() { return seed; }

    // ---------------------------------------------------------------- column parameters

    /** Column shape parameters: [height, squash, mountainousness, riverFactor]. */
    public void columnParams(double x, double z, double[] out) {
        double c = continental.sample2(x / 1100.0, z / 1100.0) * 1.7;
        double e = erosion.sample2(x / 600.0, z / 600.0) * 1.7;
        double pk = peaks.ridged(x / 420.0, z / 420.0);

        double h = spline(c);
        double mountain = smooth(-0.05, 0.45, c) * (1 - smooth(-0.35, 0.25, e));
        double hills = smooth(-0.2, 0.3, c) * (1 - smooth(-0.1, 0.6, e));
        h += mountain * Math.pow(pk, 1.5) * 130;
        h += detail.sample2(x / 110.0, z / 110.0) * (4 + 14 * hills + 10 * mountain);

        // Rivers carve valleys through land that is not too high
        double rv = Math.abs(river.sample2(x / 900.0, z / 900.0));
        double riverFactor = 0;
        if (c > -0.2) {
            riverFactor = (1 - smooth(0.0, 0.055, rv)) * (1 - mountain * 0.85) * smooth(-0.2, -0.05, c);
            h = h + (SEA_LEVEL - 5 - h) * riverFactor;
        }

        double squash = 2.5 + 26 * mountain + 5 * hills;
        out[0] = h;
        out[1] = squash;
        out[2] = mountain;
        out[3] = riverFactor;
    }

    private static double spline(double c) {
        double[] xs = {-1.5, -0.55, -0.25, -0.1, 0.0, 0.3, 0.7, 1.5};
        double[] ys = {28, 38, 52, 60, 64, 70, 80, 92};
        if (c <= xs[0]) return ys[0];
        for (int i = 1; i < xs.length; i++) {
            if (c <= xs[i]) {
                double t = (c - xs[i - 1]) / (xs[i] - xs[i - 1]);
                t = t * t * (3 - 2 * t);
                return ys[i - 1] + (ys[i] - ys[i - 1]) * t;
            }
        }
        return ys[ys.length - 1];
    }

    private static double smooth(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    public double temperature01(double x, double z) {
        return clamp01(0.5 + temperature.sample2(x / 1400.0, z / 1400.0) * 1.1);
    }

    public double humidity01(double x, double z) {
        return clamp01(0.5 + humidity.sample2(x / 1200.0, z / 1200.0) * 1.1);
    }

    private static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    /** Rough surface height for spawn finding (ignores 3D noise and caves). */
    public int estimateHeight(int x, int z) {
        double[] p = new double[4];
        columnParams(x, z, p);
        return (int) p[0];
    }

    public Biome biomeAt(double temp, double hum, double height, double mountain, double river) {
        if (river > 0.55 && height < SEA_LEVEL + 1) return temp < 0.2 ? Biome.FROZEN_OCEAN : Biome.RIVER;
        if (height < SEA_LEVEL - 3) return temp < 0.18 ? Biome.FROZEN_OCEAN : Biome.OCEAN;
        if (height < SEA_LEVEL + 2.5 && mountain < 0.2) return temp < 0.2 ? Biome.SNOWY_TAIGA : Biome.BEACH;
        if (mountain > 0.4 && height > 110) return height > 150 || temp < 0.3 ? Biome.SNOWY_PEAKS : Biome.MOUNTAINS;
        if (temp > 0.72 && hum < 0.4) return hum < 0.22 && temp > 0.82 ? Biome.BADLANDS : Biome.DESERT;
        if (temp < 0.2) return Biome.SNOWY_TAIGA;
        if (temp < 0.35) return Biome.TAIGA;
        if (hum > 0.55) return temp > 0.5 ? Biome.FOREST : Biome.BIRCH_FOREST;
        if (hum > 0.47) return Biome.FOREST;
        return Biome.PLAINS;
    }

    // ---------------------------------------------------------------- chunk generation

    public void generate(Chunk chunk) {
        int bx = chunk.cx * 16, bz = chunk.cz * 16;
        byte[] blocks = chunk.blocks;

        // Column parameters on a 5x5 grid (every 4 blocks) for density interpolation
        double[][] params = new double[25][4];
        for (int gx = 0; gx < 5; gx++)
            for (int gz = 0; gz < 5; gz++)
                columnParams(bx + gx * 4, bz + gz * 4, params[gx * 5 + gz]);

        // Density at grid points: 5 x 33 x 5 (vertical cell size 8)
        double[] dens = new double[5 * 33 * 5];
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                double[] p = params[gx * 5 + gz];
                double wx = bx + gx * 4, wz = bz + gz * 4;
                for (int gy = 0; gy < 33; gy++) {
                    int y = gy * 8;
                    double d = p[0] - y;
                    // 3D noise matters most close to the surface
                    if (Math.abs(d) < p[1] * 2.5 + 8) {
                        d += density.sample(wx / 90.0, y / 70.0, wz / 90.0) * p[1];
                    }
                    if (y >= 248) d = -50;
                    dens[(gx * 33 + gy) * 5 + gz] = d;
                }
            }
        }

        // Trilinear interpolation into blocks
        for (int x = 0; x < 16; x++) {
            int gx = x >> 2; double tx = (x & 3) / 4.0;
            for (int z = 0; z < 16; z++) {
                int gz = z >> 2; double tz = (z & 3) / 4.0;
                for (int y = 0; y < 256; y++) {
                    int gy = y >> 3; double ty = (y & 7) / 8.0;
                    double d000 = dens[(gx * 33 + gy) * 5 + gz], d100 = dens[((gx + 1) * 33 + gy) * 5 + gz];
                    double d010 = dens[(gx * 33 + gy + 1) * 5 + gz], d110 = dens[((gx + 1) * 33 + gy + 1) * 5 + gz];
                    double d001 = dens[(gx * 33 + gy) * 5 + gz + 1], d101 = dens[((gx + 1) * 33 + gy) * 5 + gz + 1];
                    double d011 = dens[(gx * 33 + gy + 1) * 5 + gz + 1], d111 = dens[((gx + 1) * 33 + gy + 1) * 5 + gz + 1];
                    double d00 = d000 + (d100 - d000) * tx, d10 = d010 + (d110 - d010) * tx;
                    double d01 = d001 + (d101 - d001) * tx, d11 = d011 + (d111 - d011) * tx;
                    double d0 = d00 + (d10 - d00) * ty, d1 = d01 + (d11 - d01) * ty;
                    double d = d0 + (d1 - d0) * tz;
                    int id = 0;
                    if (d > 0 || y < 5) id = Block.STONE.id;
                    else if (y <= SEA_LEVEL) id = Block.WATER.id;
                    blocks[Chunk.index(x, y, z)] = (byte) id;
                }
            }
        }

        Random rand = new Random(seed ^ (chunk.cx * 341873128712L) ^ (chunk.cz * 132897987541L));
        double[] p = new double[4];
        int[] tops = new int[256];
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                tops[z * 16 + x] = topOf(blocks, x, z);

        // Biomes, tints and surface rules
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = bx + x, wz = bz + z;
                columnParams(wx, wz, p);
                int top = tops[z * 16 + x];
                double temp = temperature01(wx, wz);
                double hum = humidity01(wx, wz);
                // Higher ground is colder
                double tAdj = clamp01(temp - Math.max(0, top - 90) * 0.006);
                Biome biome = biomeAt(tAdj, hum, top, p[2], p[3]);
                chunk.biome[z * 16 + x] = (byte) biome.ordinal();
                chunk.grassColor[z * 16 + x] = grassColor(tAdj, hum, biome);
                chunk.foliageColor[z * 16 + x] = foliageColor(tAdj, hum, biome);

                int slope = 0;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = Math.max(0, Math.min(15, x + d[0])), nz = Math.max(0, Math.min(15, z + d[1]));
                    slope = Math.max(slope, Math.abs(tops[nz * 16 + nx] - top));
                }
                applySurface(chunk, x, z, top, biome, slope, rand, wx, wz, tAdj);
            }
        }

        carveCaves(chunk, params);
        placeOres(chunk, rand);

        // Bedrock floor
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 5; y++)
                    if (y == 0 || rand.nextInt(y + 1) == 0) blocks[Chunk.index(x, y, z)] = (byte) Block.BEDROCK.id;

        chunk.recomputeMaxY();
        chunk.state = Chunk.STATE_TERRAIN;
    }

    private static int topOf(byte[] blocks, int x, int z) {
        for (int y = 255; y >= 0; y--) {
            int id = blocks[Chunk.index(x, y, z)];
            if (id != 0 && id != Block.WATER.id) return y;
        }
        return 0;
    }

    private void applySurface(Chunk chunk, int x, int z, int top, Biome biome, int slope, Random rand, int wx, int wz, double temp) {
        byte[] b = chunk.blocks;
        double sn = surfaceNoise.sample2(wx / 24.0, wz / 24.0);
        int depth = -1;
        int fillerDepth = 3 + (int) (sn * 2 + 1.5 + rand.nextDouble());
        int topId, fillerId;
        boolean underwater = top < SEA_LEVEL;

        switch (biome) {
            case DESERT -> { topId = Block.SAND.id; fillerId = Block.SAND.id; }
            case BADLANDS -> { topId = Block.TERRACOTTA.id; fillerId = Block.TERRACOTTA.id; }
            case BEACH -> { topId = Block.SAND.id; fillerId = Block.SAND.id; }
            case SNOWY_TAIGA -> { topId = Block.SNOWY_GRASS.id; fillerId = Block.DIRT.id; }
            case SNOWY_PEAKS -> { topId = Block.SNOW.id; fillerId = Block.SNOW.id; }
            case MOUNTAINS -> { topId = slope > 3 ? Block.STONE.id : Block.GRASS.id; fillerId = slope > 3 ? Block.STONE.id : Block.DIRT.id; }
            default -> { topId = Block.GRASS.id; fillerId = Block.DIRT.id; }
        }
        if (biome == Biome.TAIGA && sn > 0.35) topId = Block.COARSE_DIRT.id;
        if (biome == Biome.SNOWY_PEAKS && slope > 2 + (int) (sn * 3)) { topId = Block.STONE.id; fillerId = Block.STONE.id; }
        if (biome == Biome.MOUNTAINS && top > 150 + sn * 10) { topId = Block.SNOW.id; }
        if (underwater) {
            if (biome == Biome.DESERT || biome == Biome.BEACH || top > SEA_LEVEL - 6) topId = sn > 0.3 ? Block.GRAVEL.id : Block.SAND.id;
            else topId = sn > 0.1 ? Block.GRAVEL.id : sn < -0.35 ? Block.CLAY.id : Block.SAND.id;
            if (biome == Biome.RIVER && sn < -0.2) topId = Block.CLAY.id;
            fillerId = topId == Block.CLAY.id ? Block.CLAY.id : topId == Block.GRAVEL.id ? Block.GRAVEL.id : Block.SAND.id;
        }

        for (int y = top; y >= 1; y--) {
            int i = Chunk.index(x, y, z);
            int id = b[i];
            if (id == 0 || id == Block.WATER.id) { depth = -1; continue; }
            if (id != Block.STONE.id) continue;
            if (depth == -1) {
                depth = fillerDepth;
                b[i] = (byte) (y >= SEA_LEVEL - 1 || underwater ? topId : fillerId);
                if (topId == Block.GRASS.id && y < SEA_LEVEL) b[i] = (byte) Block.DIRT.id;
            } else if (depth > 0) {
                depth--;
                b[i] = (byte) fillerId;
                if (depth == 0 && (biome == Biome.DESERT || biome == Biome.BEACH) && fillerId == Block.SAND.id) {
                    depth = 2 + rand.nextInt(3);
                    fillerId = Block.SANDSTONE.id;
                }
            }
            if (depth == 0 && y < top - 20) break;
        }

        // Badlands get horizontal terracotta bands
        if (biome == Biome.BADLANDS) {
            for (int y = top; y > top - 30 && y > 0; y--) {
                int i = Chunk.index(x, y, z);
                if (b[i] == Block.TERRACOTTA.id || b[i] == Block.STONE.id) {
                    int band = Math.floorMod(y + (int) (sn * 3), 9);
                    b[i] = (byte) (band == 0 ? Block.RED_WOOL.id : band == 4 ? Block.YELLOW_WOOL.id : band == 6 ? Block.WHITE_WOOL.id : Block.TERRACOTTA.id);
                }
            }
        }

        // Freeze the ocean surface in cold biomes
        if (temp < 0.18 && top < SEA_LEVEL) {
            int i = Chunk.index(x, SEA_LEVEL, z);
            if (b[i] == Block.WATER.id) b[i] = (byte) Block.ICE.id;
        }
    }

    private void carveCaves(Chunk chunk, double[][] params) {
        int bx = chunk.cx * 16, bz = chunk.cz * 16;
        byte[] b = chunk.blocks;
        // Cave field on a 5 x 65 x 5 grid (cell 4x4x4); positive = air
        double[] cave = new double[5 * 65 * 5];
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                double wx = bx + gx * 4, wz = bz + gz * 4;
                double[] p = params[gx * 5 + gz];
                double surface = p[0];
                boolean wet = surface < SEA_LEVEL + 4;
                for (int gy = 0; gy < 65; gy++) {
                    int y = gy * 4;
                    double v = -1;
                    double limit = wet ? surface - 10 : surface + 8;
                    if (y > 2 && y < limit) {
                        double a = spaghettiA.sample(wx / 55.0, y / 38.0, wz / 55.0);
                        double c = spaghettiB.sample(wx / 55.0, y / 38.0, wz / 55.0);
                        double w = 0.010 + 0.012 * (spaghettiWidth.sample(wx / 120.0, y / 120.0, wz / 120.0) + 1) * 0.5;
                        double sp1 = w - (a * a + c * c);
                        double a2 = spaghettiC.sample(wx / 75.0, y / 28.0, wz / 75.0);
                        double c2 = spaghettiD.sample(wx / 75.0, y / 28.0, wz / 75.0);
                        double sp2 = w * 0.8 - (a2 * a2 + c2 * c2);
                        v = Math.max(sp1, sp2);
                        if (y < surface - 14) {
                            double ch = cheese.sample(wx / 85.0, y / 50.0, wz / 85.0);
                            double threshold = 0.36 + Math.max(0, (y - 40)) * 0.004;
                            v = Math.max(v, (ch - threshold) * 0.25);
                        }
                        // Fade out near the limit so caves close off before breaching the ocean
                        if (y > limit - 8) v -= (y - (limit - 8)) * 0.004;
                    }
                    cave[(gx * 65 + gy) * 5 + gz] = v;
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            int gx = x >> 2; double tx = (x & 3) / 4.0;
            for (int z = 0; z < 16; z++) {
                int gz = z >> 2; double tz = (z & 3) / 4.0;
                for (int y = 1; y < 255; y++) {
                    int gy = y >> 2; double ty = (y & 3) / 4.0;
                    double c000 = cave[(gx * 65 + gy) * 5 + gz], c100 = cave[((gx + 1) * 65 + gy) * 5 + gz];
                    double c010 = cave[(gx * 65 + gy + 1) * 5 + gz], c110 = cave[((gx + 1) * 65 + gy + 1) * 5 + gz];
                    double c001 = cave[(gx * 65 + gy) * 5 + gz + 1], c101 = cave[((gx + 1) * 65 + gy) * 5 + gz + 1];
                    double c011 = cave[(gx * 65 + gy + 1) * 5 + gz + 1], c111 = cave[((gx + 1) * 65 + gy + 1) * 5 + gz + 1];
                    double c00 = c000 + (c100 - c000) * tx, c10 = c010 + (c110 - c010) * tx;
                    double c01 = c001 + (c101 - c001) * tx, c11 = c011 + (c111 - c011) * tx;
                    double c0 = c00 + (c10 - c00) * ty, c1 = c01 + (c11 - c01) * ty;
                    double v = c0 + (c1 - c0) * tz;
                    if (v <= 0) continue;
                    int i = Chunk.index(x, y, z);
                    int id = b[i];
                    if (id == 0 || id == Block.WATER.id || id == Block.ICE.id || id == Block.BEDROCK.id) continue;
                    // Never open a cave directly beneath water
                    int above = b[Chunk.index(x, y + 1, z)];
                    if (above == Block.WATER.id || above == Block.ICE.id) continue;
                    if (y <= 10) {
                        b[i] = (byte) Block.LAVA.id;
                    } else {
                        b[i] = 0;
                        // Expose the grass beneath a cave opening
                        int belowI = Chunk.index(x, y - 1, z);
                        if (b[belowI] == Block.DIRT.id && y > SEA_LEVEL && b[Chunk.index(x, Math.min(255, y + 1), z)] == 0) {
                            b[belowI] = (byte) Block.GRASS.id;
                        }
                    }
                }
            }
        }
    }

    private void placeOres(Chunk chunk, Random rand) {
        vein(chunk, rand, Block.DIRT.id, 10, 33, 0, 150);
        vein(chunk, rand, Block.GRAVEL.id, 8, 33, 0, 150);
        vein(chunk, rand, Block.GRANITE.id, 10, 33, 0, 90);
        vein(chunk, rand, Block.DIORITE.id, 10, 33, 0, 90);
        vein(chunk, rand, Block.ANDESITE.id, 10, 33, 0, 90);
        vein(chunk, rand, Block.COAL_ORE.id, 20, 17, 5, 130);
        vein(chunk, rand, Block.IRON_ORE.id, 20, 9, 5, 70);
        vein(chunk, rand, Block.GOLD_ORE.id, 2, 9, 5, 34);
        vein(chunk, rand, Block.DIAMOND_ORE.id, 1, 8, 5, 17);
        vein(chunk, rand, Block.REDSTONE_ORE.id, 8, 8, 5, 17);
        vein(chunk, rand, Block.LAPIS_ORE.id, 1, 7, 5, 32);
    }

    /** Minecraft style ellipsoid vein generator, clipped to the chunk. */
    private static void vein(Chunk chunk, Random rand, int ore, int count, int size, int minY, int maxY) {
        byte[] b = chunk.blocks;
        for (int n = 0; n < count; n++) {
            double cx = rand.nextInt(16), cy = minY + rand.nextInt(maxY - minY), cz = rand.nextInt(16);
            double angle = rand.nextDouble() * Math.PI;
            double x0 = cx + Math.sin(angle) * size / 8.0, x1 = cx - Math.sin(angle) * size / 8.0;
            double z0 = cz + Math.cos(angle) * size / 8.0, z1 = cz - Math.cos(angle) * size / 8.0;
            double y0 = cy + rand.nextInt(3) - 2, y1 = cy + rand.nextInt(3) - 2;
            for (int i = 0; i <= size; i++) {
                double px = x0 + (x1 - x0) * i / size, py = y0 + (y1 - y0) * i / size, pz = z0 + (z1 - z0) * i / size;
                double rad = (Math.sin(i * Math.PI / size) + 1) * (rand.nextDouble() * size / 16.0) + 1;
                double r = rad / 2;
                int minX = (int) Math.floor(px - r), maxX = (int) Math.floor(px + r);
                int minYY = (int) Math.floor(py - r), maxYY = (int) Math.floor(py + r);
                int minZ = (int) Math.floor(pz - r), maxZ = (int) Math.floor(pz + r);
                for (int x = Math.max(0, minX); x <= Math.min(15, maxX); x++) {
                    double dx = (x + 0.5 - px) / r;
                    for (int y = Math.max(1, minYY); y <= Math.min(254, maxYY); y++) {
                        double dy = (y + 0.5 - py) / r;
                        for (int z = Math.max(0, minZ); z <= Math.min(15, maxZ); z++) {
                            double dz = (z + 0.5 - pz) / r;
                            if (dx * dx + dy * dy + dz * dz < 1) {
                                int idx = Chunk.index(x, y, z);
                                if (b[idx] == Block.STONE.id) b[idx] = (byte) ore;
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- colours

    private static int lerpColor(int a, int b, double t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Approximation of Minecraft's grass colour map (temperature x downfall triangle). */
    public static int grassColor(double temp, double hum, Biome biome) {
        if (biome == Biome.BADLANDS) return 0x90814d;
        double down = hum * temp;
        int warm = lerpColor(0xbfb755, 0x47cd33, Math.min(1, down * 1.6));
        return lerpColor(0x80b497, warm, Math.min(1, temp * 1.25));
    }

    public static int foliageColor(double temp, double hum, Biome biome) {
        if (biome == Biome.BADLANDS) return 0x9e814d;
        double down = hum * temp;
        int warm = lerpColor(0xaea42a, 0x2fae0f, Math.min(1, down * 1.6));
        return lerpColor(0x60a17b, warm, Math.min(1, temp * 1.25));
    }

    /** Recompute per-column biome data (used for chunks loaded from disk). */
    public void computeBiomeData(Chunk chunk) {
        double[] p = new double[4];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunk.cx * 16 + x, wz = chunk.cz * 16 + z;
                columnParams(wx, wz, p);
                int top = topOf(chunk.blocks, x, z);
                double temp = temperature01(wx, wz), hum = humidity01(wx, wz);
                double tAdj = clamp01(temp - Math.max(0, top - 90) * 0.006);
                Biome biome = biomeAt(tAdj, hum, top, p[2], p[3]);
                chunk.biome[z * 16 + x] = (byte) biome.ordinal();
                chunk.grassColor[z * 16 + x] = grassColor(tAdj, hum, biome);
                chunk.foliageColor[z * 16 + x] = foliageColor(tAdj, hum, biome);
            }
        }
    }
}
