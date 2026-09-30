package mc.world.gen;

import mc.entity.Mob;
import mc.entity.MobType;
import mc.item.Item;
import mc.item.ItemStack;
import mc.world.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generated structures: villages (well, gravel roads, houses, farms, a smithy, lamp posts, villagers and an iron
 * golem), underground dungeons with a monster spawner and loot chests, and emerald ore in the mountains.
 * <p>
 * A village is built in one go when the chunk holding its centre is decorated; the world makes sure the chunks
 * around it exist first (see {@link #decorationRadius}).
 */
public final class Structures {
    /** One possible village per square of this many chunks. */
    public static final int VILLAGE_REGION = 20;
    public static final int VILLAGE_RADIUS = 3;

    private final World world;
    private final long seed;
    private final TerrainGenerator gen;

    public Structures(World world, long seed, TerrainGenerator gen) {
        this.world = world;
        this.seed = seed;
        this.gen = gen;
    }

    // ------------------------------------------------------------------ placement rules

    /** Centre chunk of the village in a region, or null if that region has none. */
    public int[] villageIn(int rx, int rz) {
        Random r = new Random(seed ^ (rx * 341873128712L + rz * 132897987541L) ^ 0x5DEECE66DL);
        int cx = rx * VILLAGE_REGION + 4 + r.nextInt(VILLAGE_REGION - 8);
        int cz = rz * VILLAGE_REGION + 4 + r.nextInt(VILLAGE_REGION - 8);
        int x = cx * 16 + 8, z = cz * 16 + 8;
        Biome b = biomeAt(x, z);
        if (b != Biome.PLAINS && b != Biome.DESERT && b != Biome.TAIGA) return null;
        // Needs fairly flat land above sea level
        int min = 999, max = -999;
        for (int dx = -24; dx <= 24; dx += 8)
            for (int dz = -24; dz <= 24; dz += 8) {
                int h = gen.estimateHeight(x + dx, z + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        if (min < TerrainGenerator.SEA_LEVEL + 1 || max - min > 14) return null;
        return new int[]{cx, cz};
    }

    private Biome biomeAt(int x, int z) {
        double[] p = new double[4];
        gen.columnParams(x, z, p);
        return gen.biomeAt(gen.temperature01(x, z), gen.humidity01(x, z), p[0], p[2], p[3]);
    }

    public boolean isVillageCenter(int cx, int cz) {
        int[] v = villageIn(Math.floorDiv(cx, VILLAGE_REGION), Math.floorDiv(cz, VILLAGE_REGION));
        return v != null && v[0] == cx && v[1] == cz;
    }

    /** How many chunks around this one must exist before it can be decorated. */
    public int decorationRadius(int cx, int cz) {
        return isVillageCenter(cx, cz) ? VILLAGE_RADIUS : 1;
    }

    /** Nearest village centre (block coordinates) to a position, searching nearby regions; null if none. */
    public int[] nearestVillage(int x, int z) {
        int rx = Math.floorDiv(x >> 4, VILLAGE_REGION), rz = Math.floorDiv(z >> 4, VILLAGE_REGION);
        int[] best = null;
        long bd = Long.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                int[] v = villageIn(rx + dx, rz + dz);
                if (v == null) continue;
                long vx = v[0] * 16L + 8, vz = v[1] * 16L + 8;
                long d = (vx - x) * (vx - x) + (vz - z) * (vz - z);
                if (d < bd) { bd = d; best = new int[]{(int) vx, (int) vz}; }
            }
        return best;
    }

    public void decorate(Chunk chunk, Random rand) {
        Biome biome = Biome.VALUES[chunk.biome[8 * 16 + 8]];
        if (biome == Biome.MOUNTAINS || biome == Biome.SNOWY_PEAKS) emeralds(chunk, rand);
        if (rand.nextInt(5) == 0) dungeon(chunk, rand);
        if (isVillageCenter(chunk.cx, chunk.cz)) village(chunk.cx * 16 + 8, chunk.cz * 16 + 8, new Random(seed ^ chunk.key()), biomeAt(chunk.cx * 16 + 8, chunk.cz * 16 + 8));
    }

    // ------------------------------------------------------------------ helpers

    private int get(int x, int y, int z) { return world.getBlock(x, y, z); }

    private void set(int x, int y, int z, Block b) { world.setBlockGen(x, y, z, b.id); }

    private void set(int x, int y, int z, Block b, int meta) { world.setBlockGen(x, y, z, b.id, meta); }

    /** Highest ground block (ignores plants, leaves and logs); water surfaces count, flagged by the caller. */
    private int surface(int x, int z) {
        for (int y = 200; y > 1; y--) {
            int id = get(x, y, z);
            if (id == 0) continue;
            Block b = Block.get(id);
            if (b.model == Block.Model.CROSS || b == Block.OAK_LEAVES || b == Block.BIRCH_LEAVES || b == Block.SPRUCE_LEAVES
                    || b == Block.OAK_LOG || b == Block.BIRCH_LOG || b == Block.SPRUCE_LOG || b == Block.SNOW_LAYER) continue;
            return y;
        }
        return -1;
    }

    private void emeralds(Chunk c, Random rand) {
        for (int i = 0; i < 3 + rand.nextInt(6); i++) {
            int x = c.cx * 16 + rand.nextInt(16), y = 4 + rand.nextInt(28), z = c.cz * 16 + rand.nextInt(16);
            if (get(x, y, z) == Block.STONE.id) set(x, y, z, Block.EMERALD_ORE);
        }
    }

    // ------------------------------------------------------------------ loot

    private record Loot(Item item, int min, int max, int weight) { }

    private static final Loot[] DUNGEON_LOOT = {
            new Loot(Item.BREAD, 1, 3, 20), new Loot(Item.WHEAT, 1, 4, 20), new Loot(Item.GUNPOWDER, 1, 4, 15), new Loot(Item.STRING, 1, 4, 15),
            new Loot(Item.BUCKET, 1, 1, 10), new Loot(Item.IRON_INGOT, 1, 4, 10), new Loot(Item.GOLD_INGOT, 1, 3, 5), new Loot(Item.APPLE, 1, 3, 10),
            new Loot(Item.BONE, 1, 6, 10), new Loot(Item.ROTTEN_FLESH, 1, 6, 10), new Loot(Item.DIAMOND, 1, 2, 2), new Loot(Item.EMERALD, 1, 3, 4),
            new Loot(Item.ENDER_PEARL, 1, 1, 3), new Loot(Item.armor(2, 1), 1, 1, 2), new Loot(Item.COAL, 3, 8, 10)};
    private static final Loot[] SMITH_LOOT = {
            new Loot(Item.IRON_INGOT, 1, 5, 10), new Loot(Item.GOLD_INGOT, 1, 3, 5), new Loot(Item.BREAD, 1, 3, 15), new Loot(Item.APPLE, 1, 3, 15),
            new Loot(Item.IRON_PICKAXE, 1, 1, 5), new Loot(Item.IRON_SWORD, 1, 1, 5), new Loot(Item.armor(2, 0), 1, 1, 5), new Loot(Item.armor(2, 1), 1, 1, 5),
            new Loot(Item.armor(2, 2), 1, 1, 5), new Loot(Item.armor(2, 3), 1, 1, 5), new Loot(Item.of(Block.OBSIDIAN), 3, 7, 5),
            new Loot(Item.of(Block.SAPLING), 3, 7, 5), new Loot(Item.DIAMOND, 1, 3, 3), new Loot(Item.EMERALD, 1, 2, 5)};
    private static final Loot[] HOUSE_LOOT = {
            new Loot(Item.BREAD, 1, 4, 15), new Loot(Item.WHEAT, 1, 6, 15), new Loot(Item.SEEDS, 2, 6, 10), new Loot(Item.APPLE, 1, 3, 10),
            new Loot(Item.CARROT, 1, 4, 10), new Loot(Item.POTATO, 1, 4, 10), new Loot(Item.EMERALD, 1, 1, 3), new Loot(Item.PAPER, 1, 4, 5),
            new Loot(Item.STICK, 2, 6, 5), new Loot(Item.of(Block.TORCH), 1, 6, 5)};

    private void fillChest(int x, int y, int z, Loot[] table, int rolls, Random rand) {
        if (!(world.getOrCreateBlockEntity(x, y, z) instanceof BlockEntity.Chest chest)) return;
        int total = 0;
        for (Loot l : table) total += l.weight;
        for (int i = 0; i < rolls; i++) {
            int pick = rand.nextInt(total);
            for (Loot l : table) {
                pick -= l.weight;
                if (pick < 0) {
                    int slot = rand.nextInt(27);
                    if (chest.slots[slot] == null) chest.slots[slot] = new ItemStack(l.item, l.min + rand.nextInt(l.max - l.min + 1));
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ dungeons

    private void dungeon(Chunk c, Random rand) {
        int cx = c.cx * 16 + 4 + rand.nextInt(8), cz = c.cz * 16 + 4 + rand.nextInt(8), y = 12 + rand.nextInt(40);
        int rx = 2 + rand.nextInt(2), rz = 2 + rand.nextInt(2);
        // Only fully underground, away from water and lava; walls may have a few openings into caves
        int openings = 0;
        for (int x = cx - rx - 1; x <= cx + rx + 1; x++)
            for (int z = cz - rz - 1; z <= cz + rz + 1; z++)
                for (int yy = y - 1; yy <= y + 4; yy++) {
                    Block b = Block.get(get(x, yy, z));
                    if (b.isLiquid()) return;
                    boolean floorOrCeiling = yy == y - 1 || yy == y + 4;
                    if (floorOrCeiling && !b.solid) return;
                    boolean wall = x == cx - rx - 1 || x == cx + rx + 1 || z == cz - rz - 1 || z == cz + rz + 1;
                    if (wall && yy == y && b == Block.AIR && get(x, y + 1, z) == 0) openings++;
                }
        if (openings > 5) return;
        for (int x = cx - rx - 1; x <= cx + rx + 1; x++)
            for (int z = cz - rz - 1; z <= cz + rz + 1; z++)
                for (int yy = y - 1; yy <= y + 4; yy++) {
                    boolean edge = x == cx - rx - 1 || x == cx + rx + 1 || z == cz - rz - 1 || z == cz + rz + 1 || yy == y - 1 || yy == y + 4;
                    if (!edge) set(x, yy, z, Block.AIR);
                    else if (yy == y - 1 || get(x, yy, z) != 0 || yy == y + 4)
                        set(x, yy, z, yy == y - 1 && rand.nextInt(4) != 0 ? Block.MOSSY_COBBLESTONE : rand.nextInt(3) == 0 ? Block.MOSSY_COBBLESTONE : Block.COBBLESTONE);
                }
        set(cx, y, cz, Block.SPAWNER);
        if (world.getOrCreateBlockEntity(cx, y, cz) instanceof BlockEntity.Spawner sp) {
            int r = rand.nextInt(4);
            sp.mob = r < 2 ? "ZOMBIE" : r == 2 ? "SKELETON" : "SPIDER";
        }
        // Chests against the walls
        for (int n = 0, tries = 0; n < 2 && tries < 12; tries++) {
            int x = cx + rand.nextInt(rx * 2 + 1) - rx, z = cz + rand.nextInt(rz * 2 + 1) - rz;
            boolean againstWall = Math.abs(x - cx) == rx || Math.abs(z - cz) == rz;
            if (!againstWall || get(x, y, z) != 0) continue;
            set(x, y, z, Block.CHEST, rand.nextInt(4));
            fillChest(x, y, z, DUNGEON_LOOT, 4 + rand.nextInt(5), rand);
            n++;
        }
    }

    // ------------------------------------------------------------------ villages

    /** Building materials for the local biome. */
    private record Style(Block planks, Block log, Block stairs, Block slab, Block foundation, Block path, Block door) { }

    private static Style style(Biome b) {
        return switch (b) {
            case DESERT -> new Style(Block.SANDSTONE, Block.SANDSTONE, Block.SANDSTONE_STAIRS, Block.SANDSTONE_SLAB, Block.SANDSTONE, Block.SANDSTONE, Block.OAK_DOOR);
            case TAIGA -> new Style(Block.SPRUCE_PLANKS, Block.SPRUCE_LOG, Block.SPRUCE_STAIRS, Block.SPRUCE_SLAB, Block.COBBLESTONE, Block.GRAVEL, Block.OAK_DOOR);
            default -> new Style(Block.PLANKS, Block.OAK_LOG, Block.OAK_STAIRS, Block.OAK_SLAB, Block.COBBLESTONE, Block.GRAVEL, Block.OAK_DOOR);
        };
    }

    /** Places blocks in a building's local frame: x along the road, z away from it (the front is z = 0). */
    private final class Placer {
        final int ox, oy, oz, f;
        final int xdx, xdz, zdx, zdz;

        /** f = world facing from the building towards the road. */
        Placer(int ox, int oy, int oz, int f) {
            this.ox = ox; this.oy = oy; this.oz = oz; this.f = f;
            int back = Shapes.opposite(f), right = rotate(3);
            zdx = Shapes.DX[back]; zdz = Shapes.DZ[back];
            xdx = Shapes.DX[right]; xdz = Shapes.DZ[right];
        }

        /** Local facing (0 +z/back, 1 -x, 2 -z/front, 3 +x) to world facing. */
        int rotate(int local) { return (local + f - 2) & 3; }

        int wx(int x, int z) { return ox + x * xdx + z * zdx; }

        int wz(int x, int z) { return oz + x * xdz + z * zdz; }

        void set(int x, int y, int z, Block b) { world.setBlockGen(wx(x, z), oy + y, wz(x, z), b.id); }

        void set(int x, int y, int z, Block b, int meta) { world.setBlockGen(wx(x, z), oy + y, wz(x, z), b.id, meta); }

        /** Block with a horizontal facing given in local terms (keeps the upper meta bits). */
        void facing(int x, int y, int z, Block b, int localFacing, int extra) { set(x, y, z, b, rotate(localFacing) | extra); }

        int get(int x, int y, int z) { return world.getBlock(wx(x, z), oy + y, wz(x, z)); }

        /** Solid foundation under the floor and air above it, for a w x d footprint. */
        void prepare(int w, int d, int height, Block foundation) {
            for (int x = -1; x <= w; x++)
                for (int z = -1; z <= d; z++) {
                    boolean inside = x >= 0 && x < w && z >= 0 && z < d;
                    for (int y = 0; y <= height; y++) set(x, y, z, Block.AIR);
                    if (!inside) continue;
                    for (int y = -1; y >= -8; y--) {
                        Block b = Block.get(get(x, y, z));
                        if (b.solid && b.opaque && y < -1) break;
                        set(x, y, z, y == -1 ? foundation : Block.DIRT);
                    }
                }
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, b);
        }
    }

    private void village(int cx, int cz, Random rand, Biome biome) {
        Style st = style(biome);
        // The height estimate ignores some terrain detail: find dry, level ground near the planned centre
        int[] c = dryCenter(cx, cz);
        if (c == null) return;
        cx = c[0];
        cz = c[1];
        int cy = surface(cx, cz);
        List<int[]> houses = new ArrayList<>();
        well(cx, cy, cz, st);
        // Four roads out from the well with buildings on both sides
        for (int dir = 0; dir < 4; dir++) {
            int len = 18 + rand.nextInt(14);
            int dx = Shapes.DX[dir], dz = Shapes.DZ[dir];
            int px = Shapes.DX[(dir + 1) & 3], pz = Shapes.DZ[(dir + 1) & 3];
            int lastY = cy;
            int end = len;
            for (int s = 3; s <= len; s++) {
                int rx = cx + dx * s, rz = cz + dz * s;
                int y = surface(rx, rz);
                if (y < 0 || Math.abs(y - lastY) > 2) { end = s - 1; break; }
                lastY = y;
                for (int w = -1; w <= 1; w++) road(rx + px * w, rz + pz * w, st);
            }
            for (int s = 7; s + 4 <= end; s += 10 + rand.nextInt(3)) {
                for (int side = -1; side <= 1; side += 2) {
                    if (rand.nextInt(5) == 0) continue;
                    // The plot starts two blocks beside the road
                    int ax = cx + dx * s + px * side * 3, az = cz + dz * s + pz * side * 3;
                    int towardsRoad = Shapes.facingOf(-px * side, -pz * side);
                    int kind = rand.nextInt(10);
                    int[] placed = building(kind, ax, az, towardsRoad, st, rand);
                    if (placed != null) houses.add(placed);
                }
            }
        }
        // Lamp posts beside the well
        lamp(cx + 3, cz + 3, st);
        lamp(cx - 3, cz - 3, st);
        // Villagers and a golem
        String[] prof = {"farmer", "librarian", "priest", "smith", "butcher"};
        for (int[] h : houses) {
            int n = h[3] == 1 ? 2 : 1;
            for (int i = 0; i < n; i++) {
                Mob v = new Mob(MobType.VILLAGER);
                v.profession = h[4] >= 0 ? h[4] : rand.nextInt(prof.length);
                v.setPos(h[0] + 0.5, h[1], h[2] + 0.5);
                v.yaw = rand.nextFloat() * 360;
                world.addEntity(v);
            }
        }
        if (houses.size() >= 3) {
            Mob golem = new Mob(MobType.IRON_GOLEM);
            golem.setPos(cx + 0.5 + 3, cy + 1, cz + 0.5);
            world.addEntity(golem);
        }
    }

    private boolean dry(int x, int z) {
        int y = surface(x, z);
        return y >= TerrainGenerator.SEA_LEVEL && !Block.get(get(x, y, z)).isLiquid();
    }

    private int[] dryCenter(int cx, int cz) {
        for (int r = 0; r <= 20; r += 2)
            for (int dx = -r; dx <= r; dx += 2)
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = cx + dx, z = cz + dz;
                    if (!dry(x, z)) continue;
                    // The well needs a level 5x5 patch
                    int y = surface(x, z);
                    boolean ok = true;
                    for (int ox = -2; ox <= 2 && ok; ox++)
                        for (int oz = -2; oz <= 2 && ok; oz++) ok = dry(x + ox, z + oz) && Math.abs(surface(x + ox, z + oz) - y) <= 1;
                    if (ok) return new int[]{x, z};
                }
        return null;
    }

    private void road(int x, int z, Style st) {
        int y = surface(x, z);
        if (y < 0) return;
        Block top = Block.get(get(x, y, z));
        if (top.isLiquid()) set(x, y, z, st.planks);
        else if (top == Block.GRASS || top == Block.DIRT || top == Block.SAND || top == Block.SNOWY_GRASS || top == Block.COARSE_DIRT
                || top == Block.GRAVEL || top == Block.STONE) set(x, y, z, st.path);
        else return;
        for (int k = 1; k <= 2; k++) {
            Block above = Block.get(get(x, y + k, z));
            if (above.model == Block.Model.CROSS || above == Block.SNOW_LAYER) set(x, y + k, z, Block.AIR);
        }
    }

    private void well(int cx, int cy, int cz, Style st) {
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++) {
                for (int y = 1; y <= 5; y++) set(cx + x, cy + y, cz + z, Block.AIR);
                set(cx + x, cy, cz + z, st.foundation);
                for (int y = -1; y >= -6; y--) {
                    if (Block.get(get(cx + x, cy + y, cz + z)).opaque && y < -3) break;
                    set(cx + x, cy + y, cz + z, st.foundation);
                }
            }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                boolean rim = Math.abs(x) == 1 || Math.abs(z) == 1;
                if (x == 0 && z == 0) {
                    for (int y = cy - 3; y <= cy; y++) set(cx, y, cz, Block.WATER);
                } else if (rim) set(cx + x, cy + 1, cz + z, st.foundation);
            }
        // Water in the middle of the rim, posts and a roof
        set(cx, cy + 1, cz, Block.AIR);
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            set(cx + c[0], cy + 2, cz + c[1], Block.OAK_FENCE);
            set(cx + c[0], cy + 3, cz + c[1], Block.OAK_FENCE);
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(cx + x, cy + 4, cz + z, st.slab == Block.SANDSTONE_SLAB ? Block.SANDSTONE_SLAB : Block.COBBLESTONE_SLAB);
    }

    private void lamp(int x, int z, Style st) {
        int y = surface(x, z);
        if (y < 0 || Block.get(get(x, y, z)).isLiquid()) return;
        set(x, y + 1, z, Block.OAK_FENCE);
        set(x, y + 2, z, Block.OAK_FENCE);
        set(x, y + 3, z, Block.BLACK_WOOL);
        for (int d = 0; d < 4; d++) set(x + Shapes.DX[d], y + 3, z + Shapes.DZ[d], Block.TORCH, Shapes.opposite(d) + 1);
    }

    /** Average ground height under a footprint, or -1 if it is too uneven or wet. */
    private int groundLevel(Placer probe, int w, int d) {
        int sum = 0, n = 0, min = 999, max = -1;
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                int wx = probe.wx(x, z), wz = probe.wz(x, z);
                int y = surface(wx, wz);
                if (y < 0 || Block.get(get(wx, y, wz)).isLiquid()) return -1;
                Block b = Block.get(get(wx, y, wz));
                if (b == Block.GRAVEL || b == Block.SANDSTONE) return -1; // road or another building
                sum += y; n++;
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        if (max - min > 6) return -1;
        return Math.round((float) sum / n);
    }

    /**
     * Builds one plot. (ax, az) is the middle of the plot's front edge; facing points from the plot to the road.
     * Returns {x, y, z, big, profession} for villager placement, or null.
     */
    private int[] building(int kind, int ax, int az, int facing, Style st, Random rand) {
        int w, d;
        if (kind <= 3) { w = 5; d = 5; }
        else if (kind <= 5) { w = 7; d = 7; }
        else if (kind <= 7) { w = 9; d = 7; }
        else if (kind == 8) { w = 7; d = 6; }
        else { w = 1; d = 1; }
        // Origin at the front-left corner so the plot is centred on (ax, az)
        Placer probe = new Placer(0, 0, 0, facing);
        int ox = ax - probe.xdx * (w / 2), oz = az - probe.xdz * (w / 2);
        probe = new Placer(ox, 0, oz, facing);
        int gy = groundLevel(probe, w, d);
        if (gy < 0) return null;
        Placer p = new Placer(ox, gy, oz, facing);
        return switch (kind) {
            case 0, 1, 2, 3 -> smallHouse(p, st, rand);
            case 4, 5 -> bigHouse(p, st, rand);
            case 6, 7 -> farm(p, st, rand);
            case 8 -> smithy(p, st, rand);
            default -> { lamp(p.wx(0, 0), p.wz(0, 0), st); yield null; }
        };
    }

    /** Walls with log corners, a door in the middle of the front and glass windows. */
    private void walls(Placer p, int w, int d, int h, Style st) {
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edgeX = x == 0 || x == w - 1, edgeZ = z == 0 || z == d - 1;
                p.set(x, 0, z, edgeX || edgeZ ? st.foundation : st.planks);
                if (!edgeX && !edgeZ) continue;
                for (int y = 1; y <= h; y++) {
                    Block b = edgeX && edgeZ ? st.log : st.planks;
                    boolean window = y == 2 && !(edgeX && edgeZ) && ((edgeZ && x % 2 == 1 && x != w / 2) || (edgeX && z % 2 == 1));
                    p.set(x, y, z, window ? Block.GLASS_PANE : b);
                }
            }
        int door = w / 2;
        p.facing(door, 1, 0, st.door, 2, 0);
        p.facing(door, 2, 0, st.door, 2, 8);
        // Torch on the wall beside the door
        p.set(door + 1, 2, -1, Block.TORCH, 1 + p.rotate(0));
    }

    /** Gable roof of stairs along z, spanning x = -1..w. */
    private void roof(Placer p, int w, int d, int base, Style st) {
        int half = (w + 1) / 2;
        for (int i = 0; i <= half; i++) {
            int y = base + i;
            int xl = i - 1, xr = w - i;
            for (int z = -1; z <= d; z++) {
                if (xl < xr) {
                    // Low step outwards, high side towards the ridge
                    p.facing(xl, y, z, st.stairs, 1, 0);
                    p.facing(xr, y, z, st.stairs, 3, 0);
                } else if (xl == xr) {
                    p.set(xl, y, z, st.slab, 0);
                }
            }
            // Gable ends
            for (int x = xl + 1; x < xr; x++) {
                p.set(x, y, 0, st.planks);
                p.set(x, y, d - 1, st.planks);
            }
        }
    }

    private int[] smallHouse(Placer p, Style st, Random rand) {
        p.prepare(5, 5, 8, st.foundation);
        walls(p, 5, 5, 3, st);
        roof(p, 5, 5, 4, st);
        p.set(1, 1, 3, Block.CRAFTING_TABLE);
        p.set(3, 1, 3, Block.BED, p.rotate(2));
        p.set(3, 1, 2, Block.BED, p.rotate(2) | 4);
        p.set(2, 3, 3, Block.TORCH, 1 + p.rotate(0));
        return new int[]{p.wx(2, 2), p.oy + 1, p.wz(2, 2), 0, -1};
    }

    private int[] bigHouse(Placer p, Style st, Random rand) {
        p.prepare(7, 7, 10, st.foundation);
        walls(p, 7, 7, 4, st);
        roof(p, 7, 7, 5, st);
        boolean library = rand.nextInt(3) == 0;
        if (library) for (int x = 1; x <= 5; x++) { p.set(x, 1, 5, Block.BOOKSHELF); p.set(x, 2, 5, Block.BOOKSHELF); }
        else {
            p.set(1, 1, 5, Block.CRAFTING_TABLE);
            p.facing(2, 1, 5, Block.FURNACE, 2, 0);
            p.facing(5, 1, 5, Block.CHEST, 2, 0);
            fillChest(p.wx(5, 5), p.oy + 1, p.wz(5, 5), HOUSE_LOOT, 3 + rand.nextInt(4), rand);
            p.set(1, 1, 2, Block.BED, p.rotate(0) | 4);
            p.set(1, 1, 1, Block.BED, p.rotate(0));
        }
        p.set(3, 3, 5, Block.TORCH, 1 + p.rotate(0));
        p.set(1, 3, 3, Block.TORCH, 1 + p.rotate(1));
        return new int[]{p.wx(3, 3), p.oy + 1, p.wz(3, 3), 1, library ? 1 : -1};
    }

    private int[] farm(Placer p, Style st, Random rand) {
        p.prepare(9, 7, 3, Block.DIRT);
        Block[] crops = {Block.WHEAT, Block.WHEAT, Block.CARROTS, Block.POTATOES};
        Block left = crops[rand.nextInt(crops.length)], right = crops[rand.nextInt(crops.length)];
        for (int x = 0; x < 9; x++)
            for (int z = 0; z < 7; z++) {
                boolean border = x == 0 || x == 8 || z == 0 || z == 6;
                if (border) { p.set(x, 0, z, st.log == Block.SANDSTONE ? Block.SANDSTONE : st.log); continue; }
                if (x == 4) { p.set(x, 0, z, Block.WATER); continue; }
                p.set(x, 0, z, Block.FARMLAND, 1);
                p.set(x, 1, z, x < 4 ? left : right, rand.nextInt(8));
            }
        return new int[]{p.wx(4, -1), p.oy + 1, p.wz(4, -1), 0, 0};
    }

    private int[] smithy(Placer p, Style st, Random rand) {
        p.prepare(7, 6, 8, Block.COBBLESTONE);
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 6; z++) {
                p.set(x, 0, z, Block.COBBLESTONE);
                boolean wall = x == 0 || x == 6 || z == 5;
                if (wall) for (int y = 1; y <= 3; y++) p.set(x, y, z, (x == 0 || x == 6) && (z == 0 || z == 5) ? st.log : Block.COBBLESTONE);
                else if (z == 0 && (x == 0 || x == 6)) for (int y = 1; y <= 3; y++) p.set(x, y, z, st.log);
                p.set(x, 4, z, Block.COBBLESTONE_SLAB);
            }
        // Open front on fence posts, furnaces and a loot chest inside
        p.set(3, 1, 0, Block.AIR);
        p.facing(1, 1, 4, Block.FURNACE, 2, 0);
        p.facing(2, 1, 4, Block.FURNACE, 2, 0);
        p.facing(5, 1, 4, Block.CHEST, 2, 0);
        fillChest(p.wx(5, 4), p.oy + 1, p.wz(5, 4), SMITH_LOOT, 3 + rand.nextInt(5), rand);
        p.set(4, 1, 4, Block.IRON_BLOCK);
        p.set(3, 3, 4, Block.TORCH, 1 + p.rotate(0));
        return new int[]{p.wx(3, 2), p.oy + 1, p.wz(3, 2), 0, 3};
    }
}
