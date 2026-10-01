package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

import java.util.OptionalInt;
import java.util.function.Consumer;

/** The server's view of a player (reamc-compat: in singleplayer it is the local player). */
public class ServerPlayer extends Player {
    public ServerPlayer(mc.entity.Player engine) { super(engine); }

    public OptionalInt openMenu(MenuProvider provider) { return mc.mod.Bridge.openMenu(this, provider, null); }

    public OptionalInt openMenu(MenuProvider provider, BlockPos pos) { return mc.mod.Bridge.openMenu(this, provider, buf -> buf.writeBlockPos(pos)); }

    public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) { return mc.mod.Bridge.openMenu(this, provider, extraData); }

    public boolean hasDisconnected() { return reamc$player().removed; }

    public ServerLevel serverLevel() { return (ServerLevel) level(); }
}
