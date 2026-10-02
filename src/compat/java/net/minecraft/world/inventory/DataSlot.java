package net.minecraft.world.inventory;

/** One synced number (reamc-compat). */
public abstract class DataSlot {
    private int prev;

    public static DataSlot forContainer(ContainerData data, int index) {
        return new DataSlot() {
            @Override public int get() { return data.get(index); }
            @Override public void set(int v) { data.set(index, v); }
        };
    }

    public static DataSlot shared(int[] array, int index) {
        return new DataSlot() {
            @Override public int get() { return array[index]; }
            @Override public void set(int v) { array[index] = v; }
        };
    }

    public static DataSlot standalone() {
        return new DataSlot() {
            private int value;
            @Override public int get() { return value; }
            @Override public void set(int v) { value = v; }
        };
    }

    public abstract int get();

    public abstract void set(int value);

    public boolean checkAndClearUpdateFlag() {
        int v = get();
        boolean changed = v != prev;
        prev = v;
        return changed;
    }
}
