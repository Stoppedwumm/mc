package net.neoforged.neoforge.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Changes every matching loot roll (reamc-compat). */
public interface IGlobalLootModifier {
    @SuppressWarnings({"unchecked", "rawtypes"})
    Codec<IGlobalLootModifier> DIRECT_CODEC = net.neoforged.neoforge.registries.NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.byNameCodec()
            .dispatch(IGlobalLootModifier::codec, c -> (MapCodec) c);
    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = LootItemCondition.DIRECT_CODEC.listOf().xmap(l -> l.toArray(LootItemCondition[]::new), java.util.List::of);
    Codec<IGlobalLootModifier> CONDITIONAL_CODEC = DIRECT_CODEC;

    ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context);

    MapCodec<? extends IGlobalLootModifier> codec();
}
