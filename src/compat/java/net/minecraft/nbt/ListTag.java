package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;

/** A list of tags of one type (reamc-compat). */
public class ListTag implements Tag {
    final List<Object> items = new ArrayList<>();

    @Override public byte getId() { return TAG_LIST; }

    byte elementType() { return items.isEmpty() ? TAG_END : CompoundTag.idOf(items.get(0)); }
    public int getElementType() { return elementType(); }

    public int size() { return items.size(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public boolean add(Tag t) { return items.add(t instanceof CompoundTag.Value v ? v.value() : t); }
    public Tag get(int i) { Object v = items.get(i); return v instanceof Tag t ? t : new CompoundTag.Value(v); }
    public CompoundTag getCompound(int i) { return i < items.size() && items.get(i) instanceof CompoundTag c ? c : new CompoundTag(); }
    public String getString(int i) { return i < items.size() && items.get(i) instanceof String s ? s : ""; }
    public int getInt(int i) { return i < items.size() && items.get(i) instanceof Number n ? n.intValue() : 0; }
    public Tag remove(int i) { Object v = items.remove(i); return v instanceof Tag t ? t : new CompoundTag.Value(v); }

    @Override
    public ListTag copy() {
        ListTag l = new ListTag();
        for (Object o : items) l.items.add(o instanceof Tag t ? t.copy() : o);
        return l;
    }
}
