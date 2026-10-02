package net.minecraft.stats;

import java.util.HashMap;
import java.util.Map;

/** A kind of statistic, one per value (reamc-compat). */
public class StatType<T> implements Iterable<Stat<T>> {
    private final Map<T, Stat<T>> stats = new HashMap<>();

    public StatType(Object registry, net.minecraft.network.chat.Component name) { }

    public Stat<T> get(T value) { return stats.computeIfAbsent(value, v -> new Stat<>(this, v)); }
    public boolean contains(T value) { return stats.containsKey(value); }
    @Override public java.util.Iterator<Stat<T>> iterator() { return stats.values().iterator(); }
}
