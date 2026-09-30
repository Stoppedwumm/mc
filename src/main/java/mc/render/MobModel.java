package mc.render;

import mc.entity.MobType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cuboid mob models in Minecraft's style (units are 1/16 block, y up from the feet, facing +Z).
 * Each model paints its own skin texture procedurally.
 */
public final class MobModel {
    public interface Painter { int color(int face, int x, int y, int w, int h, Random r); }

    public static final int TOP = 0, BOTTOM = 1, WEST = 2, FRONT = 3, EAST = 4, BACK = 5;

    public static final class Box {
        final float x0, y0, z0, w, h, d;
        final Painter painter;
        int u, v;
        /** Armor slot this box belongs to (armor models only), -1 otherwise. */
        int slot = -1;

        Box(float x0, float y0, float z0, float w, float h, float d, Painter painter) {
            this.x0 = x0; this.y0 = y0; this.z0 = z0; this.w = w; this.h = h; this.d = d;
            this.painter = painter;
        }
    }

    public static final class Part {
        final String name;
        final float px, py, pz;
        final List<Box> boxes = new ArrayList<>();
        public float rx, ry, rz;
        public boolean visible = true;
        /** Extra scale around the pivot (baby heads). */
        public float scale = 1;
        /** Per-part colour multiplier (sheep wool, wolf collar), -1 = none. */
        public int tint = -1;

        /** Resets the per-frame pose. */
        public void reset() {
            rx = ry = rz = 0;
            visible = true;
            scale = 1;
            tint = -1;
        }

        Part(String name, float px, float py, float pz) {
            this.name = name; this.px = px; this.py = py; this.pz = pz;
        }

        Part box(float x0, float y0, float z0, float w, float h, float d, Painter p) {
            boxes.add(new Box(x0, y0, z0, w, h, d, p));
            return this;
        }

        Part armorBox(int slot, float x0, float y0, float z0, float w, float h, float d, Painter p) {
            Box b = new Box(x0, y0, z0, w, h, d, p);
            b.slot = slot;
            boxes.add(b);
            return this;
        }
    }

    public final List<Part> parts = new ArrayList<>();
    public Texture skin;
    private int texW = 128, texH = 128;

    Part part(String name, float px, float py, float pz) {
        Part p = new Part(name, px, py, pz);
        parts.add(p);
        return p;
    }

    public Part get(String name) {
        for (Part p : parts) if (p.name.equals(name)) return p;
        return null;
    }

    /** Packs box UV regions and paints the skin. */
    void bake(long seed) {
        int[] pixels = new int[texW * texH];
        int cx = 0, cy = 0, rowH = 0;
        Random r = new Random(seed);
        for (Part p : parts)
            for (Box b : p.boxes) {
                int bw = (int) Math.ceil(2 * b.d + 2 * b.w), bh = (int) Math.ceil(b.d + b.h);
                if (cx + bw > texW) { cx = 0; cy += rowH; rowH = 0; }
                b.u = cx; b.v = cy;
                cx += bw;
                rowH = Math.max(rowH, bh);
                int w = (int) b.w, h = (int) b.h, d = (int) b.d;
                paintRegion(pixels, b, TOP, b.u + d, b.v, w, d, r);
                paintRegion(pixels, b, BOTTOM, b.u + d + w, b.v, w, d, r);
                paintRegion(pixels, b, WEST, b.u, b.v + d, d, h, r);
                paintRegion(pixels, b, FRONT, b.u + d, b.v + d, w, h, r);
                paintRegion(pixels, b, EAST, b.u + d + w, b.v + d, d, h, r);
                paintRegion(pixels, b, BACK, b.u + d + w + d, b.v + d, w, h, r);
            }
        skin = new Texture(pixels, texW, texH, false, false);
    }

