package mc.render;

import mc.item.Item;
import mc.item.ItemStack;
import mc.world.Block;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

/** Draws items: block items as cubes, other items as 1-pixel-thick extruded sprites like Minecraft. */
public final class ItemRenderer {
    public final Texture itemAtlas;
    private final int[] pixels;
    private final Map<Integer, float[]> extruded = new HashMap<>();

    public ItemRenderer() {
        pixels = new ItemTextureGen().generate();
        itemAtlas = new Texture(pixels, 256, 256, false, false);
    }

    public boolean usesBlockAtlas(Item item) {
        return item.isBlock();
    }

    public static boolean isCube(Item item) {
        return item.isBlock() && item.block.model == Block.Model.CUBE;
    }

    private boolean opaque(int tile, int x, int y) {
        if (x < 0 || y < 0 || x > 15 || y > 15) return false;
        return (pixels[((tile >> 4) * 16 + y) * 256 + (tile & 15) * 16 + x] >>> 24) > 0;
    }

    /**
     * Geometry for an extruded sprite in [0,1]x[0,1]x[-1/32,1/32], as floats x,y,z,u,v,shade per vertex (6 per quad).
     * For block sprites (plants, torches) the block atlas is sampled, otherwise the item atlas.
     */
    private float[] extrude(int tile, boolean blockAtlas, int[] source) {
        int key = tile | (blockAtlas ? 1 << 16 : 0);
        float[] cached = extruded.get(key);
        if (cached != null) return cached;
        java.util.function.BiPredicate<Integer, Integer> op = (x, y) -> {
            if (x < 0 || y < 0 || x > 15 || y > 15) return false;
            return (source[((tile >> 4) * 16 + y) * 256 + (tile & 15) * 16 + x] >>> 24) > 64;
        };
        FloatList out = new FloatList();
        float u0 = (tile & 15) / 16f, v0 = (tile >> 4) / 16f, t = 1 / 16f, th = 1 / 32f;
        // Front and back faces
        addQuad(out, 0, 1, th, 0, 0, th, 1, 0, th, 1, 1, th, u0, v0, u0 + t, v0 + t, 1f);
        addQuad(out, 1, 1, -th, 1, 0, -th, 0, 0, -th, 0, 1, -th, u0 + t, v0, u0, v0 + t, 0.8f);
        float p = 1 / 16f;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!op.test(x, y)) continue;
                float px0 = x * p, px1 = (x + 1) * p, py1 = 1 - y * p, py0 = 1 - (y + 1) * p;
                float cu = u0 + (x + 0.5f) / 256f, cv = v0 + (y + 0.5f) / 256f;
                if (!op.test(x, y - 1)) addQuad(out, px0, py1, -th, px0, py1, th, px1, py1, th, px1, py1, -th, cu, cv, cu, cv, 0.95f);
                if (!op.test(x, y + 1)) addQuad(out, px0, py0, th, px0, py0, -th, px1, py0, -th, px1, py0, th, cu, cv, cu, cv, 0.55f);
                if (!op.test(x - 1, y)) addQuad(out, px0, py1, -th, px0, py0, -th, px0, py0, th, px0, py1, th, cu, cv, cu, cv, 0.7f);
                if (!op.test(x + 1, y)) addQuad(out, px1, py1, th, px1, py0, th, px1, py0, -th, px1, py1, -th, cu, cv, cu, cv, 0.7f);
            }
        cached = out.toArray();
        extruded.put(key, cached);
        return cached;
    }

    private static void addQuad(FloatList o, float x0, float y0, float z0, float x1, float y1, float z1,
                                float x2, float y2, float z2, float x3, float y3, float z3, float ua, float va, float ub, float vb, float shade) {
        float[][] v = {{x0, y0, z0, ua, va}, {x1, y1, z1, ua, vb}, {x2, y2, z2, ub, vb}, {x3, y3, z3, ub, va}};
        int[] order = {0, 1, 2, 2, 3, 0};
        for (int i : order) o.add(v[i][0], v[i][1], v[i][2], v[i][3], v[i][4], shade);
    }

    private static final class FloatList {
        float[] a = new float[1024];
        int n;

        void add(float... v) {
            if (n + v.length > a.length) a = java.util.Arrays.copyOf(a, a.length * 2);
            System.arraycopy(v, 0, a, n, v.length);
            n += v.length;
        }

        float[] toArray() { return java.util.Arrays.copyOf(a, n); }
    }

    private int[] blockPixels;

    public void setBlockPixels(int[] px) { blockPixels = px; }

    /** Emits the item into the batch using matrix m; the caller binds the right atlas (see atlasFor). */
    public void emit(Batch batch, Matrix4f m, Item item, int tint) {
        if (isCube(item)) {
            Vector3f v = new Vector3f();
            Batch temp = batch;
            // cube centred on origin, size 1
            Matrix4f mm = new Matrix4f(m);
            emitCube(temp, mm, item.block);
            return;
        }
        boolean blockAtlas = item.isBlock();
        int tile = blockAtlas ? item.block.texSide : item.icon;
        float[] g = extrude(tile, blockAtlas, blockAtlas ? blockPixels : pixels);
        Vector3f v = new Vector3f();
        int tr = tint >> 16 & 255, tg = tint >> 8 & 255, tb = tint & 255;
        for (int i = 0; i < g.length; i += 6) {
            m.transformPosition(v.set(g[i] - 0.5f, g[i + 1] - 0.5f, g[i + 2]));
            float s = g[i + 5];
            int c = 0xFF000000 | (int) (tr * s) << 16 | (int) (tg * s) << 8 | (int) (tb * s);
            batch.v(v.x, v.y, v.z, g[i + 3], g[i + 4], c);
        }
    }

    private static void emitCube(Batch batch, Matrix4f m, Block block) {
        // Reuse the GUI/held cube, transformed on the CPU
        Batch tmp = batch;
        float[] shades = {1.0f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f};
        int tint = WorldRenderer.tintFor(block);
        Vector3f[] c = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            c[i] = new Vector3f((i & 1) == 0 ? -0.5f : 0.5f, (i & 2) == 0 ? -0.5f : 0.5f, (i & 4) == 0 ? -0.5f : 0.5f);
            m.transformPosition(c[i]);
        }
        // corner index = x | y<<1 | z<<2
        int[][] faces = {{2, 6, 7, 3}, {0, 1, 5, 4}, {3, 1, 0, 2}, {6, 4, 5, 7}, {2, 0, 4, 6}, {7, 5, 1, 3}};
        // faces: up, down, north(-z), south(+z), west(-x), east(+x); vertex order = top-left, bottom-left, bottom-right, top-right
        int[][] fixed = {{2, 6, 7, 3}, {4, 5, 1, 0}, {3, 1, 0, 2}, {6, 4, 5, 7}, {2, 0, 4, 6}, {7, 5, 1, 3}};
        for (int f = 0; f < 6; f++) {
            int tex = block.textureForFace(f);
            if (block == Block.GRASS) tex = f == 0 ? Block.Tex.GRASS_TOP_ITEM : f == 1 ? Block.Tex.DIRT : Block.Tex.GRASS_SIDE_ITEM;
            int ft = block == Block.GRASS ? 0xFFFFFF : tint;
            float s = shades[f];
            int col = 0xFF000000 | (int) ((ft >> 16 & 255) * s) << 16 | (int) ((ft >> 8 & 255) * s) << 8 | (int) ((ft & 255) * s);
            float u0 = (tex & 15) / 16f, v0 = (tex >> 4) / 16f, u1 = u0 + 1 / 16f, v1 = v0 + 1 / 16f;
            int[] q = fixed[f];
            Vector3f a = c[q[0]], b = c[q[1]], cc = c[q[2]], d = c[q[3]];
            tmp.quad(a.x, a.y, a.z, u0, v0, b.x, b.y, b.z, u0, v1, cc.x, cc.y, cc.z, u1, v1, d.x, d.y, d.z, u1, v0, col);
        }
    }

    /** Binds the atlas an item's sprite lives in. */
    public void bindFor(Item item, WorldRenderer wr) {
        glActiveTexture(GL_TEXTURE0);
        if (item.isBlock()) wr.atlas.bind();
        else itemAtlas.bind();
    }

    public static int tint(Item item) {
        return item.isBlock() ? WorldRenderer.tintFor(item.block) : 0xFFFFFF;
    }

    public static boolean isEmpty(ItemStack s) { return ItemStack.isEmpty(s); }
}
