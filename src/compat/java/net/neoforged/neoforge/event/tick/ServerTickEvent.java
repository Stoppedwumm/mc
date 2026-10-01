package net.neoforged.neoforge.event.tick;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Fired before and after every server tick (reamc-compat: every tick of the simulated world). */
public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;

    protected ServerTickEvent(MinecraftServer server) { this.server = server; }

    public MinecraftServer getServer() { return server; }

    public boolean hasTime() { return true; }

    public static class Pre extends ServerTickEvent {
        public Pre(MinecraftServer server) { super(server); }
    }

    public static class Post extends ServerTickEvent {
        public Post(MinecraftServer server) { super(server); }
    }
}