    private void paintRegion(int[] px, Box b, int face, int u, int v, int w, int h, Random r) {
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int xx = u + x, yy = v + y;
                if (xx < texW && yy < texH) px[yy * texW + xx] = b.painter.color(face, x, y, w, h, r);
            }
    }

    private static final Vector3f[] tmp = new Vector3f[8];

    static {
        for (int i = 0; i < 8; i++) tmp[i] = new Vector3f();
    }

    /** Appends the model's triangles to the batch with the given transform (model units = pixels). */
    public void render(Batch batch, Matrix4f base, int tint) {
        render(batch, base, tint, -1);
    }

    /** Transform of a part (its pivot and rotation) under the model transform. */
    public Matrix4f partMatrix(Matrix4f base, String name) {
        Part p = get(name);
        return new Matrix4f(base).translate(p.px, p.py, p.pz).rotateY(p.ry).rotateX(p.rx).rotateZ(p.rz);
    }

    /** Copies part rotations from another model with the same part names (armor follows the body). */
    public void copyPose(MobModel from) {
        for (Part p : parts) {
            Part o = from.get(p.name);
            if (o != null) { p.rx = o.rx; p.ry = o.ry; p.rz = o.rz; }
        }
    }

    /** Renders only armor boxes whose slot bit is set in slotMask (-1 renders everything). */
    public void render(Batch batch, Matrix4f base, int tint, int slotMask) {
        Matrix4f m = new Matrix4f();
        float iw = 1f / texW, ih = 1f / texH;
        int modelTint = tint;
        for (Part p : parts) {
            if (!p.visible) continue;
            m.set(base).translate(p.px, p.py, p.pz).rotateY(p.ry).rotateX(p.rx).rotateZ(p.rz);
            if (p.scale != 1) m.scale(p.scale);
            tint = p.tint >= 0 ? multiply(modelTint, p.tint) : modelTint;
            for (Box b : p.boxes) {
                if (slotMask != -1 && (b.slot < 0 || (slotMask & (1 << b.slot)) == 0)) continue;
                float x0 = b.x0, y0 = b.y0, z0 = b.z0, x1 = b.x0 + b.w, y1 = b.y0 + b.h, z1 = b.z0 + b.d;
                Vector3f[] c = tmp;
                m.transformPosition(c[0].set(x0, y0, z0)); m.transformPosition(c[1].set(x1, y0, z0));
                m.transformPosition(c[2].set(x1, y0, z1)); m.transformPosition(c[3].set(x0, y0, z1));
                m.transformPosition(c[4].set(x0, y1, z0)); m.transformPosition(c[5].set(x1, y1, z0));
                m.transformPosition(c[6].set(x1, y1, z1)); m.transformPosition(c[7].set(x0, y1, z1));
                float u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
                // top (y1): u+d..u+d+w, v..v+d  (z0 at v, z1 at v+d)
                face(batch, c[4], c[7], c[6], c[5], (u + d) * iw, v * ih, (u + d + w) * iw, (v + d) * ih, shade(tint, 1f), true);
                face(batch, c[3], c[0], c[1], c[2], (u + d + w) * iw, v * ih, (u + d + w + w) * iw, (v + d) * ih, shade(tint, 0.5f), true);
                // front (+z): columns x0..x1
                face(batch, c[7], c[3], c[2], c[6], (u + d) * iw, (v + d) * ih, (u + d + w) * iw, (v + d + h) * ih, shade(tint, 0.85f), false);
                // back (-z)
                face(batch, c[5], c[1], c[0], c[4], (u + d + w + d) * iw, (v + d) * ih, (u + d + w + d + w) * iw, (v + d + h) * ih, shade(tint, 0.85f), false);
                // west (-x): z0..z1
                face(batch, c[4], c[0], c[3], c[7], u * iw, (v + d) * ih, (u + d) * iw, (v + d + h) * ih, shade(tint, 0.65f), false);
                // east (+x): z1..z0
                face(batch, c[6], c[2], c[1], c[5], (u + d + w) * iw, (v + d) * ih, (u + d + w + d) * iw, (v + d + h) * ih, shade(tint, 0.65f), false);
            }
        }
    }

    private static int multiply(int a, int b) {
        int r = (a >> 16 & 255) * (b >> 16 & 255) / 255, g = (a >> 8 & 255) * (b >> 8 & 255) / 255, bl = (a & 255) * (b & 255) / 255;
        return r << 16 | g << 8 | bl;
    }

    private static int shade(int tint, float f) {
        int r = (int) ((tint >> 16 & 255) * f), g = (int) ((tint >> 8 & 255) * f), b = (int) ((tint & 255) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Quad a-b-c-d where a is the top-left of the texture region, b bottom-left, c bottom-right, d top-right. */
    private static void face(Batch batch, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1, int color, boolean flat) {
        batch.quad(a.x, a.y, a.z, u0, v0, b.x, b.y, b.z, u0, v1, c.x, c.y, c.z, u1, v1, d.x, d.y, d.z, u1, v0, color);
    }

    // ------------------------------------------------------------------ painters

    private static int noise(int base, Random r, double amount) {
        double f = 1 + (r.nextDouble() - 0.5) * amount;
        int rr = Math.min(255, Math.max(0, (int) ((base >> 16 & 255) * f)));
        int g = Math.min(255, Math.max(0, (int) ((base >> 8 & 255) * f)));
        int b = Math.min(255, Math.max(0, (int) ((base & 255) * f)));
        return 0xFF000000 | rr << 16 | g << 8 | b;
    }

    private static Painter solid(int color, double amount) {
        return (face, x, y, w, h, r) -> noise(color, r, amount);
    }

    /** Head painter: skin colour with eyes on the front face. */
    private static Painter face(int skin, int eye, int pupil, int eyeRow, boolean mouth, int mouthColor) {
        return (face, x, y, w, h, r) -> {
            if (face == FRONT) {
                int ex1 = w / 2 - 3, ex2 = w / 2 + 1;
                if (y == eyeRow && (x == ex1 || x == ex1 + 1 || x == ex2 || x == ex2 + 1)) return 0xFF000000 | ((x == ex1 + 1 || x == ex2) ? pupil : eye);
                if (mouth && y == eyeRow + 3 && x >= w / 2 - 2 && x < w / 2 + 2) return 0xFF000000 | mouthColor;
            }
            return noise(skin, r, 0.12);
        };
    }

    public static MobModel create(MobType type) {
        MobModel m = new MobModel();
        switch (type) {
            case PIG -> {
                Painter skin = solid(0xf0a0a0, 0.1);
                m.part("body", 0, 10, 0).box(-5, -4, -8, 10, 8, 16, skin);
                m.part("head", 0, 12, 8).box(-4, -4, -1, 8, 8, 8, face(0xf0a0a0, 0xffffff, 0x000000, 1, false, 0))
                        .box(-2, -3, 7, 4, 3, 1, (f, x, y, w, h, r) -> (f == FRONT && y == 1 && (x == 0 || x == 3)) ? 0xFF6a3a3a : noise(0xe07a8a, r, 0.08));
                legs4(m, 3, 6, 5, 4, 6, skin);
            }
            case COW -> {
                Painter hide = (f, x, y, w, h, r) -> {
                    double n = Math.sin(x * 0.7 + y * 0.3) + Math.cos(y * 0.5 - x * 0.2 + f);
                    return noise(n > 0.6 ? 0xf0f0f0 : 0x3a2a20, r, 0.1);
                };
                m.part("body", 0, 17, 0).box(-6, -5, -9, 12, 10, 18, hide)
                        .box(-2, -6, -6, 4, 1, 5, solid(0xf0a0a8, 0.1));
                m.part("head", 0, 20, 9).box(-4, -4, 0, 8, 8, 6, face(0x3a2a20, 0xffffff, 0x000000, 2, false, 0))
                        .box(-5, 3, 2, 1, 3, 1, solid(0xe8e0d0, 0.05)).box(4, 3, 2, 1, 3, 1, solid(0xe8e0d0, 0.05))
                        .box(-2, -4, 6, 4, 3, 1, solid(0xd0a8a0, 0.08));
                legs4(m, 4, 12, 6, 4, 12, hide);
            }
            case SHEEP -> {
                Painter wool = solid(0xeaeaea, 0.12);
                m.part("body", 0, 16, 0).box(-4, -3, -7, 8, 6, 14, solid(0xe0b8a8, 0.08));
                m.part("wool", 0, 16, 0).box(-5, -5, -8, 10, 10, 16, wool);
                m.part("head", 0, 18, 7).box(-3, -3, 0, 6, 6, 7, face(0xd8c8b0, 0xffffff, 0x000000, 1, false, 0));
                m.part("headWool", 0, 18, 7).box(-4, 1, 0, 8, 3, 5, wool);
                legs4(m, 3, 11, 5, 4, 11, (f, x, y, w, h, r) -> noise(0xd8c8b0, r, 0.1));
            }
            case WOLF -> {
                Painter fur = (f, x, y, w, h, r) -> noise(f == TOP ? 0xb8b0a8 : 0xd8d2cc, r, 0.1);
                m.part("body", 0, 10, 0).box(-3, -3, -6, 6, 6, 8, fur).box(-4, -3.5f, 1, 8, 7, 6, fur);
                m.part("collar", 0, 10, 0).box(-4.3f, -3.8f, 6.2f, 8.6f, 7.6f, 1, solid(0xffffff, 0.02));
                m.part("head", 0, 11.5f, 7).box(-3, -3, 0, 6, 6, 4, (f, x, y, w, h, r) -> {
                            if (f == FRONT && y == 2 && (x == 1 || x == 4)) return 0xFF201a14;
                            return noise(0xd8d2cc, r, 0.08);
                        })
                        .box(-1.5f, -3, 4, 3, 3, 4, (f, x, y, w, h, r) -> f == FRONT && y == 0 && x == 1 ? 0xFF1a1a1a : noise(0xc8c0b8, r, 0.08))
                        .box(-3, 3, 1, 2, 2, 1, fur).box(1, 3, 1, 2, 2, 1, fur);
                m.part("tail", 0, 11, -6).box(-1, -8, -1, 2, 8, 2, fur);
                m.part("leg0", -1.5f, 8, -4).box(-1, -8, -1, 2, 8, 2, fur);
                m.part("leg1", 1.5f, 8, -4).box(-1, -8, -1, 2, 8, 2, fur);
                m.part("leg2", -1.5f, 8, 5).box(-1, -8, -1, 2, 8, 2, fur);
                m.part("leg3", 1.5f, 8, 5).box(-1, -8, -1, 2, 8, 2, fur);
            }
            case SQUID -> {
                Painter skin = (f, x, y, w, h, r) -> noise(y < 3 ? 0x3a4a8a : 0x2a3a6a, r, 0.15);
                Painter head = (f, x, y, w, h, r) -> {
                    if ((f == FRONT || f == BACK) && y == 11 && (x == 2 || x == w - 3)) return 0xFFf0f0f0;
                    if ((f == FRONT || f == BACK) && y == 11 && (x == 3 || x == w - 4)) return 0xFF101010;
                    return skin.color(f, x, y, w, h, r);
                };
                m.part("body", 0, 6, 0).box(-5, 0, -5, 10, 14, 10, head);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    m.part("t" + i, (float) Math.cos(a) * 4, 7, (float) Math.sin(a) * 4).box(-1, -14, -1, 2, 14, 2, skin);
                }
            }
            case ENDERMAN -> {
                Painter black = solid(0x151515, 0.25);
                Painter head = (f, x, y, w, h, r) -> {
                    if (f == FRONT && y == 4 && (x == 1 || x == 2 || x == 5 || x == 6)) return (x == 2 || x == 5) ? 0xFFf0a0ff : 0xFFc050e0;
                    return noise(0x151515, r, 0.25);
                };
                m.part("body", 0, 28, 0).box(-4, 0, -2, 8, 12, 4, black);
                m.part("head", 0, 40, 0).box(-4, 0, -4, 8, 8, 8, head);
                m.part("armL", -5, 39, 0).box(-1, -29, -1, 2, 30, 2, black);
                m.part("armR", 5, 39, 0).box(-1, -29, -1, 2, 30, 2, black);
                m.part("legL", -2, 28, 0).box(-1, -28, -1, 2, 28, 2, black);
                m.part("legR", 2, 28, 0).box(-1, -28, -1, 2, 28, 2, black);
            }
            case SLIME -> {
                Painter core = (f, x, y, w, h, r) -> {
                    if (f == FRONT && y == 1 && (x == 0 || x == 4)) return 0xFF102810;
                    if (f == FRONT && y == 3 && x == 2) return 0xFF102810;
                    return noise(0x5aa84a, r, 0.1);
                };
                m.part("inner", 0, 0, 0).box(-3, 1, -3, 6, 6, 6, core);
                m.part("outer", 0, 0, 0).box(-4, 0, -4, 8, 8, 8, (f, x, y, w, h, r) -> (noise(0x78d060, r, 0.12) & 0xFFFFFF) | 0x90000000);
            }
            case CHICKEN -> {
                Painter white = solid(0xf8f8f8, 0.06);
                m.part("body", 0, 8, 0).box(-3, -3, -4, 6, 6, 8, white);
                m.part("wingL", -3, 10, 0).box(-1, -4, -3, 1, 4, 6, white);
                m.part("wingR", 3, 10, 0).box(0, -4, -3, 1, 4, 6, white);
                m.part("head", 0, 10, 3).box(-2, 0, 0, 4, 6, 3, face(0xf8f8f8, 0x000000, 0x000000, 1, false, 0))
                        .box(-2, 2, 3, 4, 2, 2, solid(0xf0b020, 0.05))
                        .box(-1, 0, 3, 2, 2, 1, solid(0xd02020, 0.05));
                m.part("legL", -1.5f, 5, 0).box(-0.5f, -5, -0.5f, 1, 5, 1, solid(0xe8a018, 0.05));
                m.part("legR", 1.5f, 5, 0).box(-0.5f, -5, -0.5f, 1, 5, 1, solid(0xe8a018, 0.05));
            }
            case ZOMBIE -> biped(m, 4, face(0x4a8a3a, 0x1a2a1a, 0x1a2a1a, 3, false, 0),
                    (f, x, y, w, h, r) -> noise(y > 9 ? 0x3a3a8a : 0x2a8a9a, r, 0.12),
                    (f, x, y, w, h, r) -> noise(y < 4 ? 0x2a8a9a : 0x4a8a3a, r, 0.1),
                    (f, x, y, w, h, r) -> noise(0x3a3a8a, r, 0.1));
            case SKELETON -> {
                Painter bone = (f, x, y, w, h, r) -> noise(0xc8c8c8, r, 0.12);
                Painter ribs = (f, x, y, w, h, r) -> (y % 2 == 1 && y < 9) || (x == w / 2 && f == FRONT) ? noise(0xb8b8b8, r, 0.1) : 0xFF2a2a2a;
                Painter skull = (f, x, y, w, h, r) -> {
                    if (f == FRONT && (y == 3 || y == 4) && (x == 1 || x == 2 || x == 5 || x == 6)) return 0xFF101010;
                    if (f == FRONT && y == 6 && x >= 2 && x <= 5 && x % 2 == 0) return 0xFF202020;
                    return noise(0xd8d8d0, r, 0.08);
                };
                m.part("body", 0, 12, 0).box(-4, 0, -2, 8, 12, 4, ribs);
                m.part("head", 0, 24, 0).box(-4, 0, -4, 8, 8, 8, skull);
                m.part("armL", -5, 22, 0).box(-1, -12, -1, 2, 12, 2, bone);
                m.part("armR", 5, 22, 0).box(-1, -12, -1, 2, 12, 2, bone);
                m.part("legL", -2, 12, 0).box(-1, -12, -1, 2, 12, 2, bone);
                m.part("legR", 2, 12, 0).box(-1, -12, -1, 2, 12, 2, bone);
            }
            case CREEPER -> {
                Painter green = (f, x, y, w, h, r) -> {
                    double v = r.nextDouble();
                    return 0xFF000000 | (v < 0.3 ? 0x3a8a2a : v < 0.6 ? 0x5cbc4c : v < 0.85 ? 0x4aa83a : 0xa8d8a0);
                };
                Painter head = (f, x, y, w, h, r) -> {
                    if (f == FRONT) {
                        boolean eyes = (y == 2 || y == 3) && (x == 1 || x == 2 || x == 5 || x == 6);
                        boolean mouth = (y == 4 && (x == 3 || x == 4)) || (y == 5 && x >= 2 && x <= 5) || (y == 6 && (x == 2 || x == 5));
                        if (eyes || mouth) return 0xFF101410;
                    }
                    return green.color(f, x, y, w, h, r);
                };
                m.part("body", 0, 6, 0).box(-4, 0, -2, 8, 12, 4, green);
                m.part("head", 0, 18, 0).box(-4, 0, -4, 8, 8, 8, head);
                legs4(m, 2, 6, 4, 4, 6, green);
            }
            case SPIDER -> {
                Painter dark = solid(0x2a2420, 0.2);
                Painter head = (f, x, y, w, h, r) -> {
                    if (f == FRONT && ((y == 2 && (x == 2 || x == 5)) || (y == 3 && (x == 1 || x == 3 || x == 4 || x == 6)))) return 0xFFd01010;
                    return noise(0x2a2420, r, 0.2);
                };
                m.part("head", 0, 9, 3).box(-4, -4, 0, 8, 8, 8, head);
                m.part("neck", 0, 9, 0).box(-3, -3, -3, 6, 6, 6, dark);
                m.part("body", 0, 9, -3).box(-5, -4, -12, 10, 8, 12, (f, x, y, w, h, r) -> noise((x + y) % 5 == 0 ? 0x5a3a2a : 0x2a2420, r, 0.2));
                for (int i = 0; i < 4; i++) {
                    m.part("legL" + i, -3, 9, 1 - i).box(-16, -1, -1, 16, 2, 2, dark);
                    m.part("legR" + i, 3, 9, 1 - i).box(0, -1, -1, 16, 2, 2, dark);
                }
            }
        }
        m.bake(type.ordinal() * 1337L);
        return m;
    }

    /** Steve-like player model: biped with skin, hair, shirt and trousers. */
    public static MobModel createPlayer() {
        MobModel m = new MobModel();
        int skin = 0xc8906a, hair = 0x3a2616, shirt = 0x2aa8b8, pants = 0x3040a0, shoes = 0x4a4a4a;
        Painter head = (f, x, y, w, h, r) -> {
            if (f == TOP) return noise(hair, r, 0.1);
            if (f == BOTTOM) return noise(skin, r, 0.06);
            if (y < 2 || (f != FRONT && y < 4) || (f == BACK)) return noise(hair, r, 0.12);
            if (f == FRONT) {
                if (y == 4 && (x == 1 || x == 6)) return 0xFFffffff;
                if (y == 4 && (x == 2 || x == 5)) return 0xFF4a3aa0;
                if (y == 6 && x >= 3 && x <= 4) return 0xFF8a5a40;
                if (y == 7 && x >= 2 && x <= 5) return 0xFF6a3a2a;
            }
            return noise(skin, r, 0.06);
        };
        Painter body = (f, x, y, w, h, r) -> noise(y < 10 ? shirt : pants, r, 0.08);
        Painter arm = (f, x, y, w, h, r) -> noise(y < 4 ? shirt : skin, r, 0.07);
        Painter leg = (f, x, y, w, h, r) -> noise(y < 10 ? pants : shoes, r, 0.08);
        biped(m, 4, head, body, arm, leg);
        m.bake(4242);
        return m;
    }

    /**
     * Armor overlay for biped models: helmet, chestplate (body + arms), leggings (waist + legs) and boots, slightly
     * larger than the body parts they cover.
     */
    public static MobModel createArmor(int material, int color) {
        MobModel m = new MobModel();
        boolean chain = material == 1;
        Painter p = (f, x, y, w, h, r) -> {
            if (chain && (x + y) % 2 == 0 && y > 0 && y < h - 1) return 0;
            int c = noise(color, r, 0.1);
            if (y == 0 || x == 0 || x == w - 1 || y == h - 1) c = noise(scaleRgb(color, 0.7), r, 0.05);
            return c;
        };
        Painter helmet = (f, x, y, w, h, r) -> f == FRONT && y >= 3 && y <= 8 && x >= 2 && x <= 7 ? 0 : p.color(f, x, y, w, h, r);
        m.part("head", 0, 24, 0).armorBox(0, -5, -1, -5, 10, 10, 10, helmet);
        m.part("body", 0, 12, 0).armorBox(1, -5, -1, -3, 10, 14, 6, p).armorBox(2, -4.5f, -0.5f, -2.5f, 9, 6, 5, p);
        m.part("armL", -6, 22, 0).armorBox(1, -3, -8, -3, 6, 10, 6, p);
        m.part("armR", 6, 22, 0).armorBox(1, -3, -8, -3, 6, 10, 6, p);
        m.part("legL", -2, 12, 0).armorBox(2, -2.5f, -9, -2.5f, 5, 9.5f, 5, p).armorBox(3, -3, -13, -3, 6, 6, 6, p);
        m.part("legR", 2, 12, 0).armorBox(2, -2.5f, -9, -2.5f, 5, 9.5f, 5, p).armorBox(3, -3, -13, -3, 6, 6, 6, p);
        m.bake(9000 + material);
        return m;
    }

    private static int scaleRgb(int c, double f) {
        return (int) ((c >> 16 & 255) * f) << 16 | (int) ((c >> 8 & 255) * f) << 8 | (int) ((c & 255) * f);
    }

    private static void legs4(MobModel m, float xo, float legTop, float zo, float size, float length, Painter p) {
        float hs = size / 2;
        m.part("leg0", -xo, legTop, zo).box(-hs, -length, -hs, size, length, size, p);
        m.part("leg1", xo, legTop, zo).box(-hs, -length, -hs, size, length, size, p);
        m.part("leg2", -xo, legTop, -zo).box(-hs, -length, -hs, size, length, size, p);
        m.part("leg3", xo, legTop, -zo).box(-hs, -length, -hs, size, length, size, p);
    }

    private static void biped(MobModel m, int limb, Painter head, Painter body, Painter arm, Painter leg) {
        m.part("body", 0, 12, 0).box(-4, 0, -2, 8, 12, 4, body);
        m.part("head", 0, 24, 0).box(-4, 0, -4, 8, 8, 8, head);
        m.part("armL", -6, 22, 0).box(-2, -12, -2, limb, 12, limb, arm);
        m.part("armR", 6, 22, 0).box(-2, -12, -2, limb, 12, limb, arm);
        m.part("legL", -2, 12, 0).box(-2, -12, -2, limb, 12, limb, leg);
        m.part("legR", 2, 12, 0).box(-2, -12, -2, limb, 12, limb, leg);
    }
}
