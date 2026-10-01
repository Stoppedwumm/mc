package net.minecraft.world.item;

public class PickaxeItem extends DiggerItem {
    public PickaxeItem(Tier tier, Properties properties) {
        super(tier, net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }
}
