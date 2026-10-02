package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Extra data and behaviour kept at a block position (reamc-compat: saved with the world as JSON). */
public abstract class BlockEntity {
    private final BlockEntityType<?> type;
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;
    private CompoundTag persistentData;

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
    public boolean isValidBlockState(BlockState state) { return type.isValid(state); }

    public static BlockPos getPosFromTag(CompoundTag tag) { return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")); }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { }

    public final void loadWithComponents(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }

    public final void loadCustomOnly(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        if (persistentData != null) tag.put("NeoForgeData", persistentData.copy());
        return tag;
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    public final CompoundTag saveWithId(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        addEntityType(tag, type);
        return tag;
    }

    public final CompoundTag saveCustomAndMetadata(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithId(registries);
        tag.putInt("x", worldPosition.getX());
        tag.putInt("y", worldPosition.getY());
        tag.putInt("z", worldPosition.getZ());
        return tag;
    }

    public static void addEntityType(CompoundTag tag, BlockEntityType<?> type) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        if (id != null) tag.putString("id", id.toString());
    }

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        stack.applyComponents(collectComponents());
    }

    public void setChanged() { if (level != null) setChanged(level, worldPosition, blockState); }

    protected static void setChanged(Level level, BlockPos pos, BlockState state) { mc.mod.Bridge.blockEntityChanged(level.reamc$world(), pos); }

    public boolean isRemoved() { return remove; }
    public void setRemoved() { remove = true; }
    public void clearRemoved() { remove = false; }
    public void onLoad() { }
    public void onChunkUnloaded() { }
    public boolean triggerEvent(int id, int param) { return false; }
    public boolean onlyOpCanSetNbt() { return false; }
    public Packet<?> getUpdatePacket() { return null; }
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return new CompoundTag(); }
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadWithComponents(tag, registries); }
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (!tag.isEmpty()) loadWithComponents(tag, registries);
    }

    /** NeoForge: data other mods may keep on this block entity. */
    public CompoundTag getPersistentData() {
        if (persistentData == null) persistentData = new CompoundTag();
        return persistentData;
    }

    public void requestModelDataUpdate() { }
    public void invalidateCapabilities() { }
    public boolean hasCustomOutlineRendering(net.minecraft.world.entity.player.Player player) { return false; }

    // ------------------------------------------------------------------ data components

    protected void applyImplicitComponents(DataComponentInput input) { }

    public final void applyComponentsFromItemStack(ItemStack stack) { applyComponents(stack.getPrototype(), stack.getComponentsPatch()); }

    public final void applyComponents(DataComponentMap prototype, DataComponentPatch patch) {
        DataComponentMap.Builder all = DataComponentMap.builder().addAll(prototype);
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet()) set(all, e.getKey(), e.getValue().orElse(null));
        DataComponentMap merged = all.build();
        Set<DataComponentType<?>> used = new java.util.HashSet<>();
        applyImplicitComponents(new DataComponentInput() {
            @Override public <T> T get(DataComponentType<T> type) { used.add(type); return merged.get(type); }
            @Override public <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) { used.add(type); return merged.getOrDefault(type, fallback); }
        });
        DataComponentMap.Builder rest = DataComponentMap.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : patch.entrySet())
            if (!used.contains(e.getKey()) && e.getValue().isPresent()) set(rest, e.getKey(), e.getValue().get());
        components = rest.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(DataComponentMap.Builder b, DataComponentType<T> type, Object value) { b.set(type, (T) value); }

    protected void collectImplicitComponents(DataComponentMap.Builder builder) { }

    public void removeComponentsFromTag(CompoundTag tag) { }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder b = DataComponentMap.builder();
        b.addAll(components);
        collectImplicitComponents(b);
        return b.build();
    }

    public DataComponentMap components() { return components; }

    public void setComponents(DataComponentMap components) { this.components = components; }

    public static Component parseCustomNameSafe(String json, HolderLookup.Provider registries) {
        try {
            return Component.Serializer.fromJson(json, registries);
        } catch (Exception e) {
            return null;
        }
    }

    /** Components read from the item a block entity was placed from. */
    protected interface DataComponentInput {
        <T> T get(DataComponentType<T> type);

        <T> T getOrDefault(DataComponentType<? extends T> type, T fallback);
    }

    public void reamc$load(CompoundTag tag) {
        if (tag.contains("NeoForgeData")) persistentData = tag.getCompound("NeoForgeData");
        loadAdditional(tag, HolderLookup.Provider.EMPTY);
    }

    public CompoundTag reamc$save() { return saveWithoutMetadata(HolderLookup.Provider.EMPTY); }
}
