package mc;

import mc.entity.Player;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.world.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Slabs, stairs, doors, ladders and beds: collision, support and two-block behaviour. */
class BuildingBlocksTest {
    private World w;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        w = new World(42, new WorldStorage(Files.createTempDirectory("mcblocks")));
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 26; x++)
            for (int z = -10; z < 26; z++) {
                for (int y = 100; y < 110; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
        player = new Player();
        w.setPlayer(player);
    }

    @AfterEach
    void tearDown() {
        w.shutdown();
    }

    private void walk(int ticks, float forward, boolean jump) {
        for (int i = 0; i < ticks; i++) player.tick(w, forward, 0, jump, false, false);
    }

    @Test
    void standsOnBottomAndTopSlabs() {
        w.setBlock(5, 100, 5, Block.OAK_SLAB.id, 0, false);
        player.setPos(5.5, 101, 5.5);
        walk(40, 0, false);
        assertEquals(100.5, player.y, 1e-6);
        w.setBlock(5, 100, 5, Block.OAK_SLAB.id, 1, false);
        player.setPos(5.5, 101.5, 5.5);
        walk(40, 0, false);
        assertEquals(101.0, player.y, 1e-6);
    }

    @Test
    void walksUpStairsWithoutJumping() {
        // Stairs rising towards +z (step side faces the player at -z: facing north = 2)
        w.setBlock(5, 100, 6, Block.OAK_STAIRS.id, 2, false);
        w.setBlock(5, 100, 7, Block.STONE.id, 0, false);
        player.setPos(5.5, 100, 4.5);
        player.yaw = 0; // facing +z
        double maxY = 0;
        for (int i = 0; i < 40; i++) { walk(1, 1, false); maxY = Math.max(maxY, player.y); }
        assertEquals(101, maxY, 1e-6, "player should have climbed the stairs onto the block behind");
    }

    @Test
    void closedDoorBlocksAndOpenDoorLetsThrough() {
        w.setBlock(5, 101, 6, Block.OAK_DOOR.id, 2 | 8, false);
        w.setBlock(5, 100, 6, Block.OAK_DOOR.id, 2, true);
        assertEquals(Block.OAK_DOOR.id, w.getBlock(5, 100, 6), "door must survive its own placement");
        player.setPos(5.5, 100, 4.5);
        player.yaw = 0;
        walk(40, 1, false);
        assertTrue(player.z < 6.1, "closed door should block, z=" + player.z);
        w.setBlock(5, 100, 6, Block.OAK_DOOR.id, 2 | 4, false);
        w.setBlock(5, 101, 6, Block.OAK_DOOR.id, 2 | 4 | 8, false);
        walk(40, 1, false);
        assertTrue(player.z > 7, "open door should let the player through, z=" + player.z);
    }

    @Test
    void breakingOneHalfRemovesTheOther() {
        w.setBlock(5, 101, 6, Block.OAK_DOOR.id, 8, false);
        w.setBlock(5, 100, 6, Block.OAK_DOOR.id, 0, true);
        w.breakBlock(5, 101, 6, null, true);
        assertEquals(0, w.getBlock(5, 100, 6));
        w.setBlock(8, 100, 9, Block.BED.id, 4, false);
        w.setBlock(8, 100, 8, Block.BED.id, 0, true);
        w.breakBlock(8, 100, 8, null, true);
        assertEquals(0, w.getBlock(8, 100, 9));
        // Door lost its support block below
        w.setBlock(12, 101, 6, Block.OAK_DOOR.id, 8, false);
        w.setBlock(12, 100, 6, Block.OAK_DOOR.id, 0, true);
        w.setBlock(12, 99, 6, 0);
        assertEquals(0, w.getBlock(12, 100, 6));
        assertEquals(0, w.getBlock(12, 101, 6));
    }

    @Test
    void climbsLadder() {
        for (int y = 100; y < 106; y++) {
            w.setBlock(5, y, 7, Block.STONE.id, 0, false);
            w.setBlock(5, y, 6, Block.LADDER.id, 0, false);
        }
        player.setPos(5.5, 100, 6.5);
        player.yaw = 0;
        walk(60, 1, false);
        assertTrue(player.y > 103, "should climb the ladder, y=" + player.y);
        // Letting go descends slowly without fall damage
        walk(80, 0, false);
        assertEquals(20, player.health, 1e-6);
    }

    @Test
    void fenceIsTallerThanAJump() {
        for (int x = 0; x < 12; x++) w.setBlock(x, 100, 6, Block.OAK_FENCE.id, 0, false);
        player.setPos(5.5, 100, 4.5);
        player.yaw = 0;
        walk(60, 1, true);
        assertTrue(player.z < 6.2 && player.y < 101, "fence should not be jumpable, z=" + player.z + " y=" + player.y);
    }

    @Test
    void recipes() {
        Item planks = Item.of(Block.PLANKS), cobble = Item.of(Block.COBBLESTONE);
        ItemStack r = Recipes.match(new ItemStack[]{null, null, null, st(cobble), st(cobble), st(cobble), null, null, null}, 3);
        assertEquals(Item.of(Block.COBBLESTONE_SLAB), r.item);
        assertEquals(6, r.count);
        r = Recipes.match(new ItemStack[]{st(planks), st(planks), null, st(planks), st(planks), null, st(planks), st(planks), null}, 3);
        assertEquals(Item.of(Block.OAK_DOOR), r.item);
        r = Recipes.match(new ItemStack[]{st(planks), null, null, st(planks), st(planks), null, st(planks), st(planks), st(planks)}, 3);
        assertEquals(Item.of(Block.OAK_STAIRS), r.item);
    }

    private static ItemStack st(Item i) { return new ItemStack(i, 1); }
}
