package net.minecraft.world.item;

import net.minecraft.world.item.component.ItemAttributeModifiers;

public class SwordItem extends TieredItem {
    public SwordItem(Tier tier, Properties properties) { super(tier, properties); }

    public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
        return new ItemAttributeModifiers(attackDamage + tier.getAttackDamageBonus(), attackSpeed);
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
        return new ItemAttributeModifiers(attackDamage + tier.getAttackDamageBonus(), attackSpeed);
    }
}
