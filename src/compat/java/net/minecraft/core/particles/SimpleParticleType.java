package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** A particle without parameters (reamc-compat). */
public class SimpleParticleType extends ParticleType<SimpleParticleType> implements ParticleOptions {
    private final MapCodec<SimpleParticleType> codec = MapCodec.unit(this::getType);
    private final StreamCodec<RegistryFriendlyByteBuf, SimpleParticleType> streamCodec = StreamCodec.unit(this);

    public SimpleParticleType(boolean overrideLimiter) { super(overrideLimiter); }

    @Override public SimpleParticleType getType() { return this; }
    @Override public MapCodec<SimpleParticleType> codec() { return codec; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, SimpleParticleType> streamCodec() { return streamCodec; }
}
