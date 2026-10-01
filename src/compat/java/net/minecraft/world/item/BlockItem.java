package net.minecraft.world.item;

import net.minecraft.world.level.block.Block;

/** An item that places a block (reamc-compat: the engine item takes the block's id). */
public class BlockItem extends Item {
    private final Block block;

    public BlockItem(Block block, Properties properties) {
        super(properties);
        this.block = block;
    }

    public Block getBlock() { return block; }

    @Override
    public String getDescriptionId() { return block.getDescriptionId(); }
}
