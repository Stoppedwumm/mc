package net.minecraft.world.item;

import net.minecraft.core.Holder;

/** A piece of armor of a material (reamc-compat). */
public class ArmorItem extends Item {
    protected final Type type;
    protected final Holder<ArmorMaterial> material;

    public ArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(properties.stacksTo(1));
        this.material = material;
        this.type = type;
    }

    public Type getType() { return type; }
    public Holder<ArmorMaterial> getMaterial() { return material; }
    public int getDefense() { return material == null ? 0 : material.value().getDefense(type); }
    public float getToughness() { return material == null ? 0 : material.value().toughness(); }

    @Override public int getEnchantmentValue() { return material == null ? 0 : material.value().enchantmentValue(); }

    public enum Type {
        HELMET(11, 0, "helmet"), CHESTPLATE(16, 1, "chestplate"), LEGGINGS(15, 2, "leggings"), BOOTS(13, 3, "boots"), BODY(16, 1, "body");

        private final int durability, slot;
        private final String name;

        Type(int durability, int slot, String name) {
            this.durability = durability; this.slot = slot; this.name = name;
        }

        public int getDurability(int multiplier) { return durability * multiplier; }
        public String getName() { return name; }
        /** reamc's armor slot: 0 head, 1 chest, 2 legs, 3 feet. */
        public int reamc$slot() { return slot; }
    }
}
