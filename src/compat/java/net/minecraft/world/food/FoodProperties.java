package net.minecraft.world.food;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** What eating an item does (reamc-compat). */
public record FoodProperties(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, Optional<ItemStack> usingConvertsTo, List<PossibleEffect> effects) {
    public static final Codec<FoodProperties> DIRECT_CODEC = Codec.unit(() -> new FoodProperties(0, 0, false, 1.6f, Optional.empty(), List.of()));

    public int eatDurationTicks() { return (int) (eatSeconds * 20); }

    public record PossibleEffect(Supplier<MobEffectInstance> effectSupplier, float probability) {
        public MobEffectInstance effect() { return new MobEffectInstance(effectSupplier.get()); }
    }

    public static class Builder {
        private int nutrition;
        private float saturationModifier;
        private boolean canAlwaysEat;
        private float eatSeconds = 1.6f;
        private Optional<ItemStack> usingConvertsTo = Optional.empty();
        private final List<PossibleEffect> effects = new ArrayList<>();

        public Builder() { }

        public Builder nutrition(int n) { nutrition = n; return this; }
        public Builder saturationModifier(float m) { saturationModifier = m; return this; }
        public Builder alwaysEdible() { canAlwaysEat = true; return this; }
        public Builder fast() { eatSeconds = 0.8f; return this; }
        public Builder effect(MobEffectInstance e, float probability) { effects.add(new PossibleEffect(() -> e, probability)); return this; }
        public Builder effect(Supplier<MobEffectInstance> e, float probability) { effects.add(new PossibleEffect(e, probability)); return this; }
        public Builder usingConvertsTo(ItemLike item) { usingConvertsTo = Optional.of(new ItemStack(item)); return this; }

        public FoodProperties build() {
            return new FoodProperties(nutrition, nutrition * saturationModifier * 2, canAlwaysEat, eatSeconds, usingConvertsTo, List.copyOf(effects));
        }
    }
}
