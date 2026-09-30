package mc.world.gen;

import mc.world.Block;
import mc.world.Chunk;
import mc.world.World;

import java.util.Random;

/**
 * Second generation pass: trees and plants. Runs on the main thread once all 8 neighbours of a chunk exist,
 * so features may spill over chunk borders like Minecraft's population stage.
 */
public final class Decorator {
    private final World world;
    private final long seed;

    public final Structures structures;

    public Decorator(World world, long seed) {
        this.world = world;
        this.seed = seed;
        this.structures = new Structures(world, seed, world.generator);
    }

    public void decorate(Chunk chunk) {
        Random rand = new Random(seed * 31 ^ (chunk.cx * 49632L + 1) * 73856093L ^ (chunk.cz * 325176L + 7) * 19349663L);
        int bx = chunk.cx * 16, bz = chunk.cz * 16;
        if (world.dimension == mc.world.Dimension.NETHER) {
            decorateNether(chunk, rand, bx, bz);
            chunk.state = Chunk.STATE_DECORATED;
            return;
        }
        Biome biome = Biome.VALUES[chunk.biome[8 * 16 + 8]];

        int trees = switch (biome) {
            case FOREST -> 7 + rand.nextInt(4);
            case BIRCH_FOREST -> 7 + rand.nextInt(3);
            case TAIGA -> 6 + rand.nextInt(4);
            case SNOWY_TAIGA -> 2 + rand.nextInt(3);
            case PLAINS -> rand.nextInt(12) == 0 ? 1 : 0;
            case MOUNTAINS -> rand.nextInt(3) == 0 ? 1 : 0;
            default -> 0;
        };
        for (int i = 0; i < trees; i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
            Biome local = Biome.VALUES[chunk.biome[(z - bz) * 16 + (x - bx)]];
            int y = surface(x, z);
            if (y < 0) continue;
            int ground = world.getBlock(x, y, z);
            if (ground != Block.GRASS.id && ground != Block.DIRT.id && ground != Block.SNOWY_GRASS.id && ground != Block.COARSE_DIRT.id) continue;
            switch (local) {
                case TAIGA, SNOWY_TAIGA, MOUNTAINS -> spruce(rand, x, y + 1, z);
                case BIRCH_FOREST -> { if (rand.nextInt(5) == 0) oak(rand, x, y + 1, z); else birch(rand, x, y + 1, z); }
                case FOREST -> {
                    if (rand.nextInt(5) == 0) birch(rand, x, y + 1, z);
                    else if (rand.nextInt(8) == 0) bigOak(rand, x, y + 1, z);
                    else oak(rand, x, y + 1, z);
                }
                default -> { if (rand.nextInt(6) == 0) bigOak(rand, x, y + 1, z); else oak(rand, x, y + 1, z); }
            }
        }

        // Ground cover
        int grass = switch (biome) {
            case PLAINS -> 48; case FOREST, BIRCH_FOREST -> 20; case TAIGA -> 16; case MOUNTAINS -> 10; case SNOWY_TAIGA -> 2;
            default -> 0;
        };
        for (int i = 0; i < grass; i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
            int y = surface(x, z);
            if (y < 0 || world.getBlock(x, y, z) != Block.GRASS.id || world.getBlock(x, y + 1, z) != 0) continue;
            int plant = biome == Biome.TAIGA || biome == Biome.SNOWY_TAIGA ? (rand.nextInt(3) == 0 ? Block.TALL_GRASS.id : Block.FERN.id) : Block.TALL_GRASS.id;
            world.setBlockGen(x, y + 1, z, plant);
        }
        int flowers = switch (biome) { case PLAINS -> 4; case FOREST, BIRCH_FOREST -> 3; default -> 0; };
        if (flowers > 0) {
            // Flowers grow in small patches
            int cx = bx + rand.nextInt(16), cz = bz + rand.nextInt(16);
            int type = rand.nextInt(10) == 0 ? Block.BLUE_ORCHID.id : rand.nextBoolean() ? Block.DANDELION.id : Block.POPPY.id;
            for (int i = 0; i < flowers * 4; i++) {
                int x = cx + rand.nextInt(7) - 3, z = cz + rand.nextInt(7) - 3;
                if (!insideDecorationArea(chunk, x, z)) continue;
                int y = surface(x, z);
                if (y < 0 || world.getBlock(x, y, z) != Block.GRASS.id || world.getBlock(x, y + 1, z) != 0) continue;
                world.setBlockGen(x, y + 1, z, type);
            }
        }
        if (biome == Biome.PLAINS && rand.nextInt(24) == 0) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
            int y = surface(x, z);
            if (y >= 0 && world.getBlock(x, y, z) == Block.GRASS.id && world.getBlock(x, y + 1, z) == 0) world.setBlockGen(x, y + 1, z, Block.PUMPKIN.id);
        }

