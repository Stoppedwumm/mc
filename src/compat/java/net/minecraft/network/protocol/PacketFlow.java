package net.minecraft.network.protocol;

public enum PacketFlow {
    SERVERBOUND, CLIENTBOUND;

    public boolean isClientbound() { return this == CLIENTBOUND; }
    public boolean isServerbound() { return this == SERVERBOUND; }
}
