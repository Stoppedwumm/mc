package net.neoforged.neoforge.network.registration;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/** Registers a mod's payload types (reamc-compat: delivered in-process in singleplayer). */
public class PayloadRegistrar {
    private final String version;

    public PayloadRegistrar(String version) { this.version = version; }

    public PayloadRegistrar versioned(String v) { return new PayloadRegistrar(v); }
    public PayloadRegistrar optional() { return this; }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec, IPayloadHandler<T> handler) {
        mc.mod.Bridge.registerPayload(type, handler);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec, IPayloadHandler<T> handler) {
        return playBidirectional(type, codec, handler);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec, IPayloadHandler<T> handler) {
        return playBidirectional(type, codec, handler);
    }
}
