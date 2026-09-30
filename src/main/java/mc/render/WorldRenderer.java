package mc.render;

import mc.entity.Player;
import mc.util.RayCast;
import mc.world.Block;
import mc.world.Chunk;
import mc.world.World;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL33C.*;

public final class WorldRenderer {
    public final ShaderProgram chunkShader, skyShader, basicShader;
    public final Texture atlas, white;
    private final Texture clouds;
    private final boolean[] cloudMap = new boolean[128 * 128];
    public final Batch batch = new Batch();
    private final int emptyVao;
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final List<Chunk> visible = new ArrayList<>();

    public final Matrix4f projection = new Matrix4f(), view = new Matrix4f(), projView = new Matrix4f();
    public double camX, camY, camZ;
    public int renderedChunks;
    public float fogStart, fogEnd;
    public final Vector3f fogColor = new Vector3f();
    public float daylight, sunset, night, celestialAngle;
    public final Vector3f zenith = new Vector3f(), horizon = new Vector3f(), sunDir = new Vector3f();
    public float gamma = 0.5f;
    public boolean clouds3d = true;

    public WorldRenderer(long seed) {
        chunkShader = new ShaderProgram("chunk");
        skyShader = new ShaderProgram("sky");
        basicShader = new ShaderProgram("basic");
        atlas = new Texture(new TextureGen().generate(), TextureGen.ATLAS, TextureGen.ATLAS, true, false);
        white = new Texture(new int[]{0xFFFFFFFF}, 1, 1, false, true);
        emptyVao = glGenVertexArrays();

        // Cloud map from thresholded smooth noise
        Random r = new Random(seed ^ 0x5DEECE66DL);
        double[] grid = new double[16 * 16];
        for (int i = 0; i < grid.length; i++) grid[i] = r.nextDouble();
        int[] px = new int[128 * 128];
        for (int y = 0; y < 128; y++)
            for (int x = 0; x < 128; x++) {
                double v = 0, amp = 1, norm = 0;
                for (int o = 0; o < 3; o++) {
                    int cell = 16 >> o;
                    double fx = (double) x / cell, fy = (double) y / cell;
                    int n = 128 / cell;
                    int x0 = (int) fx, y0 = (int) fy;
                    double tx = fx - x0, ty = fy - y0;
                    double a = grid[((y0 % n) * 16 + x0 % n) % 256], b = grid[((y0 % n) * 16 + (x0 + 1) % n) % 256];
                    double c = grid[(((y0 + 1) % n) * 16 + x0 % n) % 256], d = grid[(((y0 + 1) % n) * 16 + (x0 + 1) % n) % 256];
                    v += ((a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty) * amp;
                    norm += amp;
                    amp *= 0.5;
                }
                v /= norm;
                boolean on = v + (r.nextDouble() - 0.5) * 0.06 > 0.56;
                cloudMap[y * 128 + x] = on;
                px[y * 128 + x] = on ? 0xFFFFFFFF : 0;
            }
        clouds = new Texture(px, 128, 128, false, true);
    }

    // ------------------------------------------------------------------ environment

    public void updateEnvironment(World world, float pt, boolean underwater, boolean inLava, int renderDistance) {
        double t = (world.time % 24000) + pt;
        double f = t / 24000.0 - 0.25;
        f = f - Math.floor(f);
        double base = f;
        f = f + (1 - (Math.cos(f * Math.PI) + 1) / 2 - f) / 3;
        celestialAngle = (float) f;
        double a = f * Math.PI * 2;
        sunDir.set((float) -Math.sin(a), (float) Math.cos(a), 0.12f).normalize();
        float d = (float) Math.max(0, Math.min(1, Math.cos(a) * 2 + 0.5));
        daylight = 0.16f + 0.84f * d;
        night = 1 - (float) Math.max(0, Math.min(1, Math.cos(a) * 3 + 0.4));
        double c = Math.cos(a);
        sunset = c > -0.45 && c < 0.45 ? (float) Math.pow(Math.sin((c / 0.45 * 0.5 + 0.5) * Math.PI), 2) : 0;

        zenith.set(0.47f, 0.65f, 1.0f).mul(d).add(0.005f, 0.006f, 0.02f);
        horizon.set(0.72f, 0.83f, 1.0f).mul(d).add(0.02f, 0.025f, 0.05f);
        Vector3f sunsetColor = new Vector3f(1.0f, 0.45f, 0.18f);
        horizon.lerp(sunsetColor, sunset * 0.25f);

        fogEnd = renderDistance * 16f;
        fogStart = fogEnd * 0.72f;
        fogColor.set(horizon);
        if (underwater) {
            fogColor.set(0.04f, 0.12f, 0.45f).mul(Math.max(0.15f, d));
            fogStart = 0;
            fogEnd = 28;
        } else if (inLava) {
            fogColor.set(0.6f, 0.1f, 0f);
            fogStart = 0;
            fogEnd = 2;
        }
    }

    public void setupCamera(Player p, float pt, float fov, int width, int height, int renderDistance, boolean bobbing) {
        camX = p.interpX(pt);
        camY = p.interpY(pt) + p.prevEyeHeight + (p.eyeHeight - p.prevEyeHeight) * pt;
        camZ = p.interpZ(pt);
        float far = Math.max(256f, renderDistance * 16f * 2f);
        projection.setPerspective((float) Math.toRadians(fov), (float) width / height, 0.05f, far);
        view.identity();
        float tilt = p.prevTilt + (p.tilt - p.prevTilt) * pt;
        view.rotateX((float) Math.toRadians(tilt));
        if (bobbing) {
            float wd = p.walkDist - p.prevWalkDist;
            float walk = -(p.walkDist + wd * pt);
            float bob = p.prevBob + (p.bob - p.prevBob) * pt;
            view.translate((float) Math.sin(walk * Math.PI) * bob * 0.5f, -(float) Math.abs(Math.cos(walk * Math.PI) * bob), 0);
            view.rotateZ((float) Math.toRadians(Math.sin(walk * Math.PI) * bob * 3));
            view.rotateX((float) Math.toRadians(Math.abs(Math.cos(walk * Math.PI - 0.2) * bob) * 5));
        }
        view.rotateX((float) Math.toRadians(p.pitch));
        view.rotateY((float) Math.toRadians(p.yaw + 180));
        projection.mul(view, projView);
        frustum.set(projView);
    }

    // ------------------------------------------------------------------ passes

    public void renderSky() {
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        skyShader.bind();
        skyShader.set("uInvProjView", new Matrix4f(projView).invert());
        skyShader.set("uSunDir", sunDir.x, sunDir.y, sunDir.z);
        skyShader.set("uZenith", zenith.x, zenith.y, zenith.z);
        skyShader.set("uHorizon", fogColor.x, fogColor.y, fogColor.z);
        skyShader.set("uSunsetColor", 1.0f, 0.42f, 0.16f);
        skyShader.set("uSunset", sunset);
        skyShader.set("uNight", night);
        skyShader.set("uStarAngle", celestialAngle * (float) Math.PI * 2);
        skyShader.set("uMoonPhase", 0.3f);
        glBindVertexArray(emptyVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    private void setupChunkShader(boolean translucent) {
        chunkShader.bind();
        chunkShader.set("uProjView", projView);
        chunkShader.set("uTex", 0);
        chunkShader.set("uDaylight", daylight);
        float n = 1 - (daylight - 0.16f) / 0.84f;
        chunkShader.set("uSkyTint", 1 - 0.25f * n, 1 - 0.18f * n, 1);
        chunkShader.set("uFogColor", fogColor.x, fogColor.y, fogColor.z);
        chunkShader.set("uFogStart", fogStart);
        chunkShader.set("uFogEnd", fogEnd);
        chunkShader.set("uTranslucent", translucent ? 1 : 0);
        chunkShader.set("uGamma", gamma);
        chunkShader.set("uCamFrac", (float) (camX - Math.floor(camX)), (float) (camY - Math.floor(camY)), (float) (camZ - Math.floor(camZ)));
    }

    public void renderOpaque(World world) {
        visible.clear();
        for (Chunk c : world.chunks()) {
            if (c.mesh == null) continue;
            float ox = (float) (c.cx * 16 - camX), oz = (float) (c.cz * 16 - camZ);
            if (!frustum.testAab(ox, (float) -camY, oz, ox + 16, (float) (c.maxY + 1 - camY), oz + 16)) continue;
            visible.add(c);
        }
        visible.sort((a, b) -> Double.compare(dist2(a), dist2(b)));
        renderedChunks = visible.size();

        glEnable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
        atlas.bind();
        setupChunkShader(false);
        for (Chunk c : visible) {
            if (!c.mesh.hasSolid()) continue;
            chunkShader.set("uOffset", (float) (c.cx * 16 - Math.floor(camX)), (float) -Math.floor(camY), (float) (c.cz * 16 - Math.floor(camZ)));
            c.mesh.drawSolid();
        }
        glBindVertexArray(0);
    }

    public void renderTranslucent() {
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        atlas.bind();
        setupChunkShader(true);
        for (int i = visible.size() - 1; i >= 0; i--) {
            Chunk c = visible.get(i);
            if (!c.mesh.hasTranslucent()) continue;
            chunkShader.set("uOffset", (float) (c.cx * 16 - Math.floor(camX)), (float) -Math.floor(camY), (float) (c.cz * 16 - Math.floor(camZ)));
            c.mesh.drawTranslucent();
        }
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    private double dist2(Chunk c) {
        double dx = c.cx * 16 + 8 - camX, dz = c.cz * 16 + 8 - camZ;
        return dx * dx + dz * dz;
    }

    private void setupBasic(Matrix4f mvp, boolean texture, int fogMode, float alphaCut) {
        basicShader.bind();
        basicShader.set("uMVP", mvp);
        basicShader.set("uTex", 0);
        basicShader.set("uUseTex", texture ? 1 : 0);
        basicShader.set("uFog", fogMode);
        basicShader.set("uAlphaCut", alphaCut);
        basicShader.set("uFogColor", fogColor.x, fogColor.y, fogColor.z);
        basicShader.set("uFogStart", fogStart);
        basicShader.set("uFogEnd", fogEnd);
    }

    /** Minecraft "fancy" style clouds: extruded 12x4x12 boxes at y = 192. */
    public void renderClouds(World world, float pt, int renderDistance) {
        float cloudY = 192;
        double drift = (world.time + pt) * 0.03;
        double cx = camX + drift, cz = camZ + 3.96;
        float cell = 12, thickness = 4;
        int range = (int) Math.min(64, Math.max(10, renderDistance * 16 * 1.2 / cell));
        int bx = (int) Math.floor(cx / cell), bz = (int) Math.floor(cz / cell);
        float ry = (float) (cloudY - camY);
        float bright = Math.max(0.2f, daylight);
        int top = argb(0.8f, bright, bright, bright), bottom = argb(0.8f, bright * 0.7f, bright * 0.7f, bright * 0.75f);
        int sideX = argb(0.8f, bright * 0.9f, bright * 0.9f, bright * 0.9f), sideZ = argb(0.8f, bright * 0.8f, bright * 0.8f, bright * 0.82f);

        setupBasic(projView, false, 2, 0.01f);
        glActiveTexture(GL_TEXTURE0);
        white.bind();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        basicShader.set("uFogStart", range * cell * 0.5f);
        basicShader.set("uFogEnd", range * cell);
        batch.begin(GL_TRIANGLES);
        boolean below = camY < cloudY, above = camY > cloudY + thickness;
        for (int gx = bx - range; gx <= bx + range; gx++) {
            for (int gz = bz - range; gz <= bz + range; gz++) {
                if (!cloud(gx, gz)) continue;
                float x0 = (float) (gx * cell - cx), z0 = (float) (gz * cell - cz);
                if (x0 * x0 + z0 * z0 > (range * cell) * (range * cell)) continue;
                float x1 = x0 + cell, z1 = z0 + cell, y0 = ry, y1 = ry + thickness;
                if (!below) batch.quad(x0, y1, z0, 0, 0, x0, y1, z1, 0, 0, x1, y1, z1, 0, 0, x1, y1, z0, 0, 0, top);
                if (!above) batch.quad(x0, y0, z0, 0, 0, x1, y0, z0, 0, 0, x1, y0, z1, 0, 0, x0, y0, z1, 0, 0, bottom);
                if (!cloud(gx - 1, gz)) batch.quad(x0, y1, z0, 0, 0, x0, y0, z0, 0, 0, x0, y0, z1, 0, 0, x0, y1, z1, 0, 0, sideX);
                if (!cloud(gx + 1, gz)) batch.quad(x1, y1, z1, 0, 0, x1, y0, z1, 0, 0, x1, y0, z0, 0, 0, x1, y1, z0, 0, 0, sideX);
                if (!cloud(gx, gz - 1)) batch.quad(x1, y1, z0, 0, 0, x1, y0, z0, 0, 0, x0, y0, z0, 0, 0, x0, y1, z0, 0, 0, sideZ);
                if (!cloud(gx, gz + 1)) batch.quad(x0, y1, z1, 0, 0, x0, y0, z1, 0, 0, x1, y0, z1, 0, 0, x1, y1, z1, 0, 0, sideZ);
            }
        }
        batch.end();
        glDisable(GL_BLEND);
    }

    private boolean cloud(int gx, int gz) {
        return cloudMap[Math.floorMod(gz, 128) * 128 + Math.floorMod(gx, 128)];
    }

    private static int argb(float a, float r, float g, float b) {
        return ((int) (a * 255) << 24) | ((int) (Math.min(1, r) * 255) << 16) | ((int) (Math.min(1, g) * 255) << 8) | (int) (Math.min(1, b) * 255);
    }

    public void renderSelection(World world, RayCast.Hit hit, float breakProgress) {
        if (hit == null) return;
        Block b = Block.get(world.getBlock(hit.x, hit.y, hit.z));
        double[] s = RayCast.shape(b);
        float e = 0.002f;
        float x0 = (float) (hit.x + s[0] - camX) - e, y0 = (float) (hit.y + s[1] - camY) - e, z0 = (float) (hit.z + s[2] - camZ) - e;
        float x1 = (float) (hit.x + s[3] - camX) + e, y1 = (float) (hit.y + s[4] - camY) + e, z1 = (float) (hit.z + s[5] - camZ) + e;

        if (breakProgress > 0 && b.model == Block.Model.CUBE) {
            int stage = Math.min(9, (int) (breakProgress * 10));
            int tex = Block.Tex.BREAK_0 + stage;
            float u0 = (tex & 15) / 16f, v0 = (tex >> 4) / 16f, u1 = u0 + 1 / 16f, v1 = v0 + 1 / 16f;
            setupBasic(projView, true, 0, 0.01f);
            atlas.bind();
            glEnable(GL_BLEND);
            glBlendFunc(GL_DST_COLOR, GL_SRC_COLOR);
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(-1, -10);
            glDepthMask(false);
            int c = 0xFFFFFFFF;
            batch.begin(GL_TRIANGLES);
            batch.quad(x0, y1, z0, u0, v0, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0, c);
            batch.quad(x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1, c);
            batch.quad(x1, y1, z0, u0, v0, x1, y0, z0, u0, v1, x0, y0, z0, u1, v1, x0, y1, z0, u1, v0, c);
            batch.quad(x0, y1, z1, u0, v0, x0, y0, z1, u0, v1, x1, y0, z1, u1, v1, x1, y1, z1, u1, v0, c);
            batch.quad(x0, y1, z0, u0, v0, x0, y0, z0, u0, v1, x0, y0, z1, u1, v1, x0, y1, z1, u1, v0, c);
            batch.quad(x1, y1, z1, u0, v0, x1, y0, z1, u0, v1, x1, y0, z0, u1, v1, x1, y1, z0, u1, v0, c);
            batch.end();
            glDepthMask(true);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }

        setupBasic(projView, false, 0, 0);
        white.bind();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int c = 0x66000000;
        batch.begin(GL_LINES);
        float[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] ed : edges) {
            float[] a = corners[ed[0]], bb = corners[ed[1]];
            batch.v(a[0], a[1], a[2], 0, 0, c).v(bb[0], bb[1], bb[2], 0, 0, c);
        }
        batch.end();
        glDisable(GL_BLEND);
    }

    public void renderParticles(Particles particles, Player p, float pt) {
        if (particles.isEmpty()) return;
        setupBasic(projView, true, 1, 0.1f);
        atlas.bind();
        glDisable(GL_CULL_FACE);
        batch.begin(GL_TRIANGLES);
        particles.render(batch, camX, camY, camZ, p.yaw, p.pitch, pt);
        batch.end();
        glEnable(GL_CULL_FACE);
    }

    /** Draws the held block in the lower right like the first-person hand. */
    public void renderHeldItem(Block block, float swing, float equip, float brightness, float aspect, float fov) {
        if (block == null || block == Block.AIR) return;
        glClear(GL_DEPTH_BUFFER_BIT);
        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(70), aspect, 0.01f, 10f);
        float sw = (float) Math.sin(swing * Math.PI);
        float swSqrt = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        Matrix4f m = new Matrix4f(proj)
                .translate(0.62f - swSqrt * 0.4f, -0.6f - equip * 0.6f + (float) Math.sin(Math.sqrt(swing) * Math.PI * 2) * 0.2f, -0.9f - sw * 0.2f)
                .rotateY((float) Math.toRadians(45 - swSqrt * 20))
                .rotateX((float) Math.toRadians(-sw * 80 * 0.5f))
                .scale(0.36f);
        setupBasic(m, true, 0, 0.3f);
        atlas.bind();
        glEnable(GL_CULL_FACE);
        batch.begin(GL_TRIANGLES);
        if (block.model == Block.Model.CUBE) {
            cubeInto(batch, block, -0.5f, -0.5f, -0.5f, 1, brightness);
        } else {
            int tex = block.texSide;
            float u0 = (tex & 15) / 16f, v0 = (tex >> 4) / 16f, u1 = u0 + 1 / 16f, v1 = v0 + 1 / 16f;
            int c = shade(brightness, tintFor(block));
            glDisable(GL_CULL_FACE);
            batch.quad(-0.5f, 0.5f, 0, u0, v0, -0.5f, -0.5f, 0, u0, v1, 0.5f, -0.5f, 0, u1, v1, 0.5f, 0.5f, 0, u1, v0, c);
        }
        batch.end();
        glEnable(GL_CULL_FACE);
    }

    public static int tintFor(Block b) {
        return switch (b.tint) {
            case GRASS -> 0x7fb238;
            case FOLIAGE -> 0x59ae30;
            case BIRCH -> 0x80a755;
            case SPRUCE -> 0x619961;
            default -> 0xFFFFFF;
        };
    }

    private static int shade(float f, int rgb) {
        int r = (int) ((rgb >> 16 & 255) * f), g = (int) ((rgb >> 8 & 255) * f), b = (int) ((rgb & 255) * f);
        return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }

    /** Adds a textured, shaded cube to the batch (used for held item and GUI icons). */
    public static void cubeInto(Batch batch, Block block, float x, float y, float z, float s, float brightness) {
        float[] shades = {1.0f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f};
        int tint = tintFor(block);
        for (int f = 0; f < 6; f++) {
            int tex = block.textureForFace(f);
            // Grass sides aren't fully tinted; approximate with a light tint on sides
            int faceTint = tint;
            if (block == Block.GRASS) {
                faceTint = 0xFFFFFF;
                tex = f == 0 ? Block.Tex.GRASS_TOP_ITEM : f == 1 ? Block.Tex.DIRT : Block.Tex.GRASS_SIDE_ITEM;
            }
            int c = shade(brightness * shades[f], faceTint);
            float u0 = (tex & 15) / 16f, v0 = (tex >> 4) / 16f, u1 = u0 + 1 / 16f, v1 = v0 + 1 / 16f;
            float X0 = x, Y0 = y, Z0 = z, X1 = x + s, Y1 = y + s, Z1 = z + s;
            switch (f) {
                case 0 -> batch.quad(X0, Y1, Z0, u0, v0, X0, Y1, Z1, u0, v1, X1, Y1, Z1, u1, v1, X1, Y1, Z0, u1, v0, c);
                case 1 -> batch.quad(X0, Y0, Z0, u0, v0, X1, Y0, Z0, u1, v0, X1, Y0, Z1, u1, v1, X0, Y0, Z1, u0, v1, c);
                case 2 -> batch.quad(X1, Y1, Z0, u0, v0, X1, Y0, Z0, u0, v1, X0, Y0, Z0, u1, v1, X0, Y1, Z0, u1, v0, c);
                case 3 -> batch.quad(X0, Y1, Z1, u0, v0, X0, Y0, Z1, u0, v1, X1, Y0, Z1, u1, v1, X1, Y1, Z1, u1, v0, c);
                case 4 -> batch.quad(X0, Y1, Z0, u0, v0, X0, Y0, Z0, u0, v1, X0, Y0, Z1, u1, v1, X0, Y1, Z1, u1, v0, c);
                default -> batch.quad(X1, Y1, Z1, u0, v0, X1, Y0, Z1, u0, v1, X1, Y0, Z0, u1, v1, X1, Y1, Z0, u1, v0, c);
            }
        }
    }

    public void renderOverlay(boolean underwater, boolean inLava, int width, int height) {
        if (!underwater && !inLava) return;
        Matrix4f ortho = new Matrix4f().setOrtho(0, 1, 1, 0, -1, 1);
        setupBasic(ortho, false, 0, 0);
        white.bind();
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        batch.begin(GL_TRIANGLES);
        batch.rect(0, 0, 1, 1, 0, 0, 1, 1, inLava ? 0xB0FF4000 : 0x40102A80);
        batch.end();
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
}
