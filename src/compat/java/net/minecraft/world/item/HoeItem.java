package net.minecraft.world.item;

public class HoeItem extends DiggerItem {
    public HoeItem(Tier tier, Properties properties) {
        super(tier, net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE, properties);
    }
}
