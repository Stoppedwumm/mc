package mc.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mc.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Packet ids and field encoding. Every frame is one packet: an id byte followed by its fields.
 * Positions travel as doubles, rotations as floats, entities (on spawn) as the same JSON used for saving.
 */
public final class Protocol {
    private Protocol() { }

    public static final int VERSION = 4;

    /** Handshake intents. */
    public static final int INTENT_STATUS = 0, INTENT_LOGIN = 1;

    // Client to server
    public static final int C_HELLO = 0, C_POS = 1, C_SET_BLOCK = 2, C_BREAK = 3, C_ADD_ENTITY = 4, C_ATTACK = 5,
            C_INTERACT = 6, C_CHAT = 7, C_OPEN = 8, C_CONTAINER = 9, C_CLOSE = 10, C_SWING = 11, C_VIEW = 12,
            C_EQUIP = 13, C_KEEPALIVE = 14, C_RESPAWN = 15, C_STEER = 16, C_REEL = 17;

    // Server to client
    public static final int S_STATUS = 64, S_LOGIN = 65, S_KICK = 66, S_CHUNK = 67, S_UNLOAD = 68, S_BLOCK = 69,
            S_TIME = 70, S_SPAWN = 71, S_MOVE = 72, S_STATE = 73, S_REMOVE = 74, S_EVENT = 75, S_SOUND = 76,
            S_PARTICLE = 77, S_BROKEN = 78, S_CHAT = 79, S_HURT = 80, S_KNOCKBACK = 81, S_EFFECT = 82, S_HEAL = 83,
            S_GIVE = 84, S_XP = 85, S_SET_HELD = 86, S_CONTAINER = 87, S_TELEPORT = 88, S_KEEPALIVE = 89, S_PLAYERS = 90,
            S_RIDE = 91;

    /** Entity events. */
    public static final int EV_HURT = 0, EV_SWING = 1, EV_DEATH = 2;

    public static ByteBuf packet(int id) {
        ByteBuf b = Unpooled.buffer(64);
        b.writeByte(id);
        return b;
    }

    public static void writeString(ByteBuf b, String s) {
        byte[] d = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        b.writeInt(d.length);
        b.writeBytes(d);
    }

    public static String readString(ByteBuf b) {
        int n = b.readInt();
        if (n < 0 || n > b.readableBytes()) throw new IllegalArgumentException("Bad string length " + n);
        byte[] d = new byte[n];
        b.readBytes(d);
        return new String(d, StandardCharsets.UTF_8);
    }

    /** Item stack as [id, count, damage, enchantments...]; a zero length means empty. */
    public static void writeStack(ByteBuf b, ItemStack s) {
        if (ItemStack.isEmpty(s)) { b.writeByte(0); return; }
        int[] a = s.toArray();
        b.writeByte(a.length);
        for (int v : a) b.writeInt(v);
    }

    public static ItemStack readStack(ByteBuf b) {
        int n = b.readUnsignedByte();
        if (n == 0) return null;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = b.readInt();
        return ItemStack.fromArray(a);
    }

    public static void writeStacks(ByteBuf b, ItemStack[] slots) {
        b.writeShort(slots.length);
        for (ItemStack s : slots) writeStack(b, s);
    }

    public static ItemStack[] readStacks(ByteBuf b) {
        int n = b.readUnsignedShort();
        ItemStack[] out = new ItemStack[n];
        for (int i = 0; i < n; i++) out[i] = readStack(b);
        return out;
    }

    public static byte[] deflate(byte[] data) {
        Deflater d = new Deflater(Deflater.BEST_SPEED);
        d.setInput(data);
        d.finish();
        byte[] buf = new byte[Math.max(1024, data.length / 4)];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(buf.length);
        while (!d.finished()) {
            int n = d.deflate(buf);
            out.write(buf, 0, n);
        }
        d.end();
        return out.toByteArray();
    }

    public static byte[] inflate(byte[] data, int size) {
        Inflater inf = new Inflater();
        inf.setInput(data);
        byte[] out = new byte[size];
        try {
            int off = 0;
            while (off < size && !inf.finished()) {
                int n = inf.inflate(out, off, size - off);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                off += n;
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Corrupt chunk data", e);
        } finally {
            inf.end();
        }
        return out;
    }
}
