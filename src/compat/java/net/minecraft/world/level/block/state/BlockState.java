package net.minecraft.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** A block with its property values (reamc-compat). */
public class BlockState extends StateHolder<Block, BlockState> {
    public BlockState(Block owner, StateDefinition<Block, ?> definition, int index) { super(owner, definition, index); }

    public Block getBlock() { return owner; }
    public boolean is(Block block) { return owner == block; }
    public boolean isAir() { return owner.reamc$block == null || owner.reamc$block == mc.world.Block.AIR; }
    public boolean hasBlockEntity() { return owner instanceof EntityBlock; }
    public BlockState rotate(Rotation rotation) { return owner.rotate(this, rotation); }
    public BlockState mirror(Mirror mirror) { return owner.mirror(this, mirror); }
    public int getLightEmission() { return owner.reamc$properties().reamc$light(this); }
    public float getDestroySpeed(Object level, BlockPos pos) { return owner.reamc$properties().reamc$destroyTime(); }
    public boolean requiresCorrectToolForDrops() { return owner.reamc$properties().reamc$requiresTool(); }
    public MenuProvider getMenuProvider(Level level, BlockPos pos) { return owner.reamc$menuProvider(this, level, pos); }
    public boolean triggerEvent(Level level, BlockPos pos, int id, int param) { return owner.reamc$triggerEvent(this, level, pos, id, param); }
    public boolean hasAnalogOutputSignal() { return owner.reamc$hasAnalogOutput(this); }
    public boolean canBeReplaced() { return owner.reamc$block != null && owner.reamc$block.replaceable; }
}
