package net.minecraft.world;

public class DifficultyInstance {
    private final Difficulty base;
    private final float effective;

    public DifficultyInstance(Difficulty base, long dayTime, long inhabited, float moon) {
        this.base = base;
        this.effective = base == Difficulty.PEACEFUL ? 0 : 0.75f + Math.min(1, dayTime / 1440000f) * 0.25f;
    }

    public Difficulty getDifficulty() { return base; }
    public float getEffectiveDifficulty() { return effective; }
    public boolean isHard() { return base == Difficulty.HARD; }
    public boolean isHarderThan(float f) { return effective > f; }
    public float getSpecialMultiplier() { return 0; }
}
