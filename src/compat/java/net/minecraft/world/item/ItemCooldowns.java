package net.minecraft.world.item;

import java.util.HashMap;
import java.util.Map;

/** Per-player item cooldowns (reamc-compat: counted in game ticks). */
public class ItemCooldowns {
    private final Map<Item, long[]> cooldowns = new HashMap<>();
    private long tick;

    public boolean isOnCooldown(Item item) { return getCooldownPercent(item, 0) > 0; }

    public float getCooldownPercent(Item item, float partial) {
        long[] c = cooldowns.get(item);
        if (c == null) return 0;
        float total = c[1] - c[0], left = c[1] - (tick + partial);
        return Math.max(0, Math.min(1, left / total));
    }

    public void tick() {
        tick++;
        cooldowns.values().removeIf(c -> c[1] <= tick);
    }

    public void addCooldown(Item item, int ticks) { cooldowns.put(item, new long[]{tick, tick + ticks}); }

    public void removeCooldown(Item item) { cooldowns.remove(item); }
}
