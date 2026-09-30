package mc.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import mc.render.ChunkMesh;
import mc.render.ChunkMesher;
import mc.render.MeshData;
import mc.world.gen.Biome;
import mc.world.gen.Decorator;
import mc.world.gen.TerrainGenerator;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns all loaded chunks. Terrain generation and meshing run on a worker pool; decoration, block edits and
 * GL uploads happen on the main thread.
 */
public final class World {
    public final TerrainGenerator generator;
    public final WorldStorage storage;
    private final Decorator decorator;
    public final long seed;

    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet generating = new LongOpenHashSet();
    private final ConcurrentLinkedQueue<Chunk> generated = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MeshData> meshed = new ConcurrentLinkedQueue<>();

    private final ThreadPoolExecutor workers;
    private final int threads;
    private final AtomicLong taskSeq = new AtomicLong();
    private int meshJobsInFlight;

    /** Ticks since world creation; 24000 per day. */
    public long time = 1000;

    private int[][] offsets = new int[0][];
    private int offsetsRadius = -1;

    public World(long seed, WorldStorage storage) {
        this.seed = seed;
        this.storage = storage;
        this.generator = new TerrainGenerator(seed);
        this.decorator = new Decorator(this, seed);
        threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "Chunk Worker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
        workers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), tf);
    }

    private final class Task implements Runnable, Comparable<Task> {
        final double priority;
        final long seq = taskSeq.incrementAndGet();
        final Runnable body;

        Task(double priority, Runnable body) {
            this.priority = priority;
            this.body = body;
        }

        public void run() {
            try {
                body.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public int compareTo(Task o) {
            int c = Double.compare(priority, o.priority);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }

    private static final ThreadLocal<ChunkMesher> MESHERS = ThreadLocal.withInitial(ChunkMesher::new);

    // ------------------------------------------------------------------ block access

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(Chunk.key(cx, cz));
    }

    public int getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return 0;
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null) return 0;
        return c.blocks[Chunk.index(x & 15, y, z & 15)];
    }

    public boolean isLoaded(int x, int z) {
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        return c != null && c.state == Chunk.STATE_DECORATED;
    }

    /** Used by world generation features; never triggers urgent remeshing. */
    public void setBlockGen(int x, int y, int z, int id) {
        if (y < 0 || y >= Chunk.HEIGHT) return;
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null) return;
        c.set(x & 15, y, z & 15, id);
        c.touched = true;
        c.needsMesh = true;
    }

    /** Player edit: updates the block and schedules remeshing of every chunk whose lighting may change. */
    public boolean setBlock(int x, int y, int z, int id) {
        if (y < 0 || y >= Chunk.HEIGHT) return false;
        int cx = x >> 4, cz = z >> 4;
        Chunk c = chunks.get(Chunk.key(cx, cz));
        if (c == null) return false;
        c.set(x & 15, y, z & 15, id);
        c.touched = true;
        int lx = x & 15, lz = z & 15;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk n = chunks.get(Chunk.key(cx + dx, cz + dz));
                if (n == null) continue;
                // Distance from the edited block to this chunk horizontally
                int ddx = dx < 0 ? lx + 1 : dx > 0 ? 16 - lx : 0;
                int ddz = dz < 0 ? lz + 1 : dz > 0 ? 16 - lz : 0;
                if (ddx > 15 || ddz > 15) continue;
                n.needsMesh = true;
                if (ddx <= 1 && ddz <= 1) n.urgentMesh = true;
            }
        }
        return true;
    }

    public Biome biomeAt(int x, int z) {
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null) return Biome.PLAINS;
        return Biome.VALUES[c.biome[(z & 15) * 16 + (x & 15)]];
    }

    public int loadedChunkCount() { return chunks.size(); }

    public Iterable<Chunk> chunks() { return chunks.values(); }

    public int pendingJobs() { return workers.getQueue().size() + workers.getActiveCount() + meshed.size() + generated.size(); }

    // ------------------------------------------------------------------ scheduling

    private void ensureOffsets(int radius) {
        if (radius == offsetsRadius) return;
        List<int[]> list = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= (radius + 0.5) * (radius + 0.5)) list.add(new int[]{dx, dz, dx * dx + dz * dz});
        list.sort((a, b) -> Integer.compare(a[2], b[2]));
        offsets = list.toArray(new int[0][]);
        offsetsRadius = radius;
    }

    private boolean decorated(int cx, int cz) {
        Chunk c = chunks.get(Chunk.key(cx, cz));
        return c != null && c.state == Chunk.STATE_DECORATED;
    }

    /** A chunk's blocks are final once it and all its neighbours have been decorated. */
    private boolean isFinal(int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (!decorated(cx + dx, cz + dz)) return false;
        return true;
    }

    private boolean canMesh(int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (!isFinal(cx + dx, cz + dz)) return false;
        return true;
    }

    /** Called every frame on the main thread. */
    public void update(double px, double pz, int renderDistance, long frameBudgetNanos) {
        long start = System.nanoTime();
        int pcx = (int) Math.floor(px) >> 4, pcz = (int) Math.floor(pz) >> 4;
        int genR = renderDistance + 3;
        int unloadR = renderDistance + 5;
        ensureOffsets(genR);

        // 1. Collect generated chunks
        Chunk g;
        while ((g = generated.poll()) != null) {
            generating.remove(g.key());
            int dx = g.cx - pcx, dz = g.cz - pcz;
            if (dx * dx + dz * dz > (unloadR + 1) * (unloadR + 1)) continue;
            chunks.put(g.key(), g);
        }

        // 2. Request generation, nearest first
        int maxGen = threads * 3;
        for (int[] o : offsets) {
            if (generating.size() >= maxGen) break;
            int cx = pcx + o[0], cz = pcz + o[1];
            long key = Chunk.key(cx, cz);
            if (chunks.containsKey(key) || generating.contains(key)) continue;
            generating.add(key);
            workers.execute(new Task(10 + o[2], () -> generated.add(loadOrGenerate(cx, cz))));
        }

        // 3. Decorate chunks whose neighbours exist
        for (int[] o : offsets) {
            if (o[2] > (genR - 1) * (genR - 1) + genR) break;
            int cx = pcx + o[0], cz = pcz + o[1];
            Chunk c = chunks.get(Chunk.key(cx, cz));
            if (c == null || c.state != Chunk.STATE_TERRAIN) continue;
            boolean ok = true;
            for (int dx = -1; dx <= 1 && ok; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (!chunks.containsKey(Chunk.key(cx + dx, cz + dz))) { ok = false; break; }
            if (!ok) continue;
            decorator.decorate(c);
            c.touched = true;
            if (System.nanoTime() - start > frameBudgetNanos / 2) break;
        }

        // 4. Urgent remeshes (player edits) first, then everything else nearest first
        int maxMesh = threads * 2;
        for (int[] o : offsets) {
            if (o[2] > renderDistance * renderDistance + renderDistance) break;
            Chunk c = chunks.get(Chunk.key(pcx + o[0], pcz + o[1]));
            if (c != null && c.urgentMesh && c.needsMesh && !c.meshInFlight && canMesh(c.cx, c.cz)) submitMesh(c, -1000 + o[2]);
        }
        for (int[] o : offsets) {
            if (meshJobsInFlight >= maxMesh) break;
            if (o[2] > renderDistance * renderDistance + renderDistance) break;
            Chunk c = chunks.get(Chunk.key(pcx + o[0], pcz + o[1]));
            if (c == null || !c.needsMesh || c.meshInFlight) continue;
            if (!canMesh(c.cx, c.cz)) continue;
            submitMesh(c, o[2] + (c.mesh != null ? -500 : 0));
        }

        // 5. Upload finished meshes
        MeshData m;
        while ((m = meshed.poll()) != null) {
            meshJobsInFlight--;
            Chunk c = chunks.get(Chunk.key(m.cx, m.cz));
            if (c == null) {
                if (m.solid != null) MemoryUtil.memFree(m.solid);
                if (m.translucent != null) MemoryUtil.memFree(m.translucent);
                continue;
            }
            c.meshInFlight = false;
            if (c.mesh == null) c.mesh = new ChunkMesh();
            c.mesh.upload(m);
        }

        // 6. Unload far chunks
        if (chunks.size() > offsets.length + 64) {
            var it = chunks.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<Chunk> e = it.next();
                Chunk c = e.getValue();
                int dx = c.cx - pcx, dz = c.cz - pcz;
                if (dx * dx + dz * dz > unloadR * unloadR) {
                    unload(c);
                    it.remove();
                }
            }
        }
    }

    private void unload(Chunk c) {
        if (c.touched) storage.save(c);
        if (c.mesh != null) {
            c.mesh.delete();
            c.mesh = null;
        }
    }

    private void submitMesh(Chunk c, double priority) {
        ChunkMesher.Job job = new ChunkMesher.Job(c.cx, c.cz);
        int height = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                height = Math.max(height, chunks.get(Chunk.key(c.cx + dx, c.cz + dz)).maxY);
        job.height = height;
        byte[] region = job.region;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                byte[] src = chunks.get(Chunk.key(c.cx + dx, c.cz + dz)).blocks;
                int ox = (dx + 1) * 16, oz = (dz + 1) * 16;
                for (int y = 0; y < height; y++) {
                    int srcBase = y << 8;
                    int dstBase = y * ChunkMesher.AREA + oz * ChunkMesher.W + ox;
                    for (int z = 0; z < 16; z++)
                        System.arraycopy(src, srcBase + (z << 4), region, dstBase + z * ChunkMesher.W, 16);
                }
            }
        }
        System.arraycopy(c.grassColor, 0, job.grass, 0, 256);
        System.arraycopy(c.foliageColor, 0, job.foliage, 0, 256);
        c.needsMesh = false;
        c.urgentMesh = false;
        c.meshInFlight = true;
        meshJobsInFlight++;
        workers.execute(new Task(priority, () -> meshed.add(MESHERS.get().build(job))));
    }

    private Chunk loadOrGenerate(int cx, int cz) {
        byte[] data = storage.load(cx, cz);
        if (data != null) {
            byte[] blocks = new byte[Chunk.VOLUME];
            System.arraycopy(data, 1, blocks, 0, Chunk.VOLUME);
            Chunk c = new Chunk(cx, cz, blocks);
            generator.computeBiomeData(c);
            c.state = data[0];
            c.recomputeMaxY();
            return c;
        }
        Chunk c = new Chunk(cx, cz);
        generator.generate(c);
        return c;
    }

    /** Generates and decorates the chunks around (cx, cz) on the calling thread, without meshing. */
    public void loadAreaBlocking(int cx, int cz, int radius) {
        for (int dx = -radius - 1; dx <= radius + 1; dx++)
            for (int dz = -radius - 1; dz <= radius + 1; dz++)
                chunks.computeIfAbsent(Chunk.key(cx + dx, cz + dz), k -> loadOrGenerate((int) (k >> 32), (int) k));
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk c = chunks.get(Chunk.key(cx + dx, cz + dz));
                if (c.state == Chunk.STATE_TERRAIN) decorator.decorate(c);
            }
    }

    /** Saves every modified chunk; called on exit. */
    public void saveAll() {
        for (Chunk c : chunks.values()) {
            if (c.touched) {
                storage.save(c);
                c.touched = false;
            }
        }
    }

    public void shutdown() {
        workers.shutdownNow();
        for (Chunk c : chunks.values()) if (c.mesh != null) c.mesh.delete();
        storage.flush();
    }
}
