package net.minecraft.server.level;

import net.minecraft.world.level.Level;

/** The server's copy of a world (reamc-compat: singleplayer and servers run mods on this). */
public abstract class ServerLevel extends Level {
    protected ServerLevel() { super(false); }
}
