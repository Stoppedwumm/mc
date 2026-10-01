package net.minecraft.world.item;

public class AxeItem extends DiggerItem {
    public AxeItem(Tier tier, Properties properties) {
        super(tier, net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE, properties);
    }
}
