package mc.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

/** Immediate-mode style vertex batch (position, uv, colour) used for GUI, particles, clouds and outlines. */
public final class Batch implements QuadSink {

    private static final int STRIDE = 24;
    private final int vao, vbo;
    private ByteBuffer buf = MemoryUtil.memAlloc(STRIDE * 65536);
    private int count;
    private int mode = GL_TRIANGLES;

    public Batch() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, 12);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, STRIDE, 20);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void begin(int glMode) {
        mode = glMode;
        count = 0;
        buf.clear();
    }

    public Batch v(float x, float y, float z, float u, float w, int argb) {
        if (buf.remaining() < STRIDE) buf = MemoryUtil.memRealloc(buf, buf.capacity() * 2);
        buf.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(w);
        buf.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
        count++;
        return this;
    }

    /** Adds a quad given in counter-clockwise order as two triangles. */
    public void quad(float x0, float y0, float z0, float u0, float v0,
                     float x1, float y1, float z1, float u1, float v1,
                     float x2, float y2, float z2, float u2, float v2,
                     float x3, float y3, float z3, float u3, float v3, int argb) {
        v(x0, y0, z0, u0, v0, argb); v(x1, y1, z1, u1, v1, argb); v(x2, y2, z2, u2, v2, argb);
        v(x2, y2, z2, u2, v2, argb); v(x3, y3, z3, u3, v3, argb); v(x0, y0, z0, u0, v0, argb);
    }

    /** Screen-space rectangle (y down). */
    public void rect(float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
        quad(x, y, 0, u0, v0, x, y + h, 0, u0, v1, x + w, y + h, 0, u1, v1, x + w, y, 0, u1, v0, argb);
    }

    public void end() {
        if (count == 0) return;
        buf.flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STREAM_DRAW);
        glDrawArrays(mode, 0, count);
        glBindVertexArray(0);
        count = 0;
        buf.clear();
    }
}
