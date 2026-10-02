package net.minecraft.world.inventory;

/** Numbers a menu shows (cooking progress...) (reamc-compat). */
public interface ContainerData {
    int get(int index);

    void set(int index, int value);

    int getCount();
}
