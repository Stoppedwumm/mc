package net.minecraft.world.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** A player's hunger, backed by reamc's player (reamc-compat). */
public class FoodData {
    private final mc.entity.Player player;
    private int lastFoodLevel = 20;

    public FoodData() { this(new mc.entity.Player()); }

    public FoodData(mc.entity.Player player) { this.player = player; }

    public void eat(int nutrition, float saturation) { player.eat(nutrition, saturation); }

    public void eat(FoodProperties food) { eat(food.nutrition(), food.saturation()); }

    public void tick(Player p) { lastFoodLevel = player.food; }

    public void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("foodLevel", 99)) {
            player.food = t.getInt("foodLevel");
            player.saturation = t.getFloat("foodSaturationLevel");
            player.exhaustion = t.getFloat("foodExhaustionLevel");
        }
    }

    public void addAdditionalSaveData(CompoundTag t) {
        t.putInt("foodLevel", player.food);
        t.putFloat("foodSaturationLevel", player.saturation);
        t.putFloat("foodExhaustionLevel", player.exhaustion);
    }

    public int getFoodLevel() { return player.food; }
    public int getLastFoodLevel() { return lastFoodLevel; }
    public boolean needsFood() { return player.food < 20; }
    public void addExhaustion(float f) { player.addExhaustion(f); }
    public float getExhaustionLevel() { return player.exhaustion; }
    public float getSaturationLevel() { return player.saturation; }
    public void setFoodLevel(int f) { player.food = Math.max(0, Math.min(20, f)); }
    public void setSaturation(float s) { player.saturation = Math.max(0, Math.min(player.food, s)); }
    public void setExhaustion(float e) { player.exhaustion = e; }
}
