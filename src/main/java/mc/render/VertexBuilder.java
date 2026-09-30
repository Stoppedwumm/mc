package mc.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/** Growable off-heap buffer of packed chunk vertices (20 bytes each). */
public final class VertexBuilder {
    public static final int STRIDE = 20;
    private ByteBuffer buf;
    private int vertices;

    public VertexBuilder(int initialQuads) {
        buf = MemoryUtil.memAlloc(Math.max(1, initialQuads) * 4 * STRIDE);
    }

    public void vertex(int x, int y, int z, int u, int v, int sky, int block, int shade, int rgb, int flags) {
        if (buf.remaining() < STRIDE) {
            ByteBuffer n = MemoryUtil.memRealloc(buf, buf.capacity() * 2);
            buf = n;
        }
        buf.putShort((short) x).putShort((short) y).putShort((short) z).putShort((short) 0);
        buf.putShort((short) u).putShort((short) v);
        buf.put((byte) sky).put((byte) block).put((byte) shade).put((byte) flags);
        buf.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb).put((byte) (rgb >>> 24));
        vertices++;
    }

    public int quads() { return vertices / 4; }

    public boolean isEmpty() { return vertices == 0; }

    /** Hands over ownership of the memory; the caller must memFree it. */
    public ByteBuffer finish() {
        ByteBuffer b = buf;
        buf = null;
        b.flip();
        return b;
    }

    public void free() {
        if (buf != null) MemoryUtil.memFree(buf);
        buf = null;
    }
}
