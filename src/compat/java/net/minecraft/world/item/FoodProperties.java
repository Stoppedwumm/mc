package net.minecraft.world.item;

/** Hunger restored by a food (reamc-compat keeps nutrition and saturation). */
public record FoodProperties(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds) {
    public static class Builder {
        private int nutrition;
        private float modifier;
        private boolean always;
        private float seconds = 1.6f;

        public Builder nutrition(int n) { nutrition = n; return this; }
        public Builder saturationModifier(float m) { modifier = m; return this; }
        public Builder alwaysEdible() { always = true; return this; }
        public Builder fast() { seconds = 0.8f; return this; }
        public Builder effect(Object effect, float probability) { return this; }
        public FoodProperties build() { return new FoodProperties(nutrition, nutrition * modifier * 2, always, seconds); }
    }
}
