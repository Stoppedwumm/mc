package mc.world.gen;

import mc.world.Block;
import mc.world.Chunk;

import java.util.Random;

/**
 * The Nether: a 128 block tall cavern world of netherrack between bedrock floor and ceiling, with a lava sea at
 * y 31, soul sand and gravel shores, and quartz ore.
 */
public final class NetherGenerator {
    public static final int HEIGHT = 128, LAVA_LEVEL = 31;
    private final long seed;
    private final Noise.Octaves density, detail, shore;

    public NetherGenerator(long seed) {
        this.seed = seed;
        Random r = new Random(seed ^ 0x4E657468L);
        density = new Noise.Octaves(r, 4);
        detail = new Noise.Octaves(r, 3);
        shore = new Noise.Octaves(r, 3);
    }

    /** Positive = solid. Shaped so the middle heights are mostly open and the floor/ceiling close up. */
    private double density(double x, double y, double z) {
        double n = density.sample(x / 55.0, y / 32.0, z / 55.0) * 1.5 + detail.sample(x / 18.0, y / 12.0, z / 18.0) * 0.35;
        double floor = y < 26 ? (26 - y) / 7.0 : 0;
        double ceil = y > 100 ? (y - 100) / 9.0 : 0;
        // Slightly more rock towards the top and bottom of the open band
        double band = Math.abs(y - 64) / 64.0 * 0.25;
        return n + floor + ceil + band - 0.12;
    }

    public void generate(Chunk chunk) {
        int bx = chunk.cx * 16, bz = chunk.cz * 16;
        byte[] blocks = chunk.blocks;
        Random rand = new Random(seed * 341873128712L + chunk.cx * 132897987541L + chunk.cz);
        // Density on a 4x8x4 grid, trilinearly interpolated
        int gx = 5, gy = HEIGHT / 8 + 1, gz = 5;
        double[] grid = new double[gx * gy * gz];
        for (int i = 0; i < gx; i++)
            for (int j = 0; j < gy; j++)
                for (int k = 0; k < gz; k++) grid[(i * gy + j) * gz + k] = density(bx + i * 4, j * 8, bz + k * 4);
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int i = x / 4, k = z / 4;
                double fx = (x % 4) / 4.0, fz = (z % 4) / 4.0;
                for (int y = 0; y < HEIGHT; y++) {
                    int j = y / 8;
                    double fy = (y % 8) / 8.0;
                    double d = lerp3(grid, gy, gz, i, j, k, fx, fy, fz);
                    int id;
                    if (d > 0) id = Block.NETHERRACK.id;
                    else if (y <= LAVA_LEVEL) id = Block.LAVA.id;
                    else id = 0;
                    blocks[Chunk.index(x, y, z)] = (byte) id;
                }
                // Bedrock floor and ceiling with a ragged edge
                for (int y = 0; y < 5; y++) if (y == 0 || rand.nextInt(y + 1) == 0) blocks[Chunk.index(x, y, z)] = (byte) Block.BEDROCK.id;
                for (int y = HEIGHT - 5; y < HEIGHT; y++) if (y == HEIGHT - 1 || rand.nextInt(HEIGHT - y) == 0) blocks[Chunk.index(x, y, z)] = (byte) Block.BEDROCK.id;
                // Soul sand and gravel near the lava sea
                double s = shore.sample2((bx + x) / 30.0, (bz + z) / 30.0);
                for (int y = LAVA_LEVEL - 4; y < LAVA_LEVEL + 6; y++) {
                    int idx = Chunk.index(x, y, z);
                    if (blocks[idx] != Block.NETHERRACK.id || blocks[Chunk.index(x, y + 1, z)] != 0) continue;
                    if (s > 0.25) { blocks[idx] = (byte) Block.SOUL_SAND.id; if (y > 1) blocks[Chunk.index(x, y - 1, z)] = (byte) Block.SOUL_SAND.id; }
                    else if (s < -0.3) blocks[idx] = (byte) Block.GRAVEL.id;
                }
            }
        // Quartz ore
        for (int n = 0; n < 16; n++) {
            int x = rand.nextInt(16), y = 10 + rand.nextInt(108), z = rand.nextInt(16);
            for (int k = 0; k < 6; k++) {
                int px = Math.min(15, Math.max(0, x + rand.nextInt(3) - 1)), py = y + rand.nextInt(3) - 1, pz = Math.min(15, Math.max(0, z + rand.nextInt(3) - 1));
                int idx = Chunk.index(px, py, pz);
                if (blocks[idx] == Block.NETHERRACK.id) blocks[idx] = (byte) Block.NETHER_QUARTZ_ORE.id;
            }
        }
        computeBiomeData(chunk);
        chunk.recomputeMaxY();
        chunk.state = Chunk.STATE_TERRAIN;
    }

    private static double lerp3(double[] g, int gy, int gz, int i, int j, int k, double fx, double fy, double fz) {
        double c000 = g[(i * gy + j) * gz + k], c100 = g[((i + 1) * gy + j) * gz + k];
        double c010 = g[(i * gy + j + 1) * gz + k], c110 = g[((i + 1) * gy + j + 1) * gz + k];
        double c001 = g[(i * gy + j) * gz + k + 1], c101 = g[((i + 1) * gy + j) * gz + k + 1];
        double c011 = g[(i * gy + j + 1) * gz + k + 1], c111 = g[((i + 1) * gy + j + 1) * gz + k + 1];
        double x00 = c000 + (c100 - c000) * fx, x10 = c010 + (c110 - c010) * fx;
        double x01 = c001 + (c101 - c001) * fx, x11 = c011 + (c111 - c011) * fx;
        double y0 = x00 + (x10 - x00) * fy, y1 = x01 + (x11 - x01) * fy;
        return y0 + (y1 - y0) * fz;
    }

    public void computeBiomeData(Chunk chunk) {
        java.util.Arrays.fill(chunk.biome, (byte) Biome.NETHER.ordinal());
        java.util.Arrays.fill(chunk.grassColor, 0xbfb755);
        java.util.Arrays.fill(chunk.foliageColor, 0xaea42a);
    }
}
