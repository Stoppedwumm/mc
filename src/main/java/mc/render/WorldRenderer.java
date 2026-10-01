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

import static org.lwjgl.opengl.GL33C.*;

/**
 * Renders the world into the HDR scene buffer: sun shadow map, physically inspired sky, lit terrain,
 * reflective water with depth absorption, particles, selection and the held item.
 */
public final class WorldRenderer {
    public static final int SHADOW_SIZE = 2048;
    private static final float SHADOW_EXTENT = 80;

    public final ShaderProgram chunkShader, skyShader, basicShader, shadowShader;
    public final Texture atlas, white;
    public final Batch batch = new Batch();
    private final int emptyVao;
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final FrustumIntersection shadowFrustum = new FrustumIntersection();
    private final List<Chunk> visible = new ArrayList<>();
    private final Framebuffer shadowFb;

    public final Matrix4f projection = new Matrix4f(), view = new Matrix4f(), projView = new Matrix4f();
    private final Matrix4f shadowMatrix = new Matrix4f();
    public double camX, camY, camZ;
    public float near = 0.05f, far = 512;
    public int renderedChunks;
    public float fogEnd;
    /** Exponential haze; lowered when distant terrain extends the view. */
    public float fogDensity = 0.0025f;
    public float daylight, celestialAngle, time, rain;
    public final Vector3f sunDir = new Vector3f(), lightDir = new Vector3f();
    public float brightness = 0.5f;
    /** 0..1 strength of the night vision effect (fades out in its last seconds). */
    public float nightVision;
    public boolean shadows = true, clouds = true, underwater, inLava;
    /** Rendering the Nether: no sky, sun or shadows; red fog. */
    public boolean nether;
    public static final float[] NETHER_FOG = {0.045f, 0.006f, 0.004f};
    public int width, height;

    public WorldRenderer(long seed) {
        chunkShader = new ShaderProgram("chunk");
        skyShader = new ShaderProgram("sky");
        basicShader = new ShaderProgram("basic");
        shadowShader = new ShaderProgram("shadow");
        atlas = new Texture(new TextureGen().generate(), TextureGen.ATLAS, TextureGen.ATLAS, true, false);
        white = new Texture(new int[]{0xFFFFFFFF}, 1, 1, false, true);
        emptyVao = glGenVertexArrays();
        shadowFb = new Framebuffer(SHADOW_SIZE, SHADOW_SIZE, 0, true, true);
    }

    // ------------------------------------------------------------------ environment

    public void updateEnvironment(World world, float pt, boolean underwater, boolean inLava, int renderDistance, float rain) {
        double t = (world.time % 24000) + pt;
        time = (float) ((world.time + pt) / 20.0 % 3600.0);
        this.rain = rain;
        this.underwater = underwater;
        this.inLava = inLava;
        double f = t / 24000.0 - 0.25;
        f = f - Math.floor(f);
        f = f + (1 - (Math.cos(f * Math.PI) + 1) / 2 - f) / 3;
        celestialAngle = (float) f;
        double a = f * Math.PI * 2;
        sunDir.set((float) -Math.sin(a), (float) Math.cos(a), 0.12f).normalize();
        float d = (float) Math.max(0, Math.min(1, Math.cos(a) * 2 + 0.5));
        daylight = (0.16f + 0.84f * d) * (1 - rain * 0.35f);
        if (sunDir.y > -0.05f) lightDir.set(sunDir); else lightDir.set(sunDir).negate();
        fogEnd = renderDistance * 16f;
    }

    /** Camera orientation (degrees) for billboards; differs from the player's in front third-person view. */
    public float camYaw, camPitch;

