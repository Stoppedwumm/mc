package mc.world;

import mc.entity.Entity;
import mc.entity.Mob;
import mc.entity.MobType;
import mc.entity.Player;

import java.util.Random;

/** Spawns animals on grass in daylight and monsters in darkness around the player. */
public final class MobSpawner {
    private static final int PASSIVE_CAP = 12, HOSTILE_CAP = 24;

    public void tick(World world, Player player, Random random) {
        if (world.dimension == Dimension.NETHER) {
            tickNether(world, player, random);
            return;
        }
        int passive = 0, hostile = 0;
        for (Entity e : world.entities()) {
            if (e instanceof Mob m) {
                if (m.type.hostile) hostile++;
                else if (m.type.isAnimal() || m.type == MobType.SQUID) passive++;
            }
        }
        int squid = 0;
        for (Entity e : world.entities()) if (e instanceof Mob m && m.type == MobType.SQUID) squid++;
        if (world.tickCount % 40 == 0 && passive - squid < PASSIVE_CAP) spawnPassive(world, player, random);
        if (world.tickCount % 40 == 20 && squid < 5) spawnSquid(world, player, random);
        if (hostile < HOSTILE_CAP) for (int i = 0; i < 2; i++) spawnHostile(world, player, random);
    }

    /** Zombie pigmen in groups on netherrack, rare ghasts in open caverns, magma cubes anywhere. */
    private void tickNether(World world, Player player, Random random) {
        int count = 0;
        for (Entity e : world.entities()) if (e instanceof Mob) count++;
        if (count >= 30 || world.tickCount % 4 != 0) return;
        double ang = random.nextDouble() * Math.PI * 2;
        double dist = 24 + random.nextDouble() * 40;
        int x = (int) Math.floor(player.x + Math.cos(ang) * dist), z = (int) Math.floor(player.z + Math.sin(ang) * dist);
        if (!world.isLoaded(x, z)) return;
        int y = 32 + random.nextInt(90);
        int r = random.nextInt(100);
        if (r < 6) {
            // Ghasts need a big open space
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = 0; dy <= 4; dy++)
                    for (int dz = -2; dz <= 2; dz++) if (world.getBlock(x + dx, y + dy, z + dz) != 0) return;
            spawn(world, MobType.GHAST, x + 0.5, y, z + 0.5, random);
            return;
        }
        for (int i = 0; i < 16 && y > 32; i++, y--) if (Block.get(world.getBlock(x, y - 1, z)).solid) break;
        Block floor = Block.get(world.getBlock(x, y - 1, z));
        if (!floor.solid || floor == Block.BEDROCK || world.getBlock(x, y, z) != 0 || world.getBlock(x, y + 1, z) != 0) return;
        if (r < 16) {
            spawn(world, MobType.MAGMA_CUBE, x + 0.5, y, z + 0.5, random);
            return;
        }
        if (floor != Block.NETHERRACK && floor != Block.SOUL_SAND && floor != Block.GRAVEL) return;
        int group = 2 + random.nextInt(3);
        for (int i = 0; i < group; i++) {
            int gx = x + random.nextInt(5) - 2, gz = z + random.nextInt(5) - 2;
            if (world.getBlock(gx, y, gz) == 0 && world.getBlock(gx, y + 1, gz) == 0 && Block.get(world.getBlock(gx, y - 1, gz)).solid)
                spawn(world, MobType.ZOMBIE_PIGMAN, gx + 0.5, y, gz + 0.5, random);
        }
    }

    private void spawnSquid(World world, Player player, Random random) {
        double ang = random.nextDouble() * Math.PI * 2;
        double dist = 24 + random.nextDouble() * 40;
        int x = (int) Math.floor(player.x + Math.cos(ang) * dist), z = (int) Math.floor(player.z + Math.sin(ang) * dist);
        if (!world.isLoaded(x, z)) return;
        int y = 62 - random.nextInt(10);
        if (world.getBlock(x, y, z) != Block.WATER.id || world.getBlock(x, y + 1, z) != Block.WATER.id || world.getBlock(x, y - 2, z) != Block.WATER.id) return;
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            int gx = x + random.nextInt(3) - 1, gz = z + random.nextInt(3) - 1;
            if (world.getBlock(gx, y, gz) == Block.WATER.id) spawn(world, MobType.SQUID, gx + 0.5, y, gz + 0.5, random);
        }
    }

    /** Minecraft-style slime chunks: one in ten, decided by the world seed. */
    public static boolean isSlimeChunk(long seed, int cx, int cz) {
        Random r = new Random(seed + (long) cx * cx * 4987142 + cx * 5947611L + (long) cz * cz * 4392871L + cz * 389711L ^ 987234911L);
        return r.nextInt(10) == 0;
    }

    private void spawnPassive(World world, Player player, Random random) {
        double ang = random.nextDouble() * Math.PI * 2;
        double dist = 32 + random.nextDouble() * 64;
        int x = (int) Math.floor(player.x + Math.cos(ang) * dist), z = (int) Math.floor(player.z + Math.sin(ang) * dist);
        Chunk c = world.getChunk(x >> 4, z >> 4);
        if (c == null || c.light == null) return;
        int y = c.topSolid(x & 15, z & 15);
        if (y < 0 || world.getBlock(x, y, z) != Block.GRASS.id || world.getSkyLight(x, y + 1, z) < 9) return;
        MobType type = MobType.PASSIVE[random.nextInt(MobType.PASSIVE.length)];
        mc.world.gen.Biome biome = world.biomeAt(x, z);
        boolean woods = biome == mc.world.gen.Biome.FOREST || biome == mc.world.gen.Biome.TAIGA || biome == mc.world.gen.Biome.SNOWY_TAIGA;
        if (woods && random.nextInt(5) == 0) type = MobType.WOLF;
        int group = type == MobType.WOLF ? 4 : 2 + random.nextInt(3);
        for (int i = 0; i < group; i++) {
            int gx = x + random.nextInt(5) - 2, gz = z + random.nextInt(5) - 2;
            Chunk gc = world.getChunk(gx >> 4, gz >> 4);
            if (gc == null) continue;
            int gy = gc.topSolid(gx & 15, gz & 15);
            int ground = world.getBlock(gx, gy, gz);
            if (type == MobType.WOLF && (ground == Block.SNOWY_GRASS.id || ground == Block.SNOW.id)) ground = Block.GRASS.id;
            if (gy < 0 || ground != Block.GRASS.id || world.getBlock(gx, gy + 1, gz) != 0 && Block.get(world.getBlock(gx, gy + 1, gz)).solid) continue;
            spawn(world, type, gx + 0.5, gy + 1, gz + 0.5, random);
        }
    }

    private void spawnHostile(World world, Player player, Random random) {
        double ang = random.nextDouble() * Math.PI * 2;
        double dist = 24 + random.nextDouble() * 40;
        int x = (int) Math.floor(player.x + Math.cos(ang) * dist), z = (int) Math.floor(player.z + Math.sin(ang) * dist);
        if (!world.isLoaded(x, z) || world.getChunk(x >> 4, z >> 4).light == null) return;
        int y = (int) player.y + random.nextInt(48) - 24;
        if (y < 1 || y > 250) return;
        // Find a floor below
        for (int i = 0; i < 16 && y > 1; i++, y--) {
            if (Block.get(world.getBlock(x, y - 1, z)).solid) break;
        }
        Block floor = Block.get(world.getBlock(x, y - 1, z));
        if (!floor.solid || !floor.opaque || floor == Block.BEDROCK) return;
        if (world.getBlock(x, y, z) != 0 || world.getBlock(x, y + 1, z) != 0) return;
        if (y < 40 && random.nextInt(4) == 0 && isSlimeChunk(world.seed, x >> 4, z >> 4)) {
            if (world.getBlock(x + 1, y, z) == 0 && world.getBlock(x, y, z + 1) == 0) spawn(world, MobType.SLIME, x + 0.5, y, z + 0.5, random);
            return;
        }
        int sky = world.getSkyLight(x, y, z), blk = world.getBlockLight(x, y, z);
        int effective = Math.max(blk, Math.round(sky * world.dayFactor));
        if (effective > 0) return;
        MobType type = MobType.HOSTILE[random.nextInt(MobType.HOSTILE.length)];
        if (type == MobType.SPIDER && (world.getBlock(x + 1, y, z) != 0 || world.getBlock(x - 1, y, z) != 0)) return;
        if (type == MobType.SLIME) return;
        if (type == MobType.ENDERMAN && (world.getBlock(x, y + 2, z) != 0 || random.nextInt(3) != 0)) return;
        spawn(world, type, x + 0.5, y, z + 0.5, random);
    }

    private void spawn(World world, MobType type, double x, double y, double z, Random random) {
        Mob m = new Mob(type);
        m.setPos(x, y, z);
        m.yaw = m.bodyYaw = m.headYaw = random.nextFloat() * 360;
        world.addEntity(m);
    }
}
