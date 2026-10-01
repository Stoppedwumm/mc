package net.minecraft.network;

import io.netty.buffer.ByteBuf;

/** A buffer with registry access for encoding items (reamc-compat). */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    public RegistryFriendlyByteBuf(ByteBuf source) { super(source); }
}
