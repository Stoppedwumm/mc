package net.neoforged.neoforge.common.loot;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** A global loot modifier with conditions (reamc-compat). */
public abstract class LootModifier implements IGlobalLootModifier {
    protected final LootItemCondition[] conditions;

    protected LootModifier(LootItemCondition[] conditions) { this.conditions = conditions; }

    protected static <T extends LootModifier> Products.P1<RecordCodecBuilder.Mu<T>, LootItemCondition[]> codecStart(RecordCodecBuilder.Instance<T> i) {
        return i.group(IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(m -> m.conditions));
    }

    @Override
    public final ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        for (LootItemCondition c : conditions) if (!c.test(ctx)) return loot;
        return doApply(loot, ctx);
    }

    protected abstract ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx);
}
