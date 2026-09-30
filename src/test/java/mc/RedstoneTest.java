package mc;

import mc.entity.Player;
import mc.world.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class RedstoneTest {
    private World w;

    @BeforeEach
    void setUp() throws Exception {
        w = new World(42, new WorldStorage(Files.createTempDirectory("redstone")));
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 30; x++)
            for (int z = -10; z < 26; z++) {
                for (int y = 100; y < 110; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
    }

    @AfterEach
    void tearDown() { w.shutdown(); }

    private void ticks(int n) { for (int i = 0; i < n; i++) w.tick(); }

    private void place(int x, int y, int z, Block b, int meta) { w.setBlock(x, y, z, b.id, meta, true); }

    @Test
    void wireCarriesPowerFromLeverToLamp() {
        place(0, 100, 5, Block.LEVER, 0);
        for (int x = 1; x <= 5; x++) place(x, 100, 5, Block.REDSTONE_WIRE, 0);
        place(6, 100, 5, Block.REDSTONE_LAMP, 0);
        assertEquals(Block.REDSTONE_LAMP.id, w.getBlock(6, 100, 5));
        Redstone.toggleLever(w, 0, 100, 5);
        assertEquals(15, w.getMeta(1, 100, 5));
        assertEquals(11, w.getMeta(5, 100, 5));
        assertEquals(Block.LIT_REDSTONE_LAMP.id, w.getBlock(6, 100, 5));
        Redstone.toggleLever(w, 0, 100, 5);
        assertEquals(0, w.getMeta(3, 100, 5));
        ticks(6);
        assertEquals(Block.REDSTONE_LAMP.id, w.getBlock(6, 100, 5));
    }

    @Test
    void signalDiesAfterFifteenBlocks() {
        place(0, 100, 5, Block.REDSTONE_BLOCK, 0);
        for (int x = 1; x <= 17; x++) place(x, 100, 5, Block.REDSTONE_WIRE, 0);
        assertEquals(15, w.getMeta(1, 100, 5));
        assertEquals(1, w.getMeta(15, 100, 5));
        assertEquals(0, w.getMeta(16, 100, 5));
    }

    @Test
    void torchInvertsItsSupportBlock() {
        place(5, 100, 5, Block.STONE, 0);
        place(5, 101, 5, Block.REDSTONE_TORCH, 0); // on top of the stone
        place(4, 100, 5, Block.LEVER, 3 + 1);       // on the stone's west... attached towards +x
        assertEquals(Block.REDSTONE_TORCH.id, w.getBlock(5, 101, 5));
        Redstone.toggleLever(w, 4, 100, 5);
        ticks(4);
        assertEquals(Block.UNLIT_REDSTONE_TORCH.id, w.getBlock(5, 101, 5));
        Redstone.toggleLever(w, 4, 100, 5);
        ticks(4);
        assertEquals(Block.REDSTONE_TORCH.id, w.getBlock(5, 101, 5));
    }

    @Test
    void repeaterDelaysAndStrengthens() {
        place(0, 100, 8, Block.LEVER, 0);
        place(1, 100, 8, Block.REDSTONE_WIRE, 0);
        // Facing east (3), delay 4 redstone ticks (bits 2-3 = 3)
        place(2, 100, 8, Block.REPEATER, 3 | (3 << 2));
        place(3, 100, 8, Block.REDSTONE_LAMP, 0);
        Redstone.toggleLever(w, 0, 100, 8);
        ticks(4);
        assertEquals(Block.REDSTONE_LAMP.id, w.getBlock(3, 100, 8), "not yet: 4 redstone ticks = 8 game ticks");
        ticks(6);
        assertEquals(Block.POWERED_REPEATER.id, w.getBlock(2, 100, 8));
        assertEquals(Block.LIT_REDSTONE_LAMP.id, w.getBlock(3, 100, 8));
    }

    @Test
    void stickyPistonPushesAndPulls() {
        place(0, 100, 12, Block.STICKY_PISTON, 5); // facing east
        place(1, 100, 12, Block.COBBLESTONE, 0);
        place(2, 100, 12, Block.DIRT, 0);
        place(-1, 100, 12, Block.LEVER, 0);
        Redstone.toggleLever(w, -1, 100, 12);
        ticks(3);
        assertEquals(Block.PISTON_HEAD.id, w.getBlock(1, 100, 12));
        assertEquals(Block.COBBLESTONE.id, w.getBlock(2, 100, 12));
        assertEquals(Block.DIRT.id, w.getBlock(3, 100, 12));
        Redstone.toggleLever(w, -1, 100, 12);
        ticks(3);
        assertEquals(Block.COBBLESTONE.id, w.getBlock(1, 100, 12), "sticky piston pulls the block back");
        assertEquals(0, w.getBlock(2, 100, 12));
        assertEquals(Block.DIRT.id, w.getBlock(3, 100, 12));
    }

    @Test
    void buttonOpensIronDoorBriefly() {
        w.setBlock(10, 101, 5, Block.IRON_DOOR.id, 2 | 8, false);
        place(10, 100, 5, Block.IRON_DOOR, 2);
        place(9, 100, 5, Block.STONE_BUTTON, 0);
        Redstone.pressButton(w, 9, 100, 5);
        assertTrue((w.getMeta(10, 100, 5) & 4) != 0, "door opens");
        ticks(25);
        assertEquals(0, w.getMeta(10, 100, 5) & 4, "and closes when the button pops out");
    }

    @Test
    void pressurePlateUnderPlayer() {
        Player p = new Player();
        w.setPlayer(p);
        place(15, 100, 15, Block.STONE_PRESSURE_PLATE, 0);
        place(16, 100, 15, Block.REDSTONE_LAMP, 0);
        p.setPos(15.5, 100, 15.5);
        ticks(2);
        assertEquals(Block.LIT_REDSTONE_LAMP.id, w.getBlock(16, 100, 15));
        p.setPos(20.5, 100, 20.5);
        ticks(30);
        assertEquals(Block.REDSTONE_LAMP.id, w.getBlock(16, 100, 15));
    }
}
