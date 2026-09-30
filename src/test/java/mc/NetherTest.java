package mc;

import mc.entity.Mob;
import mc.entity.MobType;
import mc.world.*;
import mc.world.gen.NetherGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Portals, fire and the Nether dimension. */
class NetherTest {
    private static World flat(Dimension d) throws Exception {
        World w = new World(42, new WorldStorage(Files.createTempDirectory("nether")), d);
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 26; x++)
            for (int z = -10; z < 26; z++) {
                for (int y = 70; y < 90; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 69, z, Block.STONE.id, 0, false);
            }
        return w;
    }

    private static void frame(World w, int x0, int y0, int z) {
        for (int i = 0; i < 4; i++) {
            w.setBlock(x0 + i, y0, z, Block.OBSIDIAN.id);
            w.setBlock(x0 + i, y0 + 4, z, Block.OBSIDIAN.id);
        }
        for (int h = 1; h <= 3; h++) {
            w.setBlock(x0, y0 + h, z, Block.OBSIDIAN.id);
            w.setBlock(x0 + 3, y0 + h, z, Block.OBSIDIAN.id);
        }
    }

    @Test
    void portalLightsAndBreaks() throws Exception {
        World w = flat(Dimension.OVERWORLD);
        frame(w, 4, 70, 8);
        assertTrue(Portal.tryLight(w, 5, 71, 8));
        for (int x = 5; x <= 6; x++)
            for (int y = 71; y <= 73; y++) assertEquals(Block.NETHER_PORTAL.id, w.getBlock(x, y, 8));
        // Incomplete frame: nothing happens
        frame(w, 12, 70, 8);
        w.setBlock(12, 74, 8, 0);
        w.setBlock(13, 74, 8, 0);
        assertFalse(Portal.tryLight(w, 13, 71, 8));
        // Breaking the frame destroys the portal
        w.breakBlock(4, 72, 8, null, false);
        for (int x = 5; x <= 6; x++)
            for (int y = 71; y <= 73; y++) assertEquals(0, w.getBlock(x, y, 8));
        w.shutdown();
    }

    @Test
    void portalIsBuiltAtTheDestination() throws Exception {
        World w = flat(Dimension.NETHER);
        double[] p = Portal.findOrCreate(w, 8.5, 75, 8.5, NetherGenerator.HEIGHT);
        int bx = (int) Math.floor(p[0]), by = (int) Math.floor(p[1]), bz = (int) Math.floor(p[2]);
        assertEquals(Block.NETHER_PORTAL.id, w.getBlock(bx, by, bz));
        double[] again = Portal.findOrCreate(w, 10.5, 75, 10.5, NetherGenerator.HEIGHT);
        assertEquals(p[1], again[1], 1e-6, "an existing portal is reused");
        w.shutdown();
    }

    @Test
    void netherTerrain() throws Exception {
        World w = new World(7, new WorldStorage(Files.createTempDirectory("netherterrain")), Dimension.NETHER);
        w.loadAreaBlocking(0, 0, 2);
        int netherrack = 0, lava = 0, glow = 0, bedrockFloor = 0;
        for (int x = -16; x < 32; x++)
            for (int z = -16; z < 32; z++) {
                if (w.getBlock(x, 0, z) == Block.BEDROCK.id && w.getBlock(x, 127, z) == Block.BEDROCK.id) bedrockFloor++;
                for (int y = 1; y < 127; y++) {
                    int id = w.getBlock(x, y, z);
                    if (id == Block.NETHERRACK.id) netherrack++;
                    else if (id == Block.LAVA.id && y <= NetherGenerator.LAVA_LEVEL) lava++;
                    else if (id == Block.GLOWSTONE.id) glow++;
                }
            }
        assertEquals(48 * 48, bedrockFloor);
        assertTrue(netherrack > 48 * 48 * 20, "mostly netherrack");
        assertTrue(lava > 0, "a lava sea");
        assertTrue(glow > 0, "glowstone clusters");
        w.shutdown();
    }

    @Test
    void fireSpreadsAndNetherMobsDontBurn() throws Exception {
        World w = flat(Dimension.OVERWORLD);
        mc.entity.Player p = new mc.entity.Player();
        w.setPlayer(p);
        p.setPos(5, 70, 12);
        for (int x = 0; x < 5; x++) w.setBlock(x, 70, 5, Block.PLANKS.id);
        w.setBlock(0, 71, 5, Block.FIRE.id);
        for (int i = 0; i < 3000; i++) w.tick();
        int planks = 0;
        for (int x = 0; x < 5; x++) if (w.getBlock(x, 70, 5) == Block.PLANKS.id) planks++;
        assertTrue(planks < 5, "fire should have burned some planks");
        Mob pigman = new Mob(MobType.ZOMBIE_PIGMAN);
        assertTrue(pigman.fireImmune());
        w.shutdown();
    }
}
