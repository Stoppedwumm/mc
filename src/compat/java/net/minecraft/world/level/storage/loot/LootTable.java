package net.minecraft.world.level.storage.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

/** A loot table, rolled by reamc's evaluator (reamc-compat). */
public class LootTable {
    public static final LootTable EMPTY = new LootTable(null);
    private final ResourceLocation id;

    public LootTable(ResourceLocation id) { this.id = id; }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params) {
        ObjectArrayList<ItemStack> l = new ObjectArrayList<>();
        if (id != null) l.addAll(mc.mod.Loot.roll(id, new LootContext.Builder(params).create(java.util.Optional.of(id))));
        return l;
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params, long seed) { return getRandomItems(params); }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams params, LootContextParamSet set) { return getRandomItems(params); }
}
