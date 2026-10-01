package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Defense values, enchantability and textures shared by a set of armor (reamc-compat). */
public record ArmorMaterial(Map<ArmorItem.Type, Integer> defense, int enchantmentValue, Holder<SoundEvent> equipSound,
                            Supplier<Ingredient> repairIngredient, List<Layer> layers, float toughness, float knockbackResistance) {
    public int getDefense(ArmorItem.Type type) { return defense.getOrDefault(type, 0); }

    public static final class Layer {
        private final ResourceLocation assetName;
        private final String suffix;
        private final boolean dyeable;

        public Layer(ResourceLocation assetName) { this(assetName, "", false); }

        public Layer(ResourceLocation assetName, String suffix, boolean dyeable) {
            this.assetName = assetName;
            this.suffix = suffix;
            this.dyeable = dyeable;
        }

        public ResourceLocation texture(boolean inner) {
            return assetName.withPath(p -> "textures/models/armor/" + p + "_layer_" + (inner ? 2 : 1) + suffix + ".png");
        }

        public boolean dyeable() { return dyeable; }
        public ResourceLocation reamc$asset() { return assetName; }
    }
}
