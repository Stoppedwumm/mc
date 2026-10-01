package net.minecraft.world.item;

public class TieredItem extends Item {
    private final Tier tier;

    public TieredItem(Tier tier, Properties properties) {
        super(properties.durability(properties.reamc$maxDamage() > 0 ? properties.reamc$maxDamage() : tier.getUses()));
        this.tier = tier;
    }

    public Tier getTier() { return tier; }

    @Override public int getEnchantmentValue() { return tier.getEnchantmentValue(); }
}
