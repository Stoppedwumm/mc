package mc.render;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Bitmap font rasterised once at startup into a texture (16x6 grid of printable ASCII).
 * Glyphs are drawn at a small size without antialiasing to get a crisp pixel look.
 */
public final class Font {
    private static final int CELL = 32;
    /** Glyphs are rasterised large and drawn scaled down so text stays crisp at any GUI scale. */
    private static final float SCALE = 0.42f;
    private final Texture texture;
    private final int[] advance = new int[128];
    public final int lineHeight = 10;
    private int ascent;

    public Font() {
        System.setProperty("java.awt.headless", "true");
        int w = CELL * 16, h = CELL * 8;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Font f = new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 20);
        g.setFont(f);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        ascent = fm.getAscent();
        for (int c = 32; c < 127; c++) {
            int cx = (c % 16) * CELL, cy = (c / 16) * CELL;
            g.drawString(String.valueOf((char) c), cx + 1, cy + fm.getAscent());
            advance[c] = Math.max(3, fm.charWidth((char) c));
        }
        g.dispose();
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        texture = new Texture(argb, w, h, false, false);
        texture.bind();
        org.lwjgl.opengl.GL33C.glTexParameteri(org.lwjgl.opengl.GL33C.GL_TEXTURE_2D, org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL33C.GL_LINEAR);
        org.lwjgl.opengl.GL33C.glTexParameteri(org.lwjgl.opengl.GL33C.GL_TEXTURE_2D, org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL33C.GL_LINEAR);
    }

    public void bind() { texture.bind(); }

    public int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += c < 128 ? advance[c] : advance['?'];
        }
        return Math.round(w * SCALE);
    }

    /** Appends a string to the batch at (x, y) with the given scale. */
    public void draw(Batch batch, String s, float x, float y, float scale, int argb, boolean shadow) {
        scale *= SCALE;
        if (shadow) {
            int a = argb & 0xFF000000;
            int sc = a | ((argb >> 2) & 0x3F3F3F);
            drawRaw(batch, s, x + 1, y + 1, scale, sc);
        }
        drawRaw(batch, s, x, y, scale, argb);
    }

    private void drawRaw(Batch batch, String s, float x, float y, float scale, int argb) {
        float tw = CELL * 16f, th = CELL * 8f;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 128) c = '?';
            if (c != ' ') {
                float u0 = (c % 16) * CELL / tw, v0 = (c / 16) * CELL / th;
                float u1 = u0 + CELL / tw, v1 = v0 + CELL / th;
                batch.rect(x - scale, y - 2 * scale - 2, CELL * scale, CELL * scale, u0, v0, u1, v1, argb);
            }
            x += advance[c] * scale;
        }
    }
}
