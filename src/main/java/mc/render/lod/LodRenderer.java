package mc.render.lod;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import mc.render.QuadIndices;
import mc.render.ShaderProgram;
import mc.render.WorldRenderer;
import mc.world.Chunk;
import mc.world.World;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Distant terrain beyond the render distance, using the techniques of Distant Horizons and Voxy:
 * <ul>
 * <li>a quadtree of tiles whose cells double in size with distance (1, 2, 4 ... 128 blocks), so far terrain costs
 * about as much as near terrain;</li>
 * <li>tiles are built on background threads from real chunk summaries where the world has been loaded, and from
 * the terrain generator's noise elsewhere;</li>
 * <li>a separate pass with its own far projection, drawn before the normal terrain with the depth buffer cleared
 * afterwards, and a per-chunk mask that discards LOD pixels wherever real chunks are rendered;</li>
 * <li>a parent tile stays visible until all its children are built, so nothing pops out while loading.</li>
 * </ul>
 */
public final class LodRenderer {
    public static final int CELLS = 32;
    static final int MAX_LEVEL = 7;
    /** Split a tile while the camera is closer than this many tile sizes. */
    private static final double SPLIT = 2.0;
    private static final int MAX_TILES = 900;

    private final ShaderProgram shader;
    private final LodPalette palette;
    private final ExecutorService builders;
    private final ConcurrentLinkedQueue<LodMesher.Built> built = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Long2ObjectOpenHashMap<Tile> tiles = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet requested = new LongOpenHashSet();
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f projection = new Matrix4f(), projView = new Matrix4f();
    private final int maskTex;
    private ByteBuffer mask;
    private int maskSize;
    private World world;
    private LodData data;
    private int generation;
    private long frame;
    public int lastDrawn, lastQuads;

    private static final class Tile {
        int level, tx, tz;
        int vao, vbo, opaqueQuads, waterQuads, indexVersion;
        float minY, maxY = 256;
        boolean ready, dirty;
        long lastUsed;
    }

