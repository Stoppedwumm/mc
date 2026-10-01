package net.minecraft.world.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;

/** Pickaxes, axes, shovels and hoes (reamc-compat). */
public class DiggerItem extends TieredItem {
    private final TagKey<Block> blocks;

    public DiggerItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, properties);
        this.blocks = blocks;
    }

    public TagKey<Block> reamc$mineable() { return blocks; }

    public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
        return new ItemAttributeModifiers(attackDamage + tier.getAttackDamageBonus(), attackSpeed);
    }
}
