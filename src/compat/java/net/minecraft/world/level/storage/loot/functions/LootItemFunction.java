package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.function.BiFunction;

/** Changes a looted stack (reamc-compat). */
public interface LootItemFunction extends BiFunction<ItemStack, LootContext, ItemStack> {
    Codec<LootItemFunction> ROOT_CODEC = mc.mod.Loot.FUNCTION_CODEC;

    LootItemFunctionType<? extends LootItemFunction> getType();

    interface Builder {
        LootItemFunction build();
    }
}