    public LodRenderer(int[] atlasPixels, int atlasSize) {
        shader = new ShaderProgram("lod");
        palette = new LodPalette(atlasPixels, atlasSize);
        int threads = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 3));
        builders = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "LOD Builder");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        maskTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, maskTex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public World world() { return world; }

    public LodData data() { return data; }

    /** Switches to another world (or none); saves what was learned about the previous one. */
    public void setWorld(World w, Path lodDir) {
        if (data != null) data.save();
        clear();
        world = w;
        data = w == null ? null : new LodData(w.generator, palette, lodDir, w.nether == null);
        if (w != null) w.meshListener = this::chunkMeshed;
    }

    private void clear() {
        generation++;
        for (Tile t : tiles.values()) deleteTile(t);
        tiles.clear();
        requested.clear();
        LodMesher.Built b;
        while ((b = built.poll()) != null) MemoryUtil.memFree(b.data);
    }

    public void save() {
        if (data != null) data.save();
    }

    /** A chunk was (re)meshed: remember its surface and rebuild the tiles that cover it. */
    public void chunkMeshed(Chunk c) {
        if (data == null) return;
        data.capture(c);
        for (int level = 0; level <= MAX_LEVEL; level++) {
            int size = CELLS << level;
            long key = key(level, Math.floorDiv(c.cx * 16, size), Math.floorDiv(c.cz * 16, size));
            Tile t = tiles.get(key);
            if (t != null) t.dirty = true;
        }
    }

    static long key(int level, int tx, int tz) {
        return (long) level << 56 | (tx & 0xFFFFFFFL) << 28 | (tz & 0xFFFFFFFL);
    }

    // ------------------------------------------------------------------ selection

    private double camX, camZ;
    private int nearRadius, lodRadius;
    private final List<Tile> drawList = new ArrayList<>();
    private final List<long[]> wanted = new ArrayList<>();

    private boolean isReady(long key) {
        Tile t = tiles.get(key);
        return t != null && t.ready;
    }

    /** Distance from the camera to a tile's square (0 inside). */
    private double distance(int level, int tx, int tz) {
        double size = CELLS << level;
        double x0 = tx * size, z0 = tz * size;
        double dx = Math.max(0, Math.max(x0 - camX, camX - (x0 + size)));
        double dz = Math.max(0, Math.max(z0 - camZ, camZ - (z0 + size)));
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Whether real chunks fully cover the tile, so it never needs drawing. */
    private boolean coveredByChunks(int level, int tx, int tz) {
        double size = CELLS << level;
        double x0 = tx * size, z0 = tz * size;
        double fx = Math.max(Math.abs(x0 - camX), Math.abs(x0 + size - camX));
        double fz = Math.max(Math.abs(z0 - camZ), Math.abs(z0 + size - camZ));
        return Math.sqrt(fx * fx + fz * fz) < nearRadius;
    }

    private void select(int level, int tx, int tz) {
        double d = distance(level, tx, tz);
        if (d > lodRadius || coveredByChunks(level, tx, tz)) return;
        double size = CELLS << level;
        long self = key(level, tx, tz);
        Tile st = tiles.get(self);
        if (st != null) st.lastUsed = frame;
        if (level > 0 && d < size * SPLIT) {
            boolean childrenReady = true;
            for (int c = 0; c < 4; c++) {
                int cx = tx * 2 + (c & 1), cz = tz * 2 + (c >> 1);
                if (distance(level - 1, cx, cz) > lodRadius || coveredByChunks(level - 1, cx, cz)) continue;
                if (!isReady(key(level - 1, cx, cz))) childrenReady = false;
            }
            if (childrenReady || st == null || !st.ready) {
                for (int c = 0; c < 4; c++) select(level - 1, tx * 2 + (c & 1), tz * 2 + (c >> 1));
                return;
            }
            // Keep the parent on screen while the children are being built
            for (int c = 0; c < 4; c++) {
                int cx = tx * 2 + (c & 1), cz = tz * 2 + (c >> 1);
                if (distance(level - 1, cx, cz) <= lodRadius && !coveredByChunks(level - 1, cx, cz) && !isReady(key(level - 1, cx, cz)))
                    want(level - 1, cx, cz, distance(level - 1, cx, cz));
            }
        }
        if (st == null || !st.ready || st.dirty) want(level, tx, tz, d);
        if (st != null && st.ready) drawList.add(st);
    }

    private void want(int level, int tx, int tz, double d) {
        wanted.add(new long[]{key(level, tx, tz), (long) d, level, tx, tz});
    }

    /** Uploads finished tiles and starts building the most needed ones (nearest first). */
    private void schedule() {
        LodMesher.Built b;
        int uploads = 0;
        while (uploads < 12 && (b = built.poll()) != null) {
            requested.remove(b.key);
            if (b.generation != generation) { MemoryUtil.memFree(b.data); continue; }
            Tile t = tiles.get(b.key);
            if (t == null) {
                t = new Tile();
                t.level = (int) (b.key >>> 56);
                t.tx = b.tx;
                t.tz = b.tz;
                tiles.put(b.key, t);
            }
            upload(t, b);
            uploads++;
        }
        wanted.sort((x, y) -> Long.compare(x[1], y[1]));
        lastWanted = wanted.size();
        int maxInFlight = 6;
        for (long[] w : wanted) {
            if (inFlight.get() >= maxInFlight) break;
            long key = w[0];
            if (requested.contains(key)) continue;
            Tile t = tiles.get(key);
            if (t != null && t.ready && !t.dirty) continue;
            if (t != null) t.dirty = false;
            requested.add(key);
            inFlight.incrementAndGet();
            int level = (int) w[2], tx = (int) w[3], tz = (int) w[4], gen = generation;
            LodData d = data;
            builders.execute(() -> {
                try {
                    built.add(new LodMesher().build(d, level, tx, tz, key, gen));
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    inFlight.decrementAndGet();
                }
            });
        }
        wanted.clear();
    }

    private void upload(Tile t, LodMesher.Built b) {
        t.opaqueQuads = b.opaqueQuads;
        t.waterQuads = b.waterQuads;
        t.minY = b.minY;
        t.maxY = b.maxY;
        t.ready = true;
        t.lastUsed = frame;
        int quads = b.opaqueQuads + b.waterQuads;
        if (quads > 0) {
            QuadIndices.ensure(quads);
            if (t.vao == 0) {
                t.vao = glGenVertexArrays();
                t.vbo = glGenBuffers();
                glBindVertexArray(t.vao);
                glBindBuffer(GL_ARRAY_BUFFER, t.vbo);
                glVertexAttribPointer(0, 3, GL_FLOAT, false, LodMesher.STRIDE, 0);
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, false, LodMesher.STRIDE, 12);
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QuadIndices.buffer());
                t.indexVersion = QuadIndices.version();
                glBindVertexArray(0);
            }
            glBindBuffer(GL_ARRAY_BUFFER, t.vbo);
            glBufferData(GL_ARRAY_BUFFER, b.data, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
        MemoryUtil.memFree(b.data);
    }

    private static void deleteTile(Tile t) {
        if (t.vao != 0) {
            glDeleteVertexArrays(t.vao);
            glDeleteBuffers(t.vbo);
            t.vao = t.vbo = 0;
        }
    }

    private void evict() {
        if (tiles.size() <= MAX_TILES) return;
        List<Long> old = new ArrayList<>();
        for (var e : tiles.long2ObjectEntrySet()) if (frame - e.getValue().lastUsed > 120) old.add(e.getLongKey());
        old.sort((a, b) -> Long.compare(tiles.get(a).lastUsed, tiles.get(b).lastUsed));
        for (int i = 0; i < old.size() && tiles.size() > MAX_TILES * 3 / 4; i++) deleteTile(tiles.remove((long) old.get(i)));
    }

    // ------------------------------------------------------------------ chunk mask

    /** One texel per chunk around the camera: 1 where a real chunk mesh is drawn (LOD pixels are discarded there). */
    private void updateMask(int renderDistance, int camCx, int camCz) {
        int r = renderDistance + 3;
        int size = 2 * r + 1;
        if (mask == null || maskSize != size) {
            if (mask != null) MemoryUtil.memFree(mask);
            mask = MemoryUtil.memAlloc(size * size);
            maskSize = size;
            glBindTexture(GL_TEXTURE_2D, maskTex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, size, size, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
        for (int dz = -r; dz <= r; dz++)
            for (int dx = -r; dx <= r; dx++) {
                Chunk c = world.getChunk(camCx + dx, camCz + dz);
                mask.put((dz + r) * size + dx + r, (byte) (c != null && c.mesh != null ? 255 : 0));
            }
        glBindTexture(GL_TEXTURE_2D, maskTex);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, size, size, GL_RED, GL_UNSIGNED_BYTE, mask);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Draws distant terrain out to lodChunks (in chunks). Call after the sky and before the normal terrain, then
     * clear the depth buffer. Returns false if nothing was drawn.
     */
    public boolean render(WorldRenderer wr, int renderDistance, int lodChunks, float fogDensity) {
        if (world == null || data == null || lodChunks <= renderDistance) return false;
        frame++;
        camX = wr.camX;
        camZ = wr.camZ;
        lodRadius = lodChunks * 16;
        nearRadius = Math.max(0, (renderDistance - 2) * 16);
        drawList.clear();
        int rootSize = CELLS << MAX_LEVEL;
        int r0x = (int) Math.floor((camX - lodRadius) / rootSize), r1x = (int) Math.floor((camX + lodRadius) / rootSize);
        int r0z = (int) Math.floor((camZ - lodRadius) / rootSize), r1z = (int) Math.floor((camZ + lodRadius) / rootSize);
        for (int tx = r0x; tx <= r1x; tx++)
            for (int tz = r0z; tz <= r1z; tz++) select(MAX_LEVEL, tx, tz);
        schedule();
        evict();

        // Projection with the same field of view but a far plane covering the LOD range
        float far = lodRadius * 1.5f + 600, near = 2f;
        projection.set(wr.projection);
        projection.m22(-(far + near) / (far - near));
        projection.m32(-2f * far * near / (far - near));
        projection.mul(wr.view, projView);
        frustum.set(projView);

        int camCx = (int) Math.floor(camX) >> 4, camCz = (int) Math.floor(camZ) >> 4;
        updateMask(renderDistance, camCx, camCz);

        shader.bind();
        shader.set("uProjView", projView);
        shader.set("uSunDir", wr.sunDir.x, wr.sunDir.y, wr.sunDir.z);
        shader.set("uLightDir", wr.lightDir.x, wr.lightDir.y, wr.lightDir.z);
        shader.set("uTime", wr.time);
        shader.set("uRain", wr.rain);
        shader.set("uBrightness", wr.brightness);
        shader.set("uNightVision", wr.nightVision);
        shader.set("uFogEnd", (float) lodRadius);
        shader.set("uFogDensity", fogDensity);
        shader.set("uMask", 0);
        shader.set("uMaskOrigin", (float) (camCx - (maskSize - 1) / 2), (float) (camCz - (maskSize - 1) / 2));
        shader.set("uMaskSize", (float) maskSize);
        shader.set("uCamPos", (float) camX, (float) wr.camY, (float) camZ);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, maskTex);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        List<Tile> visible = new ArrayList<>();
        List<float[]> offsets = new ArrayList<>();
        int quads = 0;
        for (Tile t : drawList) {
            double size = CELLS << t.level;
            float ox = (float) (t.tx * size - camX), oz = (float) (t.tz * size - camZ);
            if (t.opaqueQuads + t.waterQuads == 0) continue;
            if (!frustum.testAab(ox, (float) (t.minY - wr.camY), oz, ox + (float) size, (float) (t.maxY - wr.camY) + 1, oz + (float) size)) continue;
            visible.add(t);
            offsets.add(new float[]{ox, (float) -wr.camY, oz});
            quads += t.opaqueQuads + t.waterQuads;
        }
        shader.set("uWater", 0);
        for (int i = 0; i < visible.size(); i++) {
            Tile t = visible.get(i);
            if (t.opaqueQuads == 0) continue;
            float[] o = offsets.get(i);
            shader.set("uOffset", o[0], o[1], o[2]);
            draw(t, 0, t.opaqueQuads);
        }
        // Water on top, blended
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        shader.set("uWater", 1);
        for (int i = 0; i < visible.size(); i++) {
            Tile t = visible.get(i);
            if (t.waterQuads == 0) continue;
            float[] o = offsets.get(i);
            shader.set("uOffset", o[0], o[1], o[2]);
            draw(t, t.opaqueQuads, t.waterQuads);
        }
        glDepthMask(true);
        glDisable(GL_BLEND);
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        lastDrawn = visible.size();
        lastQuads = quads;
        return true;
    }

    private void draw(Tile t, int firstQuad, int count) {
        glBindVertexArray(t.vao);
        if (t.indexVersion != QuadIndices.version()) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QuadIndices.buffer());
            t.indexVersion = QuadIndices.version();
        }
        glDrawElements(GL_TRIANGLES, count * 6, GL_UNSIGNED_INT, (long) firstQuad * 6 * 4);
    }

    /** Whether tiles are still being built (scripted screenshots wait for this). */
    public boolean busy() {
        return inFlight.get() > 0 || !built.isEmpty() || lastWanted > 0;
    }

    private int lastWanted;

    /** Tiles loaded and builds waiting (debug screen). */
    public String stats() {
        return "LOD: " + lastDrawn + " tiles drawn, " + tiles.size() + " cached, " + inFlight.get() + " building, " + (lastQuads / 1000) + "k quads";
    }

    public void shutdown() {
        save();
        clear();
        builders.shutdownNow();
    }
}
