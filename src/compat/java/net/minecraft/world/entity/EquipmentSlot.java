package net.minecraft.world.entity;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum EquipmentSlot implements StringRepresentable {
    MAINHAND(Type.HAND, 0, 0, "mainhand"), OFFHAND(Type.HAND, 1, 5, "offhand"), FEET(Type.HUMANOID_ARMOR, 0, 1, 1, "feet"),
    LEGS(Type.HUMANOID_ARMOR, 1, 1, 2, "legs"), CHEST(Type.HUMANOID_ARMOR, 2, 1, 3, "chest"), HEAD(Type.HUMANOID_ARMOR, 3, 1, 4, "head"),
    BODY(Type.ANIMAL_ARMOR, 0, 1, 6, "body");

    public static final int NO_COUNT_LIMIT = 0;
    public static final StringRepresentable.EnumCodec<EquipmentSlot> CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);

    private final Type type;
    private final int index, countLimit, filterFlag;
    private final String name;

    EquipmentSlot(Type type, int index, int filterFlag, String name) { this(type, index, 0, filterFlag, name); }

    EquipmentSlot(Type type, int index, int countLimit, int filterFlag, String name) {
        this.type = type; this.index = index; this.countLimit = countLimit; this.filterFlag = filterFlag; this.name = name;
    }

    public Type getType() { return type; }
    public int getIndex() { return index; }
    public int getIndex(int base) { return base + index; }
    public ItemStack limit(ItemStack s) { return countLimit > 0 ? s.split(countLimit) : s; }
    public int getFilterFlag() { return filterFlag; }
    public String getName() { return name; }
    public boolean isArmor() { return type == Type.HUMANOID_ARMOR || type == Type.ANIMAL_ARMOR; }
    @Override public String getSerializedName() { return name; }

    public static EquipmentSlot byName(String n) {
        for (EquipmentSlot s : values()) if (s.name.equals(n)) return s;
        throw new IllegalArgumentException("Invalid slot '" + n + "'");
    }

    public enum Type { HAND, HUMANOID_ARMOR, ANIMAL_ARMOR }
}
