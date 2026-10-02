package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

/** Read access to blocks (reamc-compat). */
public interface BlockGetter extends LevelHeightAccessor {
    BlockEntity getBlockEntity(BlockPos pos);

    @SuppressWarnings("unchecked")
    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        BlockEntity be = getBlockEntity(pos);
        return be != null && be.getType() == type ? Optional.of((T) be) : Optional.empty();
    }

    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    default int getLightEmission(BlockPos pos) { return getBlockState(pos).getLightEmission(); }

    default int getMaxLightLevel() { return 15; }

    default java.util.stream.Stream<BlockState> getBlockStates(net.minecraft.world.phys.AABB box) {
        return BlockPos.betweenClosedStream(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX - 1e-7, box.maxY - 1e-7, box.maxZ - 1e-7)).map(this::getBlockState);
    }
}
