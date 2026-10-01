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
        itemAtlas = new Texture(pixels, Atlas.SIZE, Atlas.SIZE, false, false);
    }

    public boolean usesBlockAtlas(Item item) {
        return item.isBlock();
    }

    public static boolean isCube(Item item) {
        return item.isBlock() && item.block.has3dItem();
    }

    private boolean opaque(int tile, int x, int y) {
        if (x < 0 || y < 0 || x > 15 || y > 15) return false;
        return (pixels[Atlas.index(tile, x, y)] >>> 24) > 0;
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
            return (source[Atlas.index(tile, x, y)] >>> 24) > 64;
        };
        FloatList out = new FloatList();
        float u0 = Atlas.u(tile), v0 = Atlas.v(tile), t = Atlas.STEP, th = Atlas.STEP / 2;
        // Front and back faces
        addQuad(out, 0, 1, th, 0, 0, th, 1, 0, th, 1, 1, th, u0, v0, u0 + t, v0 + t, 1f);
        addQuad(out, 1, 1, -th, 1, 0, -th, 0, 0, -th, 0, 1, -th, u0 + t, v0, u0, v0 + t, 0.8f);
        float p = 1 / 16f;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!op.test(x, y)) continue;
                float px0 = x * p, px1 = (x + 1) * p, py1 = 1 - y * p, py0 = 1 - (y + 1) * p;
                float cu = u0 + (x + 0.5f) / Atlas.SIZE, cv = v0 + (y + 0.5f) / Atlas.SIZE;
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
        int tile = blockAtlas ? item.block.spriteTex() : item.icon;
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

    /** The tinted liquid inside a potion bottle, filling the bottle sprite's empty belly. */
    public void emitPotion(Batch batch, Matrix4f m, Item item, int meta) {
        if (item != Item.POTION && item != Item.SPLASH_POTION) return;
        float[] g = extrude(Item.POTION_LIQUID_ICON, false, pixels);
        Vector3f v = new Vector3f();
        int tint = mc.item.Potions.color(meta), tr = tint >> 16 & 255, tg = tint >> 8 & 255, tb = tint & 255;
        for (int i = 0; i < g.length; i += 6) {
            m.transformPosition(v.set(g[i] - 0.5f, g[i + 1] - 0.5f, g[i + 2]));
            float s = g[i + 5];
            batch.v(v.x, v.y, v.z, g[i + 3], g[i + 4], 0xFF000000 | (int) (tr * s) << 16 | (int) (tg * s) << 8 | (int) (tb * s));
        }
    }

    private static void emitCube(Batch batch, Matrix4f m, Block block) {
        Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f(), d = new Vector3f();
        WorldRenderer.cubeInto((x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3, argb) -> {
            m.transformPosition(a.set(x0, y0, z0));
            m.transformPosition(b.set(x1, y1, z1));
            m.transformPosition(c.set(x2, y2, z2));
            m.transformPosition(d.set(x3, y3, z3));
            batch.quad(a.x, a.y, a.z, u0, v0, b.x, b.y, b.z, u1, v1, c.x, c.y, c.z, u2, v2, d.x, d.y, d.z, u3, v3, argb);
        }, block, -0.5f, -0.5f, -0.5f, 1, 1);
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
