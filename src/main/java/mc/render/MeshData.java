package mc.render;

import java.nio.ByteBuffer;

/** Result of a meshing job, produced on a worker and uploaded on the render thread. */
public final class MeshData {
    public final int cx, cz;
    public final ByteBuffer solid, translucent;
    public final int solidQuads, translucentQuads;

    public MeshData(int cx, int cz, ByteBuffer solid, int solidQuads, ByteBuffer translucent, int translucentQuads) {
        this.cx = cx;
        this.cz = cz;
        this.solid = solid;
        this.solidQuads = solidQuads;
        this.translucent = translucent;
        this.translucentQuads = translucentQuads;
    }
}
