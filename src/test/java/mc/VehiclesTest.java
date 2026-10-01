package mc;

import com.google.gson.JsonObject;
import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.world.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Rails, minecarts, boats and fishing on a flat stone floor at y = 99. */
class VehiclesTest {
    private World w;

    @BeforeEach
    void setUp() throws Exception {
        w = new World(42, new WorldStorage(Files.createTempDirectory("vehicles")));
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 40; x++)
            for (int z = -10; z < 30; z++) {
                for (int y = 100; y < 112; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
    }

    @AfterEach
    void tearDown() { w.shutdown(); }

    private void ticks(int n) { for (int i = 0; i < n; i++) w.tick(); }

    /** Places a rail the way a player does: shaped by its neighbours, which bend towards it. */
    private void rail(int x, int y, int z, Block b, int facing) {
        int shape = Rails.placementShape(w, x, y, z, b.id, facing);
        w.setBlock(x, y, z, b.id, shape, true);
        Rails.connectNeighbours(w, x, y, z);
        if (b == Block.POWERED_RAIL) Rails.updatePowered(w, x, y, z);
    }

    private int shape(int x, int y, int z) { return Rails.shape(w.getBlock(x, y, z), w.getMeta(x, y, z)); }

    private MinecartEntity cartAt(double x, double y, double z) {
        MinecartEntity c = new MinecartEntity();
        c.setPos(x, y, z);
        w.addEntity(c);
        w.flushPendingEntities();
        return c;
    }

    @Test
    void railsConnectIntoLinesCornersAndSlopes() {
        int east = Rails.fromFacing(3);
        for (int x = 0; x < 4; x++) rail(x, 100, 5, Block.RAIL, east);
        for (int x = 0; x < 4; x++) assertEquals(1, shape(x, 100, 5), "east-west line at " + x);
        // Turning south at the end makes a corner
        rail(3, 100, 6, Block.RAIL, Rails.fromFacing(0));
        assertTrue(shape(3, 100, 5) >= 6, "corner at the end of the line, was " + shape(3, 100, 5));
        // A rail one block up next to the line's start makes it a slope
        w.setBlock(-1, 100, 5, Block.STONE.id, 0, false);
        rail(-1, 101, 5, Block.RAIL, east);
        assertTrue(Rails.ascending(shape(0, 100, 5)), "slope up to the higher rail");
        // Rails pop off when their floor goes
        w.setBlock(2, 99, 5, 0, 0, true);
        assertEquals(0, w.getBlock(2, 100, 5));
    }

    @Test
    void cartRollsAlongTrackAndRoundCorners() {
        int east = Rails.fromFacing(3);
        for (int x = 0; x < 12; x++) rail(x, 100, 5, Block.RAIL, east);
        for (int z = 6; z < 14; z++) rail(11, 100, z, Block.RAIL, Rails.fromFacing(0));
        assertTrue(shape(11, 100, 5) >= 6);
        MinecartEntity c = cartAt(7.5, 100.0625, 5.5);
        c.motionX = 0.4;
        for (int i = 0; i < 200 && c.z < 9; i++) w.tick();
        assertTrue(c.z > 8, "followed the curve south: " + c.x + ", " + c.z);
        assertEquals(11.5, c.x, 1e-6, "centred on the north-south track");
        assertEquals(100.0625, c.y, 1e-6, "riding on the rails");
        assertTrue(c.motionZ > 0, "now moving south");
    }

    @Test
    void cartsSpeedUpDownhillAndPoweredRailsBoostOrBrake() {
        // A ramp descending eastwards: rails at y 104 down to 100
        for (int i = 0; i < 4; i++) {
            for (int y = 99; y < 104 - i; y++) w.setBlock(i, y, 5, Block.STONE.id, 0, false);
        }
        int east = Rails.fromFacing(3);
        for (int i = 0; i < 4; i++) rail(i, 104 - i, 5, Block.RAIL, east);
        for (int x = 4; x < 30; x++) rail(x, 100, 5, x == 9 ? Block.POWERED_RAIL : Block.RAIL, east);
        assertTrue(Rails.ascending(shape(1, 103, 5)));
        MinecartEntity c = cartAt(1.5, 103.6, 5.5);
        ticks(60);
        assertTrue(c.x > 6, "rolled down the slope: x = " + c.x);

        // An unpowered powered rail stops the cart; powering it launches it again
        ticks(200);
        assertEquals(9.5, c.x, 0.6, "braked on the unpowered rail");
        assertTrue(Math.abs(c.motionX) < 0.05);
        w.setBlock(9, 100, 6, Block.REDSTONE_BLOCK.id, 0, true);
        assertTrue((w.getMeta(9, 100, 5) & Rails.ACTIVE) != 0, "powered by the redstone block");
        c.motionX = 0.05;
        ticks(5);
        assertTrue(c.motionX > 0.3, "boosted: " + c.motionX);
    }

    @Test
    void detectorRailPowersWhileACartIsOnIt() {
        int east = Rails.fromFacing(3);
        for (int x = 0; x < 6; x++) rail(x, 100, 5, x == 3 ? Block.DETECTOR_RAIL : Block.RAIL, east);
        w.setBlock(3, 100, 6, Block.REDSTONE_LAMP.id, 0, true);
        MinecartEntity c = cartAt(3.5, 100.0625, 5.5);
        ticks(2);
        assertTrue((w.getMeta(3, 100, 5) & Rails.ACTIVE) != 0);
        assertEquals(Block.LIT_REDSTONE_LAMP.id, w.getBlock(3, 100, 6));
        c.remove();
        ticks(30);
        assertEquals(0, w.getMeta(3, 100, 5) & Rails.ACTIVE);
    }

    @Test
    void playersRideAndGetOffAndCartsBreakIntoItems() {
        rail(5, 100, 5, Block.RAIL, Rails.fromFacing(3));
        MinecartEntity c = cartAt(5.5, 100.0625, 5.5);
        Player p = new Player();
        p.setPos(4, 100, 5);
        w.setPlayer(p);
        assertTrue(c.mount(p));
        p.tick(w, 0, 0, false, false, false);
        w.tick();
        assertSame(c, p.vehicle);
        assertEquals(c.x, p.x, 1e-9);
        assertEquals(c.y + c.riderOffset(), p.y, 1e-9);
        // Sneaking gets off on top
        p.tick(w, 0, 0, false, true, false);
        assertNull(p.vehicle);
        assertNull(c.passenger);
        assertTrue(p.y >= c.y + c.height);
        // Five punches break it into an item
        for (int i = 0; i < 5 && !c.removed; i++) c.hit(p, 1);
        assertTrue(c.removed);
        w.flushPendingEntities();
        assertTrue(w.entities().stream().anyMatch(e -> e instanceof ItemEntity it && it.stack.item == Item.MINECART));
    }

    @Test
    void boatsFloatAndFollowTheRider() {
        for (int x = 0; x < 30; x++)
            for (int z = 0; z < 10; z++) {
                w.setBlock(x, 99, z, Block.WATER.id, 0, false);
                w.setBlock(x, 98, z, Block.SAND.id, 0, false);
            }
        BoatEntity b = new BoatEntity();
        b.setPos(5.5, 100, 5.5);
        w.addEntity(b);
        w.flushPendingEntities();
        ticks(100);
        assertTrue(b.y > 99 && b.y < 100, "floating at the surface: " + b.y);
        Player p = new Player();
        p.setPos(5, 100, 5);
        w.setPlayer(p);
        assertTrue(b.mount(p));
        p.yaw = -90; // facing east
        for (int i = 0; i < 100; i++) {
            p.tick(w, 1, 0, false, false, false);
            w.tick();
        }
        assertTrue(b.x > 9, "rowed east: " + b.x);
        assertTrue(b.y > 99 && b.y < 100, "still afloat: " + b.y);
    }

    @Test
    void fishingCatchesSomethingWhenReeledInDuringABite() {
        for (int x = 0; x < 12; x++)
            for (int z = 0; z < 12; z++) {
                w.setBlock(x, 99, z, Block.WATER.id, 0, false);
                w.setBlock(x, 98, z, Block.WATER.id, 0, false);
            }
        Player p = new Player();
        p.setPos(5.5, 100, 2.5);
        p.world = w;
        p.inventory.setHeld(new ItemStack(Item.FISHING_ROD, 1));
        w.setPlayer(p);
        FishingBobberEntity b = new FishingBobberEntity(p);
        b.world = w;
        b.setPos(5.5, 99.9, 6.5);
        w.addEntity(b);
        w.flushPendingEntities();
        boolean bit = false;
        for (int i = 0; i < 4000 && !bit; i++) {
            w.tick();
            bit = b.biting();
        }
        assertTrue(bit, "a fish bit");
        assertFalse(b.removed);
        assertTrue(b.y > 98.5 && b.y < 100.2, "bobber floats: " + b.y);
        assertEquals(1, b.reel());
        w.flushPendingEntities();
        assertTrue(w.entities().stream().anyMatch(e -> e instanceof ItemEntity), "the catch flies out");
        assertTrue(w.entities().stream().anyMatch(e -> e instanceof XpOrbEntity));
    }

    @Test
    void vehiclesSaveAndRecipesExist() {
        MinecartEntity c = cartAt(3.5, 100.0625, 3.5);
        JsonObject o = EntityCodec.write(c);
        Entity back = EntityCodec.read(o);
        assertInstanceOf(MinecartEntity.class, back);
        assertEquals(3.5, back.x, 1e-9);
        assertInstanceOf(BoatEntity.class, EntityCodec.read(EntityCodec.write(new BoatEntity())));
        assertTrue(EntityCodec.persistent(c));
        assertInstanceOf(FishingBobberEntity.class, EntityCodec.readNet(EntityCodec.writeNet(new FishingBobberEntity(null)), null));

        ItemStack[] grid = new ItemStack[9];
        ItemStack iron = new ItemStack(Item.IRON_INGOT, 1);
        grid[0] = iron; grid[2] = iron; grid[3] = iron; grid[4] = iron; grid[5] = iron;
        ItemStack out = Recipes.match(grid, 3);
        assertNotNull(out);
        assertEquals(Item.MINECART, out.item);
        assertEquals(Item.COOKED_FISH, Recipes.smelting(Item.RAW_FISH).item);
    }
}
