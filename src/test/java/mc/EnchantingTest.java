package mc;

import mc.entity.DamageSource;
import mc.entity.Player;
import mc.item.Enchantment;
import mc.item.Item;
import mc.item.ItemStack;
import mc.world.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EnchantingTest {
    @Test
    void rollsOnlyApplicableEnchantments() {
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            Map<Enchantment, Integer> e = Enchantment.roll(Item.DIAMOND_PICKAXE, 30, r);
            assertFalse(e.isEmpty());
            for (var en : e.entrySet()) {
                assertTrue(en.getKey().canApply(Item.DIAMOND_PICKAXE), en.getKey() + " on a pickaxe");
                assertTrue(en.getValue() >= 1 && en.getValue() <= en.getKey().maxLevel);
            }
            assertFalse(e.containsKey(Enchantment.SILK_TOUCH) && e.containsKey(Enchantment.FORTUNE));
        }
        Map<Enchantment, Integer> armor = Enchantment.roll(Item.armor(2, 3), 30, new Random(5));
        for (Enchantment e : armor.keySet()) assertTrue(e.kind == Enchantment.Kind.ARMOR || e.kind == Enchantment.Kind.FEET || e.kind == Enchantment.Kind.BREAKABLE);
    }

    @Test
    void silkTouchAndFortune() {
        ItemStack silk = new ItemStack(Item.DIAMOND_PICKAXE, 1);
        silk.enchant(Enchantment.SILK_TOUCH, 1);
        List<ItemStack> d = Drops.of(Block.DIAMOND_ORE, 0, silk, new Random(1));
        assertEquals(Item.of(Block.DIAMOND_ORE), d.get(0).item);
        d = Drops.of(Block.STONE, 0, silk, new Random(1));
        assertEquals(Item.of(Block.STONE), d.get(0).item);
        ItemStack fortune = new ItemStack(Item.DIAMOND_PICKAXE, 1);
        fortune.enchant(Enchantment.FORTUNE, 3);
        int total = 0;
        Random r = new Random(3);
        for (int i = 0; i < 400; i++) total += Drops.of(Block.DIAMOND_ORE, 0, fortune, r).get(0).count;
        assertTrue(total > 600, "Fortune III averages 2.2 diamonds per ore, got " + total / 400.0);
    }

    @Test
    void unbreakingSavesDurability() {
        ItemStack plain = new ItemStack(Item.IRON_PICKAXE, 1), unb = new ItemStack(Item.IRON_PICKAXE, 1);
        unb.enchant(Enchantment.UNBREAKING, 3);
        for (int i = 0; i < 200; i++) { plain.damageTool(1); unb.damageTool(1); }
        assertEquals(200, plain.damage);
        assertTrue(unb.damage < 100, "Unbreaking III uses about a quarter: " + unb.damage);
    }

    @Test
    void protectionAndFeatherFalling() throws Exception {
        World w = new World(1, new WorldStorage(Files.createTempDirectory("ench")));
        Player p = new Player();
        w.setPlayer(p);
        ItemStack boots = new ItemStack(Item.armor(0, 3), 1);
        boots.enchant(Enchantment.FEATHER_FALLING, 4);
        p.inventory.armor[3] = boots;
        p.damage(DamageSource.FALL, 10, null);
        // EPF 12 -> 52% less
        assertEquals(20 - 10 * (1 - 12 / 25f), p.health, 1e-3);
        w.shutdown();
    }

    @Test
    void enchantmentsSurviveSaving() {
        ItemStack s = new ItemStack(Item.DIAMOND_SWORD, 1, 12);
        s.enchant(Enchantment.SHARPNESS, 5);
        s.enchant(Enchantment.LOOTING, 3);
        ItemStack back = ItemStack.fromArray(s.toArray());
        assertEquals(12, back.damage);
        assertEquals(5, back.level(Enchantment.SHARPNESS));
        assertEquals(3, back.level(Enchantment.LOOTING));
    }
}
