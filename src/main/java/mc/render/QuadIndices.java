package mc.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33C.*;

/** A shared element buffer with the index pattern 0,1,2,2,3,0 for quads; grows on demand. */
public final class QuadIndices {
    private static int buffer;
    private static int capacityQuads;
    private static int version;

    public static int version() { return version; }

    public static int buffer() { return buffer; }

    /** Makes sure the buffer covers the quad count. Must be called with no VAO bound. */
    public static void ensure(int quads) {
        if (quads <= capacityQuads) return;
        int cap = Math.max(65536, Integer.highestOneBit(quads) << 1);
        IntBuffer data = MemoryUtil.memAllocInt(cap * 6);
        for (int i = 0; i < cap; i++) {
            int b = i * 4;
            data.put(b).put(b + 1).put(b + 2).put(b + 2).put(b + 3).put(b);
        }
        data.flip();
        if (buffer == 0) buffer = glGenBuffers();
        glBindVertexArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        MemoryUtil.memFree(data);
        capacityQuads = cap;
        version++;
    }
}
