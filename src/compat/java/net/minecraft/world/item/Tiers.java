package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** The built-in tool materials (reamc-compat). */
public enum Tiers implements Tier {
    WOOD(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2, 0, 15),
    STONE(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4, 1, 5),
    IRON(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6, 2, 14),
    DIAMOND(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8, 3, 10),
    GOLD(BlockTags.INCORRECT_FOR_GOLD_TOOL, 32, 12, 0, 22),
    NETHERITE(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9, 4, 15);

    private final TagKey<Block> incorrect;
    private final int uses, enchant;
    private final float speed, damage;

    Tiers(TagKey<Block> incorrect, int uses, float speed, float damage, int enchant) {
        this.incorrect = incorrect; this.uses = uses; this.speed = speed; this.damage = damage; this.enchant = enchant;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrect; }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(); }
}
