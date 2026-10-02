package net.minecraft.world.level;

import java.util.HashMap;
import java.util.Map;

/** Game rules (reamc-compat: Minecraft's defaults, read-only). */
public class GameRules {
    public static final GameRules DEFAULTS = new GameRules();
    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();

    private static <T extends Value<T>> Key<T> register(String id, Category c, Object def) {
        DEFAULT_VALUES.put(id, def);
        return new Key<>(id, c);
    }

    public static final Key<BooleanValue> RULE_DOFIRETICK = register("doFireTick", Category.UPDATES, true);
    public static final Key<BooleanValue> RULE_MOBGRIEFING = register("mobGriefing", Category.MOBS, true);
    public static final Key<BooleanValue> RULE_KEEPINVENTORY = register("keepInventory", Category.PLAYER, false);
    public static final Key<BooleanValue> RULE_DOMOBSPAWNING = register("doMobSpawning", Category.SPAWNING, true);
    public static final Key<BooleanValue> RULE_DOMOBLOOT = register("doMobLoot", Category.DROPS, true);
    public static final Key<BooleanValue> RULE_DOBLOCKDROPS = register("doTileDrops", Category.DROPS, true);
    public static final Key<BooleanValue> RULE_DOENTITYDROPS = register("doEntityDrops", Category.DROPS, true);
    public static final Key<BooleanValue> RULE_NATURAL_REGENERATION = register("naturalRegeneration", Category.PLAYER, true);
    public static final Key<BooleanValue> RULE_DAYLIGHT = register("doDaylightCycle", Category.UPDATES, true);
    public static final Key<BooleanValue> RULE_WEATHER_CYCLE = register("doWeatherCycle", Category.UPDATES, true);
    public static final Key<IntegerValue> RULE_RANDOMTICKING = register("randomTickSpeed", Category.UPDATES, 3);

    public boolean getBoolean(Key<BooleanValue> key) { return (Boolean) DEFAULT_VALUES.getOrDefault(key.getId(), false); }

    public int getInt(Key<IntegerValue> key) { return (Integer) DEFAULT_VALUES.getOrDefault(key.getId(), 0); }

    public enum Category { PLAYER, MOBS, SPAWNING, DROPS, UPDATES, CHAT, MISC }

    public static final class Key<T extends Value<T>> {
        private final String id;
        private final Category category;

        Key(String id, Category category) { this.id = id; this.category = category; }

        public String getId() { return id; }
        public Category getCategory() { return category; }
        @Override public String toString() { return id; }
    }

    public abstract static class Value<T extends Value<T>> { }

    public static class BooleanValue extends Value<BooleanValue> {
        private final boolean value;
        BooleanValue(boolean v) { value = v; }
        public boolean get() { return value; }
    }

    public static class IntegerValue extends Value<IntegerValue> {
        private final int value;
        IntegerValue(int v) { value = v; }
        public int get() { return value; }
    }
}
