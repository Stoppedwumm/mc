package net.minecraft.nbt;

/** reamc-compat: strings are stored as plain values. */
public final class StringTag {
    private StringTag() { }

    public static Tag valueOf(String s) { return new CompoundTag.Value(s); }
}
