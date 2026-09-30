package mc.client;

import mc.render.Texture;
import mc.render.WorldRenderer;
import mc.world.Block;
import mc.world.Chunk;
import mc.world.World;
import mc.world.gen.Biome;
import org.joml.Matrix4f;

import java.util.Random;

import static org.lwjgl.opengl.GL33C.*;

/** Rain and snow: weather cycle plus Minecraft-style falling-streak curtains around the camera. */
public final class Weather {
    public boolean raining;
    public int timer = 12000 + new Random().nextInt(60000);
    private float level, prevLevel;
    private Texture rainTex, snowTex;
    private final Random random = new Random();

    public void tick() {
        if (--timer <= 0) {
            raining = !raining;
            timer = raining ? 12000 + random.nextInt(12000) : 12000 + random.nextInt(96000);
        }
        prevLevel = level;
        level = Math.max(0, Math.min(1, level + (raining ? 0.01f : -0.01f)));
    }

    public float rain(float pt) {
        return prevLevel + (level - prevLevel) * pt;
    }

    private void makeTextures() {
        int[] rain = new int[16 * 64], snow = new int[16 * 64];
        Random r = new Random(3);
        for (int i = 0; i < 14; i++) {
            int x = r.nextInt(16), y = r.nextInt(64), len = 5 + r.nextInt(8);
            for (int k = 0; k < len; k++) rain[((y + k) & 63) * 16 + x] = (int) (0x70 * (0.4 + 0.6 * k / (double) len)) << 24 | 0xC0D0E8;
        }
        for (int i = 0; i < 30; i++) {
            int x = r.nextInt(15), y = r.nextInt(63);
            snow[y * 16 + x] = 0xF0FFFFFF;
            snow[y * 16 + x + 1] = 0xC0FFFFFF;
            snow[(y + 1) * 16 + x] = 0xC0FFFFFF;
        }
        rainTex = new Texture(rain, 16, 64, false, true);
        snowTex = new Texture(snow, 16, 64, false, true);
    }

    private static int rainTop(World world, int x, int z) {
        Chunk c = world.getChunk(x >> 4, z >> 4);
        if (c == null) return 256;
        for (int y = Math.min(255, c.maxY); y >= 0; y--) {
            int id = c.blocks[Chunk.index(x & 15, y, z & 15)] & 255;
            if (id != 0 && Block.get(id).model != Block.Model.CROSS) return y + 1;
        }
        return 0;
    }

    public void render(WorldRenderer wr, World world, float pt) {
        float lvl = rain(pt);
        if (lvl <= 0.01f) return;
        if (rainTex == null) makeTextures();
        int cx = (int) Math.floor(wr.camX), cy = (int) Math.floor(wr.camY), cz = (int) Math.floor(wr.camZ);
        float t = wr.time;
        int radius = 10;
        for (int pass = 0; pass < 2; pass++) {
            boolean snowPass = pass == 1;
            wr.setupBasic(wr.projView, true, 0, 0.02f);
            wr.basicShader.set("uLight", Math.max(0.05f, wr.daylight) * 0.9f);
            glActiveTexture(GL_TEXTURE0);
            (snowPass ? snowTex : rainTex).bind();
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
            glDisable(GL_CULL_FACE);
            wr.batch.begin(GL_TRIANGLES);
            for (int x = cx - radius; x <= cx + radius; x++)
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double dx = x + 0.5 - wr.camX, dz = z + 0.5 - wr.camZ;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > radius) continue;
                    Biome b = world.biomeAt(x, z);
                    int top = rainTop(world, x, z);
                    boolean snow = b == Biome.SNOWY_TAIGA || b == Biome.SNOWY_PEAKS || b == Biome.FROZEN_OCEAN || top > 150;
                    if (snow != snowPass || b == Biome.DESERT || b == Biome.BADLANDS) continue;
                    int y0 = Math.max(top, cy - 10), y1 = Math.max(top, cy + 12);
                    if (y0 >= y1) continue;
                    int hash = (x * 3121 + z * 45238971) * 1103515245;
                    float off = (hash >>> 16 & 255) / 255f;
                    float speed = snow ? 0.08f : 1.6f;
                    float v0 = (y0 / 4f + t * speed * 4 + off), v1 = (y1 / 4f + t * speed * 4 + off);
                    float sway = snow ? (float) Math.sin(t * 0.8 + off * 6) * 0.3f : 0;
                    // Plane through the column centre, facing the camera
                    float nx = (float) (-dz / Math.max(dist, 0.01)) * 0.5f, nz = (float) (dx / Math.max(dist, 0.01)) * 0.5f;
                    float px = (float) dx, pz = (float) dz;
                    int a = (int) (Math.min(1, (1 - dist / radius) * 1.5) * lvl * (snow ? 255 : 170));
                    int col = a << 24 | 0xFFFFFF;
                    float ya = (float) (y0 - wr.camY), yb = (float) (y1 - wr.camY);
                    wr.batch.quad(px - nx, yb, pz - nz, sway, -v1, px - nx, ya, pz - nz, sway, -v0,
                            px + nx, ya, pz + nz, 1 + sway, -v0, px + nx, yb, pz + nz, 1 + sway, -v1, col);
                }
            wr.batch.end();
            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
        }
    }
}