    /**
     * perspective: 0 first person, 1 third person behind, 2 third person in front (looking back at the player).
     * Third-person cameras back off along the view ray until they would hit a block.
     */
    public void setupCamera(Player p, float pt, float fov, int width, int height, int renderDistance, boolean bobbing,
                            int perspective, mc.world.World world) {
        this.width = width;
        this.height = height;
        camX = p.interpX(pt);
        camY = p.interpY(pt) + p.prevEyeHeight + (p.eyeHeight - p.prevEyeHeight) * pt;
        camZ = p.interpZ(pt);
        float yaw = p.yaw, pitch = p.pitch;
        if (perspective == 2) { yaw += 180; pitch = -pitch; }
        camYaw = yaw;
        camPitch = pitch;
        if (perspective != 0) {
            double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
            double dx = Math.sin(ry) * Math.cos(rp), dy = Math.sin(rp), dz = -Math.cos(ry) * Math.cos(rp);
            double dist = 4;
            // Test a few rays around the camera so the near plane doesn't clip into walls
            for (int i = 0; i < 8; i++) {
                double ox = ((i & 1) * 2 - 1) * 0.1, oy = ((i >> 1 & 1) * 2 - 1) * 0.1, oz = ((i >> 2 & 1) * 2 - 1) * 0.1;
                mc.util.RayCast.Hit h = mc.util.RayCast.cast(world, camX + ox, camY + oy, camZ + oz, dx, dy, dz, dist);
                if (h != null && Block.get(world.getBlock(h.x, h.y, h.z)).solid) dist = Math.min(dist, h.distance);
            }
            camX += dx * dist;
            camY += dy * dist;
            camZ += dz * dist;
            bobbing = false;
        }
        far = Math.max(256f, renderDistance * 16f * 2f);
        projection.setPerspective((float) Math.toRadians(fov), (float) width / height, near, far);
        view.identity();
        float tilt = p.prevTilt + (p.tilt - p.prevTilt) * pt;
        if (perspective == 0) view.rotateX((float) Math.toRadians(tilt));
        float hurt = p.hurtTime > 0 ? (p.hurtTime - pt) / 10f : 0;
        if (hurt > 0 && perspective == 0) view.rotateZ((float) Math.toRadians(Math.sin(hurt * hurt * hurt * hurt * Math.PI) * 14));
        if (bobbing) {
            float wd = p.walkDist - p.prevWalkDist;
            float walk = -(p.walkDist + wd * pt);
            float bob = p.prevBob + (p.bob - p.prevBob) * pt;
            view.translate((float) Math.sin(walk * Math.PI) * bob * 0.5f, -(float) Math.abs(Math.cos(walk * Math.PI) * bob), 0);
            view.rotateZ((float) Math.toRadians(Math.sin(walk * Math.PI) * bob * 3));
            view.rotateX((float) Math.toRadians(Math.abs(Math.cos(walk * Math.PI - 0.2) * bob) * 5));
        }
        view.rotateX((float) Math.toRadians(pitch));
        view.rotateY((float) Math.toRadians(yaw + 180));
        projection.mul(view, projView);
        frustum.set(projView);
    }

    private void setCommon(ShaderProgram s) {
        s.set("uSunDir", sunDir.x, sunDir.y, sunDir.z);
        s.set("uTime", time);
        s.set("uRain", rain);
    }

    private void setChunkOffsets(ShaderProgram s, Chunk c) {
        s.set("uOffset", (float) (c.cx * 16 - Math.floor(camX)), (float) -Math.floor(camY), (float) (c.cz * 16 - Math.floor(camZ)));
    }

    private void setCamera(ShaderProgram s) {
        s.set("uCamFrac", (float) (camX - Math.floor(camX)), (float) (camY - Math.floor(camY)), (float) (camZ - Math.floor(camZ)));
        s.set("uCamInt", (float) Math.floor(camX), (float) Math.floor(camY), (float) Math.floor(camZ));
    }

    // ------------------------------------------------------------------ shadows

