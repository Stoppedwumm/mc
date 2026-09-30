package mc;

import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.world.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameplayTest {
    private World w;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        w = new World(42, new WorldStorage(Files.createTempDirectory("mcgame")));
        w.loadAreaBlocking(0, 0, 3);
        // Flat stone test area at y=100 with open air above
        for (int x = -20; x < 36; x++)
            for (int z = -20; z < 36; z++) {
                for (int y = 100; y < 120; y++) w.setBlock(x, y, z, 0, 0, false);
                w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            }
        player = new Player();
        w.setPlayer(player);
        player.setPos(30.5, 100, 30.5);
    }

    @AfterEach
    void tearDown() {
        w.shutdown();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) w.tick();
    }

    private static ItemStack[] grid(Item... items) {
        ItemStack[] g = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) g[i] = items[i] == null ? null : new ItemStack(items[i], 1);
        return g;
    }

    @Test
    void crafting() {
        Item log = Item.of(Block.OAK_LOG), planks = Item.of(Block.PLANKS), stick = Item.STICK, cobble = Item.of(Block.COBBLESTONE);
        ItemStack r = Recipes.match(grid(null, log, null, null), 2);
        assertEquals(planks, r.item);
        assertEquals(4, r.count);
        r = Recipes.match(grid(planks, null, planks, null), 2);
        assertEquals(Item.STICK, r.item);
        r = Recipes.match(grid(planks, planks, planks, planks), 2);
        assertEquals(Item.of(Block.CRAFTING_TABLE), r.item);
        r = Recipes.match(grid(cobble, cobble, cobble, null, stick, null, null, stick, null), 3);
        assertEquals(Item.STONE_PICKAXE, r.item);
        // Axe works mirrored
        r = Recipes.match(grid(planks, planks, null, stick, planks, null, stick, null, null), 3);
        assertEquals(Item.WOODEN_AXE, r.item);
        // Pattern shifted into a corner of the grid still matches
        r = Recipes.match(grid(null, null, null, null, null, Item.COAL, null, null, stick), 3);
        assertEquals(Item.of(Block.TORCH), r.item);
        assertNull(Recipes.match(grid(stick, stick, stick, stick), 2));
    }

    @Test
    void furnaceSmelts() {
        w.setBlock(5, 100, 5, Block.FURNACE.id);
        var f = (BlockEntity.Furnace) w.getOrCreateBlockEntity(5, 100, 5);
        f.slots[0] = new ItemStack(Item.of(Block.IRON_ORE), 2);
        f.slots[1] = new ItemStack(Item.COAL, 1);
        ticks(205);
        assertEquals(Block.LIT_FURNACE.id, w.getBlock(5, 100, 5));
        assertNotNull(f.slots[2]);
        assertEquals(Item.IRON_INGOT, f.slots[2].item);
        ticks(205);
        assertEquals(2, f.slots[2].count);
    }

    @Test
    void waterFlowsAndDriesUp() {
        w.setBlock(0, 100, 0, Block.WATER.id, 0, true);
        ticks(200);
        assertEquals(Block.WATER.id, w.getBlock(7, 100, 0), "water spreads 7 blocks");
        assertEquals(7, w.getMeta(7, 100, 0));
        assertEquals(0, w.getBlock(8, 100, 0));
        w.setBlock(0, 100, 0, 0, 0, true);
        ticks(300);
        assertEquals(0, w.getBlock(3, 100, 0), "flowing water disappears without a source");
    }

    @Test
    void infiniteWaterSource() {
        w.setBlock(0, 100, 0, Block.WATER.id, 0, true);
        w.setBlock(2, 100, 0, Block.WATER.id, 0, true);
        ticks(100);
        assertEquals(Block.WATER.id, w.getBlock(1, 100, 0));
        assertEquals(0, w.getMeta(1, 100, 0), "block between two sources becomes a source");
    }

    @Test
    void lavaMeetsWater() {
        w.setBlock(0, 100, 0, Block.LAVA.id, 0, true);
        w.setBlock(1, 100, 0, Block.WATER.id, 0, true);
        ticks(60);
        assertEquals(Block.OBSIDIAN.id, w.getBlock(0, 100, 0));
    }

    @Test
    void harvestRules() {
        List<ItemStack> hand = Drops.of(Block.STONE, 0, null);
        assertTrue(hand.isEmpty(), "stone needs a pickaxe");
        List<ItemStack> pick = Drops.of(Block.STONE, 0, new ItemStack(Item.WOODEN_PICKAXE, 1));
        assertEquals(Item.of(Block.COBBLESTONE), pick.get(0).item);
        assertTrue(Drops.of(Block.DIAMOND_ORE, 0, new ItemStack(Item.STONE_PICKAXE, 1)).isEmpty());
        assertEquals(Item.DIAMOND, Drops.of(Block.DIAMOND_ORE, 0, new ItemStack(Item.IRON_PICKAXE, 1)).get(0).item);
        assertEquals(Item.of(Block.DIRT), Drops.of(Block.GRASS, 0, null).get(0).item);
    }

    @Test
    void sandFalls() {
        w.setBlock(3, 105, 3, Block.SAND.id);
        w.checkFalling(3, 105, 3);
        ticks(60);
        assertEquals(0, w.getBlock(3, 105, 3));
        assertEquals(Block.SAND.id, w.getBlock(3, 100, 3));
    }

    @Test
    void explosionMakesCrater() {
        player.setPos(10.5, 100, 10.5);
        float before = player.health;
        w.explode(10.5, 100.5, 12.5, 3, null);
        assertEquals(0, w.getBlock(10, 99, 12), "floor destroyed");
        assertTrue(player.health < before, "player hurt by the blast");
    }

    @Test
    void zombieChasesAndAttacks() {
        player.setPos(20.5, 100, 20.5);
        w.time = 18000; // night so it doesn't burn
        Mob z = new Mob(MobType.ZOMBIE);
        z.setPos(12.5, 100, 20.5);
        w.addEntity(z);
        float before = player.health;
        for (int i = 0; i < 200; i++) {
            player.tick(0, 0, false, false, false);
            w.tick();
        }
        assertTrue(z.distanceTo(player) < 3, "zombie reached the player");
        assertTrue(player.health < before, "zombie hurt the player");
    }

    @Test
    void mobDropsLootOnDeath() {
        Mob cow = new Mob(MobType.COW);
        cow.setPos(5.5, 100, 5.5);
        w.addEntity(cow);
        ticks(1);
        cow.damage(DamageSource.ATTACK, 100, player);
        ticks(25);
        assertTrue(cow.removed);
        boolean beef = w.entities().stream().anyMatch(e -> e instanceof ItemEntity it && it.stack.item == Item.RAW_BEEF);
        assertTrue(beef, "cow dropped beef");
    }

    @Test
    void hungerDrainsWhenSprinting() {
        player.setPos(0.5, 100, 0.5);
        player.saturation = 0;
        int food = player.food;
        for (int i = 0; i < 400; i++) {
            player.yaw = (i / 40) % 2 == 0 ? 0 : 180;
            player.tick(1, 0, true, false, true);
        }
        assertTrue(player.food < food, "sprint-jumping costs food");
    }

    @Test
    void fallDamage() {
        player.setPos(0.5, 110, 0.5);
        player.saturation = 0;
        player.food = 17;
        for (int i = 0; i < 60; i++) player.tick(0, 0, false, false, false);
        assertEquals(20 - 7, player.health, 0.01, "falling 10 blocks deals 7 damage");
    }
}
