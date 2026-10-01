package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** A container that can be filled from a loot table on first open (reamc-compat: loot tables in chests are not used). */
public abstract class RandomizableContainerBlockEntity extends BaseContainerBlockEntity {
    protected RandomizableContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    protected boolean tryLoadLootTable(CompoundTag tag) { return false; }

    protected boolean trySaveLootTable(CompoundTag tag) { return false; }

    public void unpackLootTable(Object player) { }
}
