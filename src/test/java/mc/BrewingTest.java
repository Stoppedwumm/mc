package mc;

import mc.entity.DamageSource;
import mc.entity.Effect;
import mc.entity.Mob;
import mc.entity.MobType;
import mc.entity.Player;
import mc.entity.ThrownEntity;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Potions;
import mc.world.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class BrewingTest {
    @Test
    void brewingRecipes() {
        int water = Potions.meta(Potions.Type.WATER);
        int awkward = Potions.brew(water, Item.NETHER_WART);
        assertEquals(Potions.Type.AWKWARD, Potions.type(awkward));
        int speed = Potions.brew(awkward, Item.SUGAR);
        assertEquals(Potions.Type.SWIFTNESS, Potions.type(speed));
        int longSpeed = Potions.brew(speed, Item.REDSTONE);
        assertEquals(9600, Potions.duration(longSpeed));
        assertEquals(-1, Potions.brew(longSpeed, Item.REDSTONE));
        int slow = Potions.brew(longSpeed, Item.FERMENTED_SPIDER_EYE);
        assertEquals(Potions.Type.SLOWNESS, Potions.type(slow));
        assertEquals(4800, Potions.duration(slow));
        int healing2 = Potions.brew(Potions.brew(awkward, Item.GLISTERING_MELON), Item.GLOWSTONE_DUST);
        assertEquals(1, Potions.amplifier(healing2));
        assertEquals(Potions.Type.HARMING, Potions.type(Potions.brew(healing2, Item.FERMENTED_SPIDER_EYE)));
        assertEquals(-1, Potions.brew(water, Item.DIAMOND));
        assertEquals("Potion of Swiftness", Item.displayName(new ItemStack(Item.POTION, 1, speed)));
    }

    @Test
    void brewingStandBrewsWithBlazePowder() throws Exception {
        World w = new World(1, new WorldStorage(Files.createTempDirectory("brew")));
        BlockEntity.BrewingStand stand = new BlockEntity.BrewingStand(0, 70, 0);
        stand.slots[0] = new ItemStack(Item.POTION, 1, 0);
        stand.slots[2] = new ItemStack(Item.POTION, 1, 0);
        stand.slots[3] = new ItemStack(Item.NETHER_WART, 2);
        stand.slots[4] = new ItemStack(Item.BLAZE_POWDER, 1);
        for (int i = 0; i < BlockEntity.BrewingStand.BREW_TOTAL + 5; i++) stand.tick(w);
        assertEquals(Potions.Type.AWKWARD, Potions.type(stand.slots[0].damage));
        assertEquals(Potions.Type.AWKWARD, Potions.type(stand.slots[2].damage));
        assertNull(stand.slots[1]);
        assertEquals(1, stand.slots[3].count, "one ingredient used per brew");
        assertEquals(19, stand.fuel, "blaze powder gives 20 brews");
        w.shutdown();
    }

    @Test
    void effectsTickAndExpire() throws Exception {
        World w = new World(1, new WorldStorage(Files.createTempDirectory("fx")));
        Player p = new Player();
        w.setPlayer(p);
        p.setPos(0.5, 200, 0.5);
        p.health = 10;
        Potions.apply(p, Potions.meta(Potions.Type.POISON), 1);
        assertTrue(p.hasEffect(Effect.POISON));
        for (int i = 0; i < 900; i++) { p.invulnerableTime = 0; p.tickEffects(); }
        assertEquals(1, p.health, 1e-3, "poison never kills");
        assertFalse(p.hasEffect(Effect.POISON));

        Potions.apply(p, Potions.meta(Potions.Type.HEALING) | Potions.STRONG, 1);
        assertEquals(9, p.health, 1e-3);

        p.addEffect(Effect.ABSORPTION, 0, 2400);
        assertEquals(4, p.absorption, 1e-3);
        p.invulnerableTime = 0;
        p.damage(DamageSource.ATTACK, 3, null);
        assertEquals(9, p.health, 1e-3, "absorption soaks damage first");
        assertEquals(1, p.absorption, 1e-3);

        p.addEffect(Effect.SPEED, 1, 100);
        assertEquals(1.4f, p.speedFactor(), 1e-4);
        p.clearEffects();
        assertEquals(1f, p.speedFactor(), 1e-4);
        w.shutdown();
    }

    @Test
    void splashPotionsHitNearbyAndHealingHurtsUndead() throws Exception {
        World w = new World(1, new WorldStorage(Files.createTempDirectory("splash")));
        Player p = new Player();
        w.setPlayer(p);
        p.setPos(0.5, 200, 0.5);
        Mob zombie = new Mob(MobType.ZOMBIE);
        zombie.setPos(1.5, 200, 0.5);
        w.addEntity(zombie);
        Mob far = new Mob(MobType.COW);
        far.setPos(20.5, 200, 0.5);
        w.addEntity(far);
        w.flushPendingEntities();
        float before = zombie.health;
        Potions.apply(zombie, Potions.meta(Potions.Type.HEALING), 1);
        assertTrue(zombie.health < before, "healing damages the undead");

        ThrownEntity t = new ThrownEntity(Item.SPLASH_POTION, null);
        t.potionMeta = Potions.meta(Potions.Type.NIGHT_VISION);
        t.setPos(0.5, 200.2, 0.5);
        w.addEntity(t);
        w.flushPendingEntities();
        t.impact(null);
        assertTrue(p.hasEffect(Effect.NIGHT_VISION));
        assertTrue(zombie.hasEffect(Effect.NIGHT_VISION));
        assertFalse(far.hasEffect(Effect.NIGHT_VISION));
        w.shutdown();
    }
}
