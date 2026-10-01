package net.neoforged.neoforge.common;

import net.neoforged.bus.api.IEventBus;

/** The game event bus (reamc-compat). */
public final class NeoForge {
    private NeoForge() { }

    public static final IEventBus EVENT_BUS = new mc.mod.ModEventBus("game");
}
