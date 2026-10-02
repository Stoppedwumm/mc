package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.function.Predicate;

/** A loot condition (reamc-compat: Minecraft's are evaluated by reamc, mods' through their registered codecs). */
public interface LootItemCondition extends Predicate<LootContext> {
    Codec<LootItemCondition> DIRECT_CODEC = mc.mod.Loot.CONDITION_CODEC;
    Codec<LootItemCondition> CODEC = DIRECT_CODEC;

    LootItemConditionType getType();

    interface Builder {
        LootItemCondition build();

        default Builder invert() { LootItemCondition c = build(); return () -> new LootItemCondition() {
            @Override public LootItemConditionType getType() { return c.getType(); }
            @Override public boolean test(LootContext ctx) { return !c.test(ctx); }
        }; }
    }
}
