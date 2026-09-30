package mc.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

/** GPU buffers for one chunk: an opaque/cutout layer and a translucent (water/ice) layer. */
public final class ChunkMesh {
    private final Layer solid = new Layer(), translucent = new Layer();

    private static final class Layer {
        int vao, vbo, quads, indexVersion;

        void upload(ByteBuffer data, int q) {
            quads = q;
            if (data == null) return;
            QuadIndices.ensure(q);
            if (vao == 0) {
                vao = glGenVertexArrays();
                vbo = glGenBuffers();
                glBindVertexArray(vao);
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                int s = VertexBuilder.STRIDE;
                glVertexAttribPointer(0, 3, GL_SHORT, false, s, 0);
                glVertexAttribPointer(1, 2, GL_UNSIGNED_SHORT, false, s, 8);
                glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, false, s, 12);
                glVertexAttribPointer(3, 4, GL_UNSIGNED_BYTE, true, s, 16);
                for (int i = 0; i < 4; i++) glEnableVertexAttribArray(i);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QuadIndices.buffer());
                indexVersion = QuadIndices.version();
                glBindVertexArray(0);
            }
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            MemoryUtil.memFree(data);
        }

        void draw() {
            if (quads == 0 || vao == 0) return;
            glBindVertexArray(vao);
            if (indexVersion != QuadIndices.version()) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QuadIndices.buffer());
                indexVersion = QuadIndices.version();
            }
            glDrawElements(GL_TRIANGLES, quads * 6, GL_UNSIGNED_INT, 0);
        }

        void delete() {
            if (vao != 0) {
                glDeleteVertexArrays(vao);
                glDeleteBuffers(vbo);
                vao = vbo = 0;
            }
        }
    }

    public void upload(MeshData m) {
        solid.upload(m.solid, m.solidQuads);
        translucent.upload(m.translucent, m.translucentQuads);
    }

    public boolean hasSolid() { return solid.quads > 0; }

    public boolean hasTranslucent() { return translucent.quads > 0; }

    public int quads() { return solid.quads + translucent.quads; }

    public void drawSolid() { solid.draw(); }

    public void drawTranslucent() { translucent.draw(); }

    public void delete() {
        solid.delete();
        translucent.delete();
    }
}
