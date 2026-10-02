package net.neoforged.neoforge.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;

/** Something blocks can provide, such as an item handler (reamc-compat). */
public final class BlockCapability<T, C> {
    private final ResourceLocation name;
    final Map<BlockEntityType<?>, ICapabilityProvider<BlockEntity, C, T>> byType = new IdentityHashMap<>();
    final Map<Block, IBlockCapabilityProvider<T, C>> byBlock = new IdentityHashMap<>();

    private BlockCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> BlockCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new BlockCapability<>(name); }

    public static <T> BlockCapability<T, net.minecraft.core.Direction> createSided(ResourceLocation name, Class<T> type) { return new BlockCapability<>(name); }

    public static <T> BlockCapability<T, Void> createVoid(ResourceLocation name, Class<T> type) { return new BlockCapability<>(name); }

    public ResourceLocation name() { return name; }

    public T getCapability(Level level, BlockPos pos, BlockState state, BlockEntity be, C context) {
        if (state == null) state = level.getBlockState(pos);
        IBlockCapabilityProvider<T, C> b = byBlock.get(state.getBlock());
        if (b != null) {
            T t = b.getCapability(level, pos, state, be, context);
            if (t != null) return t;
        }
        if (be == null) be = level.getBlockEntity(pos);
        if (be == null) return null;
        ICapabilityProvider<BlockEntity, C, T> p = byType.get(be.getType());
        return p == null ? null : p.getCapability(be, context);
    }
}
