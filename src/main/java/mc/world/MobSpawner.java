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
        int passive = 0, hostile = 0;
        for (Entity e : world.entities()) {
            if (e instanceof Mob m) {
                if (m.type.hostile) hostile++;
                else passive++;
            }
        }
        if (world.tickCount % 40 == 0 && passive < PASSIVE_CAP) spawnPassive(world, player, random);
        if (hostile < HOSTILE_CAP) for (int i = 0; i < 2; i++) spawnHostile(world, player, random);
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
        int group = 2 + random.nextInt(3);
        for (int i = 0; i < group; i++) {
            int gx = x + random.nextInt(5) - 2, gz = z + random.nextInt(5) - 2;
            Chunk gc = world.getChunk(gx >> 4, gz >> 4);
            if (gc == null) continue;
            int gy = gc.topSolid(gx & 15, gz & 15);
            if (gy < 0 || world.getBlock(gx, gy, gz) != Block.GRASS.id || world.getBlock(gx, gy + 1, gz) != 0 && Block.get(world.getBlock(gx, gy + 1, gz)).solid) continue;
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
        int sky = world.getSkyLight(x, y, z), blk = world.getBlockLight(x, y, z);
        int effective = Math.max(blk, Math.round(sky * world.dayFactor));
        if (effective > 0) return;
        MobType type = MobType.HOSTILE[random.nextInt(MobType.HOSTILE.length)];
        if (type == MobType.SPIDER && (world.getBlock(x + 1, y, z) != 0 || world.getBlock(x - 1, y, z) != 0)) return;
        spawn(world, type, x + 0.5, y, z + 0.5, random);
    }

    private void spawn(World world, MobType type, double x, double y, double z, Random random) {
        Mob m = new Mob(type);
        m.setPos(x, y, z);
        m.yaw = m.bodyYaw = m.headYaw = random.nextFloat() * 360;
        world.addEntity(m);
    }
}
