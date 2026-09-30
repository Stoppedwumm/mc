package mc;

import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.world.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Breeding, shearing, taming and the new mobs. */
class AnimalsTest {
    private World w;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        w = new World(42, new WorldStorage(Files.createTempDirectory("mcanimals")));
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 26; x++)
            for (int z = -10; z < 26; z++) {
                for (int y = 100; y < 110; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
        player = new Player();
        w.setPlayer(player);
        player.setPos(20.5, 100, 20.5);
        w.time = 6000;
    }

    @AfterEach
    void tearDown() { w.shutdown(); }

    private Mob spawn(MobType t, double x, double z) {
        Mob m = new Mob(t);
        java.util.Arrays.fill(m.armor, null);
        m.setPos(x, 100, z);
        w.addEntity(m);
        w.flushPendingEntities();
        return m;
    }

    private void ticks(int n) { for (int i = 0; i < n; i++) w.tick(); }

    private int count(MobType t) {
        int n = 0;
        for (Entity e : w.entities()) if (e instanceof Mob m && m.type == t && !m.removed) n++;
        return n;
    }

    @Test
    void cowsBreedWithWheat() {
        Mob a = spawn(MobType.COW, 5.5, 5.5), b = spawn(MobType.COW, 6.5, 5.5);
        player.inventory.setHeld(new ItemStack(Item.WHEAT, 2));
        assertTrue(a.interact(player));
        assertTrue(b.interact(player));
        assertNull(player.inventory.held());
        ticks(200);
        assertEquals(3, count(MobType.COW));
        Mob baby = null;
        for (Entity e : w.entities()) if (e instanceof Mob m && m.isBaby()) baby = m;
        assertNotNull(baby);
        assertTrue(baby.height < 1);
        assertTrue(a.growingAge > 0, "parents get a breeding cooldown");
    }

    @Test
    void shearingAndMilking() {
        Mob sheep = spawn(MobType.SHEEP, 5.5, 5.5);
        player.inventory.setHeld(new ItemStack(Item.SHEARS, 1));
        assertTrue(sheep.interact(player));
        assertTrue(sheep.sheared);
        assertFalse(sheep.interact(player), "already sheared");
        w.flushPendingEntities();
        int wool = 0;
        for (Entity e : w.entities()) if (e instanceof ItemEntity it && it.stack.item.block != null && it.stack.item.block.name.endsWith("Wool")) wool += it.stack.count;
        assertTrue(wool >= 1 && wool <= 3);
        Mob cow = spawn(MobType.COW, 8.5, 5.5);
        player.inventory.setHeld(new ItemStack(Item.BUCKET, 1));
        assertTrue(cow.interact(player));
        assertEquals(Item.MILK_BUCKET, player.inventory.held().item);
    }

    @Test
    void wolvesCanBeTamedAndDefendTheirOwner() {
        Mob wolf = spawn(MobType.WOLF, 18.5, 18.5);
        player.inventory.setHeld(new ItemStack(Item.BONE, 64));
        for (int i = 0; i < 64 && !wolf.tamed; i++) wolf.interact(player);
        assertTrue(wolf.tamed);
        assertTrue(wolf.sitting);
        assertEquals(20, wolf.maxHealth);
        wolf.interact(player); // stand up
        assertFalse(wolf.sitting);
        Mob zombie = spawn(MobType.ZOMBIE, 14.5, 18.5);
        w.commandWolves(zombie);
        assertSame(zombie, wolf.attackTarget);
        ticks(300);
        assertTrue(zombie.isDead() || zombie.health < 20, "the wolf should have attacked the zombie");
    }

    @Test
    void bigSlimesSplit() {
        Mob slime = spawn(MobType.SLIME, 5.5, 5.5);
        slime.setSlimeSize(4);
        slime.damage(DamageSource.ATTACK, 100, player);
        w.flushPendingEntities();
        int small = 0;
        for (Entity e : w.entities()) if (e instanceof Mob m && m.type == MobType.SLIME && m.slimeSize == 2) small++;
        assertTrue(small >= 2 && small <= 4);
    }

    @Test
    void skeletonArrowsHitThePlayer() {
        player.setPos(5.5, 100, 12.5);
        w.time = 18000;
        Mob sk = spawn(MobType.SKELETON, 5.5, 3.5);
        // Skeletons are inaccurate, so give it a few volleys
        for (int i = 0; i < 1200 && player.health >= 20; i++) w.tick();
        assertTrue(player.health < 20, "skeleton should have shot the player");
    }

    @Test
    void squidSwimsAndSuffocatesOnLand() {
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) for (int y = 100; y < 104; y++) w.setBlock(x, y, z, Block.WATER.id, 0, false);
        Mob squid = spawn(MobType.SQUID, 2.5, 2.5);
        squid.setPos(2.5, 101, 2.5);
        ticks(200);
        assertEquals(10, squid.health, 1e-6);
        Mob beached = spawn(MobType.SQUID, 15.5, 15.5);
        ticks(400);
        assertTrue(beached.health < 10);
    }
}
