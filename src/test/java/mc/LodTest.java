package mc;

import mc.render.TextureGen;
import mc.render.lod.LodData;
import mc.render.lod.LodPalette;
import mc.world.Block;
import mc.world.Chunk;
import mc.world.gen.TerrainGenerator;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Distant terrain: chunk summaries, their persistence, estimates from the generator, and tile meshes. */
class LodTest {
    private static final LodPalette PALETTE = new LodPalette(new TextureGen().generate(), TextureGen.ATLAS);

    private static Chunk testChunk() {
        Chunk c = new Chunk(2, 3);
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= 69; y++) c.set(x, y, z, Block.STONE.id);
                c.set(x, 70, z, Block.GRASS.id);
                c.grassColor[z * 16 + x] = 0x79c05a;
                c.foliageColor[z * 16 + x] = 0x59ae30;
            }
        // A tree: trunk and a leaf crown
        for (int y = 71; y <= 74; y++) c.set(5, y, 5, Block.OAK_LOG.id);
        c.set(5, 75, 5, Block.OAK_LEAVES.id);
        c.set(6, 74, 5, Block.OAK_LEAVES.id);
        // A flower doesn't count, a pond does
        c.set(9, 71, 9, Block.POPPY.id);
        c.set(12, 70, 12, Block.WATER.id);
        c.set(12, 69, 12, Block.SAND.id);
        c.recomputeMaxY();
        return c;
    }

    @Test
    void chunkSummariesKeepSurfaceTreesAndWater() throws Exception {
        Path dir = Files.createTempDirectory("lod");
        LodData data = new LodData(new TerrainGenerator(5), PALETTE, dir, true);
        Chunk c = testChunk();
        data.capture(c);
        int[] s = new int[6];
        double[] tmp = new double[4];
        int bx = 32, bz = 48;
        assertTrue(data.sample(bx, bz, s, tmp));
        assertEquals(71, s[0], "grass surface");
        assertEquals(0, s[1], "dry");
        assertEquals(0, s[4], "no crown");
        assertTrue(data.sample(bx + 5, bz + 5, s, tmp));
        assertEquals(76, s[0], "top of the crown");
        assertEquals(71, s[4], "ground under the tree");
        assertTrue(data.sample(bx + 9, bz + 9, s, tmp));
        assertEquals(71, s[0], "flowers are ignored");
        assertTrue(data.sample(bx + 12, bz + 12, s, tmp));
        assertEquals(70, s[0], "pond floor");
        assertEquals(71, s[1], "water surface");
        // Grass is tinted green, sand is pale
        int g = s[2];
        data.sample(bx, bz, s, tmp);
        assertTrue((s[2] >> 8 & 255) > (s[2] >> 16 & 255), "grass is green");

        // Saved summaries come back in a fresh store
        data.save();
        LodData reloaded = new LodData(new TerrainGenerator(5), PALETTE, dir, false);
        assertTrue(reloaded.hasChunk(2, 3));
        assertTrue(reloaded.sample(bx + 5, bz + 5, s, tmp));
        assertEquals(76, s[0]);
        assertEquals(71, s[4]);
        assertFalse(reloaded.sample(10_000, 10_000, s, tmp), "no estimates when disabled");
        assertNotEquals(0, g);
    }

    @Test
    void unexploredTerrainIsEstimatedFromTheNoise() {
        TerrainGenerator gen = new TerrainGenerator(777);
        LodData data = new LodData(gen, PALETTE, null, true);
        int[] s = new int[6];
        double[] tmp = new double[4];
        int land = 0, sea = 0;
        for (int i = 0; i < 400; i++) {
            int x = i * 157 - 30000, z = i * 311 - 20000;
            assertTrue(data.sample(x, z, 64, s, tmp));
            int est = gen.estimateHeight(x, z);
            if (s[1] > 0) {
                sea++;
                assertEquals(TerrainGenerator.SEA_LEVEL + 1, s[1], "sea level water");
            } else land++;
            boolean ice = s[1] == 0 && s[0] == TerrainGenerator.SEA_LEVEL + 2 && est < TerrainGenerator.SEA_LEVEL;
            assertTrue(ice || (s[0] >= est + 1 && s[0] <= est + 12), "height near the generator's estimate: " + s[0] + " vs " + est);
        }
        assertTrue(land > 0 && sea > 0, "both land and sea in a long transect");
    }

    @Test
    void tilesMeshFromRealAndEstimatedData() throws Exception {
        LodData data = new LodData(new TerrainGenerator(5), PALETTE, null, true);
        data.capture(testChunk());
        Class<?> mesher = Class.forName("mc.render.lod.LodMesher");
        var ctor = mesher.getDeclaredConstructor();
        ctor.setAccessible(true);
        var build = mesher.getDeclaredMethod("build", LodData.class, int.class, int.class, int.class, long.class, int.class);
        build.setAccessible(true);
        for (int level : new int[]{0, 3, 6}) {
            // Level 0 tile 1 covers blocks 32..63, which contains the captured chunk at x 32..47
            Object built = build.invoke(ctor.newInstance(), data, level, level == 0 ? 1 : 0, level == 0 ? 1 : 0, 1L, 0);
            int opaque = field(built, "opaqueQuads"), water = field(built, "waterQuads");
            var df = built.getClass().getDeclaredField("data");
            df.setAccessible(true);
            java.nio.ByteBuffer buf = (java.nio.ByteBuffer) df.get(built);
            assertTrue(opaque > 32 * 32 / 2, "columns at level " + level + ": " + opaque);
            assertEquals((opaque + water) * 4 * 16, buf.remaining(), "16 bytes per vertex");
            MemoryUtil.memFree(buf);
        }
    }

    private static int field(Object o, String name) throws Exception {
        var f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(o);
    }
}