    /** Renders terrain depth from the sun (or moon) into the shadow map, stabilised to world-space texels. */
    public void renderShadows(World world) {
        boolean lightUp = lightDir.y > 0.08f;
        if (!shadows || !lightUp || underwater || nether) {
            shadowMatrix.identity().scale(0);
            return;
        }
        Matrix4f lightView = new Matrix4f().lookAt(lightDir.x * 256, lightDir.y * 256, lightDir.z * 256, 0, 0, 0, 0, 0, 1);
        // Snap to texel grid so shadows don't shimmer when moving
        float texel = SHADOW_EXTENT * 2 / SHADOW_SIZE;
        Vector3f camLs = lightView.transformDirection(new Vector3f((float) (camX % 4096), (float) camY, (float) (camZ % 4096)));
        float offX = camLs.x - (float) Math.floor(camLs.x / texel) * texel;
        float offY = camLs.y - (float) Math.floor(camLs.y / texel) * texel;
        shadowMatrix.setOrtho(-SHADOW_EXTENT, SHADOW_EXTENT, -SHADOW_EXTENT, SHADOW_EXTENT, 0, 512)
                .translate(offX, offY, 0).mul(lightView);
        shadowFrustum.set(shadowMatrix);

        shadowFb.bind();
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.1f, 2f);
        shadowShader.bind();
        shadowShader.set("uShadowMatrix", shadowMatrix);
        shadowShader.set("uTex", 0);
        setCommon(shadowShader);
        setCamera(shadowShader);
        glActiveTexture(GL_TEXTURE0);
        atlas.bind();
        for (Chunk c : world.chunks()) {
            if (c.mesh == null || !c.mesh.hasSolid()) continue;
            float ox = (float) (c.cx * 16 - camX), oz = (float) (c.cz * 16 - camZ);
            if (!shadowFrustum.testAab(ox, (float) -camY, oz, ox + 16, (float) (c.maxY + 1 - camY), oz + 16)) continue;
            setChunkOffsets(shadowShader, c);
            c.mesh.drawSolid();
        }
        glBindVertexArray(0);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
    }

    // ------------------------------------------------------------------ passes

    public void renderSky() {
        if (nether) return;
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        skyShader.bind();
        setCommon(skyShader);
        skyShader.set("uInvProjView", new Matrix4f(projView).invert());
        skyShader.set("uStarAngle", celestialAngle * (float) Math.PI * 2);
        skyShader.set("uCamPos", (float) (camX % 100000), (float) camY, (float) (camZ % 100000));
        skyShader.set("uClouds", clouds ? 1 : 0);
        glBindVertexArray(emptyVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    private void setupChunkShader(boolean translucent, int depthTex) {
        chunkShader.bind();
        setCommon(chunkShader);
        setCamera(chunkShader);
        chunkShader.set("uProjView", projView);
        chunkShader.set("uTex", 0);
        chunkShader.set("uShadowMap", 1);
        chunkShader.set("uDepthTex", 2);
        chunkShader.set("uShadows", shadows && lightDir.y > 0.08f && !underwater && !nether ? 1 : 0);
        chunkShader.set("uNether", nether ? 1 : 0);
        chunkShader.set("uShadowMatrix", shadowMatrix);
        chunkShader.set("uLightDir", lightDir.x, lightDir.y, lightDir.z);
        chunkShader.set("uTranslucent", translucent ? 1 : 0);
        chunkShader.set("uFogEnd", inLava ? 3f : fogEnd);
        chunkShader.set("uFogDensity", fogDensity);
        chunkShader.set("uUnderwater", underwater ? 1 : 0);
        chunkShader.set("uScreen", (float) width, (float) height);
        chunkShader.set("uNear", near);
        chunkShader.set("uFar", far);
        chunkShader.set("uBrightness", brightness);
        chunkShader.set("uNightVision", nightVision);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFb.depth);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, depthTex);
        glActiveTexture(GL_TEXTURE0);
        atlas.bind();
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

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        setupChunkShader(false, 0);
        for (Chunk c : visible) {
            if (!c.mesh.hasSolid()) continue;
            setChunkOffsets(chunkShader, c);
            c.mesh.drawSolid();
        }
        glBindVertexArray(0);
    }

    public void renderTranslucent(int depthTex) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        setupChunkShader(true, depthTex);
        for (int i = visible.size() - 1; i >= 0; i--) {
            Chunk c = visible.get(i);
            if (!c.mesh.hasTranslucent()) continue;
            setChunkOffsets(chunkShader, c);
            c.mesh.drawTranslucent();
        }
        glBindVertexArray(0);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);
    }

    private double dist2(Chunk c) {
        double dx = c.cx * 16 + 8 - camX, dz = c.cz * 16 + 8 - camZ;
        return dx * dx + dz * dz;
    }

    /** Sets up the simple textured/coloured shader for drawing into the HDR scene. */
    public void setupBasic(Matrix4f mvp, boolean texture, int fogMode, float alphaCut) {
        basicShader.bind();
        basicShader.set("uMVP", mvp);
        basicShader.set("uTex", 0);
        basicShader.set("uUseTex", texture ? 1 : 0);
        basicShader.set("uFog", fogMode);
        basicShader.set("uAlphaCut", alphaCut);
        if (nether) basicShader.set("uFogColor", NETHER_FOG[0], NETHER_FOG[1], NETHER_FOG[2]);
        else basicShader.set("uFogColor", 0.5f, 0.6f, 0.8f);
        basicShader.set("uFogStart", fogEnd * 0.7f);
        basicShader.set("uFogEnd", fogEnd);
        basicShader.set("uLinear", 1);
        basicShader.set("uLight", 1f);
    }

    public void renderSelection(World world, RayCast.Hit hit, float breakProgress) {
        if (hit == null) return;
        Block b = Block.get(world.getBlock(hit.x, hit.y, hit.z));
        double[] s = RayCast.bounds(world, b, hit.x, hit.y, hit.z);
        float e = 0.002f;
        float x0 = (float) (hit.x + s[0] - camX) - e, y0 = (float) (hit.y + s[1] - camY) - e, z0 = (float) (hit.z + s[2] - camZ) - e;
        float x1 = (float) (hit.x + s[3] - camX) + e, y1 = (float) (hit.y + s[4] - camY) + e, z1 = (float) (hit.z + s[5] - camZ) + e;

        if (breakProgress > 0 && (b.model == Block.Model.CUBE || b.model == Block.Model.SHAPE)) {
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
        setupBasic(projView, true, 0, 0.1f);
        basicShader.set("uLight", 1.6f);
        atlas.bind();
        glDisable(GL_CULL_FACE);
        batch.begin(GL_TRIANGLES);
        particles.render(batch, camX, camY, camZ, camYaw, camPitch, pt, false);
        batch.end();
        white.bind();
        basicShader.set("uAlphaCut", 0.01f);
        basicShader.set("uLight", Math.max(0.3f, daylight) * 1.5f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        batch.begin(GL_TRIANGLES);
        particles.render(batch, camX, camY, camZ, camYaw, camPitch, pt, true);
        batch.end();
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
    }

    /**
     * First-person hand: the held item (block cube or extruded sprite) or the bare arm, with swing, equip,
     * eating and bow-drawing animations.
     */
    /** Damage/meta of the held stack (potion colour). */
    public int heldMeta;

    public void renderHeldItem(mc.item.Item item, ItemRenderer ir, float swing, float equip, float light, float aspect,
                               int useType, float useProgress, float time) {
        glClear(GL_DEPTH_BUFFER_BIT);
        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(70), aspect, 0.01f, 10f);
        float sw = (float) Math.sin(swing * Math.PI);
        float swSqrt = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        Matrix4f m = new Matrix4f(proj);
        if (item == null) {
            // Bare arm
            m.translate(0.64f - swSqrt * 0.3f, -0.72f - equip * 0.6f + (float) Math.sin(Math.sqrt(swing) * Math.PI * 2) * 0.15f, -0.78f - sw * 0.3f)
                    .rotateY((float) Math.toRadians(-18 + swSqrt * 30)).rotateX((float) Math.toRadians(-70 - sw * 20))
                    .rotateZ((float) Math.toRadians(-8));
            setupBasic(m, false, 0, 0);
            basicShader.set("uLight", light);
            white.bind();
            batch.begin(GL_TRIANGLES);
            armBox(batch, -0.12f, -0.1f, -0.12f, 0.24f, 0.8f, 0.24f);
            batch.end();
            return;
        }
        m.translate(0.56f - swSqrt * 0.4f, -0.52f - equip * 0.6f + (float) Math.sin(Math.sqrt(swing) * Math.PI * 2) * 0.2f, -0.72f - sw * 0.2f);
        if (useType == 1) {
            // Eating: bring to the mouth and bob
            float p = Math.min(1, useProgress * 4);
            m.translate(-0.35f * p, 0.18f * p + (float) Math.abs(Math.cos(time / 4f * Math.PI)) * 0.08f * p, 0.1f * p);
            m.rotateY((float) Math.toRadians(-40 * p)).rotateX((float) Math.toRadians(-15 * p));
        } else if (useType == 2) {
            // Drawing a bow: hold it upright in front of the camera
            float shake = useProgress >= 1 ? (float) Math.sin(time * 1.3) * 0.006f : 0;
            m.translate(-0.3f, 0.12f + shake, 0.15f + useProgress * 0.12f).rotateZ((float) Math.toRadians(-8));
        }
        boolean cube = ItemRenderer.isCube(item);
        if (cube) {
            m.rotateY((float) Math.toRadians(45 - swSqrt * 20)).rotateX((float) Math.toRadians(-sw * 40)).scale(0.4f);
        } else if (useType == 2) {
            m.rotateY((float) Math.toRadians(170)).rotateZ((float) Math.toRadians(-45)).scale(0.7f);
        } else {
            // First-person item pose: handle at the lower right, tip pointing up towards the crosshair
            m.rotateY((float) Math.toRadians(-sw * 20)).rotateZ((float) Math.toRadians(-sw * 20)).rotateX((float) Math.toRadians(-sw * 50));
            m.translate(-0.05f, 0.3f, 0).rotateY((float) Math.toRadians(170)).scale(item.handheld ? 0.5f : 0.4f);
        }
        setupBasic(m, true, 0, 0.3f);
        basicShader.set("uLight", light);
        ir.bindFor(item, this);
        glDisable(GL_CULL_FACE);
        batch.begin(GL_TRIANGLES);
        ir.emit(batch, new Matrix4f(), item, ItemRenderer.tint(item));
        ir.emitPotion(batch, new Matrix4f(), item, heldMeta);
        batch.end();
        glEnable(GL_CULL_FACE);
    }

    private void armBox(Batch b, float x, float y, float z, float w, float h, float d) {
        int skin = 0xFFc89a70, side = 0xFFa87a55, dark = 0xFF8a6040;
        float x1 = x + w, y1 = y + h, z1 = z + d;
        b.quad(x, y1, z, 0, 0, x, y1, z1, 0, 0, x1, y1, z1, 0, 0, x1, y1, z, 0, 0, skin);
        b.quad(x, y, z1, 0, 0, x, y, z, 0, 0, x1, y, z, 0, 0, x1, y, z1, 0, 0, dark);
        b.quad(x, y1, z1, 0, 0, x, y, z1, 0, 0, x1, y, z1, 0, 0, x1, y1, z1, 0, 0, side);
        b.quad(x1, y1, z, 0, 0, x1, y, z, 0, 0, x, y, z, 0, 0, x, y1, z, 0, 0, side);
        b.quad(x, y1, z, 0, 0, x, y, z, 0, 0, x, y, z1, 0, 0, x, y1, z1, 0, 0, skin);
        b.quad(x1, y1, z1, 0, 0, x1, y, z1, 0, 0, x1, y, z, 0, 0, x1, y1, z, 0, 0, dark);
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

    /** Adds a textured, shaded block model to the batch (used for held items, GUI icons and falling blocks). */
    public static void cubeInto(QuadSink batch, Block block, float x, float y, float z, float s, float brightness) {
        java.util.List<int[]> boxes = block.model == Block.Model.SHAPE ? mc.world.Shapes.itemBoxes(block)
                : java.util.List.of(new int[]{0, 0, 0, 16, 16, 16});
        for (int[] b : boxes) boxInto(batch, block, b, x, y, z, s, brightness);
    }

    private static final float[] FACE_SHADES = {1.0f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f};

    /** One box of a block model; UVs follow the box like Minecraft's block models. */
    public static void boxInto(QuadSink batch, Block block, int[] b, float x, float y, float z, float s, float brightness) {
        int tint = tintFor(block);
        float k = s / 16f;
        for (int f = 0; f < 6; f++) {
            int tex = block.textureForFace(f, block == Block.BED && f == 0 ? 4 : 0);
            int faceTint = tint;
            if (block == Block.GRASS) {
                faceTint = 0xFFFFFF;
                tex = f == 0 ? Block.Tex.GRASS_TOP_ITEM : f == 1 ? Block.Tex.DIRT : Block.Tex.GRASS_SIDE_ITEM;
            }
            int c = shade(brightness * FACE_SHADES[f], faceTint);
            float tu = (tex & 15) / 16f, tv = (tex >> 4) / 16f;
            int[][] q = switch (f) {
                case 0 -> new int[][]{{b[0], b[4], b[2]}, {b[0], b[4], b[5]}, {b[3], b[4], b[5]}, {b[3], b[4], b[2]}};
                case 1 -> new int[][]{{b[0], b[1], b[2]}, {b[3], b[1], b[2]}, {b[3], b[1], b[5]}, {b[0], b[1], b[5]}};
                case 2 -> new int[][]{{b[3], b[4], b[2]}, {b[3], b[1], b[2]}, {b[0], b[1], b[2]}, {b[0], b[4], b[2]}};
                case 3 -> new int[][]{{b[0], b[4], b[5]}, {b[0], b[1], b[5]}, {b[3], b[1], b[5]}, {b[3], b[4], b[5]}};
                case 4 -> new int[][]{{b[0], b[4], b[2]}, {b[0], b[1], b[2]}, {b[0], b[1], b[5]}, {b[0], b[4], b[5]}};
                default -> new int[][]{{b[3], b[4], b[5]}, {b[3], b[1], b[5]}, {b[3], b[1], b[2]}, {b[3], b[4], b[2]}};
            };
            float[] v = new float[20];
            for (int n = 0; n < 4; n++) {
                int px = q[n][0], py = q[n][1], pz = q[n][2];
                int u, w;
                switch (f) {
                    case 0, 1 -> { u = px; w = pz; }
                    case 2 -> { u = 16 - px; w = 16 - py; }
                    case 3 -> { u = px; w = 16 - py; }
                    case 4 -> { u = pz; w = 16 - py; }
                    default -> { u = 16 - pz; w = 16 - py; }
                }
                v[n * 5] = x + px * k; v[n * 5 + 1] = y + py * k; v[n * 5 + 2] = z + pz * k;
                v[n * 5 + 3] = tu + u / 256f; v[n * 5 + 4] = tv + w / 256f;
            }
            batch.quad(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12], v[13], v[14],
                    v[15], v[16], v[17], v[18], v[19], c);
        }
    }

}
