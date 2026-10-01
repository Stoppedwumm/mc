package net.minecraft.network;

import net.minecraft.network.chat.Component;

/** A player's network connection (reamc-compat: disconnecting is logged). */
public class Connection {
    public void disconnect(Component reason) {
        System.err.println("[mods] Connection closed: " + reason.getString());
    }
}
