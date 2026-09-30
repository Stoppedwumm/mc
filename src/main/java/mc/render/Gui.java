package mc.render;

import mc.world.Block;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33C.*;

/** 2D drawing helpers in scaled GUI coordinates (like Minecraft's GUI scale). */
public final class Gui {
    private final WorldRenderer wr;
    public final Font font;
    public int scale = 2;
    public float width, height;
    private final Matrix4f ortho = new Matrix4f();
    private final Batch batch;

    public Gui(WorldRenderer wr, Font font) {
        this.wr = wr;
        this.font = font;
        this.batch = new Batch();
    }

    public void begin(int fbWidth, int fbHeight) {
        scale = 1;
        while (fbWidth / (scale + 1) >= 320 && fbHeight / (scale + 1) >= 240 && scale < 4) scale++;
        width = (float) fbWidth / scale;
        height = (float) fbHeight / scale;
        ortho.setOrtho(0, width, height, 0, -1000, 1000);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void shader(Matrix4f m, boolean tex, float cut) {
        ShaderProgram s = wr.basicShader;
        s.bind();
        s.set("uMVP", m);
        s.set("uTex", 0);
        s.set("uUseTex", tex ? 1 : 0);
        s.set("uFog", 0);
        s.set("uAlphaCut", cut);
        s.set("uLinear", 0);
    }

    public void fill(float x, float y, float w, float h, int argb) {
        shader(ortho, false, 0);
        batch.begin(GL_TRIANGLES);
        batch.rect(x, y, w, h, 0, 0, 1, 1, argb);
        batch.end();
    }

    public void frame(float x, float y, float w, float h, float t, int argb) {
        fill(x, y, w, t, argb);
        fill(x, y + h - t, w, t, argb);
        fill(x, y + t, t, h - 2 * t, argb);
        fill(x + w - t, y + t, t, h - 2 * t, argb);
    }

    public void gradient(float x, float y, float w, float h, int top, int bottom) {
        shader(ortho, false, 0);
        batch.begin(GL_TRIANGLES);
        batch.v(x, y, 0, 0, 0, top).v(x, y + h, 0, 0, 0, bottom).v(x + w, y + h, 0, 0, 0, bottom);
        batch.v(x + w, y + h, 0, 0, 0, bottom).v(x + w, y, 0, 0, 0, top).v(x, y, 0, 0, 0, top);
        batch.end();
    }

    public void text(String s, float x, float y, int argb) {
        shader(ortho, true, 0.02f);
        glActiveTexture(GL_TEXTURE0);
        font.bind();
        batch.begin(GL_TRIANGLES);
        font.draw(batch, s, x, y, 1, argb, true);
        batch.end();
    }

    public void centered(String s, float cx, float y, int argb) {
        text(s, cx - font.width(s) / 2f, y, argb);
    }

    public void centered(String s, float cx, float y, int argb, boolean shadow) {
        shader(ortho, true, 0.02f);
        glActiveTexture(GL_TEXTURE0);
        font.bind();
        batch.begin(GL_TRIANGLES);
        font.draw(batch, s, cx - font.width(s) / 2f, y, 1, argb, shadow);
        batch.end();
    }

    public int textWidth(String s) { return font.width(s); }

    /** Draws the crosshair with an inverting blend, like Minecraft. */
    public void crosshair() {
        glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR);
        float cx = (float) Math.floor(width / 2), cy = (float) Math.floor(height / 2);
        shader(ortho, false, 0);
        batch.begin(GL_TRIANGLES);
        batch.rect(cx - 4.5f, cy - 0.5f, 9, 1, 0, 0, 1, 1, 0xFFFFFFFF);
        batch.rect(cx - 0.5f, cy - 4.5f, 1, 4, 0, 0, 1, 1, 0xFFFFFFFF);
        batch.rect(cx - 0.5f, cy + 0.5f, 1, 4, 0, 0, 1, 1, 0xFFFFFFFF);
        batch.end();
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Isometric block icon (or flat sprite for plants) of the given size at (x, y). */
    public void icon(Block block, float x, float y, float size) {
        glActiveTexture(GL_TEXTURE0);
        wr.atlas.bind();
        if (block.has3dItem()) {
            Matrix4f m = new Matrix4f(ortho)
                    .translate(x + size / 2, y + size / 2, 100)
                    .scale(size * 0.62f, -size * 0.62f, size * 0.62f)
                    .rotateX((float) Math.toRadians(30))
                    .rotateY((float) Math.toRadians(45));
            shader(m, true, 0.3f);
            glClear(GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);
            batch.begin(GL_TRIANGLES);
            WorldRenderer.cubeInto(batch, block, -0.5f, -0.5f, -0.5f, 1, 1);
            batch.end();
            glDisable(GL_DEPTH_TEST);
        } else {
            int tex = block.spriteTex();
            if (block.isCrop()) tex = block.textureForFace(2, 7);
            float u0 = (tex & 15) / 16f, v0 = (tex >> 4) / 16f;
            shader(ortho, true, 0.3f);
            batch.begin(GL_TRIANGLES);
            int t = WorldRenderer.tintFor(block);
            batch.rect(x, y, size, size, u0, v0, u0 + 1 / 16f, v0 + 1 / 16f, 0xFF000000 | t);
            batch.end();
        }
    }

    public ItemRenderer items;

    /** Draws a 3D model (e.g. the player preview) in GUI space with depth testing; emit fills the batch. */
    public void model(Texture tex, Matrix4f model, java.util.function.Consumer<Batch> emit) {
        glActiveTexture(GL_TEXTURE0);
        tex.bind();
        shader(new Matrix4f(ortho).mul(model), true, 0.1f);
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        batch.begin(GL_TRIANGLES);
        emit.accept(batch);
        batch.end();
        glDisable(GL_DEPTH_TEST);
    }

    /** Draws a HUD icon from the item atlas (hearts, food...) at size w x h. */
    public void hudIcon(int tile, float x, float y, float w, float h) {
        glActiveTexture(GL_TEXTURE0);
        items.itemAtlas.bind();
        shader(ortho, true, 0.1f);
        float u0 = (tile & 15) / 16f, v0 = (tile >> 4) / 16f;
        batch.begin(GL_TRIANGLES);
        batch.rect(x, y, w, h, u0, v0, u0 + 1 / 16f, v0 + 1 / 16f, 0xFFFFFFFF);
        batch.end();
    }

    /** Draws an item stack icon with its count and durability bar. */
    public void stack(mc.item.ItemStack s, float x, float y) {
        if (mc.item.ItemStack.isEmpty(s)) return;
        mc.item.Item it = s.item;
        if (ItemRenderer.isCube(it)) {
            icon(it.block, x, y, 16);
        } else if (it.isBlock()) {
            icon(it.block, x, y, 16);
        } else {
            glActiveTexture(GL_TEXTURE0);
            items.itemAtlas.bind();
            shader(ortho, true, 0.1f);
            float u0 = (it.icon & 15) / 16f, v0 = (it.icon >> 4) / 16f;
            batch.begin(GL_TRIANGLES);
            batch.rect(x, y, 16, 16, u0, v0, u0 + 1 / 16f, v0 + 1 / 16f, 0xFFFFFFFF);
            batch.end();
        }
        if (it.maxDamage > 0 && s.damage > 0) {
            float f = 1 - (float) s.damage / it.maxDamage;
            int r = (int) ((1 - f) * 255), g = (int) (f * 255);
            fill(x + 2, y + 13, 13, 2, 0xFF000000);
            fill(x + 2, y + 13, 13 * f, 1, 0xFF000000 | r << 16 | g << 8);
        }
        if (s.count > 1) {
            String n = String.valueOf(s.count);
            text(n, x + 17 - textWidth(n), y + 9, 0xFFFFFFFF);
        }
    }

    /** Minecraft-style tooltip box. */
    public void tooltip(String textLine, float x, float y) {
        float tw = textWidth(textLine);
        fill(x + 8, y - 12, tw + 6, 12, 0xF0100010);
        frame(x + 8, y - 12, tw + 6, 12, 1, 0xFF3a0080);
        text(textLine, x + 11, y - 10, 0xFFFFFFFF);
    }

    /** Raised light-grey panel like Minecraft's container backgrounds. */
    public void panel(float x, float y, float w, float h) {
        fill(x, y, w, h, 0xFFC6C6C6);
        fill(x, y, w, 1, 0xFFFFFFFF);
        fill(x, y, 1, h, 0xFFFFFFFF);
        fill(x, y + h - 1, w, 1, 0xFF555555);
        fill(x + w - 1, y, 1, h, 0xFF555555);
        frame(x - 1, y - 1, w + 2, h + 2, 1, 0xFF000000);
    }

    public void slot(float x, float y) {
        fill(x, y, 18, 18, 0xFF8B8B8B);
        fill(x, y, 17, 1, 0xFF373737);
        fill(x, y, 1, 17, 0xFF373737);
        fill(x + 1, y + 17, 17, 1, 0xFFFFFFFF);
        fill(x + 17, y + 1, 1, 17, 0xFFFFFFFF);
    }

    public boolean button(String label, float x, float y, float w, float h, double mx, double my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        fill(x, y, w, h, hover ? 0xFF7080B0 : 0xFF6F6F6F);
        frame(x, y, w, h, 1, hover ? 0xFFFFFFFF : 0xFF000000);
        fill(x + 1, y + 1, w - 2, 1, 0x60FFFFFF);
        fill(x + 1, y + h - 2, w - 2, 1, 0x40000000);
        centered(label, x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFFA0 : 0xFFE0E0E0);
        return hover;
    }
}
