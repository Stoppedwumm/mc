package mc.server;

import mc.client.Options;
import mc.world.Dimension;
import mc.world.World;
import mc.world.WorldStorage;
import mc.world.gen.TerrainGenerator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A server without a window: `java -jar mc.jar --server [--port 25565] [--world name]`. Ticks the world at 20 ticks
 * per second, streams it to players and reads commands from the console (stop, list, say, time, weather, kick).
 */
public final class DedicatedServer {
    private static final long TICK_NANOS = 50_000_000L;

    private final Path worldDir;
    private final World world;
    private final Server server;
    private final Options.Level level;
    private final ConcurrentLinkedQueue<String> console = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private volatile boolean running = true;
    private int weatherTimer = 12000 + new Random().nextInt(60000);

    public DedicatedServer(Path gameDir, String worldName, int port, Long seed, String motd, int maxPlayers, int viewDistance) throws Exception {
        worldDir = gameDir.resolve("saves").resolve(worldName);
        Files.createDirectories(worldDir);
        Options.Level lv = Options.Level.load(worldDir.resolve("level.json"));
        if (lv == null) {
            lv = new Options.Level();
            lv.seed = seed != null ? seed : new Random().nextLong();
            lv.name = worldName;
        }
        level = lv;
        world = new World(level.seed, new WorldStorage(worldDir), Dimension.OVERWORLD);
        world.headless = true;
        world.time = level.time;
        world.raining = level.raining;
        List<Options.BlockEntityData> bes = Options.BlockEntityData.loadList(worldDir.resolve("blockentities.json"));
        if (bes == null) bes = level.blockEntities;
        if (bes != null) Options.BlockEntityData.apply(world, bes);
        server = new Server(world, port, worldDir.resolve("players"), null, this::log, motd, false);
        server.maxPlayers = maxPlayers;
        server.viewDistanceCap = viewDistance;
        if (level.spawnY > 0) {
            server.spawnX = level.spawnX;
            server.spawnY = level.spawnY;
            server.spawnZ = level.spawnZ;
        } else {
            int[] s = findSpawn(world.generator);
            server.spawnX = s[0] + 0.5;
            server.spawnZ = s[1] + 0.5;
            server.spawnY = -1;
        }
    }

    private void log(String msg) {
        System.out.println("[Server] " + msg);
    }

    private static int[] findSpawn(TerrainGenerator gen) {
        for (int r = 0; r < 4000; r += 16)
            for (int a = 0; a < Math.max(1, r / 4); a++) {
                double ang = a * Math.PI * 2 / Math.max(1, r / 4);
                int x = (int) (Math.cos(ang) * r), z = (int) (Math.sin(ang) * r);
                int h = gen.estimateHeight(x, z);
                if (h > TerrainGenerator.SEA_LEVEL + 2 && h < 95) return new int[]{x, z};
            }
        return new int[]{0, 0};
    }

    public void run() {
        log("Starting world '" + worldDir.getFileName() + "' (seed " + level.seed + ") on port " + server.port());
        log("Type 'help' for commands, 'stop' to save and quit");
        Thread input = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = r.readLine()) != null) console.add(line);
            } catch (Exception ignored) {
            }
        }, "Console");
        input.setDaemon(true);
        input.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (running) {
                running = false;
                save();
            }
        }));
        long next = System.nanoTime();
        long ticks = 0;
        boolean spawnReady = false;
        while (running) {
            String cmd;
            while ((cmd = console.poll()) != null) consoleCommand(cmd);
            List<double[]> centers = new ArrayList<>(server.playerCenters());
            // Keep the spawn area loaded, like Minecraft's spawn chunks
            centers.add(new double[]{server.spawnX, server.spawnZ, 2});
            world.updateCenters(centers, 20_000_000L);
            if (!spawnReady && world.isLoaded((int) Math.floor(server.spawnX), (int) Math.floor(server.spawnZ))) {
                spawnReady = true;
                log("Spawn area ready");
            }
            tickWeather();
            world.tick();
            server.tick();
            ticks++;
            if (ticks % 6000 == 0) save();
            next += TICK_NANOS;
            long wait = next - System.nanoTime();
            if (wait > 0) {
                try {
                    Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
                } catch (InterruptedException e) {
                    break;
                }
            } else if (wait < -2_000_000_000L) {
                log("Can't keep up! Skipping " + (-wait / TICK_NANOS) + " ticks");
                next = System.nanoTime();
            }
        }
        server.stop();
        save();
        world.shutdown();
        log("Stopped");
    }

    /** Natural rain cycle (unless set with /weather), and daylight for mob spawning and burning. */
    private void tickWeather() {
        if (server.weatherOverride < 0 && --weatherTimer <= 0) {
            world.raining = !world.raining;
            weatherTimer = world.raining ? 12000 + random.nextInt(12000) : 12000 + random.nextInt(96000);
        }
        world.dayFactor = World.dayFactor(world.time, world.raining ? 1 : 0);
    }

    private void consoleCommand(String line) {
        String l = line.trim();
        if (l.isEmpty()) return;
        if (l.equals("stop")) {
            log("Stopping the server");
            running = false;
            return;
        }
        if (l.equals("save")) {
            save();
            log("Saved the world");
            return;
        }
        if (l.equals("help")) {
            log("Console commands: stop, save, list, say <msg>, time set <day|night|n>, weather <clear|rain>, kick <player>, seed");
            return;
        }
        String reply = server.command(l, null);
        if (!reply.isEmpty()) log(reply);
    }

    private void save() {
        world.saveAll();
        server.savePlayers();
        level.time = world.time;
        level.raining = world.raining;
        level.lastPlayed = System.currentTimeMillis();
        level.spawnX = server.spawnX;
        level.spawnY = server.spawnY;
        level.spawnZ = server.spawnZ;
        level.blockEntities = null;
        Options.BlockEntityData.saveList(worldDir.resolve("blockentities.json"), Options.BlockEntityData.capture(world));
        level.save(worldDir.resolve("level.json"));
    }
}
