package net.minecraft.world.inventory;

public class SimpleContainerData implements ContainerData {
    private final int[] ints;

    public SimpleContainerData(int size) { ints = new int[size]; }

    @Override public int get(int i) { return ints[i]; }
    @Override public void set(int i, int v) { ints[i] = v; }
    @Override public int getCount() { return ints.length; }
}
