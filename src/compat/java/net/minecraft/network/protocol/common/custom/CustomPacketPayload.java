package net.minecraft.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

/** A mod's own network message (reamc-compat). */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) { }

    static <T extends CustomPacketPayload> Type<T> createType(String id) { return new Type<>(ResourceLocation.withDefaultNamespace(id)); }
}
