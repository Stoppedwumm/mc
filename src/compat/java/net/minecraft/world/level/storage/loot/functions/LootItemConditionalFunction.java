package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** A loot function that only applies when its conditions hold (reamc-compat). */
public abstract class LootItemConditionalFunction implements LootItemFunction {
    protected final List<LootItemCondition> predicates;

    protected LootItemConditionalFunction(List<LootItemCondition> predicates) { this.predicates = predicates; }

    protected static <T extends LootItemConditionalFunction> Products.P1<RecordCodecBuilder.Mu<T>, List<LootItemCondition>> commonFields(RecordCodecBuilder.Instance<T> i) {
        return i.group(LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(f -> f.predicates));
    }

    @Override
    public final ItemStack apply(ItemStack stack, LootContext ctx) {
        for (LootItemCondition c : predicates) if (!c.test(ctx)) return stack;
        return run(stack, ctx);
    }

    protected abstract ItemStack run(ItemStack stack, LootContext ctx);

    protected static Builder<?> simpleBuilder(Function<List<LootItemCondition>, LootItemFunction> f) { return new DummyBuilder(f); }

    public abstract static class Builder<T extends Builder<T>> implements LootItemFunction.Builder {
        private final List<LootItemCondition> conditions = new ArrayList<>();

        public T when(LootItemCondition.Builder b) { conditions.add(b.build()); return getThis(); }
        protected abstract T getThis();
        protected List<LootItemCondition> getConditions() { return conditions; }
    }

    static final class DummyBuilder extends Builder<DummyBuilder> {
        private final Function<List<LootItemCondition>, LootItemFunction> f;

        DummyBuilder(Function<List<LootItemCondition>, LootItemFunction> f) { this.f = f; }

        @Override protected DummyBuilder getThis() { return this; }
        @Override public LootItemFunction build() { return f.apply(getConditions()); }
    }
}
