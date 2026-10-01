package net.minecraft.client.player;

/** The player at this computer (reamc-compat). */
public class LocalPlayer extends AbstractClientPlayer {
    public LocalPlayer(mc.entity.Player engine) { super(engine); }

    @Override
    public void closeContainer() { mc.mod.Bridge.closeMenu(this); }
}
