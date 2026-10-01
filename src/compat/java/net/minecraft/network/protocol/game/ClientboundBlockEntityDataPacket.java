package net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Block entity data for clients (reamc-compat: clients in the same process share the block entity). */
public class ClientboundBlockEntityDataPacket implements Packet<Object> {
    private final BlockEntity blockEntity;

    private ClientboundBlockEntityDataPacket(BlockEntity be) { this.blockEntity = be; }

    public static ClientboundBlockEntityDataPacket create(BlockEntity be) { return new ClientboundBlockEntityDataPacket(be); }

    public BlockEntity reamc$blockEntity() { return blockEntity; }
}