        if (biome == Biome.DESERT || biome == Biome.BADLANDS) {
            for (int i = 0; i < 3; i++) {
                int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
                int y = surface(x, z);
                int g = y < 0 ? 0 : world.getBlock(x, y, z);
                if ((g == Block.SAND.id || g == Block.TERRACOTTA.id) && world.getBlock(x, y + 1, z) == 0) world.setBlockGen(x, y + 1, z, Block.DEAD_BUSH.id);
            }
            if (biome == Biome.DESERT) {
                for (int i = 0; i < 4; i++) {
                    int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
                    int y = surface(x, z);
                    if (y < 0 || world.getBlock(x, y, z) != Block.SAND.id) continue;
                    int h = 1 + rand.nextInt(3);
                    for (int k = 1; k <= h; k++) {
                        if (world.getBlock(x, y + k, z) != 0 || solidAround(x, y + k, z)) break;
                        world.setBlockGen(x, y + k, z, Block.CACTUS.id);
                    }
                }
            }
        }

        // Sugar cane beside water
        for (int i = 0; i < 12; i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16);
            int y = surface(x, z);
            if (y != TerrainGenerator.SEA_LEVEL && y != TerrainGenerator.SEA_LEVEL + 1) continue;
            int g = world.getBlock(x, y, z);
            if (g != Block.GRASS.id && g != Block.SAND.id && g != Block.DIRT.id) continue;
            if (world.getBlock(x + 1, y, z) != Block.WATER.id && world.getBlock(x - 1, y, z) != Block.WATER.id
                    && world.getBlock(x, y, z + 1) != Block.WATER.id && world.getBlock(x, y, z - 1) != Block.WATER.id) continue;
            int h = 1 + rand.nextInt(3);
            for (int k = 1; k <= h; k++) {
                if (world.getBlock(x, y + k, z) != 0) break;
                world.setBlockGen(x, y + k, z, Block.SUGAR_CANE.id);
            }
        }

        structures.decorate(chunk, rand);
        chunk.state = Chunk.STATE_DECORATED;
    }

    private void decorateNether(Chunk chunk, Random rand, int bx, int bz) {
        // Glowstone clusters hanging from the ceiling
        for (int i = 0; i < 2 + rand.nextInt(3); i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16), y = 120;
            // Find a ceiling: the first air below netherrack
            while (y > 40 && !(world.getBlock(x, y, z) == 0 && world.getBlock(x, y + 1, z) == Block.NETHERRACK.id)) y--;
            if (y <= 40) continue;
            world.setBlockGen(x, y, z, Block.GLOWSTONE.id);
            for (int k = 0; k < 120; k++) {
                int px = x + rand.nextInt(8) - rand.nextInt(8), py = y - rand.nextInt(10), pz = z + rand.nextInt(8) - rand.nextInt(8);
                if (world.getBlock(px, py, pz) != 0) continue;
                int n = 0;
                for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}})
                    if (world.getBlock(px + d[0], py + d[1], pz + d[2]) == Block.GLOWSTONE.id) n++;
                if (n == 1) world.setBlockGen(px, py, pz, Block.GLOWSTONE.id);
            }
        }
        // Eternal fires on netherrack
        for (int i = 0; i < rand.nextInt(rand.nextInt(10) + 1); i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16), y = 32 + rand.nextInt(90);
            for (int k = 0; k < 16; k++) {
                int px = x + rand.nextInt(6) - 3, py = y + rand.nextInt(4) - 2, pz = z + rand.nextInt(6) - 3;
                if (world.getBlock(px, py, pz) == 0 && world.getBlock(px, py - 1, pz) == Block.NETHERRACK.id) world.setBlockGen(px, py, pz, Block.FIRE.id);
            }
        }
        // Lava springs in the walls
        for (int i = 0; i < 6; i++) {
            int x = bx + rand.nextInt(16), z = bz + rand.nextInt(16), y = 40 + rand.nextInt(80);
            if (world.getBlock(x, y, z) != Block.NETHERRACK.id || world.getBlock(x, y + 1, z) != Block.NETHERRACK.id) continue;
            int air = 0;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (world.getBlock(x + d[0], y, z + d[1]) == 0) air++;
            if (air == 1) {
                world.setBlockGen(x, y, z, Block.LAVA.id);
                world.scheduleTick(x, y, z, 30);
            }
        }
    }

    /** Grows a sapling into a tree suited to the biome; returns false if there wasn't room. */
    public boolean growTree(Random rand, int x, int y, int z, Biome biome) {
        int h = 7;
        for (int k = 0; k < h; k++) {
            int id = world.getBlock(x, y + k, z);
            if (id != 0 && Block.get(id).model != Block.Model.CROSS && Block.get(id).layer != Block.Layer.CUTOUT) return false;
        }
        switch (biome) {
            case TAIGA, SNOWY_TAIGA, MOUNTAINS -> spruce(rand, x, y, z);
            case BIRCH_FOREST -> birch(rand, x, y, z);
            default -> { if (rand.nextInt(10) == 0) bigOak(rand, x, y, z); else oak(rand, x, y, z); }
        }
        return world.getBlock(x, y, z) != 0;
    }

    private static boolean insideDecorationArea(Chunk c, int x, int z) {
        return x >= c.cx * 16 - 8 && x < c.cx * 16 + 24 && z >= c.cz * 16 - 8 && z < c.cz * 16 + 24;
    }

    private boolean solidAround(int x, int y, int z) {
        return Block.get(world.getBlock(x + 1, y, z)).solid || Block.get(world.getBlock(x - 1, y, z)).solid
                || Block.get(world.getBlock(x, y, z + 1)).solid || Block.get(world.getBlock(x, y, z - 1)).solid;
    }

    /** Highest solid (non-leaf) block in a column, or -1 if the top is water. */
    private int surface(int x, int z) {
        for (int y = 254; y > 0; y--) {
            int id = world.getBlock(x, y, z);
            if (id == 0) continue;
            Block b = Block.get(id);
            if (b.isLiquid() || b == Block.ICE) return -1;
            if (b.model == Block.Model.CROSS || b == Block.OAK_LEAVES || b == Block.BIRCH_LEAVES || b == Block.SPRUCE_LEAVES
                    || b == Block.OAK_LOG || b == Block.BIRCH_LOG || b == Block.SPRUCE_LOG) continue;
            return y;
        }
        return -1;
    }

    private boolean clearTrunk(int x, int y, int z, int h) {
        if (y + h + 2 >= 255) return false;
        for (int k = 0; k < h; k++) {
            int id = world.getBlock(x, y + k, z);
            if (id != 0 && !Block.get(id).replaceable && Block.get(id).layer != Block.Layer.CUTOUT) return false;
        }
        return true;
    }

    private void leaf(int x, int y, int z, int id) {
        int cur = world.getBlock(x, y, z);
        if (cur == 0 || Block.get(cur).model == Block.Model.CROSS) world.setBlockGen(x, y, z, id);
    }

    private void log(int x, int y, int z, int id) {
        int cur = world.getBlock(x, y, z);
        if (cur == 0 || Block.get(cur).replaceable || Block.get(cur).layer == Block.Layer.CUTOUT) world.setBlockGen(x, y, z, id);
    }

    private void oak(Random rand, int x, int y, int z) {
        blobTree(rand, x, y, z, 4 + rand.nextInt(3), Block.OAK_LOG.id, Block.OAK_LEAVES.id);
    }

    private void birch(Random rand, int x, int y, int z) {
        blobTree(rand, x, y, z, 5 + rand.nextInt(3), Block.BIRCH_LOG.id, Block.BIRCH_LEAVES.id);
    }

    /** Classic Minecraft oak/birch shape. */
    private void blobTree(Random rand, int x, int y, int z, int h, int logId, int leafId) {
        if (!clearTrunk(x, y, z, h)) return;
        world.setBlockGen(x, y - 1, z, Block.DIRT.id);
        for (int yy = y + h - 3; yy <= y + h; yy++) {
            int dy = yy - (y + h);
            int r = 1 - dy / 2;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    int dx = Math.abs(xx - x), dz = Math.abs(zz - z);
                    if (dx == r && dz == r && (rand.nextInt(2) == 0 || dy == 0)) continue;
                    leaf(xx, yy, zz, leafId);
                }
            }
        }
        for (int k = 0; k < h; k++) log(x, y + k, z, logId);
    }

    /** Larger oak with a rounded canopy and a few branches. */
    private void bigOak(Random rand, int x, int y, int z) {
        int h = 7 + rand.nextInt(4);
        if (!clearTrunk(x, y, z, h)) return;
        world.setBlockGen(x, y - 1, z, Block.DIRT.id);
        int top = y + h;
        for (int b = 0; b < 3 + rand.nextInt(3); b++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            int by = y + h / 2 + rand.nextInt(h / 2);
            int len = 2 + rand.nextInt(2);
            int ex = x, ez = z;
            for (int l = 1; l <= len; l++) {
                ex = x + (int) Math.round(Math.cos(ang) * l);
                ez = z + (int) Math.round(Math.sin(ang) * l);
                log(ex, by + l / 2, ez, Block.OAK_LOG.id);
            }
            sphere(ex, by + len / 2 + 1, ez, 2.2, Block.OAK_LEAVES.id, rand);
        }
        sphere(x, top - 1, z, 3.0, Block.OAK_LEAVES.id, rand);
        for (int k = 0; k < h; k++) log(x, y + k, z, Block.OAK_LOG.id);
    }

    private void sphere(int cx, int cy, int cz, double r, int id, Random rand) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++)
            for (int dy = -ir + 1; dy <= ir - 1; dy++)
                for (int dz = -ir; dz <= ir; dz++) {
                    double d = dx * dx + dy * dy * 1.6 + dz * dz;
                    if (d <= r * r - rand.nextDouble() * 1.5) leaf(cx + dx, cy + dy, cz + dz, id);
                }
    }

    private void spruce(Random rand, int x, int y, int z) {
        int h = 7 + rand.nextInt(5);
        if (!clearTrunk(x, y, z, h)) return;
        world.setBlockGen(x, y - 1, z, Block.DIRT.id);
        int leafBottom = 1 + rand.nextInt(2);
        int maxR = 2 + rand.nextInt(2);
        int r = rand.nextInt(2);
        int grow = 1;
        int reset = 0;
        for (int k = h + 1; k >= leafBottom; k--) {
            int yy = y + k;
            for (int xx = x - r; xx <= x + r; xx++) {
                for (int zz = z - r; zz <= z + r; zz++) {
                    int dx = Math.abs(xx - x), dz = Math.abs(zz - z);
                    if (r > 0 && dx == r && dz == r) continue;
                    leaf(xx, yy, zz, Block.SPRUCE_LEAVES.id);
                }
            }
            if (r >= grow) {
                r = reset;
                reset = 1;
                if (++grow > maxR) grow = maxR;
            } else {
                r++;
            }
        }
        leaf(x, y + h + 1, z, Block.SPRUCE_LEAVES.id);
        for (int k = 0; k < h; k++) log(x, y + k, z, Block.SPRUCE_LOG.id);
    }
}
