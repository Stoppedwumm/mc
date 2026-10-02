package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;

/** A buffer with registry access for encoding items (reamc-compat). */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public RegistryFriendlyByteBuf(ByteBuf source) { this(source, mc.mod.Bridge.registryAccess()); }

    public RegistryAccess registryAccess() { return registryAccess; }

    public static java.util.function.Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess access) {
        return b -> new RegistryFriendlyByteBuf(b, access);
    }
}
