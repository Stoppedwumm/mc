package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

/** Block-fragment particles (reamc-compat). */
public class BlockParticleOption implements ParticleOptions {
    private final ParticleType<BlockParticleOption> type;
    private final BlockState state;
    private BlockPos pos;

    public BlockParticleOption(ParticleType<BlockParticleOption> type, BlockState state) { this.type = type; this.state = state; }

    public static MapCodec<BlockParticleOption> codec(ParticleType<BlockParticleOption> type) { return MapCodec.unit(() -> new BlockParticleOption(type, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())); }

    public static StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec(ParticleType<BlockParticleOption> type) { return StreamCodec.unit(new BlockParticleOption(type, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())); }

    public BlockParticleOption setPos(BlockPos p) { pos = p; return this; }
    public BlockPos getPos() { return pos; }
    @Override public ParticleType<BlockParticleOption> getType() { return type; }
    public BlockState getState() { return state; }
    @Override public String reamc$name() { return "block:" + (state.getBlock().reamc$block == null ? 0 : state.getBlock().reamc$block.id); }
}
