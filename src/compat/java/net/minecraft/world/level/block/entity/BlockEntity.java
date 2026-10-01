package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Extra data and behaviour kept at a block position (reamc-compat: saved with the world as JSON). */
public abstract class BlockEntity {
    private final BlockEntityType<?> type;
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this.type = type;
        this.worldPosition = pos.immutable();
        this.blockState = state;
    }

    public BlockEntityType<?> getType() { return type; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public boolean hasLevel() { return level != null; }
    public BlockPos getBlockPos() { return worldPosition; }
    public BlockState getBlockState() { return blockState; }
    public void setBlockState(BlockState state) { blockState = state; }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { }

    public final void loadWithComponents(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public final CompoundTag saveWithId(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putInt("x", worldPosition.getX());
        tag.putInt("y", worldPosition.getY());
        tag.putInt("z", worldPosition.getZ());
        return tag;
    }

    public void setChanged() { }
    public boolean isRemoved() { return remove; }
    public void setRemoved() { remove = true; }
    public void clearRemoved() { remove = false; }
    public void onLoad() { }
    public void onChunkUnloaded() { }
    public boolean triggerEvent(int id, int param) { return false; }
    public Packet<?> getUpdatePacket() { return null; }
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return new CompoundTag(); }

    public void reamc$load(CompoundTag tag) { loadAdditional(tag, HolderLookup.Provider.EMPTY); }
    public CompoundTag reamc$save() { return saveWithoutMetadata(HolderLookup.Provider.EMPTY); }
}
