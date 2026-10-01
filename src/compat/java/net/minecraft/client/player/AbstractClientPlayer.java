package net.minecraft.client.player;

import net.minecraft.world.entity.player.Player;

public abstract class AbstractClientPlayer extends Player {
    protected AbstractClientPlayer(mc.entity.Player engine) { super(engine); }
}
