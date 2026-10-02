package net.neoforged.neoforge.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Adds the rolls of another loot table (reamc-compat). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AddTableLootModifier extends LootModifier {
    public static final MapCodec<AddTableLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> LootModifier.codecStart(i)
            .and(ResourceKey.codec((ResourceKey) Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> (ResourceKey) ((AddTableLootModifier) m).table))
            .apply(i, (c, t) -> new AddTableLootModifier((LootItemCondition[]) c, (ResourceKey) t)));

    private final ResourceKey<?> table;

    public AddTableLootModifier(LootItemCondition[] conditions, ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table) {
        super(conditions);
        this.table = table;
    }

    public ResourceKey<?> table() { return table; }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        loot.addAll(mc.mod.Loot.roll(table.location(), ctx));
        return loot;
    }

    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
