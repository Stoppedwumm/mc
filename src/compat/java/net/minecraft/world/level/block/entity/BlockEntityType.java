package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/** Creates a kind of block entity for its blocks (reamc-compat). */
public final class BlockEntityType<T extends BlockEntity> {
    private final BlockEntitySupplier<? extends T> factory;
    private final Set<Block> validBlocks;

    private BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
        this.factory = factory;
        this.validBlocks = validBlocks;
    }

    public T create(BlockPos pos, BlockState state) { return factory.create(pos, state); }

    public boolean isValid(BlockState state) { return validBlocks.contains(state.getBlock()); }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    public static final class Builder<T extends BlockEntity> {
        private final BlockEntitySupplier<? extends T> factory;
        private final Set<Block> blocks;

        private Builder(BlockEntitySupplier<? extends T> factory, Set<Block> blocks) {
            this.factory = factory;
            this.blocks = blocks;
        }

        public static <T extends BlockEntity> Builder<T> of(BlockEntitySupplier<? extends T> factory, Block... blocks) {
            return new Builder<>(factory, Set.of(blocks));
        }

        public BlockEntityType<T> build(com.mojang.datafixers.types.Type<?> type) { return new BlockEntityType<>(factory, blocks); }
    }
}
