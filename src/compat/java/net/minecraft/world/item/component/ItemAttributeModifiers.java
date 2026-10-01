package net.minecraft.world.item.component;

/** Attack damage and speed bonuses of a weapon or tool (reamc-compat keeps just these two). */
public final class ItemAttributeModifiers {
    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(0, 0);
    private final float attackDamage, attackSpeed;

    public ItemAttributeModifiers(float attackDamage, float attackSpeed) {
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
    }

    public float reamc$attackDamage() { return attackDamage; }
    public float reamc$attackSpeed() { return attackSpeed; }
}
