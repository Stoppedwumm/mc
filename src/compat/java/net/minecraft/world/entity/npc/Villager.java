package net.minecraft.world.entity.npc;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Villagers as mods see them; mods may replace the foods villagers value (NeoForge makes these writable) (reamc-compat). */
public class Villager extends Mob {
    public static Map<Item, Integer> FOOD_POINTS = new HashMap<>(Map.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1));
    public static Set<Item> WANTED_ITEMS = new HashSet<>(Set.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, Items.BEETROOT_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD));

    public Villager(mc.entity.Mob engine) { super(engine); }
}
