package mc.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.render.ChunkMesh;
import mc.render.ChunkMesher;
import mc.render.MeshData;
import mc.world.gen.Biome;
import mc.world.gen.Decorator;
import mc.world.gen.TerrainGenerator;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns all loaded chunks. Terrain generation and meshing run on a worker pool; decoration, block edits and
 * GL uploads happen on the main thread.
 */
public final class World implements Shapes.Getter {
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
    /** Total ticks simulated (unlike time, never changed by commands). */
    public long tickCount;

    /** Receives sounds, particles and block-break effects for the client. */
    public interface Listener {
        void playSound(String name, double x, double y, double z, float volume, float pitch);
        void addParticle(String type, double x, double y, double z);
        void blockBroken(int x, int y, int z, Block block, int meta);
    }

    public Listener listener;
    private Player player;
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pendingEntities = new ArrayList<>();
    public final Long2ObjectOpenHashMap<BlockEntity> blockEntities = new Long2ObjectOpenHashMap<>();
    private final PriorityQueue<long[]> scheduled = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1]));
    private final Long2LongOpenHashMap scheduledSet = new Long2LongOpenHashMap();
    private final Random random = new Random();
    private final MobSpawner spawner = new MobSpawner();
    /** 0 at night, 1 during the day (sky light multiplier used by spawning and burning). */
    public float dayFactor = 1;

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
        return c.blocks[Chunk.index(x & 15, y, z & 15)] & 255;
    }

    public int getMeta(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return 0;
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null) return 0;
        return c.meta[Chunk.index(x & 15, y, z & 15)];
    }

    /** Sky light level 0-15 (from the last mesh build; 15 above the terrain, 0 if unknown). */
    public int getSkyLight(int x, int y, int z) {
        if (y >= Chunk.HEIGHT) return 15;
        if (y < 0) return 0;
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null || c.light == null) return y > 64 ? 15 : 0;
        if (y >= c.lightHeight) return 15;
        return (c.light[Chunk.index(x & 15, y, z & 15)] >> 4) & 15;
    }

    public int getBlockLight(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return 0;
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c == null || c.light == null || y >= c.lightHeight) return 0;
        return c.light[Chunk.index(x & 15, y, z & 15)] & 15;
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

    public void setBlockGen(int x, int y, int z, int id, int meta) {
        setBlockGen(x, y, z, id);
        Chunk c = chunks.get(Chunk.key(x >> 4, z >> 4));
        if (c != null && y >= 0 && y < Chunk.HEIGHT) c.meta[Chunk.index(x & 15, y, z & 15)] = (byte) meta;
    }

    public boolean setBlock(int x, int y, int z, int id) {
        return setBlock(x, y, z, id, 0, true);
    }

    /**
     * Sets a block and its metadata, schedules remeshing of every chunk whose lighting may change and,
     * if requested, runs neighbour updates (liquids flow, unsupported plants pop off, sand falls).
     */
    public boolean setBlock(int x, int y, int z, int id, int meta, boolean notify) {
        if (y < 0 || y >= Chunk.HEIGHT) return false;
        int cx = x >> 4, cz = z >> 4;
        Chunk c = chunks.get(Chunk.key(cx, cz));
        if (c == null) return false;
        int old = c.blocks[Chunk.index(x & 15, y, z & 15)] & 255;
        c.set(x & 15, y, z & 15, id, meta);
        c.touched = true;
        if (old != id) {
            long key = posKey(x, y, z);
            if (old == Block.CHEST.id || ((old == Block.FURNACE.id || old == Block.LIT_FURNACE.id) && id != Block.FURNACE.id && id != Block.LIT_FURNACE.id)) {
                BlockEntity be = blockEntities.remove(key);
                if (be != null) for (ItemStack s : be.slots) if (!ItemStack.isEmpty(s)) spawnItem(x + 0.5, y + 0.5, z + 0.5, s);
            }
        }
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
        if (notify) {
            notifyNeighbors(x, y, z);
        }
        return true;
    }

    private static final int[][] NEIGHBORS = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private void notifyNeighbors(int x, int y, int z) {
        for (int[] d : NEIGHBORS) {
            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
            int nid = getBlock(nx, ny, nz);
            if (nid == 0) continue;
            Block b = Block.get(nid);
            if (b.isLiquid()) scheduleTick(nx, ny, nz, Liquids.delay(nid));
            else if (d[1] >= 0 || d[0] != 0 || d[2] != 0) neighborChanged(nx, ny, nz, b);
        }
    }

    /** Checks whether a block can still exist after a neighbour changed. */
    private void neighborChanged(int x, int y, int z, Block b) {
        Block below = Block.get(getBlock(x, y - 1, z));
        boolean ok = true;
        int meta = getMeta(x, y, z);
        if (b == Block.TORCH) {
            if (meta >= 1 && meta <= 4) ok = Block.get(getBlock(x + Shapes.DX[meta - 1], y, z + Shapes.DZ[meta - 1])).opaque;
            else ok = below.opaque;
        }
        else if (b == Block.LADDER) ok = Block.get(getBlock(x + Shapes.DX[meta & 3], y, z + Shapes.DZ[meta & 3])).opaque;
        else if (b.shape == Block.Shape.DOOR) {
            if ((meta & 8) != 0) ok = getBlock(x, y - 1, z) == b.id;
            else ok = getBlock(x, y + 1, z) == b.id && below.opaque;
        }
        else if (b == Block.BED) {
            int f = meta & 3, dir = (meta & 4) != 0 ? Shapes.opposite(f) : f;
            ok = getBlock(x + Shapes.DX[dir], y, z + Shapes.DZ[dir]) == b.id;
        }
        else if (b.shape == Block.Shape.CARPET) ok = below != Block.AIR && !below.isLiquid();
        else if (b.shape == Block.Shape.SNOW_LAYER) ok = below.opaque;
        else if (b == Block.WHEAT) ok = below == Block.FARMLAND;
        else if (b == Block.SUGAR_CANE) ok = below == Block.SUGAR_CANE || below == Block.GRASS || below == Block.DIRT || below == Block.SAND;
        else if (b == Block.CACTUS) ok = below == Block.CACTUS || below == Block.SAND;
        else if (b == Block.DEAD_BUSH) ok = below == Block.SAND || below == Block.TERRACOTTA || below == Block.DIRT;
        else if (b.model == Block.Model.CROSS) ok = below == Block.GRASS || below == Block.DIRT || below == Block.COARSE_DIRT || below == Block.SNOWY_GRASS || below == Block.FARMLAND;
        else if (b == Block.SAND || b == Block.GRAVEL) { checkFalling(x, y, z); return; }
        if (!ok) breakBlock(x, y, z, null, true);
    }

    /** Makes sand and gravel with nothing beneath them fall. */
    public void checkFalling(int x, int y, int z) {
        int id = getBlock(x, y, z);
        if (id != Block.SAND.id && id != Block.GRAVEL.id) return;
        Block below = Block.get(getBlock(x, y - 1, z));
        if (y > 0 && (below == Block.AIR || below.isLiquid() || below.model == Block.Model.CROSS)) {
            setBlock(x, y, z, 0);
            FallingBlockEntity f = new FallingBlockEntity(id);
            f.setPos(x + 0.5, y, z + 0.5);
            addEntity(f);
        }
    }

    /** Breaks a block, dropping its items (unless tool is irrelevant: pass drop=false for none). */
    public void breakBlock(int x, int y, int z, ItemStack tool, boolean drop) {
        int id = getBlock(x, y, z);
        if (id == 0) return;
        Block b = Block.get(id);
        int meta = getMeta(x, y, z);
        if (listener != null) listener.blockBroken(x, y, z, b, meta);
        // Two-block structures lose their other half without a second drop
        int ox = x, oy = y, oz = z;
        if (b.shape == Block.Shape.DOOR) oy += (meta & 8) != 0 ? -1 : 1;
        else if (b == Block.BED) {
            int f = meta & 3, dir = (meta & 4) != 0 ? Shapes.opposite(f) : f;
            ox += Shapes.DX[dir]; oz += Shapes.DZ[dir];
        }
        if ((ox != x || oy != y || oz != z) && getBlock(ox, oy, oz) == id) setBlock(ox, oy, oz, 0, 0, false);
        setBlock(x, y, z, 0);
        if (drop) for (ItemStack s : Drops.of(b, meta, tool)) spawnItem(x + 0.5, y + 0.5, z + 0.5, s);
    }

    public void spawnItem(double x, double y, double z, ItemStack stack) {
        if (ItemStack.isEmpty(stack)) return;
        ItemEntity e = new ItemEntity(stack);
        e.setPos(x + (random.nextDouble() - 0.5) * 0.3, y - 0.125, z + (random.nextDouble() - 0.5) * 0.3);
        e.motionX = (random.nextDouble() - 0.5) * 0.2;
        e.motionY = 0.2;
        e.motionZ = (random.nextDouble() - 0.5) * 0.2;
        addEntity(e);
    }

    public static long posKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public void scheduleTick(int x, int y, int z, int delay) {
        long key = posKey(x, y, z);
        long due = tickCount + delay;
        if (scheduledSet.containsKey(key) && scheduledSet.get(key) <= due) return;
        scheduledSet.put(key, due);
        scheduled.add(new long[]{key, due, x, y, z});
    }

    // ------------------------------------------------------------------ entities & ticking

    public void setPlayer(Player p) {
        player = p;
        p.world = this;
    }

    public Player player() { return player; }

    public List<Entity> entities() { return entities; }

    public void addEntity(Entity e) {
        e.world = this;
        pendingEntities.add(e);
    }

    public void playSound(String name, double x, double y, double z, float volume, float pitch) {
        if (listener != null) listener.playSound(name, x, y, z, volume, pitch);
    }

    public void addParticle(String type, double x, double y, double z) {
        if (listener != null) listener.addParticle(type, x, y, z);
    }

    public boolean isDaytime() {
        long t = time % 24000;
        return t < 12500 || t > 23500;
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return blockEntities.get(posKey(x, y, z));
    }

    public BlockEntity getOrCreateBlockEntity(int x, int y, int z) {
        long key = posKey(x, y, z);
        BlockEntity be = blockEntities.get(key);
        int id = getBlock(x, y, z);
        if (be == null) {
            if (id == Block.CHEST.id) be = new BlockEntity.Chest(x, y, z);
            else if (id == Block.FURNACE.id || id == Block.LIT_FURNACE.id) be = new BlockEntity.Furnace(x, y, z);
            if (be != null) blockEntities.put(key, be);
        }
        return be;
    }

    /** One game tick (20 per second): scheduled block updates, random ticks, block entities, entities, spawning. */
    public void tick() {
        time++;
        tickCount++;
        int processed = 0;
        while (!scheduled.isEmpty() && scheduled.peek()[1] <= tickCount && processed < 2000) {
            long[] e = scheduled.poll();
            if (scheduledSet.get(e[0]) != e[1]) continue;
            scheduledSet.remove(e[0]);
            int x = (int) e[2], y = (int) e[3], z = (int) e[4];
            if (!isLoaded(x, z)) continue;
            Liquids.tick(this, x, y, z);
            processed++;
        }
        if (player != null) randomTicks();
        for (BlockEntity be : new ArrayList<>(blockEntities.values())) {
            if (isLoaded(be.x, be.z)) be.tick(this);
        }
        entities.addAll(pendingEntities);
        pendingEntities.clear();
        for (Iterator<Entity> it = entities.iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if (!e.removed) {
                if (!isLoaded((int) Math.floor(e.x), (int) Math.floor(e.z))) { e.remove(); }
                else if (e instanceof Mob m && m.type.hostile && player != null) {
                    double d = m.distanceSq(player.x, player.y, player.z);
                    if (d > 128 * 128 || (d > 40 * 40 && random.nextInt(800) == 0)) m.remove();
                }
            }
            if (!e.removed) e.tick();
            if (e.removed) it.remove();
        }
        entities.addAll(pendingEntities);
        pendingEntities.clear();
        if (player != null) spawner.tick(this, player, random);
    }

    private void randomTicks() {
        int pcx = (int) Math.floor(player.x) >> 4, pcz = (int) Math.floor(player.z) >> 4;
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -6; dz <= 6; dz++) {
                Chunk c = chunks.get(Chunk.key(pcx + dx, pcz + dz));
                if (c == null || c.state != Chunk.STATE_DECORATED || c.light == null) continue;
                int sections = (c.maxY + 15) / 16;
                for (int s = 0; s < sections; s++)
                    for (int k = 0; k < 3; k++) {
                        int x = c.cx * 16 + random.nextInt(16), y = s * 16 + random.nextInt(16), z = c.cz * 16 + random.nextInt(16);
                        int id = c.blocks[Chunk.index(x & 15, y, z & 15)] & 255;
                        if (id != 0) randomTick(x, y, z, Block.get(id));
                    }
            }
    }

    private int lightAbove(int x, int y, int z) {
        return Math.max(Math.round(getSkyLight(x, y + 1, z) * dayFactor), getBlockLight(x, y + 1, z));
    }

    private void randomTick(int x, int y, int z, Block b) {
        if (b == Block.GRASS) {
            Block above = Block.get(getBlock(x, y + 1, z));
            if (above.opaque || above.isLiquid()) { setBlock(x, y, z, Block.DIRT.id); return; }
            for (int i = 0; i < 4; i++) {
                int tx = x + random.nextInt(3) - 1, ty = y + random.nextInt(5) - 3, tz = z + random.nextInt(3) - 1;
                if (getBlock(tx, ty, tz) == Block.DIRT.id && !Block.get(getBlock(tx, ty + 1, tz)).opaque
                        && getSkyLight(tx, ty + 1, tz) >= 4) setBlock(tx, ty, tz, Block.GRASS.id);
            }
        } else if (b == Block.WHEAT) {
            int age = getMeta(x, y, z);
            boolean wet = getMeta(x, y - 1, z) > 0;
            if (age < 7 && lightAbove(x, y - 1, z) >= 9 && random.nextInt(wet ? 3 : 7) == 0) setBlock(x, y, z, b.id, age + 1, false);
        } else if (b == Block.FARMLAND) {
            boolean water = false;
            for (int dx = -4; dx <= 4 && !water; dx++)
                for (int dz = -4; dz <= 4 && !water; dz++)
                    for (int dy = 0; dy <= 1; dy++) if (getBlock(x + dx, y + dy, z + dz) == Block.WATER.id) { water = true; break; }
            int meta = getMeta(x, y, z);
            if (water && meta == 0) setBlock(x, y, z, b.id, 1, false);
            else if (!water && meta > 0) setBlock(x, y, z, b.id, 0, false);
            else if (!water && getBlock(x, y + 1, z) == 0 && random.nextInt(4) == 0) setBlock(x, y, z, Block.DIRT.id);
        } else if (b == Block.SAPLING) {
            if (lightAbove(x, y - 1, z) >= 9 && random.nextInt(7) == 0) growTree(x, y, z);
        } else if (b == Block.SUGAR_CANE || b == Block.CACTUS) {
            if (getBlock(x, y + 1, z) != 0) return;
            int h = 1;
            while (getBlock(x, y - h, z) == b.id) h++;
            if (h < 3 && random.nextInt(8) == 0) setBlock(x, y + 1, z, b.id);
        } else if (b == Block.OAK_LEAVES || b == Block.BIRCH_LEAVES || b == Block.SPRUCE_LEAVES) {
            if (getMeta(x, y, z) == 0 && !logNearby(x, y, z)) breakBlock(x, y, z, null, true);
        } else if (b == Block.ICE) {
            if (getBlockLight(x, y + 1, z) > 11) setBlock(x, y, z, Block.WATER.id);
        }
    }

    private boolean logNearby(int x, int y, int z) {
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -4; dy <= 4; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 5) continue;
                    int id = getBlock(x + dx, y + dy, z + dz);
                    if (id == Block.OAK_LOG.id || id == Block.BIRCH_LOG.id || id == Block.SPRUCE_LOG.id) return true;
                }
        return !isLoaded(x - 5, z - 5) || !isLoaded(x + 5, z + 5);
    }

    public void growTree(int x, int y, int z) {
        setBlock(x, y, z, 0, 0, false);
        if (!decorator.growTree(random, x, y, z, biomeAt(x, z))) setBlock(x, y, z, Block.SAPLING.id, 0, false);
        else {
            for (int dx = -6; dx <= 6; dx += 6)
                for (int dz = -6; dz <= 6; dz += 6) {
                    Chunk c = chunks.get(Chunk.key((x + dx) >> 4, (z + dz) >> 4));
                    if (c != null) { c.needsMesh = true; c.urgentMesh = true; }
                }
        }
    }

    /** Minecraft-style explosion: rays from the centre destroy blocks; nearby entities take damage. */
    public void explode(double cx, double cy, double cz, float power, Entity source) {
        playSound("explode", cx, cy, cz, 4, 0.8f + random.nextFloat() * 0.3f);
        for (int i = 0; i < 12; i++) addParticle("explosion", cx + random.nextGaussian() * power * 0.4, cy + random.nextGaussian() * power * 0.4, cz + random.nextGaussian() * power * 0.4);
        it.unimi.dsi.fastutil.longs.LongOpenHashSet destroy = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++)
                for (int k = 0; k < 16; k++) {
                    if (i != 0 && i != 15 && j != 0 && j != 15 && k != 0 && k != 15) continue;
                    double dx = i / 15.0 * 2 - 1, dy = j / 15.0 * 2 - 1, dz = k / 15.0 * 2 - 1;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len; dy /= len; dz /= len;
                    double strength = power * (0.7 + random.nextDouble() * 0.6);
                    double px = cx, py = cy, pz = cz;
                    while (strength > 0) {
                        int bx = (int) Math.floor(px), by = (int) Math.floor(py), bz = (int) Math.floor(pz);
                        int id = getBlock(bx, by, bz);
                        if (id != 0) {
                            strength -= (resistance(Block.get(id)) + 0.3) * 0.3;
                            if (strength > 0 && !Block.get(id).isLiquid()) {
                                long key = posKey(bx, by, bz);
                                if (destroy.add(key)) list.add(new int[]{bx, by, bz});
                            }
                        }
                        px += dx * 0.3; py += dy * 0.3; pz += dz * 0.3;
                        strength -= 0.225;
                    }
                }
        for (int[] p : list) {
            int id = getBlock(p[0], p[1], p[2]);
            if (id == Block.TNT.id) {
                setBlock(p[0], p[1], p[2], 0);
                TntEntity t = new TntEntity(10 + random.nextInt(20));
                t.setPos(p[0] + 0.5, p[1], p[2] + 0.5);
                addEntity(t);
                continue;
            }
            boolean drop = random.nextFloat() < 1f / power;
            Block b = Block.get(id);
            int meta = getMeta(p[0], p[1], p[2]);
            setBlock(p[0], p[1], p[2], 0, 0, false);
            if (drop) for (ItemStack s : Drops.of(b, meta, new ItemStack(Item.DIAMOND_PICKAXE, 1))) spawnItem(p[0] + 0.5, p[1] + 0.5, p[2] + 0.5, s);
        }
        for (int[] p : list) notifyNeighbors(p[0], p[1], p[2]);
        // Damage and push entities
        double radius = power * 2;
        List<Entity> all = new ArrayList<>(entities);
        if (player != null && !all.contains(player)) all.add(player);
        for (Entity e : all) {
            if (e == source) continue;
            double dx = e.x - cx, dy = e.eyeY() - cy, dz = e.z - cz;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > radius || d < 1e-4) continue;
            double impact = 1 - d / radius;
            if (e instanceof LivingEntity le) {
                le.damage(DamageSource.EXPLOSION, (float) ((impact * impact + impact) / 2 * 7 * radius + 1), null);
            } else if (e instanceof ItemEntity) {
                e.remove();
                continue;
            }
            e.motionX += dx / d * impact;
            e.motionY += dy / d * impact;
            e.motionZ += dz / d * impact;
        }
    }

    private static float resistance(Block b) {
        if (b == Block.BEDROCK) return 3600000;
        if (b == Block.OBSIDIAN) return 1200;
        if (b.isLiquid()) return 100;
        if (b.hardness < 0) return 3600000;
        return b.hardness * 4;
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
            c.light = m.light;
            c.lightHeight = m.lightHeight;
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
        byte[] region = job.region, metaRegion = job.meta;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk n = chunks.get(Chunk.key(c.cx + dx, c.cz + dz));
                byte[] src = n.blocks, srcMeta = n.meta;
                int ox = (dx + 1) * 16, oz = (dz + 1) * 16;
                for (int y = 0; y < height; y++) {
                    int srcBase = y << 8;
                    int dstBase = y * ChunkMesher.AREA + oz * ChunkMesher.W + ox;
                    for (int z = 0; z < 16; z++) {
                        System.arraycopy(src, srcBase + (z << 4), region, dstBase + z * ChunkMesher.W, 16);
                        System.arraycopy(srcMeta, srcBase + (z << 4), metaRegion, dstBase + z * ChunkMesher.W, 16);
                    }
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
            byte[] blocks = new byte[Chunk.VOLUME], meta = new byte[Chunk.VOLUME];
            System.arraycopy(data, 1, blocks, 0, Chunk.VOLUME);
            System.arraycopy(data, 1 + Chunk.VOLUME, meta, 0, Chunk.VOLUME);
            Chunk c = new Chunk(cx, cz, blocks, meta);
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
