package mc.client;

import mc.entity.*;
import mc.item.Item;
import mc.entity.Effect;
import mc.item.ItemStack;
import mc.render.*;
import mc.world.*;
import mc.world.gen.TerrainGenerator;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImageWrite;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class Game implements World.Listener {
    private static final double TICK = 0.05;

    enum Screen { LOADING, NONE, TITLE, WORLDS, CREATE_WORLD, DELETE_WORLD, MULTIPLAYER, ADD_SERVER, DIRECT_CONNECT, CONNECTING, DISCONNECTED, OPTIONS, PAUSE, INVENTORY, CRAFTING, FURNACE, CHEST, CREATIVE, CHAT, DEATH, TRADING, ENCHANTING, ANVIL, BREWING }

    final Path gameDir;
    private Path worldDir;
    final Options options;
    private Options.Level level;
    Window window;
    Input input;
    Sound sound;
    WorldRenderer renderer;
    PostProcess post;
    Gui gui;
    private ItemRenderer itemRenderer;
    /** Distant terrain beyond the render distance. */
    mc.render.lod.LodRenderer lod;
    final EntityRenderer entityRenderer = new EntityRenderer();
    final Weather weather = new Weather();
    final Particles particles = new Particles();
    World world;
    Player player = new Player();
    Interaction interaction;
    Screens screens;
    Hud hud;
    private Commands commands;

    private Screen screen = Screen.LOADING;
    boolean running = true;
    boolean showDebug, hideGui;
    long ticks;
    private long lastSpaceTap = -100, lastForwardTap = -100;
    private boolean sprintKey;
    private float fovMod = 1, prevFovMod = 1;
    private boolean spawnNeeded;
    private long loadedTick = Long.MAX_VALUE / 2;
    int fps;
    private int frameCounter;
    private double fpsTimer;
    final StringBuilder chatInput = new StringBuilder();
    final Random random = new Random();
    private final long seedArg;
    private final boolean seedGiven;
    private final int rdArg;
    private final String screenshotAfter;
    private final List<String> startupCommands = new ArrayList<>();
    private boolean deathHandled;

    /** World given on the command line (null: start at the title screen). */
    private final String startWorld;
    private final String startServer;
    Menus menus;
    /** True while the title screens are shown (world is then the panorama world). */
    boolean inMenu;

    public Game(Path gameDir, String worldName, Long seed, int renderDistance, String screenshotAfter, List<String> commands, String connect) {
        this.gameDir = gameDir;
        this.startWorld = worldName;
        this.startServer = connect;
        this.options = Options.load(gameDir.resolve("options.json"));
        this.seedGiven = seed != null;
        this.seedArg = seed != null ? seed : new Random().nextLong();
        this.rdArg = renderDistance;
        this.screenshotAfter = screenshotAfter;
        startupCommands.addAll(commands);
    }

    Screen screen() { return screen; }

    // ------------------------------------------------------------------ lifecycle

    public void run() throws Exception {
        init();
        double last = glfwGetTime(), acc = 0;
        while (running && !window.shouldClose()) {
            double now = glfwGetTime();
            double dt = Math.min(0.25, now - last);
            last = now;
            acc += dt;
            while (acc >= TICK) {
                tick();
                acc -= TICK;
            }
            float pt = (float) (acc / TICK);
            frame(pt, dt);
            window.swap();
            // Reset per-frame input *before* polling, so the events collected now are seen next frame
            input.endFrame();
            window.pollEvents();
            window.updateSize();
            input.updateCursor(window);
            frameCounter++;
            fpsTimer += dt;
            if (fpsTimer >= 1) { fps = frameCounter; frameCounter = 0; fpsTimer -= 1; }
        }
        shutdown();
    }

    private void init() throws Exception {
        if (rdArg > 0) options.renderDistance = rdArg;
        window = new Window("Minecraft-like (LWJGL)", 1280, 720);
        window.setVsync(options.vsync);
        input = new Input(window);
        sound = new Sound();
        sound.volume = options.volume;
        renderer = new WorldRenderer(0);
        post = new PostProcess(window.width, window.height);
        itemRenderer = new ItemRenderer();
        int[] atlasPixels = new TextureGen().generate();
        itemRenderer.setBlockPixels(atlasPixels);
        lod = new mc.render.lod.LodRenderer(atlasPixels, TextureGen.ATLAS);
        entityRenderer.items = itemRenderer;
        gui = new Gui(renderer, new Font());
        gui.items = itemRenderer;
        interaction = new Interaction(this);
        screens = new Screens(this);
        hud = new Hud(this);
        commands = new Commands(this);
        menus = new Menus(this);
        if (startWorld != null) loadWorld(startWorld, seedGiven ? seedArg : null, null, null);
        else if (startServer != null) menus.connect(startServer);
        else openTitle();
    }

    /** Folder of the world being played (null in multiplayer and on the title screen). */
    Path worldDir() { return worldDir; }

    /**
     * Opens (or creates) a singleplayer world. seed/creative/displayName only apply to a new world;
     * a null seed picks a random one.
     */
    void loadWorld(String folder, Long seed, Boolean creative, String displayName) {
        try {
            if (!inMenu && world != null && multiplayer == null) {
                saveWorld();
                world.shutdown();
                world = null;
            }
            closeMenuWorld();
            worldDir = gameDir.resolve("saves").resolve(folder);
            Files.createDirectories(worldDir);
            player = new Player();
            interaction = new Interaction(this);
            particles.clear();
            hud.clearChat();
            sleepTicks = 0;
            perspective = 0;
            deathHandled = false;
            spawnNeeded = false;
            loadedTick = Long.MAX_VALUE / 2;
            level = Options.Level.load(worldDir.resolve("level.json"));
            if (level == null) {
                level = new Options.Level();
                level.seed = seed != null ? seed : new Random().nextLong();
                level.creative = creative != null && creative;
                level.name = displayName != null ? displayName : folder;
            } else if (seed != null && level.seed != seed) {
                System.out.println("World already exists with seed " + level.seed + "; ignoring --seed");
            }
            if (level.name == null) level.name = folder;
            player.name = options.playerName;
            weather.raining = false;
            weather.timer = 12000 + random.nextInt(12000);
            Dimension startDim;
            try { startDim = Dimension.valueOf(level.dimension == null ? "OVERWORLD" : level.dimension); } catch (IllegalArgumentException e) { startDim = Dimension.OVERWORLD; }
            world = createWorld(startDim, level.time);
            loadingMessage = "Generating terrain...";
            setScreen(Screen.LOADING);
            startPlayer();
        } catch (java.io.IOException e) {
            worldDir = null;
            menus.error("Could not open world", e.getMessage());
        }
    }

    private void startPlayer() {
        if (level.spawned) {
            level.readPlayer(player);
            weather.raining = level.raining;
            if (level.weatherTimer > 0) weather.timer = level.weatherTimer;
        } else {
            int[] spawn = findSpawn(world.generator);
            player.setPos(spawn[0] + 0.5, 120, spawn[1] + 0.5);
            player.creative = level.creative;
            spawnNeeded = true;
        }
        System.out.println("World seed: " + level.seed);
    }

    /** Saves and leaves the current world (or server) and returns to the title screen. */
    void leaveWorld() {
        if (isContainer(screen)) screens.onClose();
        if (worldDir != null && world != null && !inMenu) saveWorld();
        if (world != null) world.shutdown();
        world = null;
        worldDir = null;
        level = null;
        openTitle();
    }

    // ------------------------------------------------------------------ multiplayer

    /** Starts connecting to a server (the CONNECTING screen shows progress). */
    void startMultiplayer(String address) {
        if (multiplayer != null) multiplayer.disconnect("Reconnecting");
        multiplayer = new MultiplayerSession(this, address);
    }

    /** Called by the session once logged in: switches from the title screen to the server's world. */
    void enterRemoteWorld(World w, Options.Level data, boolean fresh) {
        closeMenuWorld();
        worldDir = null;
        level = data;
        world = w;
        w.listener = this;
        player = new Player();
        player.name = options.playerName;
        w.setPlayer(player);
        data.readPlayer(player);
        interaction = new Interaction(this);
        particles.clear();
        hud.clearChat();
        sleepTicks = 0;
        perspective = 0;
        deathHandled = false;
        spawnNeeded = fresh;
        weather.raining = w.raining;
        loadedTick = Long.MAX_VALUE / 2;
        loadingMessage = "Downloading terrain...";
        setScreen(Screen.LOADING);
    }

    /** The connection failed or was closed by the server. */
    void multiplayerEnded(String title, String reason) {
        multiplayer = null;
        if (isContainer(screen)) screens.onClose();
        if (world != null && !inMenu) world.shutdown();
        world = null;
        level = null;
        openTitle();
        menus.error(title, reason);
    }

    /** Pause menu: leave the world (singleplayer saves; multiplayer disconnects). */
    void quitToTitle() {
        if (multiplayer != null) {
            MultiplayerSession m = multiplayer;
            multiplayer = null;
            if (isContainer(screen)) screens.onClose();
            m.afterTick();
            m.disconnect("Quit");
            if (world != null) world.shutdown();
            world = null;
            level = null;
            openTitle();
            return;
        }
        if (lanServer != null) {
            lanServer.stop();
            lanServer = null;
        }
        leaveWorld();
    }

    /** Pause menu: lets other players on the network join this singleplayer world. */
    void openToLan() {
        if (lanServer != null || worldDir == null) return;
        if (world.dimension != Dimension.OVERWORLD) { hud.chat("Open to LAN from the Overworld"); return; }
        try {
            player.name = options.playerName;
            mc.server.Server s;
            try {
                s = new mc.server.Server(world, mc.net.Net.DEFAULT_PORT, worldDir.resolve("players"), player, m -> System.out.println("[Server] " + m),
                        options.playerName + " - " + level.name, true);
            } catch (java.io.IOException busy) {
                s = new mc.server.Server(world, 0, worldDir.resolve("players"), player, m -> System.out.println("[Server] " + m), options.playerName + " - " + level.name, true);
            }
            s.chatSink = hud::chat;
            s.spawnX = player.spawnY > 0 ? player.spawnX : player.x;
            s.spawnY = player.spawnY > 0 ? player.spawnY : player.y;
            s.spawnZ = player.spawnY > 0 ? player.spawnZ : player.z;
            lanServer = s;
            hud.chat("Local game hosted on port " + s.port());
            closeScreen();
        } catch (java.io.IOException e) {
            hud.chat("Could not open to LAN: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ title screen panorama

    private Path menuDir;
    private int[] panoramaSpot;

    /** Shows the title screen over a slowly turning view of a generated landscape. */
    void openTitle() {
        inMenu = true;
        player = new Player();
        interaction = new Interaction(this);
        particles.clear();
        sleepTicks = 0;
        perspective = 0;
        if (world == null) {
            try {
                menuDir = Files.createTempDirectory("mc-panorama");
                World w = new World(Menus.PANORAMA_SEED, new WorldStorage(menuDir), Dimension.OVERWORLD);
                w.time = 2500;
                w.listener = this;
                w.setPlayer(player);
                world = w;
                if (panoramaSpot == null) panoramaSpot = Menus.panoramaSpot(w.generator);
                player.setPos(panoramaSpot[0] + 0.5, panoramaSpot[2], panoramaSpot[1] + 0.5);
                player.pitch = 16;
                player.yaw = 30;
                player.creative = true;
                player.flying = true;
            } catch (java.io.IOException e) {
                System.err.println("No panorama: " + e);
            }
        }
        setScreen(Screen.TITLE);
    }

    private void closeMenuWorld() {
        if (!inMenu) return;
        inMenu = false;
        if (world != null) world.shutdown();
        world = null;
        if (menuDir != null) {
            try (var paths = Files.walk(menuDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(q -> q.toFile().delete());
            } catch (java.io.IOException ignored) { }
            menuDir = null;
        }
    }


    private Path dimensionDir(Dimension d) {
        return d.folder.isEmpty() ? worldDir : worldDir.resolve(d.folder);
    }

    /** Opens a dimension's world with its block entities. */
    private World createWorld(Dimension d, long time) throws java.io.IOException {
        Path dir = dimensionDir(d);
        Files.createDirectories(dir);
        World w = new World(level.seed, new WorldStorage(dir), d);
        w.time = time;
        w.listener = this;
        w.setPlayer(player);
        List<Options.BlockEntityData> bes = Options.BlockEntityData.loadList(dir.resolve("blockentities.json"));
        if (bes == null && d == Dimension.OVERWORLD) bes = level.blockEntities;
        if (bes != null) Options.BlockEntityData.apply(w, bes);
        return w;
    }

    /** Portal travel between the overworld and the Nether (coordinates scale by 8). */
    void changeDimension(Dimension target, boolean viaPortal) {
        try {
            if (isContainer(screen)) screens.onClose();
            double scale = target == Dimension.NETHER ? 0.125 : world.dimension == Dimension.NETHER ? 8 : 1;
            double tx = player.x * scale, tz = player.z * scale;
            saveWorld();
            long time = world.time;
            world.shutdown();
            world = createWorld(target, time);
            level.dimension = target.name();
            int maxY = target == Dimension.NETHER ? mc.world.gen.NetherGenerator.HEIGHT : Chunk.HEIGHT;
            if (viaPortal) {
                world.loadAreaBlocking((int) Math.floor(tx) >> 4, (int) Math.floor(tz) >> 4, 1);
                double[] p = mc.world.Portal.findOrCreate(world, tx, Math.min(player.y, maxY - 10), tz, maxY);
                player.setPos(p[0], p[1], p[2]);
                sound.playUi("portal_travel", 0.6f, 1);
            }
            player.motionX = player.motionY = player.motionZ = 0;
            player.fallDistance = 0;
            player.portalTicks = 0;
            player.portalCooldown = 300;
            setScreen(Screen.LOADING);
            loadingMessage = target == Dimension.NETHER ? "Entering the Nether" : "Leaving the Nether";
        } catch (java.io.IOException e) {
            hud.chat("Could not change dimension: " + e.getMessage());
        }
    }

    private String loadingMessage = "Generating terrain...";

    private static int[] findSpawn(TerrainGenerator gen) {
        for (int r = 0; r < 4000; r += 16) {
            for (int a = 0; a < Math.max(1, r / 4); a++) {
                double ang = a * Math.PI * 2 / Math.max(1, r / 4);
                int x = (int) (Math.cos(ang) * r), z = (int) (Math.sin(ang) * r);
                int h = gen.estimateHeight(x, z);
                if (h > TerrainGenerator.SEA_LEVEL + 2 && h < 95) return new int[]{x, z};
            }
        }
        return new int[]{0, 0};
    }

    private void shutdown() {
        if (isContainer(screen)) screens.onClose();
        if (multiplayer != null) multiplayer.disconnect("Quit");
        if (lanServer != null) lanServer.stop();
        if (inMenu) closeMenuWorld();
        else if (world != null) {
            if (worldDir != null) saveWorld();
            world.shutdown();
        }
        options.save(gameDir.resolve("options.json"));
        lod.shutdown();
        sound.destroy();
        window.destroy();
    }

    void saveWorld() {
        if (worldDir == null || inMenu || (screen == Screen.LOADING && spawnNeeded)) return;
        world.saveAll();
        level.time = world.time;
        level.writePlayer(player);
        level.raining = weather.raining;
        level.weatherTimer = weather.timer;
        level.lastPlayed = System.currentTimeMillis();
        level.blockEntities = null;
        level.dimension = world.dimension.name();
        Options.BlockEntityData.saveList(dimensionDir(world.dimension).resolve("blockentities.json"), Options.BlockEntityData.capture(world));
        lod.save();
        level.save(worldDir.resolve("level.json"));
        if (lanServer != null) lanServer.savePlayers();
    }

    // ------------------------------------------------------------------ screens

    boolean isContainer(Screen s) { return screens != null && screens.isContainer(s); }

    void setScreen(Screen s) {
        if (isContainer(screen) && !isContainer(s)) screens.onClose();
        screen = s;
        input.releaseAll();
    }

    void closeScreen() {
        setScreen(Screen.NONE);
        input.dx = input.dy = 0;
    }

    void setCreative(boolean c) {
        player.creative = c;
        if (!c) player.flying = false;
        hud.chat("Game mode set to " + (c ? "Creative" : "Survival"));
    }

    void respawn() {
        if (world.dimension != Dimension.OVERWORLD) {
            player.respawn();
            changeDimension(Dimension.OVERWORLD, false);
        }
        if (player.spawnY < 0) {
            player.spawnX = level.x;
            player.spawnY = level.y;
            player.spawnZ = level.z;
        }
        player.respawn();
        deathHandled = false;
        if (multiplayer != null) multiplayer.respawned();
        closeScreen();
    }

    // ------------------------------------------------------------------ ticking

    /** Connection to a server when playing multiplayer (null in singleplayer). */
    MultiplayerSession multiplayer;
    /** Server hosting this singleplayer world for LAN players (null unless opened to LAN). */
    mc.server.Server lanServer;

    boolean isMultiplayer() { return multiplayer != null; }

    private void tick() {
        ticks++;
        if (inMenu) {
            menus.tick();
            runStartupCommands();
            return;
        }
        if (multiplayer != null) {
            multiplayer.tick();
            if (multiplayer == null || world == null) return;
        }
        if (screen == Screen.LOADING) {
            checkLoaded();
            if (lanServer != null) lanServer.tick();
            return;
        }
        runStartupCommands();
        // Like Minecraft, the game only pauses in singleplayer
        if (screen == Screen.PAUSE && multiplayer == null && lanServer == null) return;

        if (multiplayer != null) weather.raining = world.raining; // the server decides
        weather.tick();
        if (multiplayer != null) weather.timer = Integer.MAX_VALUE;
        world.raining = weather.raining;
        world.dayFactor = World.dayFactor(world.time, weather.rain(1));

        boolean play = screen == Screen.NONE && sleepTicks == 0;
        if (sleepTicks > 0) tickSleep();
        float forward = 0, strafe = 0;
        boolean jump = false, sneak = false;
        if (play) {
            if (input.down(GLFW_KEY_W)) forward++;
            if (input.down(GLFW_KEY_S)) forward--;
            if (input.down(GLFW_KEY_A)) strafe++;
            if (input.down(GLFW_KEY_D)) strafe--;
            jump = input.down(GLFW_KEY_SPACE);
            sneak = input.down(GLFW_KEY_LEFT_SHIFT);
        }
        boolean sprint = play && (input.down(GLFW_KEY_LEFT_CONTROL) || sprintKey) && forward > 0 && interaction.useType == 0;
        if (forward <= 0) sprintKey = false;
        if (interaction.useType != 0) { forward *= 0.2f; strafe *= 0.2f; }

        float prevHealth = player.health;
        if (!player.isDead()) {
            player.tick(forward, strafe, jump, sneak, sprint);
            Block under = Block.get(world.getBlock((int) Math.floor(player.x), (int) Math.floor(player.y - 0.2), (int) Math.floor(player.z)));
            if (player.stepThisTick && under != Block.AIR) sound.step(under, player.x, player.y, player.z);
            if (player.landedThisTick && under != Block.AIR) sound.step(under, player.x, player.y, player.z);
            if (player.splashThisTick) {
                sound.splash();
                for (int i = 0; i < 20; i++) particles.spawn("splash", player.x + random.nextGaussian() * 0.3, player.y + 0.5, player.z + random.nextGaussian() * 0.3);
            }
            if (player.eyeInBlock(Block.WATER.id) && random.nextInt(8) == 0) particles.spawn("bubble", player.x, player.eyeY(), player.z);
            // Portals only work in singleplayer for now (a server hosts one dimension)
            boolean inPortal = player.inPortal() && multiplayer == null && lanServer == null;
            // After travelling, the portal only works again once the player has stepped out of it
            if (player.portalCooldown > 0) {
                if (inPortal) player.portalCooldown = Math.max(player.portalCooldown, 20);
                player.portalCooldown--;
                player.portalTicks = 0;
            } else if (inPortal) {
                if (player.portalTicks == 0) sound.playUi("portal_trigger", 0.3f, 0.8f + random.nextFloat() * 0.4f);
                player.portalTicks++;
                if (player.portalTicks >= 80 || player.creative) {
                    changeDimension(world.dimension == Dimension.NETHER ? Dimension.OVERWORLD : Dimension.NETHER, true);
                    return;
                }
            } else player.portalTicks = Math.max(0, player.portalTicks - 4);
            if (player.health < prevHealth && !player.isDead()) sound.play("hurt", player.x, player.y + 1, player.z, 1, 0.9f + random.nextFloat() * 0.2f);
            pickupItems();
        } else {
            player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
            if (!deathHandled) onPlayerDeath();
        }

        prevFovMod = fovMod;
        float targetFov = 1;
        if (player.flying) targetFov *= 1.1f;
        if (player.sprinting) targetFov *= 1.15f;
        if (interaction.useType == 2) targetFov *= 1 - interaction.bowPower() * 0.15f;
        fovMod += (targetFov - fovMod) * 0.5f;

        interaction.tick(play);
        world.tick();
        if (lanServer != null) lanServer.tick();
        if (multiplayer != null) multiplayer.afterTick();
        animateBlocks();
        particles.tick(world);
        if (ticks % 6000 == 0) saveWorld();
    }

    /** Client-side block effects near the player, like Minecraft's animateTick. */
    private void animateBlocks() {
        int px = (int) Math.floor(player.x), py = (int) Math.floor(player.y), pz = (int) Math.floor(player.z);
        for (int i = 0; i < 300; i++) {
            int x = px + random.nextInt(25) - 12, y = py + random.nextInt(25) - 12, z = pz + random.nextInt(25) - 12;
            int id = world.getBlock(x, y, z);
            if (id == 0) continue;
            if (id == Block.TORCH.id) {
                int m = world.getMeta(x, y, z);
                double fx = x + 0.5, fy = y + 0.7, fz = z + 0.5;
                if (m >= 1 && m <= 4) { fx += mc.world.Shapes.DX[m - 1] * 0.27; fz += mc.world.Shapes.DZ[m - 1] * 0.27; fy += 0.22; }
                particles.spawn("flame", fx, fy, fz);
                if (random.nextInt(2) == 0) particles.spawn("smoke", fx, fy + 0.05, fz);
            } else if (id == Block.FIRE.id) {
                if (random.nextInt(2) == 0) particles.spawn("smoke", x + random.nextDouble(), y + 0.5 + random.nextDouble() * 0.5, z + random.nextDouble());
                if (random.nextInt(3) == 0) particles.spawn("flame", x + random.nextDouble(), y + random.nextDouble() * 0.6, z + random.nextDouble());
                if (random.nextInt(24) == 0) sound.play("fizz", x + 0.5, y + 0.5, z + 0.5, 0.2f, 0.5f + random.nextFloat() * 0.3f);
            } else if (id == Block.NETHER_PORTAL.id) {
                for (int k = 0; k < 2; k++) particles.spawn("portal", x + random.nextDouble(), y + random.nextDouble(), z + random.nextDouble());
                if (random.nextInt(100) == 0) sound.play("portal_trigger", x + 0.5, y + 0.5, z + 0.5, 0.25f, 0.8f + random.nextFloat() * 0.4f);
            } else if (id == Block.LAVA.id && world.getBlock(x, y + 1, z) == 0 && random.nextInt(60) == 0) {
                particles.spawn("flame", x + random.nextDouble(), y + 1, z + random.nextDouble());
                particles.spawn("smoke", x + random.nextDouble(), y + 1.1, z + random.nextDouble());
            } else if (id == Block.BOOKSHELF.id && random.nextInt(16) == 0) {
                // Glyphs drift from bookshelves to a nearby enchanting table
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++)
                        for (int dy = -1; dy <= 0; dy++)
                            if (world.getBlock(x + dx, y + dy, z + dz) == Block.ENCHANTING_TABLE.id)
                                particles.spawn("enchant", x + 0.5 - dx * 0.3, y + 1.2, z + 0.5 - dz * 0.3);
            } else if (id == Block.LIT_FURNACE.id && random.nextInt(3) == 0) {
                int f = world.getMeta(x, y, z) & 3;
                double fx = x + 0.5 + mc.world.Shapes.DX[f] * 0.52, fz = z + 0.5 + mc.world.Shapes.DZ[f] * 0.52;
                particles.spawn("flame", fx, y + 0.3, fz);
                particles.spawn("smoke", fx, y + 0.4, fz);
            }
        }
    }

    // ------------------------------------------------------------------ sleeping

    /** Ticks spent in bed (0 = awake). After 100 ticks the night is skipped. */
    int sleepTicks;
    /** Camera: 0 first person, 1 third person behind, 2 third person in front. */
    int perspective;
    private double bedX, bedY, bedZ;

    void sleep(int x, int y, int z) {
        if (sleepTicks > 0) return;
        if (multiplayer != null || lanServer != null) {
            // Everyone would have to sleep to skip the night: in multiplayer beds only set the respawn point
            player.spawnX = x + 0.5; player.spawnY = y + 0.6; player.spawnZ = z + 0.5;
            hud.chat("Respawn point set");
            return;
        }
        long t = world.time % 24000;
        boolean night = t >= 12542 && t <= 23459;
        if (!night && !weather.raining) { hud.chat("You can only sleep at night or during thunderstorms"); return; }
        if (player.distanceSq(x + 0.5, y + 0.5, z + 0.5) > 9) { hud.chat("You may not rest now; the bed is too far away"); return; }
        for (Entity e : world.entities()) {
            if (e instanceof Mob m && m.type.hostile && !m.isDead() && Math.abs(m.x - x) < 8 && Math.abs(m.y - y) < 5 && Math.abs(m.z - z) < 8) {
                hud.chat("You may not rest now; there are monsters nearby");
                return;
            }
        }
        // Lie on the head half
        int meta = world.getMeta(x, y, z);
        if ((meta & 4) == 0) { x += mc.world.Shapes.DX[meta & 3]; z += mc.world.Shapes.DZ[meta & 3]; }
        bedX = x + 0.5; bedY = y + 0.5625; bedZ = z + 0.5;
        if (player.spawnX != bedX || player.spawnZ != bedZ) hud.chat("Respawn point set");
        player.spawnX = bedX; player.spawnY = bedY; player.spawnZ = bedZ;
        player.setPos(bedX, bedY, bedZ);
        player.motionX = player.motionY = player.motionZ = 0;
        sleepTicks = 1;
        interaction.breakProgress = 0;
    }

    private void tickSleep() {
        sleepTicks++;
        player.setPos(bedX, bedY, bedZ);
        player.eyeHeight = player.prevEyeHeight = 0.3f;
        if (input.down(GLFW_KEY_LEFT_SHIFT) && sleepTicks > 5) { wakeUp(); return; }
        if (sleepTicks >= 100) {
            world.time += 24000 - world.time % 24000;
            weather.raining = false;
            wakeUp();
        }
    }

    private void wakeUp() {
        sleepTicks = 0;
        player.setPos(bedX, bedY, bedZ);
    }

    private void onPlayerDeath() {
        deathHandled = true;
        sound.play("hurt", player.x, player.y + 1, player.z, 1, 0.7f);
        for (int i = 0; i < 20; i++) particles.spawn("poof", player.x + random.nextGaussian() * 0.3, player.y + 0.5, player.z + random.nextGaussian() * 0.3);
        if (!player.creative) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = player.inventory.slots[i];
                if (ItemStack.isEmpty(s)) continue;
                ItemEntity e = new ItemEntity(s);
                e.setPos(player.x, player.y + 1, player.z);
                double a = random.nextDouble() * Math.PI * 2, v = random.nextDouble() * 0.4;
                e.motionX = Math.cos(a) * v;
                e.motionZ = Math.sin(a) * v;
                e.motionY = 0.2;
                e.pickupDelay = 40;
                world.addEntity(e);
                player.inventory.slots[i] = null;
            }
            for (int i = 0; i < 4; i++) {
                ItemStack s = player.inventory.armor[i];
                if (s != null) world.spawnItem(player.x, player.y + 1, player.z, s);
                player.inventory.armor[i] = null;
            }
            XpOrbEntity.spawn(world, player.x, player.y + 0.5, player.z, Math.min(100, player.xpLevel * 7));
            player.xpLevel = 0;
            player.xpProgress = 0;
            player.xpTotal = 0;
        }
        hud.chat("Player " + (player.deathCause == null ? "died" : player.deathCause.message));
        setScreen(Screen.DEATH);
    }

    private void pickupItems() {
        if (multiplayer != null) return; // the server hands items over
        var box = player.box();
        box.minX -= 1; box.maxX += 1; box.minY -= 0.5; box.maxY += 0.5; box.minZ -= 1; box.maxZ += 1;
        for (Entity e : world.entities()) {
            if (e.removed) continue;
            if (e instanceof ItemEntity it && it.pickupDelay == 0 && box.intersects(e.box())) {
                int before = it.stack.count;
                ItemStack left = player.inventory.add(it.stack);
                if (left.count < before) sound.play("pop", e.x, e.y, e.z, 0.2f, (random.nextFloat() - random.nextFloat()) * 1.4f * 0.7f + 2f);
                if (left.count <= 0) it.remove();
            } else if (e instanceof XpOrbEntity orb && orb.pickupDelay == 0 && box.intersects(e.box())) {
                orb.remove();
                int before = player.xpLevel;
                player.addXp(orb.value);
                if (player.xpLevel > before && player.xpLevel % 5 == 0) sound.play("levelup", player.x, player.y, player.z, 0.75f, 1);
                else sound.play("orb", player.x, player.y, player.z, 0.1f, 0.5f + random.nextFloat() * 0.9f);
            } else if (e instanceof ArrowEntity a && a.tryPickup(player)) {
                sound.play("pop", e.x, e.y, e.z, 0.2f, 1.6f);
            }
        }
    }

    private void checkLoaded() {
        int pcx = (int) Math.floor(player.x) >> 4, pcz = (int) Math.floor(player.z) >> 4;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = world.getChunk(pcx + dx, pcz + dz);
                if (c == null || c.mesh == null) return;
            }
        if (spawnNeeded) {
            Chunk c = world.getChunk(pcx, pcz);
            int lx = (int) Math.floor(player.x) & 15, lz = (int) Math.floor(player.z) & 15;
            int top = c.topSolid(lx, lz);
            player.setPos(player.x, top + 1, player.z);
            player.spawnX = player.x;
            player.spawnY = player.y;
            player.spawnZ = player.z;
            spawnNeeded = false;
            saveWorld();
        }
        closeScreen();
        loadedTick = ticks;
    }

    /** True once the chunks around the player are generated and meshed. */
    private boolean areaReady() {
        int pcx = (int) Math.floor(player.x) >> 4, pcz = (int) Math.floor(player.z) >> 4;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = world.getChunk(pcx + dx, pcz + dz);
                if (c == null || c.mesh == null) return false;
            }
        return true;
    }

    /** Scripted commands from --cmd run one per tick; "/wait" pauses until nearby chunks are loaded, "/sleep N" waits N ticks. */
    private int scriptDelay;

    private void runStartupCommands() {
        if (startupCommands.isEmpty()) return;
        if (scriptDelay > 0) { scriptDelay--; return; }
        String c = startupCommands.get(0);
        if (c.equals("/wait") || c.equals("wait")) {
            if (!areaReady()) return;
        } else if (c.startsWith("/sleep")) {
            scriptDelay = Integer.parseInt(c.substring(7).trim());
        } else {
            commands.run(c);
        }
        startupCommands.remove(0);
        loadedTick = ticks;
    }

    // ------------------------------------------------------------------ world listener

    @Override
    public void playSound(String name, double x, double y, double z, float volume, float pitch) {
        sound.play(name, x, y, z, volume, pitch);
    }

    @Override
    public void addParticle(String type, double x, double y, double z) {
        particles.spawn(type, x, y, z);
    }

    @Override
    public void blockBroken(int x, int y, int z, Block block, int meta) {
        sound.dig(block, x + 0.5, y + 0.5, z + 0.5);
        particles.spawnBreak(world, x, y, z, block, tintOf(block, x, z), lightAt(x, y + 1, z));
    }

    int tintOf(Block b, int x, int z) {
        Chunk c = world.getChunk(x >> 4, z >> 4);
        if (c == null) return 0xFFFFFF;
        int col = (z & 15) * 16 + (x & 15);
        return switch (b.tint) {
            case GRASS -> b == Block.GRASS ? 0xFFFFFF : c.grassColor[col];
            case FOLIAGE -> c.foliageColor[col];
            case BIRCH -> 0x80a755;
            case SPRUCE -> 0x619961;
            default -> 0xFFFFFF;
        };
    }

    /** Brightness (0-1, display space) for particles, from the computed light at a block. */
    float lightAt(int x, int y, int z) {
        return (float) Math.min(1, Math.pow(lightValue(x + 0.5, y + 0.5, z + 0.5) / 1.6, 1 / 2.2));
    }

    /** Linear light multiplier at a position, matching the terrain shader's sky + torch light roughly. */
    float lightValue(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        float sky = world.getSkyLight(bx, by, bz) / 15f, blk = world.getBlockLight(bx, by, bz) / 15f;
        float b = blk * blk;
        if (renderer.nightVision > 0) b = Math.max(b, renderer.nightVision * 0.45f);
        if (world.dimension == Dimension.NETHER) return 1.8f * b * b + 0.3f * b + 0.28f;
        return renderer.daylight * 1.4f * sky * sky + 1.8f * b * b + 0.3f * b + 0.03f;
    }

    // ------------------------------------------------------------------ frame

    private void frame(float pt, double dt) {
        if (input.resized) {
            window.updateSize();
            input.resized = false;
        }
        handleInput();
        glViewport(0, 0, window.width, window.height);
        if (lod.world() != world) {
            Path dir = world != null && !inMenu && worldDir != null ? dimensionDir(world.dimension).resolve("lod") : null;
            lod.setWorld(world, dir);
        }
        if (world == null) {
            // Connecting to a server, or no panorama: menus over a plain background
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClearColor(0.1f, 0.08f, 0.06f, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            gui.begin(window.width, window.height);
            menus.render(gui, true);
            return;
        }
        if (inMenu) {
            // Slowly turn the panorama camera
            player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
            player.prevYaw = player.yaw;
            player.yaw += (float) dt * 2.2f;
            player.prevPitch = player.pitch;
            player.prevEyeHeight = player.eyeHeight;
            world.update(player.x, player.z, Math.min(options.renderDistance, 8), 4_000_000L);
        } else {
            world.update(player.x, player.z, options.renderDistance, 6_000_000L);
            if (multiplayer == null) world.updateExtraCenters(lanServer != null ? lanServer.playerCenters() : null);
            if (screen != Screen.LOADING && screen != Screen.PAUSE) interaction.pick(pt);
        }

        if (screen == Screen.LOADING) {
            renderLoading();
            return;
        }

        float fovNow = options.fov * (prevFovMod + (fovMod - prevFovMod) * pt);
        double eyeY = player.interpY(pt) + player.eyeHeight;
        int eyeBlock = world.getBlock((int) Math.floor(player.interpX(pt)), (int) Math.floor(eyeY), (int) Math.floor(player.interpZ(pt)));
        boolean underwater = eyeBlock == Block.WATER.id && (eyeY - Math.floor(eyeY)) < 0.875;
        boolean inLava = eyeBlock == Block.LAVA.id;
        if (underwater) fovNow *= 0.92f;

        renderer.brightness = options.gamma;
        Effect.Instance nv = player.effects.get(Effect.NIGHT_VISION);
        renderer.nightVision = nv == null ? 0 : nv.duration > 200 ? 1 : 0.7f + 0.3f * (float) Math.sin(nv.duration * 0.1);
        renderer.shadows = options.shadows;
        renderer.clouds = options.clouds;
        renderer.nether = world.dimension == Dimension.NETHER;
        float rain = renderer.nether ? 0 : weather.rain(pt);
        renderer.updateEnvironment(world, pt, underwater, inLava, options.renderDistance, rain);
        // Distant terrain pushes the fog out to its own range and thins the haze
        int lodChunks = inMenu ? Math.max(options.lodDistance, 0) : options.lodDistance;
        boolean lodOn = lodChunks > options.renderDistance && !renderer.nether && !underwater && !inLava;
        if (lodOn) {
            renderer.fogEnd = lodChunks * 16f;
            renderer.fogDensity = Math.min(0.0025f, 1.1f / renderer.fogEnd);
        } else renderer.fogDensity = 0.0025f;
        renderer.setupCamera(player, pt, fovNow, window.width, window.height, options.renderDistance, options.viewBobbing, perspective, world);
        sound.listener(renderer.camX, renderer.camY, renderer.camZ, player.yaw, player.pitch);
        int ex = (int) Math.floor(renderer.camX), ey = (int) Math.floor(renderer.camY), ez = (int) Math.floor(renderer.camZ);
        float skyAtEye = world.getSkyLight(ex, ey, ez) / 15f, blkAtEye = world.getBlockLight(ex, ey, ez) / 15f;
        sound.setRain(rain * skyAtEye);

        // Eye adaptation: expose for how much light reaches the camera
        float env = Math.max(Math.max(renderer.daylight * skyAtEye * skyAtEye, 0.35f * blkAtEye * blkAtEye), renderer.nether ? 0.05f : 0.015f);
        float targetExposure = Math.max(0.3f, Math.min(2.4f, 0.3f / (float) Math.pow(env, 0.6)));
        post.exposure += (targetExposure - post.exposure) * (float) Math.min(1, dt * 1.5);

        renderer.renderShadows(world);
        post.resize(window.width, window.height);
        post.beginScene();
        if (renderer.nether) glClearColor(WorldRenderer.NETHER_FOG[0], WorldRenderer.NETHER_FOG[1], WorldRenderer.NETHER_FOG[2], 1);
        else glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        renderer.renderSky();
        if (lodOn && lod.render(renderer, inMenu ? Math.min(options.renderDistance, 8) : options.renderDistance, lodChunks, renderer.fogDensity))
            glClear(GL_DEPTH_BUFFER_BIT);
        renderer.renderOpaque(world);
        entityRenderer.firstPerson = perspective == 0 ? player : null;
        entityRenderer.localPlayer = player;
        entityRenderer.render(world, renderer, pt, this::lightValue);
        for (Player np : world.networkPlayers())
            entityRenderer.renderOtherPlayer(np, renderer, pt, lightValue(np.x, np.y + 1, np.z));
        lastPt = pt;
        if (perspective != 0 && sleepTicks == 0 && !inMenu) {
            float sw = interaction.prevSwing + (interaction.swing - interaction.prevSwing) * pt;
            ItemStack heldStack = player.inventory.held();
            entityRenderer.renderPlayer(player, renderer, pt, lightValue(player.x, player.y + 1, player.z), sw,
                    ItemStack.isEmpty(heldStack) ? null : heldStack.item);
        }
        renderer.renderParticles(particles, player, pt);
        if (!renderer.nether) weather.render(renderer, world, pt);
        if (!hideGui && !inMenu) renderer.renderSelection(world, interaction.hit, interaction.breakProgress);
        post.copyDepth();
        renderer.renderTranslucent(post.depthCopy.depth);

        if (!hideGui && !inMenu && !player.isDead() && perspective == 0) {
            float sw = interaction.prevSwing + (interaction.swing - interaction.prevSwing) * pt;
            float eq = interaction.prevEquip + (interaction.equip - interaction.prevEquip) * pt;
            float br = lightValue(renderer.camX, renderer.camY, renderer.camZ);
            if (player.inLava) br = 3;
            float use = interaction.useType == 2 ? interaction.bowPower() : (interaction.useTicks + pt) / 32f;
            ItemStack shown = player.inventory.held();
            renderer.heldMeta = shown != null ? shown.damage : 0;
            renderer.renderHeldItem(interaction.shownItem, itemRenderer, sw, eq, br, (float) window.width / window.height,
                    interaction.useType, use, ticks + pt);
        }
        float damageFlash = player.hurtTime > 0 ? (player.hurtTime - pt) / 10f : 0;
        post.finish(window.width, window.height, renderer.time, underwater || inLava, Math.max(damageFlash, 0));
        renderGui();

        if (screenshotAfter != null && inMenu && startupCommands.isEmpty() && ticks > 100 && ((world.pendingJobs() == 0 && !lod.busy()) || ticks > 400)) {
            screenshot(Path.of(screenshotAfter));
            running = false;
            return;
        }
        if (screenshotAfter != null && startupCommands.isEmpty() && ticks - loadedTick > 60 && ((world.pendingJobs() == 0 && !lod.busy()) || ticks - loadedTick > 600) && ticks % 20 == 0) {
            screenshot(Path.of(screenshotAfter));
            running = false;
        }
    }

    private float lastPt;

    /** Names above other players' heads (seen through walls unless they sneak, like Minecraft). */
    private void renderNameTags() {
        List<Player> others = new ArrayList<>(world.networkPlayers());
        for (Entity e : world.entities()) if (e instanceof Player op) others.add(op);
        for (Player o : others) {
            if (o.isDead() || o.removed) continue;
            double ex = o.interpX(lastPt) - renderer.camX, ey = o.interpY(lastPt) + (o.sneaking ? 1.9 : 2.1) - renderer.camY, ez = o.interpZ(lastPt) - renderer.camZ;
            double dist = Math.sqrt(ex * ex + ey * ey + ez * ez);
            if (dist > 64) continue;
            org.joml.Vector4f v = new org.joml.Vector4f((float) ex, (float) ey, (float) ez, 1);
            renderer.projView.transform(v);
            if (v.w <= 0.05f) continue;
            float sx = (v.x / v.w * 0.5f + 0.5f) * gui.width, sy = (1 - (v.y / v.w * 0.5f + 0.5f)) * gui.height;
            if (sx < -50 || sx > gui.width + 50 || sy < -20 || sy > gui.height + 20) continue;
            int w = gui.textWidth(o.name);
            gui.fill(sx - w / 2f - 2, sy - 1, w + 4, 10, 0x40000000);
            gui.centered(o.name, sx, sy, o.sneaking ? 0x60FFFFFF : 0xFFFFFFFF, false);
        }
    }


    private void renderGui() {
        gui.begin(window.width, window.height);
        if (player.portalTicks > 0 && screen != Screen.LOADING) {
            float a = Math.min(1, player.portalTicks / 80f);
            gui.fill(0, 0, gui.width, gui.height, (int) (a * a * 200) << 24 | 0x6a20c0);
        }
        if (sleepTicks > 0) {
            float a = Math.min(1, sleepTicks / 70f);
            gui.fill(0, 0, gui.width, gui.height, (int) (a * 230) << 24 | 0x0a0a14);
            gui.centered("Sleeping... (Shift to leave bed)", gui.width / 2, gui.height - 70, 0xFFE0E0E0);
        }
        if (inMenu) { menus.render(gui, false); return; }
        if (menus.isMenuScreen(screen)) { menus.render(gui, false); return; }
        if (!hideGui) {
            renderNameTags();
            hud.render(gui);
        }
        if (isContainer(screen)) screens.renderContainer(gui, input);
        else if (screen == Screen.PAUSE) screens.renderPause(gui, input);
        else if (screen == Screen.DEATH) screens.renderDeath(gui, input);
        if (showDebug && screen != Screen.NONE) {
            // Where the game thinks the cursor is (should sit under the system pointer)
            float mx = (float) (input.mouseX / gui.scale), my = (float) (input.mouseY / gui.scale);
            gui.fill(mx - 4, my - 0.5f, 9, 1, 0xFFFF3030);
            gui.fill(mx - 0.5f, my - 4, 1, 9, 0xFFFF3030);
            gui.text("Cursor " + (int) input.pointX + "," + (int) input.pointY + " pt / window " + input.pointsW + "x" + input.pointsH
                    + " pt / " + window.width + "x" + window.height + " px", 2, gui.height - 10, 0xFFFFFF60);
        }
    }

    private void renderLoading() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClearColor(0.12f, 0.09f, 0.07f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gui.begin(window.width, window.height);
        gui.fill(0, 0, gui.width, gui.height, 0xFF3a2a1d);
        gui.centered(loadingMessage, gui.width / 2, gui.height / 2 - 20, 0xFFFFFFFF);
        int total = 0, done = 0;
        int pcx = (int) Math.floor(player.x) >> 4, pcz = (int) Math.floor(player.z) >> 4;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                total++;
                Chunk c = world.getChunk(pcx + dx, pcz + dz);
                if (c != null && c.mesh != null) done++;
            }
        float p = (float) done / total;
        float w = 200, x = gui.width / 2 - w / 2, y = gui.height / 2;
        gui.fill(x - 1, y - 1, w + 2, 6, 0xFF808080);
        gui.fill(x, y, w, 4, 0xFF000000);
        gui.fill(x, y, w * p, 4, 0xFF80FF80);
        gui.centered(world.loadedChunkCount() + " chunks", gui.width / 2, y + 14, 0xFFA0A0A0);
    }

    // ------------------------------------------------------------------ input

    private boolean cursorCaptured;

    private void handleInput() {
        // Like Minecraft: losing focus while playing opens the pause menu and frees the mouse
        if (input.focusLost && screen == Screen.NONE) setScreen(Screen.PAUSE);
        boolean captured = screen == Screen.NONE;
        if (captured != cursorCaptured || input.focusGained) {
            // Re-applying after a focus change makes macOS grab the pointer again reliably
            if (input.focusGained) glfwSetInputMode(window.handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            glfwSetInputMode(window.handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
            cursorCaptured = captured;
            input.dx = input.dy = 0;
        }
        if (input.pressed(GLFW_KEY_F11)) window.toggleFullscreen();
        if (input.pressed(GLFW_KEY_F2)) screenshot(null);
        switch (screen) {
            case NONE -> handleGameInput();
            case PAUSE -> { if (input.pressed(GLFW_KEY_ESCAPE)) closeScreen(); }
            case INVENTORY, CRAFTING, FURNACE, CHEST, CREATIVE, TRADING, ENCHANTING, ANVIL, BREWING -> {
                if (input.pressed(GLFW_KEY_ESCAPE) || input.pressed(GLFW_KEY_E)) closeScreen();
            }
            case CHAT -> handleChatInput();
            default -> { }
        }
    }

    private void handleGameInput() {
        float sens = options.sensitivity * 0.6f + 0.2f;
        float f = sens * sens * sens * 8f * 0.15f;
        player.yaw += (float) input.dx * f;
        player.pitch = Math.max(-90, Math.min(90, player.pitch + (float) input.dy * f));

        var inv = player.inventory;
        for (int i = 0; i < 9; i++) if (input.pressed(GLFW_KEY_1 + i)) inv.selected = i;
        if (input.scroll != 0) inv.selected = Math.floorMod(inv.selected - (int) Math.signum(input.scroll), 9);

        if (input.pressed(GLFW_KEY_ESCAPE)) { setScreen(Screen.PAUSE); return; }
        if (input.pressed(GLFW_KEY_E)) { screens.openInventory(); return; }
        if (input.pressed(GLFW_KEY_T) || input.pressed(GLFW_KEY_SLASH)) {
            boolean slash = input.pressed(GLFW_KEY_SLASH);
            setScreen(Screen.CHAT);
            chatInput.setLength(0);
            if (slash) chatInput.append('/');
            input.typed.setLength(0);
            return;
        }
        if (input.pressed(GLFW_KEY_Q)) interaction.drop(input.down(GLFW_KEY_LEFT_CONTROL));
        if (input.pressed(GLFW_KEY_F3)) showDebug = !showDebug;
        if (input.pressed(GLFW_KEY_F1)) hideGui = !hideGui;
        if (input.pressed(GLFW_KEY_F5)) perspective = (perspective + 1) % 3;
        if (input.pressed(GLFW_KEY_F4)) setCreative(!player.creative);
        if (input.pressed(GLFW_KEY_SPACE) && player.creative) {
            if (ticks - lastSpaceTap < 7) { player.flying = !player.flying; lastSpaceTap = -100; }
            else lastSpaceTap = ticks;
        }
        if (input.pressed(GLFW_KEY_W)) {
            if (ticks - lastForwardTap < 7) sprintKey = true;
            lastForwardTap = ticks;
        }
        if (player.isDead()) return;
        if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) interaction.attackClick();
        if (input.clicked(GLFW_MOUSE_BUTTON_RIGHT)) interaction.useClick();
        if (input.clicked(GLFW_MOUSE_BUTTON_MIDDLE)) interaction.pickBlock();
    }

    private void handleChatInput() {
        for (int i = 0; i < input.typed.length(); i++) {
            char c = input.typed.charAt(i);
            if (c >= 32 && c < 127 && chatInput.length() < 100) chatInput.append(c);
        }
        if (input.pressed(GLFW_KEY_BACKSPACE) && chatInput.length() > 0) chatInput.setLength(chatInput.length() - 1);
        if (input.pressed(GLFW_KEY_ESCAPE)) closeScreen();
        if (input.pressed(GLFW_KEY_ENTER) || input.pressed(GLFW_KEY_KP_ENTER)) {
            String msg = chatInput.toString().trim();
            closeScreen();
            if (!msg.isEmpty()) {
                if (multiplayer != null) {
                    if (msg.startsWith("/") && !Commands.isServerCommand(msg)) commands.run(msg);
                    else multiplayer.chat(msg);
                } else if (msg.startsWith("/")) commands.run(msg);
                else if (lanServer != null) lanServer.broadcastChat("<" + player.name + "> " + msg);
                else hud.chat("<" + player.name + "> " + msg);
            }
        }
    }

    // ------------------------------------------------------------------ screenshots

    private void screenshot(Path target) {
        int w = window.width, h = window.height;
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 3);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buf);
        try {
            Path file = target;
            if (file == null) {
                Path dir = gameDir.resolve("screenshots");
                Files.createDirectories(dir);
                file = dir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")) + ".png");
            }
            STBImageWrite.stbi_flip_vertically_on_write(true);
            STBImageWrite.stbi_write_png(file.toString(), w, h, 3, buf, w * 3);
            hud.chat("Saved screenshot as " + file.getFileName());
        } catch (Exception e) {
            hud.chat("Couldn't save screenshot: " + e.getMessage());
        }
    }
}
