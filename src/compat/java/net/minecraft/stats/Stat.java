package net.minecraft.stats;

/** A statistic (reamc-compat: reamc keeps no statistics). */
public class Stat<T> {
    private final StatType<T> type;
    private final T value;

    Stat(StatType<T> type, T value) { this.type = type; this.value = value; }

    public StatType<T> getType() { return type; }
    public T getValue() { return value; }
}
