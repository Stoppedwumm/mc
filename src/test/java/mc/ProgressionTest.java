package mc;

import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.world.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Armor, experience and entity persistence. */
class ProgressionTest {
    private static World flatWorld(Path dir) throws Exception {
        World w = new World(42, new WorldStorage(dir));
        w.loadAreaBlocking(0, 0, 2);
        for (int x = -10; x < 26; x++)
            for (int z = -10; z < 26; z++) {
                for (int y = 100; y < 110; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
        return w;
    }

    @Test
    void armorReducesDamageAndWears() throws Exception {
        World w = flatWorld(Files.createTempDirectory("mcarmor"));
        Player p = new Player();
        w.setPlayer(p);
        p.setPos(5, 100, 5);
        p.damage(DamageSource.ATTACK, 10, null);
        assertEquals(10, p.health, 1e-4);
        p.health = 20;
        p.invulnerableTime = 0;
        for (int i = 0; i < 4; i++) p.inventory.armor[i] = new ItemStack(Item.armor(4, i), 1);
        assertEquals(20, p.armorValue());
        p.damage(DamageSource.ATTACK, 10, null);
        // Full diamond: 20 armor, 8 toughness -> 10 * (1 - max(4, 20 - 10 / 4) / 25) = 3
        assertEquals(17, p.health, 1e-3);
        assertEquals(2, p.inventory.armor[1].damage);
        // Fall damage ignores armor
        p.invulnerableTime = 0;
        p.damage(DamageSource.FALL, 5, null);
        assertEquals(12, p.health, 1e-3);
        w.shutdown();
    }

    @Test
    void experienceLevels() {
        Player p = new Player();
        p.addXp(7);
        assertEquals(1, p.xpLevel);
        assertEquals(0, p.xpProgress, 1e-6);
        p.addXp(9 + 11 + 13 + 5);
        assertEquals(4, p.xpLevel);
        assertEquals(5f / 15, p.xpProgress, 1e-5);
    }

    @Test
    void killingMobsDropsExperience() throws Exception {
        World w = flatWorld(Files.createTempDirectory("mcxp"));
        Player p = new Player();
        w.setPlayer(p);
        p.setPos(20, 100, 20);
        Mob zombie = new Mob(MobType.ZOMBIE);
        java.util.Arrays.fill(zombie.armor, null);
        zombie.setPos(5, 100, 5);
        w.addEntity(zombie);
        w.flushPendingEntities();
        zombie.damage(DamageSource.ATTACK, 100, p);
        w.flushPendingEntities();
        int xp = 0;
        for (Entity e : w.entities()) if (e instanceof XpOrbEntity o) xp += o.value;
        assertEquals(5, xp);
        w.shutdown();
    }

    @Test
    void entitiesAreSavedWithTheirChunk() throws Exception {
        Path dir = Files.createTempDirectory("mcsave");
        World w = flatWorld(dir);
        Mob cow = new Mob(MobType.COW);
        cow.setPos(5.5, 100, 6.5);
        cow.health = 7;
        w.addEntity(cow);
        w.addEntity(new ItemEntity(new ItemStack(Item.DIAMOND, 3)));
        w.entities();
        w.flushPendingEntities();
        w.entities().get(1).setPos(8.5, 100, 8.5);
        w.saveAll();
        w.shutdown();

        World w2 = new World(42, new WorldStorage(dir));
        w2.loadAreaBlocking(0, 0, 2);
        w2.flushPendingEntities();
        Mob loaded = null;
        ItemEntity item = null;
        for (Entity e : w2.entities()) {
            if (e instanceof Mob m) loaded = m;
            if (e instanceof ItemEntity it) item = it;
        }
        assertNotNull(loaded);
        assertEquals(MobType.COW, loaded.type);
        assertEquals(7, loaded.health, 1e-6);
        assertEquals(6.5, loaded.z, 1e-6);
        assertNotNull(item);
        assertEquals(Item.DIAMOND, item.stack.item);
        assertEquals(3, item.stack.count);
        w2.shutdown();
    }

    @Test
    void armorRecipes() {
        Item i = Item.IRON_INGOT;
        ItemStack r = Recipes.match(new ItemStack[]{st(i), null, st(i), st(i), st(i), st(i), st(i), st(i), st(i)}, 3);
        assertEquals(Item.armor(2, 1), r.item);
        r = Recipes.match(new ItemStack[]{st(Item.LEATHER), st(Item.LEATHER), st(Item.LEATHER), st(Item.LEATHER), null, st(Item.LEATHER), null, null, null}, 3);
        assertEquals(Item.armor(0, 0), r.item);
    }

    private static ItemStack st(Item i) { return new ItemStack(i, 1); }
}
