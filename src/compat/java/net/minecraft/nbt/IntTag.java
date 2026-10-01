package net.minecraft.nbt;

/** reamc-compat: ints are stored as plain values. */
public final class IntTag {
    private IntTag() { }

    public static Tag valueOf(int v) { return new CompoundTag.Value(v); }
}
