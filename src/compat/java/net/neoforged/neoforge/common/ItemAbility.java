package net.neoforged.neoforge.common;

import com.mojang.serialization.Codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Something an item can do (dig as an axe, till as a hoe...) (reamc-compat). */
public final class ItemAbility {
    private static final Map<String, ItemAbility> ABILITIES = new ConcurrentHashMap<>();
    public static final Codec<ItemAbility> CODEC = Codec.STRING.xmap(ItemAbility::get, ItemAbility::name);

    private final String name;

    private ItemAbility(String name) { this.name = name; }

    public static ItemAbility get(String name) { return ABILITIES.computeIfAbsent(name, ItemAbility::new); }

    public String name() { return name; }

    @Override public String toString() { return "ItemAbility[" + name + "]"; }
}
