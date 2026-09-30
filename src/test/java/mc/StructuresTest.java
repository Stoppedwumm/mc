package mc;

import mc.entity.Entity;
import mc.entity.Mob;
import mc.entity.MobType;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Trades;
import mc.world.*;
import mc.world.gen.Structures;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Villages, dungeons, trading and golems. */
class StructuresTest {
    @Test
    void villageIsBuiltWithVillagers() throws Exception {
        World w = new World(777, new WorldStorage(Files.createTempDirectory("village")));
        Structures s = w.decorator.structures;
        int[] v = s.nearestVillage(0, 0);
        assertNotNull(v, "a village should exist near spawn");
        w.loadAreaBlocking(v[0] >> 4, v[1] >> 4, 4);
        w.flushPendingEntities();
        int doors = 0, farmland = 0;
        for (int x = v[0] - 48; x < v[0] + 48; x++)
            for (int z = v[1] - 48; z < v[1] + 48; z++)
                for (int y = 40; y < 140; y++) {
                    int id = w.getBlock(x, y, z);
                    if (id == Block.OAK_DOOR.id) doors++;
                    if (id == Block.FARMLAND.id) farmland++;
                }
        int villagers = 0, golems = 0;
        for (Entity e : w.entities()) {
            if (e instanceof Mob m && m.type == MobType.VILLAGER) villagers++;
            if (e instanceof Mob m && m.type == MobType.IRON_GOLEM) golems++;
        }
        assertTrue(doors >= 2, "houses with doors: " + doors);
        assertTrue(villagers >= 2, "villagers: " + villagers);
        assertTrue(doors + farmland >= 6, "doors " + doors + " farmland " + farmland);
        assertTrue(golems <= 1, "golems " + golems);
        w.shutdown();
    }

    @Test
    void dungeonsHaveSpawnersAndLoot() throws Exception {
        World w = new World(4242, new WorldStorage(Files.createTempDirectory("dungeon")));
        w.loadAreaBlocking(0, 0, 5);
        int spawners = 0, lootChests = 0;
        for (BlockEntity be : w.blockEntities.values()) {
            if (be instanceof BlockEntity.Spawner) spawners++;
            if (be instanceof BlockEntity.Chest c) for (ItemStack st : c.slots) if (st != null) { lootChests++; break; }
        }
        assertTrue(spawners > 0, "expected at least one dungeon in 121 chunks");
        assertTrue(lootChests > 0);
        w.shutdown();
    }

    @Test
    void tradingSwapsItems() {
        mc.item.Inventory inv = new mc.item.Inventory();
        inv.add(new ItemStack(Item.WHEAT, 30));
        Trades.Offer sell = Trades.offers(0).get(0);
        assertTrue(Trades.canAfford(inv, sell));
        ItemStack out = Trades.trade(inv, sell);
        assertEquals(Item.EMERALD, out.item);
        assertEquals(10, inv.count(Item.WHEAT));
        assertNull(Trades.trade(inv, sell));
    }

    @Test
    void pumpkinOnIronMakesAGolem() throws Exception {
        World w = new World(42, new WorldStorage(Files.createTempDirectory("golem")));
        w.loadAreaBlocking(0, 0, 1);
        w.setBlock(5, 100, 5, Block.IRON_BLOCK.id);
        w.setBlock(5, 101, 5, Block.IRON_BLOCK.id);
        w.setBlock(4, 101, 5, Block.IRON_BLOCK.id);
        w.setBlock(6, 101, 5, Block.IRON_BLOCK.id);
        w.setBlock(5, 102, 5, Block.PUMPKIN.id);
        assertTrue(w.trySpawnGolem(5, 102, 5));
        w.flushPendingEntities();
        assertEquals(0, w.getBlock(5, 101, 5));
        assertTrue(w.entities().stream().anyMatch(e -> e instanceof Mob m && m.type == MobType.IRON_GOLEM));
        w.shutdown();
    }
}
