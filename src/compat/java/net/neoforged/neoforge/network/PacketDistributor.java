package net.neoforged.neoforge.network;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Sends payloads (reamc-compat: in singleplayer both sides are this process, so they are handled at once). */
public final class PacketDistributor {
    private PacketDistributor() { }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... more) {
        mc.mod.Bridge.deliver(player, payload, PacketFlow.CLIENTBOUND);
        for (CustomPacketPayload p : more) mc.mod.Bridge.deliver(player, p, PacketFlow.CLIENTBOUND);
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... more) {
        Player p = mc.mod.Bridge.localServerPlayer();
        mc.mod.Bridge.deliver(p, payload, PacketFlow.SERVERBOUND);
        for (CustomPacketPayload m : more) mc.mod.Bridge.deliver(p, m, PacketFlow.SERVERBOUND);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... more) {
        sendToPlayer((ServerPlayer) mc.mod.Bridge.localServerPlayer(), payload, more);
    }
}
