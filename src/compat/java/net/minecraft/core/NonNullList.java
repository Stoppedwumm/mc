package net.minecraft.core;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A list that never holds null; fixed-size lists refill with a default value (reamc-compat). */
public class NonNullList<E> extends AbstractList<E> {
    private final List<E> list;
    private final E defaultValue;

    protected NonNullList(List<E> list, E defaultValue) {
        this.list = list;
        this.defaultValue = defaultValue;
    }

    public static <E> NonNullList<E> create() { return new NonNullList<>(new ArrayList<>(), null); }

    @SuppressWarnings("unchecked")
    public static <E> NonNullList<E> withSize(int size, E defaultValue) {
        Objects.requireNonNull(defaultValue);
        Object[] a = new Object[size];
        Arrays.fill(a, defaultValue);
        return new NonNullList<>(Arrays.asList((E[]) a), defaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> of(E defaultValue, E... values) { return new NonNullList<>(Arrays.asList(values), defaultValue); }

    @Override public E get(int i) { return list.get(i); }
    @Override public E set(int i, E v) { return list.set(i, Objects.requireNonNull(v)); }
    @Override public void add(int i, E v) { list.add(i, Objects.requireNonNull(v)); }
    @Override public E remove(int i) { return list.remove(i); }
    @Override public int size() { return list.size(); }

    @Override
    public void clear() {
        if (defaultValue == null) super.clear();
        else for (int i = 0; i < size(); i++) set(i, defaultValue);
    }
}
