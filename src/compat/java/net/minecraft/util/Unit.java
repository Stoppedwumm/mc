package net.minecraft.util;

/** The one value of "present, no data" (reamc-compat). */
public enum Unit {
    INSTANCE;

    public static final com.mojang.serialization.Codec<Unit> CODEC = com.mojang.serialization.Codec.unit(INSTANCE);
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Unit> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.unit(INSTANCE);
}
