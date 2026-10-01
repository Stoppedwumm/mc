package net.neoforged.neoforge.network.handling;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;

public interface IPayloadContext {
    Player player();

    PacketFlow flow();

    Connection connection();

    CompletableFuture<Void> enqueueWork(Runnable work);

    default void disconnect(net.minecraft.network.chat.Component reason) { connection().disconnect(reason); }
}
