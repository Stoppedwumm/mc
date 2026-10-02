package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * A byte buffer with Minecraft's helpers (var ints, strings, block positions), delegating to a Netty buffer
 * (reamc-compat).
 */
public class FriendlyByteBuf extends ByteBuf {
    private final ByteBuf source;

    public FriendlyByteBuf(ByteBuf source) { this.source = source; }

    public ByteBuf reamc$source() { return source; }

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

    public FriendlyByteBuf writeVarLong(long v) {
        while ((v & ~0x7FL) != 0) {
            source.writeByte((int) (v & 0x7F) | 0x80);
            v >>>= 7;
        }
        source.writeByte((int) v);
        return this;
    }

    public long readVarLong() {
        long v = 0;
        int shift = 0;
        byte b;
        do {
            b = source.readByte();
            v |= (long) (b & 0x7F) << shift;
            shift += 7;
            if (shift > 70) throw new RuntimeException("VarLong too big");
        } while ((b & 0x80) != 0);
        return v;
    }

    public FriendlyByteBuf writeUtf(String s) { return writeUtf(s, 32767); }

    public FriendlyByteBuf writeUtf(String s, int max) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(b.length);
        source.writeBytes(b);
        return this;
    }

    public String readUtf() { return readUtf(32767); }

    public String readUtf(int max) {
        int n = readVarInt();
        byte[] b = new byte[n];
        source.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public FriendlyByteBuf writeBlockPos(BlockPos p) { source.writeLong(p.asLong()); return this; }
    public BlockPos readBlockPos() { return BlockPos.of(source.readLong()); }
    public FriendlyByteBuf writeResourceLocation(ResourceLocation r) { return writeUtf(r.toString()); }
    public ResourceLocation readResourceLocation() { return ResourceLocation.parse(readUtf()); }
    public <T> void writeResourceKey(ResourceKey<T> key) { writeResourceLocation(key.location()); }
    public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> registry) { return ResourceKey.create(registry, readResourceLocation()); }
    public FriendlyByteBuf writeEnum(Enum<?> e) { return writeVarInt(e.ordinal()); }
    public <T extends Enum<T>> T readEnum(Class<T> c) { return c.getEnumConstants()[readVarInt()]; }
    public FriendlyByteBuf writeByteArray(byte[] b) { writeVarInt(b.length); source.writeBytes(b); return this; }
    public byte[] readByteArray() { byte[] b = new byte[readVarInt()]; source.readBytes(b); return b; }
    public FriendlyByteBuf writeNbt(net.minecraft.nbt.Tag tag) {
        writeUtf(tag == null ? "" : net.minecraft.nbt.CompoundTag.reamc$toJson(tag), Integer.MAX_VALUE);
        return this;
    }
    public net.minecraft.nbt.CompoundTag readNbt() {
        String s = readUtf(Integer.MAX_VALUE);
        return s.isEmpty() ? null : (net.minecraft.nbt.CompoundTag) net.minecraft.nbt.CompoundTag.reamc$fromJson(s);
    }
    public FriendlyByteBuf writeUUID(java.util.UUID id) { source.writeLong(id.getMostSignificantBits()); source.writeLong(id.getLeastSignificantBits()); return this; }
    public java.util.UUID readUUID() { return new java.util.UUID(source.readLong(), source.readLong()); }
    public <T> void writeCollection(java.util.Collection<T> c, net.minecraft.network.codec.StreamEncoder<? super FriendlyByteBuf, T> w) {
        writeVarInt(c.size());
        for (T t : c) w.encode(this, t);
    }
    public <T, C extends java.util.Collection<T>> C readCollection(java.util.function.IntFunction<C> factory, net.minecraft.network.codec.StreamDecoder<? super FriendlyByteBuf, T> r) {
        int n = readVarInt();
        C c = factory.apply(n);
        for (int i = 0; i < n; i++) c.add(r.decode(this));
        return c;
    }
    public <T> java.util.List<T> readList(net.minecraft.network.codec.StreamDecoder<? super FriendlyByteBuf, T> r) { return readCollection(java.util.ArrayList::new, r); }

    // ------------------------------------------------------------------ ByteBuf, delegated

    @Override public boolean equals(java.lang.Object a0) { return source.equals(a0); }
    @Override public java.lang.String toString(int a0, int a1, java.nio.charset.Charset a2) { return source.toString(a0, a1, a2); }
    @Override public java.lang.String toString(java.nio.charset.Charset a0) { return source.toString(a0); }
    @Override public java.lang.String toString() { return source.toString(); }
    @Override public int hashCode() { return source.hashCode(); }
    @Override public int compareTo(io.netty.buffer.ByteBuf a0) { return source.compareTo(a0); }
    @Override public int indexOf(int a0, int a1, byte a2) { return source.indexOf(a0, a1, a2); }
    @Override public boolean getBoolean(int a0) { return source.getBoolean(a0); }
    @Override public byte getByte(int a0) { return source.getByte(a0); }
    @Override public short getShort(int a0) { return source.getShort(a0); }
    @Override public char getChar(int a0) { return source.getChar(a0); }
    @Override public int getInt(int a0) { return source.getInt(a0); }
    @Override public long getLong(int a0) { return source.getLong(a0); }
    @Override public float getFloat(int a0) { return source.getFloat(a0); }
    @Override public double getDouble(int a0) { return source.getDouble(a0); }
    @Override public FriendlyByteBuf clear() { source.clear(); return this; }
    @Override public int getBytes(int a0, java.nio.channels.FileChannel a1, long a2, int a3) throws java.io.IOException { return source.getBytes(a0, a1, a2, a3); }
    @Override public FriendlyByteBuf getBytes(int a0, io.netty.buffer.ByteBuf a1) { source.getBytes(a0, a1); return this; }
    @Override public FriendlyByteBuf getBytes(int a0, byte[] a1) { source.getBytes(a0, a1); return this; }
    @Override public int getBytes(int a0, java.nio.channels.GatheringByteChannel a1, int a2) throws java.io.IOException { return source.getBytes(a0, a1, a2); }
    @Override public FriendlyByteBuf getBytes(int a0, java.nio.ByteBuffer a1) { source.getBytes(a0, a1); return this; }
    @Override public FriendlyByteBuf getBytes(int a0, java.io.OutputStream a1, int a2) throws java.io.IOException { source.getBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf getBytes(int a0, byte[] a1, int a2, int a3) { source.getBytes(a0, a1, a2, a3); return this; }
    @Override public FriendlyByteBuf getBytes(int a0, io.netty.buffer.ByteBuf a1, int a2) { source.getBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf getBytes(int a0, io.netty.buffer.ByteBuf a1, int a2, int a3) { source.getBytes(a0, a1, a2, a3); return this; }
    @Override public boolean isDirect() { return source.isDirect(); }
    @Override public boolean hasArray() { return source.hasArray(); }
    @Override public byte[] array() { return source.array(); }
    @Override public int arrayOffset() { return source.arrayOffset(); }
    @Override public FriendlyByteBuf writeInt(int a0) { source.writeInt(a0); return this; }
    @Override public int readInt() { return source.readInt(); }
    @Override public FriendlyByteBuf setBoolean(int a0, boolean a1) { source.setBoolean(a0, a1); return this; }
    @Override public FriendlyByteBuf setByte(int a0, int a1) { source.setByte(a0, a1); return this; }
    @Override public FriendlyByteBuf setChar(int a0, int a1) { source.setChar(a0, a1); return this; }
    @Override public FriendlyByteBuf setShort(int a0, int a1) { source.setShort(a0, a1); return this; }
    @Override public FriendlyByteBuf setInt(int a0, int a1) { source.setInt(a0, a1); return this; }
    @Override public FriendlyByteBuf setLong(int a0, long a1) { source.setLong(a0, a1); return this; }
    @Override public FriendlyByteBuf setFloat(int a0, float a1) { source.setFloat(a0, a1); return this; }
    @Override public FriendlyByteBuf setDouble(int a0, double a1) { source.setDouble(a0, a1); return this; }
    @Override public io.netty.buffer.ByteBuf copy(int a0, int a1) { return source.copy(a0, a1); }
    @Override public io.netty.buffer.ByteBuf copy() { return source.copy(); }
    @Override public io.netty.buffer.ByteBuf unwrap() { return source.unwrap(); }
    @Override public FriendlyByteBuf capacity(int a0) { source.capacity(a0); return this; }
    @Override public int capacity() { return source.capacity(); }
    @Override public boolean isReadOnly() { return source.isReadOnly(); }
    @Override public io.netty.buffer.ByteBuf slice(int a0, int a1) { return source.slice(a0, a1); }
    @Override public io.netty.buffer.ByteBuf slice() { return source.slice(); }
    @Override public io.netty.buffer.ByteBuf duplicate() { return source.duplicate(); }
    @Override public FriendlyByteBuf readBytes(io.netty.buffer.ByteBuf a0, int a1, int a2) { source.readBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf readBytes(byte[] a0, int a1, int a2) { source.readBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf readBytes(byte[] a0) { source.readBytes(a0); return this; }
    @Override public FriendlyByteBuf readBytes(java.io.OutputStream a0, int a1) throws java.io.IOException { source.readBytes(a0, a1); return this; }
    @Override public int readBytes(java.nio.channels.FileChannel a0, long a1, int a2) throws java.io.IOException { return source.readBytes(a0, a1, a2); }
    @Override public int readBytes(java.nio.channels.GatheringByteChannel a0, int a1) throws java.io.IOException { return source.readBytes(a0, a1); }
    @Override public io.netty.buffer.ByteBuf readBytes(int a0) { return source.readBytes(a0); }
    @Override public FriendlyByteBuf readBytes(java.nio.ByteBuffer a0) { source.readBytes(a0); return this; }
    @Override public FriendlyByteBuf readBytes(io.netty.buffer.ByteBuf a0) { source.readBytes(a0); return this; }
    @Override public FriendlyByteBuf readBytes(io.netty.buffer.ByteBuf a0, int a1) { source.readBytes(a0, a1); return this; }
    @Override public int writeBytes(java.nio.channels.FileChannel a0, long a1, int a2) throws java.io.IOException { return source.writeBytes(a0, a1, a2); }
    @Override public int writeBytes(java.nio.channels.ScatteringByteChannel a0, int a1) throws java.io.IOException { return source.writeBytes(a0, a1); }
    @Override public FriendlyByteBuf writeBytes(java.nio.ByteBuffer a0) { source.writeBytes(a0); return this; }
    @Override public FriendlyByteBuf writeBytes(byte[] a0, int a1, int a2) { source.writeBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf writeBytes(io.netty.buffer.ByteBuf a0) { source.writeBytes(a0); return this; }
    @Override public FriendlyByteBuf writeBytes(io.netty.buffer.ByteBuf a0, int a1) { source.writeBytes(a0, a1); return this; }
    @Override public FriendlyByteBuf writeBytes(io.netty.buffer.ByteBuf a0, int a1, int a2) { source.writeBytes(a0, a1, a2); return this; }
    @Override public int writeBytes(java.io.InputStream a0, int a1) throws java.io.IOException { return source.writeBytes(a0, a1); }
    @Override public FriendlyByteBuf writeBytes(byte[] a0) { source.writeBytes(a0); return this; }
    @Override public java.nio.ByteOrder order() { return source.order(); }
    @Override public io.netty.buffer.ByteBuf order(java.nio.ByteOrder a0) { return source.order(a0); }
    @Override public FriendlyByteBuf writeChar(int a0) { source.writeChar(a0); return this; }
    @Override public char readChar() { return source.readChar(); }
    @Override public FriendlyByteBuf writeFloat(float a0) { source.writeFloat(a0); return this; }
    @Override public float readFloat() { return source.readFloat(); }
    @Override public FriendlyByteBuf skipBytes(int a0) { source.skipBytes(a0); return this; }
    @Override public boolean readBoolean() { return source.readBoolean(); }
    @Override public byte readByte() { return source.readByte(); }
    @Override public short readUnsignedByte() { return source.readUnsignedByte(); }
    @Override public short readShort() { return source.readShort(); }
    @Override public int readUnsignedShort() { return source.readUnsignedShort(); }
    @Override public long readLong() { return source.readLong(); }
    @Override public double readDouble() { return source.readDouble(); }
    @Override public int getUnsignedShort(int a0) { return source.getUnsignedShort(a0); }
    @Override public FriendlyByteBuf writeBoolean(boolean a0) { source.writeBoolean(a0); return this; }
    @Override public FriendlyByteBuf writeByte(int a0) { source.writeByte(a0); return this; }
    @Override public FriendlyByteBuf writeShort(int a0) { source.writeShort(a0); return this; }
    @Override public FriendlyByteBuf writeLong(long a0) { source.writeLong(a0); return this; }
    @Override public FriendlyByteBuf writeDouble(double a0) { source.writeDouble(a0); return this; }
    @Override public boolean isReadable() { return source.isReadable(); }
    @Override public boolean isReadable(int a0) { return source.isReadable(a0); }
    @Override public boolean isWritable(int a0) { return source.isWritable(a0); }
    @Override public boolean isWritable() { return source.isWritable(); }
    @Override public FriendlyByteBuf setIndex(int a0, int a1) { source.setIndex(a0, a1); return this; }
    @Override public int maxCapacity() { return source.maxCapacity(); }
    @Override public io.netty.buffer.ByteBuf retainedSlice(int a0, int a1) { return source.retainedSlice(a0, a1); }
    @Override public io.netty.buffer.ByteBuf retainedSlice() { return source.retainedSlice(); }
    @Override public io.netty.buffer.ByteBuf retainedDuplicate() { return source.retainedDuplicate(); }
    @Override public io.netty.buffer.ByteBuf readSlice(int a0) { return source.readSlice(a0); }
    @Override public io.netty.buffer.ByteBuf readRetainedSlice(int a0) { return source.readRetainedSlice(a0); }
    @Override public io.netty.buffer.ByteBuf asReadOnly() { return source.asReadOnly(); }
    @Override public io.netty.buffer.ByteBuf discardSomeReadBytes() { return source.discardSomeReadBytes(); }
    @Override public io.netty.buffer.ByteBufAllocator alloc() { return source.alloc(); }
    @Override public FriendlyByteBuf readerIndex(int a0) { source.readerIndex(a0); return this; }
    @Override public int readerIndex() { return source.readerIndex(); }
    @Override public int writerIndex() { return source.writerIndex(); }
    @Override public FriendlyByteBuf writerIndex(int a0) { source.writerIndex(a0); return this; }
    @Override public int readableBytes() { return source.readableBytes(); }
    @Override public int writableBytes() { return source.writableBytes(); }
    @Override public int maxWritableBytes() { return source.maxWritableBytes(); }
    @Override public int maxFastWritableBytes() { return source.maxFastWritableBytes(); }
    @Override public FriendlyByteBuf markReaderIndex() { source.markReaderIndex(); return this; }
    @Override public FriendlyByteBuf resetReaderIndex() { source.resetReaderIndex(); return this; }
    @Override public FriendlyByteBuf markWriterIndex() { source.markWriterIndex(); return this; }
    @Override public FriendlyByteBuf resetWriterIndex() { source.resetWriterIndex(); return this; }
    @Override public FriendlyByteBuf discardReadBytes() { source.discardReadBytes(); return this; }
    @Override public int ensureWritable(int a0, boolean a1) { return source.ensureWritable(a0, a1); }
    @Override public FriendlyByteBuf ensureWritable(int a0) { source.ensureWritable(a0); return this; }
    @Override public short getUnsignedByte(int a0) { return source.getUnsignedByte(a0); }
    @Override public short getShortLE(int a0) { return source.getShortLE(a0); }
    @Override public int getUnsignedShortLE(int a0) { return source.getUnsignedShortLE(a0); }
    @Override public int getMedium(int a0) { return source.getMedium(a0); }
    @Override public int getMediumLE(int a0) { return source.getMediumLE(a0); }
    @Override public int getUnsignedMedium(int a0) { return source.getUnsignedMedium(a0); }
    @Override public int getUnsignedMediumLE(int a0) { return source.getUnsignedMediumLE(a0); }
    @Override public int getIntLE(int a0) { return source.getIntLE(a0); }
    @Override public long getUnsignedInt(int a0) { return source.getUnsignedInt(a0); }
    @Override public long getUnsignedIntLE(int a0) { return source.getUnsignedIntLE(a0); }
    @Override public long getLongLE(int a0) { return source.getLongLE(a0); }
    @Override public float getFloatLE(int a0) { return source.getFloatLE(a0); }
    @Override public double getDoubleLE(int a0) { return source.getDoubleLE(a0); }
    @Override public java.lang.CharSequence getCharSequence(int a0, int a1, java.nio.charset.Charset a2) { return source.getCharSequence(a0, a1, a2); }
    @Override public FriendlyByteBuf setShortLE(int a0, int a1) { source.setShortLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setMedium(int a0, int a1) { source.setMedium(a0, a1); return this; }
    @Override public FriendlyByteBuf setMediumLE(int a0, int a1) { source.setMediumLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setIntLE(int a0, int a1) { source.setIntLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setLongLE(int a0, long a1) { source.setLongLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setFloatLE(int a0, float a1) { source.setFloatLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setDoubleLE(int a0, double a1) { source.setDoubleLE(a0, a1); return this; }
    @Override public FriendlyByteBuf setBytes(int a0, byte[] a1, int a2, int a3) { source.setBytes(a0, a1, a2, a3); return this; }
    @Override public FriendlyByteBuf setBytes(int a0, java.nio.ByteBuffer a1) { source.setBytes(a0, a1); return this; }
    @Override public int setBytes(int a0, java.io.InputStream a1, int a2) throws java.io.IOException { return source.setBytes(a0, a1, a2); }
    @Override public int setBytes(int a0, java.nio.channels.ScatteringByteChannel a1, int a2) throws java.io.IOException { return source.setBytes(a0, a1, a2); }
    @Override public int setBytes(int a0, java.nio.channels.FileChannel a1, long a2, int a3) throws java.io.IOException { return source.setBytes(a0, a1, a2, a3); }
    @Override public FriendlyByteBuf setBytes(int a0, io.netty.buffer.ByteBuf a1, int a2) { source.setBytes(a0, a1, a2); return this; }
    @Override public FriendlyByteBuf setBytes(int a0, io.netty.buffer.ByteBuf a1) { source.setBytes(a0, a1); return this; }
    @Override public FriendlyByteBuf setBytes(int a0, io.netty.buffer.ByteBuf a1, int a2, int a3) { source.setBytes(a0, a1, a2, a3); return this; }
    @Override public FriendlyByteBuf setBytes(int a0, byte[] a1) { source.setBytes(a0, a1); return this; }
    @Override public FriendlyByteBuf setZero(int a0, int a1) { source.setZero(a0, a1); return this; }
    @Override public int setCharSequence(int a0, java.lang.CharSequence a1, java.nio.charset.Charset a2) { return source.setCharSequence(a0, a1, a2); }
    @Override public short readShortLE() { return source.readShortLE(); }
    @Override public int readUnsignedShortLE() { return source.readUnsignedShortLE(); }
    @Override public int readMedium() { return source.readMedium(); }
    @Override public int readMediumLE() { return source.readMediumLE(); }
    @Override public int readUnsignedMedium() { return source.readUnsignedMedium(); }
    @Override public int readUnsignedMediumLE() { return source.readUnsignedMediumLE(); }
    @Override public int readIntLE() { return source.readIntLE(); }
    @Override public long readUnsignedInt() { return source.readUnsignedInt(); }
    @Override public long readUnsignedIntLE() { return source.readUnsignedIntLE(); }
    @Override public long readLongLE() { return source.readLongLE(); }
    @Override public float readFloatLE() { return source.readFloatLE(); }
    @Override public double readDoubleLE() { return source.readDoubleLE(); }
    @Override public java.lang.CharSequence readCharSequence(int a0, java.nio.charset.Charset a1) { return source.readCharSequence(a0, a1); }
    @Override public FriendlyByteBuf writeShortLE(int a0) { source.writeShortLE(a0); return this; }
    @Override public FriendlyByteBuf writeMedium(int a0) { source.writeMedium(a0); return this; }
    @Override public FriendlyByteBuf writeMediumLE(int a0) { source.writeMediumLE(a0); return this; }
    @Override public FriendlyByteBuf writeIntLE(int a0) { source.writeIntLE(a0); return this; }
    @Override public FriendlyByteBuf writeLongLE(long a0) { source.writeLongLE(a0); return this; }
    @Override public FriendlyByteBuf writeFloatLE(float a0) { source.writeFloatLE(a0); return this; }
    @Override public FriendlyByteBuf writeDoubleLE(double a0) { source.writeDoubleLE(a0); return this; }
    @Override public FriendlyByteBuf writeZero(int a0) { source.writeZero(a0); return this; }
    @Override public int writeCharSequence(java.lang.CharSequence a0, java.nio.charset.Charset a1) { return source.writeCharSequence(a0, a1); }
    @Override public int bytesBefore(int a0, int a1, byte a2) { return source.bytesBefore(a0, a1, a2); }
    @Override public int bytesBefore(int a0, byte a1) { return source.bytesBefore(a0, a1); }
    @Override public int bytesBefore(byte a0) { return source.bytesBefore(a0); }
    @Override public int forEachByte(io.netty.util.ByteProcessor a0) { return source.forEachByte(a0); }
    @Override public int forEachByte(int a0, int a1, io.netty.util.ByteProcessor a2) { return source.forEachByte(a0, a1, a2); }
    @Override public int forEachByteDesc(int a0, int a1, io.netty.util.ByteProcessor a2) { return source.forEachByteDesc(a0, a1, a2); }
    @Override public int forEachByteDesc(io.netty.util.ByteProcessor a0) { return source.forEachByteDesc(a0); }
    @Override public int nioBufferCount() { return source.nioBufferCount(); }
    @Override public java.nio.ByteBuffer nioBuffer() { return source.nioBuffer(); }
    @Override public java.nio.ByteBuffer nioBuffer(int a0, int a1) { return source.nioBuffer(a0, a1); }
    @Override public java.nio.ByteBuffer internalNioBuffer(int a0, int a1) { return source.internalNioBuffer(a0, a1); }
    @Override public java.nio.ByteBuffer[] nioBuffers(int a0, int a1) { return source.nioBuffers(a0, a1); }
    @Override public java.nio.ByteBuffer[] nioBuffers() { return source.nioBuffers(); }
    @Override public boolean hasMemoryAddress() { return source.hasMemoryAddress(); }
    @Override public long memoryAddress() { return source.memoryAddress(); }
    @Override public boolean isContiguous() { return source.isContiguous(); }
    @Override public FriendlyByteBuf asByteBuf() { source.asByteBuf(); return this; }
    @Override public FriendlyByteBuf retain() { source.retain(); return this; }
    @Override public FriendlyByteBuf retain(int a0) { source.retain(a0); return this; }
    @Override public FriendlyByteBuf touch(java.lang.Object a0) { source.touch(a0); return this; }
    @Override public FriendlyByteBuf touch() { source.touch(); return this; }
    @Override public boolean release(int a0) { return source.release(a0); }
    @Override public boolean release() { return source.release(); }
    @Override public int refCnt() { return source.refCnt(); }
}
