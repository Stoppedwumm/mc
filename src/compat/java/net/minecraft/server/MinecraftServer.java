package net.minecraft.server;

/** The game server (reamc-compat: the singleplayer world or a reamc server). */
public class MinecraftServer {
    private int tickCount;

    public int getTickCount() { return tickCount; }
    public void reamc$tick() { tickCount++; }
    public boolean isDedicatedServer() { return mc.mod.Bridge.dedicated; }
    public boolean isSameThread() { return true; }
    public void execute(Runnable r) { r.run(); }
}
