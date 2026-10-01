package net.minecraft.world.item;

public class ShovelItem extends DiggerItem {
    public ShovelItem(Tier tier, Properties properties) {
        super(tier, net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL, properties);
    }
}
