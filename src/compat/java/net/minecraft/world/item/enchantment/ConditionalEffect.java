package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.Optional;

/** An enchantment effect with an optional condition (reamc-compat: conditions are not evaluated). */
public record ConditionalEffect<T>(T effect, Optional<Object> requirements) {
    public static <T> Codec<ConditionalEffect<T>> codec(Codec<T> effectCodec, LootContextParamSet params) {
        return RecordCodecBuilder.create(i -> i.group(effectCodec.fieldOf("effect").forGetter(ConditionalEffect::effect)).apply(i, e -> new ConditionalEffect<>(e, Optional.empty())));
    }
}
