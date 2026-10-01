package net.minecraft.nbt;

/** Base of the named binary tag types (reamc-compat keeps them in memory and saves them as JSON). */
public interface Tag {
    byte TAG_END = 0, TAG_BYTE = 1, TAG_SHORT = 2, TAG_INT = 3, TAG_LONG = 4, TAG_FLOAT = 5, TAG_DOUBLE = 6,
            TAG_BYTE_ARRAY = 7, TAG_STRING = 8, TAG_LIST = 9, TAG_COMPOUND = 10, TAG_INT_ARRAY = 11, TAG_LONG_ARRAY = 12, TAG_ANY_NUMERIC = 99;

    byte getId();

    Tag copy();

    default String getAsString() { return toString(); }
}
