package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * A byte buffer with Minecraft's helpers (var ints, strings, block positions). reamc-compat wraps a Netty buffer
 * rather than extending it.
 */
public class FriendlyByteBuf {
    private final ByteBuf source;

    public FriendlyByteBuf(ByteBuf source) { this.source = source; }

    public ByteBuf unwrap() { return source; }

    public int readableBytes() { return source.readableBytes(); }
    public int writerIndex() { return source.writerIndex(); }
    public int readerIndex() { return source.readerIndex(); }

    public FriendlyByteBuf writeBoolean(boolean v) { source.writeBoolean(v); return this; }
    public FriendlyByteBuf writeByte(int v) { source.writeByte(v); return this; }
    public FriendlyByteBuf writeShort(int v) { source.writeShort(v); return this; }
    public FriendlyByteBuf writeInt(int v) { source.writeInt(v); return this; }
    public FriendlyByteBuf writeLong(long v) { source.writeLong(v); return this; }
    public FriendlyByteBuf writeFloat(float v) { source.writeFloat(v); return this; }
    public FriendlyByteBuf writeDouble(double v) { source.writeDouble(v); return this; }
    public FriendlyByteBuf writeBytes(byte[] v) { source.writeBytes(v); return this; }

    public boolean readBoolean() { return source.readBoolean(); }
    public byte readByte() { return source.readByte(); }
    public short readUnsignedByte() { return source.readUnsignedByte(); }
    public short readShort() { return source.readShort(); }
    public int readInt() { return source.readInt(); }
    public long readLong() { return source.readLong(); }
    public float readFloat() { return source.readFloat(); }
    public double readDouble() { return source.readDouble(); }

    public FriendlyByteBuf writeVarInt(int v) {
        while ((v & ~0x7F) != 0) {
            source.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        source.writeByte(v);
        return this;
    }

    public int readVarInt() {
        int v = 0, shift = 0;
        byte b;
        do {
            b = source.readByte();
            v |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        } while ((b & 0x80) != 0);
        return v;
    }

    public FriendlyByteBuf writeUtf(String s) { return writeUtf(s, 32767); }

    public FriendlyByteBuf writeUtf(String s, int max) {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(d.length);
        source.writeBytes(d);
        return this;
    }

    public String readUtf() { return readUtf(32767); }

    public String readUtf(int max) {
        int n = readVarInt();
        if (n < 0 || n > source.readableBytes()) throw new RuntimeException("Bad string length " + n);
        byte[] d = new byte[n];
        source.readBytes(d);
        return new String(d, StandardCharsets.UTF_8);
    }

    public FriendlyByteBuf writeBlockPos(BlockPos p) { source.writeLong(p.asLong()); return this; }
    public BlockPos readBlockPos() { return BlockPos.of(source.readLong()); }
    public FriendlyByteBuf writeResourceLocation(ResourceLocation r) { return writeUtf(r.toString()); }
    public ResourceLocation readResourceLocation() { return ResourceLocation.parse(readUtf()); }
    public <T extends Enum<T>> FriendlyByteBuf writeEnum(Enum<T> e) { return writeVarInt(e.ordinal()); }
    public <T extends Enum<T>> T readEnum(Class<T> c) { return c.getEnumConstants()[readVarInt()]; }
}
