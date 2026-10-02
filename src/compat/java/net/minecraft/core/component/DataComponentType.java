package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A kind of per-stack data (reamc-compat). */
public interface DataComponentType<T> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    Codec<DataComponentType<?>> CODEC = (Codec) BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec();
    @SuppressWarnings({"unchecked", "rawtypes"})
    StreamCodec<RegistryFriendlyByteBuf, DataComponentType<?>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.registry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE);
    Codec<DataComponentType<?>> PERSISTENT_CODEC = CODEC;

    static <T> Builder<T> builder() { return new Builder<>(); }

    Codec<T> codec();

    default Codec<T> codecOrThrow() {
        Codec<T> c = codec();
        if (c == null) throw new IllegalStateException(this + " is not a persistent component");
        return c;
    }

    default boolean isTransient() { return codec() == null; }

    StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();

    class Builder<T> {
        private Codec<T> codec;
        private StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

        public Builder<T> persistent(Codec<T> c) { codec = c; return this; }
        public Builder<T> networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, T> c) { streamCodec = c; return this; }
        public Builder<T> cacheEncoding() { return this; }

        public DataComponentType<T> build() {
            Codec<T> c = codec;
            StreamCodec<? super RegistryFriendlyByteBuf, T> sc = streamCodec;
            return new DataComponentType<>() {
                @Override public Codec<T> codec() { return c; }
                @Override public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() { return sc; }
                @Override public String toString() { return "DataComponentType[" + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(this) + "]"; }
            };
        }
    }
}
