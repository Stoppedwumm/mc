package mc.client;

import mc.entity.Player;
import mc.render.*;
import mc.util.AABB;
import mc.util.RayCast;
import mc.world.Block;
import mc.world.Chunk;
import mc.world.World;
import mc.world.WorldStorage;
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

public final class Game {
    private static final double TICK = 0.05;

    private enum Screen { LOADING, NONE, PAUSE, INVENTORY, CHAT }

    private final Path gameDir, worldDir;
    private final Options options;
    private Options.Level level;
    private Window window;
    private Input input;
    private Sound sound;
    private WorldRenderer renderer;
    private Gui gui;
    private World world;
    private final Player player = new Player();
    private final Particles particles = new Particles();

    private Screen screen = Screen.LOADING;
    private boolean running = true;
    private boolean showDebug, hideGui;
    private final int[] hotbar = {Block.GRASS.id, Block.DIRT.id, Block.STONE.id, Block.COBBLESTONE.id, Block.PLANKS.id,
            Block.OAK_LOG.id, Block.GLASS.id, Block.TORCH.id, Block.BRICKS.id};
    private int selected;

    private RayCast.Hit hit;
    private float breakProgress;
    private int breakX, breakY, breakZ = Integer.MIN_VALUE;
    private int breakDelay, placeDelay, hitSoundTimer;
    private float swing, prevSwing;
    private int swingTicks = -1;
    private float equip, prevEquip;
    private int lastHeld;
    private long ticks;
    private long lastSpaceTap = -100, lastForwardTap = -100;
    private boolean sprintKey;
    private float fovMod = 1, prevFovMod = 1;
    private boolean spawnNeeded;
    private int nameShownFor = -1;
    private long nameShownAt;
    private long loadedTick = Long.MAX_VALUE / 2;

    private int fps, frameCounter;
    private double fpsTimer;
    private final List<String[]> chatLines = new ArrayList<>();
    private final List<Long> chatTimes = new ArrayList<>();
    private final StringBuilder chatInput = new StringBuilder();
    private final Random random = new Random();
    private final long seedArg;
    private final boolean seedGiven;
    private final int rdArg;
    private final String screenshotAfter;
    private final List<String> startupCommands = new ArrayList<>();

    public Game(Path gameDir, String worldName, Long seed, int renderDistance, String screenshotAfter, List<String> commands) {
        this(gameDir, worldName, seed, renderDistance, screenshotAfter);
        startupCommands.addAll(commands);
    }

    public Game(Path gameDir, String worldName, Long seed, int renderDistance, String screenshotAfter) {
        this.gameDir = gameDir;
        this.worldDir = gameDir.resolve("saves").resolve(worldName);
        this.options = Options.load(gameDir.resolve("options.json"));
        this.seedGiven = seed != null;
        this.seedArg = seed != null ? seed : new Random().nextLong();
        this.rdArg = renderDistance;
        this.screenshotAfter = screenshotAfter;
    }

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
            input.endFrame();
            frameCounter++;
            fpsTimer += dt;
            if (fpsTimer >= 1) { fps = frameCounter; frameCounter = 0; fpsTimer -= 1; }
        }
        shutdown();
    }

    private void init() throws Exception {
        Files.createDirectories(worldDir);
        if (rdArg > 0) options.renderDistance = rdArg;
        window = new Window("Minecraft-like (LWJGL)", 1280, 720);
        window.setVsync(options.vsync);
        input = new Input(window);
        sound = new Sound();
        sound.volume = options.volume;

        level = Options.Level.load(worldDir.resolve("level.json"));
        if (level == null) {
            level = new Options.Level();
            level.seed = seedArg;
        } else if (seedGiven && level.seed != seedArg) {
            System.out.println("World already exists with seed " + level.seed + "; ignoring --seed");
        }
        world = new World(level.seed, new WorldStorage(worldDir));
        world.time = level.time;
        renderer = new WorldRenderer(level.seed);
        renderer.gamma = options.gamma;
        gui = new Gui(renderer, new Font());

        if (level.spawned) {
            player.setPos(level.x, level.y, level.z);
            player.yaw = level.yaw;
            player.pitch = level.pitch;
            player.flying = level.flying;
            player.creative = level.creative;
            if (level.hotbar != null && level.hotbar.length == 9) System.arraycopy(level.hotbar, 0, hotbar, 0, 9);
            selected = level.selected;
        } else {
            int[] spawn = findSpawn(world.generator);
            player.setPos(spawn[0] + 0.5, 120, spawn[1] + 0.5);
            spawnNeeded = true;
        }
        lastHeld = hotbar[selected];
        System.out.println("World seed: " + level.seed);
    }

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
        saveWorld();
        options.save(gameDir.resolve("options.json"));
        world.shutdown();
        sound.destroy();
        window.destroy();
    }

    private void saveWorld() {
        if (screen == Screen.LOADING && spawnNeeded) return;
        world.saveAll();
        level.time = world.time;
        level.x = player.x; level.y = player.y; level.z = player.z;
        level.yaw = player.yaw; level.pitch = player.pitch;
        level.flying = player.flying;
        level.creative = player.creative;
        level.hotbar = hotbar.clone();
        level.selected = selected;
        level.spawned = true;
        level.save(worldDir.resolve("level.json"));
    }

    // ------------------------------------------------------------------ ticking

    private void tick() {
        ticks++;
        if (screen == Screen.LOADING) {
            checkLoaded();
            return;
        }
        runStartupCommands();
        if (screen == Screen.PAUSE) return;
        world.time++;

        boolean play = screen == Screen.NONE;
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
        boolean sprint = play && (input.down(GLFW_KEY_LEFT_CONTROL) || sprintKey) && forward > 0;
        if (forward <= 0) sprintKey = false;
        player.tick(world, forward, strafe, jump, sneak, sprint);

        Block under = Block.get(world.getBlock((int) Math.floor(player.x), (int) Math.floor(player.y - 0.2), (int) Math.floor(player.z)));
        if (player.stepThisTick && under != Block.AIR) sound.step(under, player.x, player.y, player.z);
        if (player.landedThisTick && under != Block.AIR) sound.step(under, player.x, player.y, player.z);
        if (player.splashThisTick) sound.splash();

        prevFovMod = fovMod;
        float targetFov = 1;
        if (player.flying) targetFov *= 1.1f;
        if (player.sprinting) targetFov *= 1.15f;
        fovMod += (targetFov - fovMod) * 0.5f;

        prevSwing = swing;
        if (swingTicks >= 0) {
            swingTicks++;
            if (swingTicks >= 6) { swingTicks = -1; swing = 0; prevSwing = 0; }
            else swing = swingTicks / 6f;
        }
        prevEquip = equip;
        if (hotbar[selected] != lastHeld) {
            equip = Math.min(1, equip + 0.4f);
            if (equip >= 1) lastHeld = hotbar[selected];
        } else equip = Math.max(0, equip - 0.4f);

        if (breakDelay > 0) breakDelay--;
        if (placeDelay > 0) placeDelay--;
        if (play) tickInteraction();
        particles.tick(world);

        if (ticks % 6000 == 0) saveWorld();
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

    /** Scripted commands from --cmd run one per tick; "/wait" pauses until nearby chunks are loaded. */
    private void runStartupCommands() {
        if (startupCommands.isEmpty()) return;
        String c = startupCommands.get(0);
        if (c.equals("/wait") || c.equals("wait")) {
            if (!areaReady()) return;
        } else {
            command(c.replaceFirst("^/", "").split("\\s+"));
        }
        startupCommands.remove(0);
        loadedTick = ticks;
    }

    private void tickInteraction() {
        boolean attack = input.button(GLFW_MOUSE_BUTTON_LEFT);
        if (!attack || hit == null) {
            breakProgress = 0;
            breakZ = Integer.MIN_VALUE;
        }
        if (attack && hit != null && breakDelay == 0) {
            Block b = Block.get(world.getBlock(hit.x, hit.y, hit.z));
            if (hit.x != breakX || hit.y != breakY || hit.z != breakZ) {
                breakX = hit.x; breakY = hit.y; breakZ = hit.z;
                breakProgress = 0;
            }
            startSwing();
            if (player.creative || b.hardness == 0) {
                breakBlock(hit.x, hit.y, hit.z);
                breakDelay = player.creative ? 5 : 2;
            } else if (b.hardness > 0) {
                float speed = 1f / (b.hardness * 15f);
                if (player.inWater && !player.flying) speed /= 5;
                if (!player.onGround && !player.flying) speed /= 5;
                breakProgress += speed;
                if (hitSoundTimer++ % 4 == 0) {
                    sound.hit(b, hit.x + 0.5, hit.y + 0.5, hit.z + 0.5);
                    particles.spawnHit(hit.x, hit.y, hit.z, hit.nx, hit.ny, hit.nz, b, tintOf(b, hit.x, hit.z), lightAt(hit.x + hit.nx, hit.y + hit.ny, hit.z + hit.nz));
                }
                if (breakProgress >= 1) {
                    breakBlock(hit.x, hit.y, hit.z);
                    breakProgress = 0;
                    breakDelay = 5;
                }
            }
        }
        if (input.button(GLFW_MOUSE_BUTTON_RIGHT) && placeDelay == 0) place();
    }

    private void startSwing() {
        if (swingTicks < 0 || swingTicks >= 3) swingTicks = 0;
    }

    private int tintOf(Block b, int x, int z) {
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

    /** Rough brightness for particles and the held item: open sky vs enclosed. */
    private float lightAt(int x, int y, int z) {
        for (int yy = y; yy < Chunk.HEIGHT; yy++) {
            if (Block.get(world.getBlock(x, yy, z)).opaque) return 0.35f + 0.1f * renderer.daylight;
        }
        return Math.max(0.3f, renderer.daylight);
    }

    private void breakBlock(int x, int y, int z) {
        int id = world.getBlock(x, y, z);
        Block b = Block.get(id);
        if (b == Block.AIR || (b == Block.BEDROCK && !player.creative)) return;
        world.setBlock(x, y, z, 0);
        sound.dig(b, x + 0.5, y + 0.5, z + 0.5);
        particles.spawnBreak(world, x, y, z, b, tintOf(b, x, z), lightAt(x, y + 1, z));
        onRemoved(x, y, z);
    }

    /** Minimal block updates: unsupported plants pop, sand falls, water flows into gaps. */
    private void onRemoved(int x, int y, int z) {
        int above = world.getBlock(x, y + 1, z);
        Block a = Block.get(above);
        if (a.model == Block.Model.CROSS || a == Block.TORCH || a == Block.CACTUS) {
            world.setBlock(x, y + 1, z, 0);
            particles.spawnBreak(world, x, y + 1, z, a, tintOf(a, x, z), lightAt(x, y + 1, z));
            onRemoved(x, y + 1, z);
        } else if (above == Block.SAND.id || above == Block.GRAVEL.id) {
            fall(x, y + 1, z);
        }
        int w = Block.WATER.id;
        if (world.getBlock(x + 1, y, z) == w || world.getBlock(x - 1, y, z) == w || world.getBlock(x, y, z + 1) == w
                || world.getBlock(x, y, z - 1) == w || world.getBlock(x, y + 1, z) == w) {
            world.setBlock(x, y, z, w);
            int yy = y - 1;
            while (yy > 0 && y - yy < 64 && (world.getBlock(x, yy, z) == 0 || Block.get(world.getBlock(x, yy, z)).model == Block.Model.CROSS)) {
                world.setBlock(x, yy, z, w);
                yy--;
            }
        }
    }

    private void fall(int x, int y, int z) {
        int id = world.getBlock(x, y, z);
        int yy = y;
        while (yy > 0) {
            Block below = Block.get(world.getBlock(x, yy - 1, z));
            if (!(below == Block.AIR || below.isLiquid() || below.model == Block.Model.CROSS)) break;
            yy--;
        }
        if (yy == y) return;
        world.setBlock(x, y, z, 0);
        world.setBlock(x, yy, z, id);
        onRemoved(x, y, z);
    }

    private void place() {
        if (hit == null) return;
        int id = hotbar[selected];
        Block b = Block.get(id);
        if (b == Block.AIR) return;
        Block target = Block.get(world.getBlock(hit.x, hit.y, hit.z));
        int x = hit.x, y = hit.y, z = hit.z;
        if (!target.replaceable || target.isLiquid()) {
            x += hit.nx; y += hit.ny; z += hit.nz;
        }
        if (y < 0 || y >= Chunk.HEIGHT) return;
        Block existing = Block.get(world.getBlock(x, y, z));
        if (!existing.replaceable) return;
        Block below = Block.get(world.getBlock(x, y - 1, z));
        if (b == Block.TORCH && !below.opaque) return;
        if (b.model == Block.Model.CROSS) {
            boolean soil = below == Block.GRASS || below == Block.DIRT || below == Block.COARSE_DIRT || below == Block.SNOWY_GRASS
                    || (b == Block.DEAD_BUSH && (below == Block.SAND || below == Block.TERRACOTTA))
                    || (b == Block.SUGAR_CANE && (below == Block.SUGAR_CANE || below == Block.SAND));
            if (!soil) return;
        }
        if (b.solid) {
            AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
            if (cell.intersects(player.box())) return;
        }
        world.setBlock(x, y, z, id);
        sound.dig(b, x + 0.5, y + 0.5, z + 0.5);
        startSwing();
        placeDelay = 4;
        if (id == Block.SAND.id || id == Block.GRAVEL.id) fall(x, y, z);
    }

    // ------------------------------------------------------------------ frame

    private void frame(float pt, double dt) {
        if (input.resized) {
            window.updateSize();
            input.resized = false;
        }
        handleInput();

        world.update(player.x, player.z, options.renderDistance, 6_000_000L);

        if (screen == Screen.NONE || screen == Screen.INVENTORY || screen == Screen.CHAT) {
            double ry = Math.toRadians(player.yaw), rp = Math.toRadians(player.pitch);
            double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
            double ex = player.interpX(pt), ey = player.interpY(pt) + player.eyeHeight, ez = player.interpZ(pt);
            hit = RayCast.cast(world, ex, ey, ez, dx, dy, dz, player.creative ? 5.0 : 4.5);
        }

        glViewport(0, 0, window.width, window.height);
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

        renderer.gamma = options.gamma;
        renderer.updateEnvironment(world, pt, underwater, inLava, options.renderDistance);
        renderer.setupCamera(player, pt, fovNow, window.width, window.height, options.renderDistance, options.viewBobbing);
        sound.listener(renderer.camX, renderer.camY, renderer.camZ, player.yaw, player.pitch);

        glClearColor(renderer.fogColor.x, renderer.fogColor.y, renderer.fogColor.z, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        if (!underwater && !inLava) renderer.renderSky();
        renderer.renderOpaque(world);
        renderer.renderParticles(particles, player, pt);
        if (!hideGui) renderer.renderSelection(world, hit, breakProgress);
        if (options.clouds) renderer.renderClouds(world, pt, options.renderDistance);
        renderer.renderTranslucent();

        if (!hideGui) {
            float sw = prevSwing + (swing - prevSwing) * pt;
            float eq = prevEquip + (equip - prevEquip) * pt;
            float br = lightAt((int) Math.floor(player.x), (int) Math.floor(eyeY), (int) Math.floor(player.z));
            if (player.inLava) br = 1;
            renderer.renderHeldItem(Block.get(lastHeld), sw, eq, br, (float) window.width / window.height, fovNow);
        }
        renderer.renderOverlay(underwater, inLava, window.width, window.height);
        renderGui();

        if (screenshotAfter != null && startupCommands.isEmpty() && ticks - loadedTick > 60 && world.pendingJobs() == 0 && ticks % 20 == 0) {
            screenshot(Path.of(screenshotAfter));
            running = false;
        }
    }

    private void handleInput() {
        boolean captured = screen == Screen.NONE;
        glfwSetInputMode(window.handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);

        if (input.pressed(GLFW_KEY_F11)) window.toggleFullscreen();
        if (input.pressed(GLFW_KEY_F2)) screenshot(null);

        switch (screen) {
            case NONE -> handleGameInput();
            case PAUSE -> { if (input.pressed(GLFW_KEY_ESCAPE)) closeScreen(); }
            case INVENTORY -> { if (input.pressed(GLFW_KEY_ESCAPE) || input.pressed(GLFW_KEY_E)) closeScreen(); }
            case CHAT -> handleChatInput();
            default -> { }
        }
    }

    private void closeScreen() {
        screen = Screen.NONE;
        input.releaseAll();
        input.dx = input.dy = 0;
    }

    private void handleGameInput() {
        float sens = options.sensitivity * 0.6f + 0.2f;
        float f = sens * sens * sens * 8f * 0.15f;
        player.yaw += (float) input.dx * f;
        player.pitch = Math.max(-90, Math.min(90, player.pitch + (float) input.dy * f));

        for (int i = 0; i < 9; i++) if (input.pressed(GLFW_KEY_1 + i)) selected = i;
        if (input.scroll != 0) selected = Math.floorMod(selected - (int) Math.signum(input.scroll), 9);

        if (input.pressed(GLFW_KEY_ESCAPE)) { screen = Screen.PAUSE; input.releaseAll(); }
        if (input.pressed(GLFW_KEY_E)) { screen = Screen.INVENTORY; input.releaseAll(); }
        if (input.pressed(GLFW_KEY_T) || input.pressed(GLFW_KEY_SLASH)) {
            screen = Screen.CHAT;
            chatInput.setLength(0);
            if (input.pressed(GLFW_KEY_SLASH)) chatInput.append('/');
            input.typed.setLength(0);
            input.releaseAll();
            return;
        }
        if (input.pressed(GLFW_KEY_F3)) showDebug = !showDebug;
        if (input.pressed(GLFW_KEY_F1)) hideGui = !hideGui;
        if (input.pressed(GLFW_KEY_F4)) setCreative(!player.creative);
        if (input.pressed(GLFW_KEY_SPACE) && player.creative) {
            if (ticks - lastSpaceTap < 7) { player.flying = !player.flying; lastSpaceTap = -100; }
            else lastSpaceTap = ticks;
        }
        if (input.pressed(GLFW_KEY_W)) {
            if (ticks - lastForwardTap < 7) sprintKey = true;
            lastForwardTap = ticks;
        }
        if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            breakDelay = 0;
            startSwing();
        }
        if (input.clicked(GLFW_MOUSE_BUTTON_RIGHT)) {
            placeDelay = 0;
            place();
        }
        if (input.clicked(GLFW_MOUSE_BUTTON_MIDDLE) && hit != null) {
            int id = world.getBlock(hit.x, hit.y, hit.z);
            if (id == Block.GRASS.id || id == Block.SNOWY_GRASS.id) id = id == Block.GRASS.id ? Block.GRASS.id : Block.SNOWY_GRASS.id;
            int found = -1;
            for (int i = 0; i < 9; i++) if (hotbar[i] == id) found = i;
            if (found >= 0) selected = found;
            else hotbar[selected] = id;
        }
    }

    private void setCreative(boolean c) {
        player.creative = c;
        if (!c) player.flying = false;
        chat("Game mode set to " + (c ? "Creative" : "Survival"));
    }

    // ------------------------------------------------------------------ chat & commands

    private void handleChatInput() {
        for (int i = 0; i < input.typed.length(); i++) {
            char c = input.typed.charAt(i);
            if (c >= 32 && c < 127 && chatInput.length() < 100) chatInput.append(c);
        }
        if (input.pressed(GLFW_KEY_BACKSPACE) && chatInput.length() > 0) chatInput.setLength(chatInput.length() - 1);
        if (input.pressed(GLFW_KEY_ESCAPE)) closeScreen();
        if (input.pressed(GLFW_KEY_ENTER) || input.pressed(GLFW_KEY_KP_ENTER)) {
            String msg = chatInput.toString().trim();
            if (!msg.isEmpty()) {
                if (msg.startsWith("/")) command(msg.substring(1).split("\\s+"));
                else chat("<Player> " + msg);
            }
            closeScreen();
        }
    }

    private void chat(String s) {
        chatLines.add(new String[]{s});
        chatTimes.add(ticks);
        while (chatLines.size() > 50) { chatLines.remove(0); chatTimes.remove(0); }
    }

    private void command(String[] a) {
        try {
            switch (a[0]) {
                case "time" -> {
                    if (a.length < 3) { chat("Usage: /time set|add <value>"); return; }
                    long v = switch (a[2]) {
                        case "day" -> 1000; case "noon" -> 6000; case "sunset" -> 12000; case "night" -> 13000; case "midnight" -> 18000; case "sunrise" -> 23000;
                        default -> Long.parseLong(a[2]);
                    };
                    if (a[1].equals("add")) world.time += v;
                    else world.time = (world.time / 24000) * 24000 + v;
                    chat("Set the time to " + world.time % 24000);
                }
                case "gamemode", "gm" -> {
                    String m = a.length > 1 ? a[1] : "";
                    if (m.startsWith("c") || m.equals("1")) setCreative(true);
                    else if (m.startsWith("s") || m.equals("0")) setCreative(false);
                    else chat("Usage: /gamemode creative|survival");
                }
                case "tp" -> {
                    double x = coord(a[1], player.x), y = coord(a[2], player.y), z = coord(a[3], player.z);
                    player.setPos(x, y, z);
                    player.motionX = player.motionY = player.motionZ = 0;
                    chat(String.format("Teleported to %.1f, %.1f, %.1f", x, y, z));
                }
                case "look" -> { player.yaw = Float.parseFloat(a[1]); player.pitch = Float.parseFloat(a[2]); }
                case "debug" -> showDebug = !showDebug;
                case "screen" -> screen = a[1].equals("pause") ? Screen.PAUSE : a[1].equals("inventory") ? Screen.INVENTORY : Screen.NONE;
                case "setblock" -> {
                    int x = (int) Math.floor(coord(a[1], player.x)), y = (int) Math.floor(coord(a[2], player.y)), z = (int) Math.floor(coord(a[3], player.z));
                    Block b = blockByName(a[4]);
                    if (b == null) { chat("Unknown block: " + a[4]); return; }
                    if (!world.isLoaded(x, z)) { chat("That position is not loaded"); return; }
                    world.setBlock(x, y, z, b.id);
                    chat("Changed the block at " + x + ", " + y + ", " + z);
                }
                case "fill" -> {
                    int x0 = (int) Math.floor(coord(a[1], player.x)), y0 = (int) Math.floor(coord(a[2], player.y)), z0 = (int) Math.floor(coord(a[3], player.z));
                    int x1 = (int) Math.floor(coord(a[4], player.x)), y1 = (int) Math.floor(coord(a[5], player.y)), z1 = (int) Math.floor(coord(a[6], player.z));
                    Block b = blockByName(a[7]);
                    if (b == null) { chat("Unknown block: " + a[7]); return; }
                    long vol = (long) (Math.abs(x1 - x0) + 1) * (Math.abs(y1 - y0) + 1) * (Math.abs(z1 - z0) + 1);
                    if (vol > 32768) { chat("Too many blocks in the specified area (" + vol + " > 32768)"); return; }
                    if (!world.isLoaded(x0, z0) || !world.isLoaded(x1, z1)) { chat("That position is not loaded"); return; }
                    for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) world.setBlock(x, y, z, b.id);
                    chat("Successfully filled " + vol + " blocks");
                }
                case "seed" -> chat("Seed: [" + world.seed + "]");
                case "fly" -> { player.flying = !player.flying; chat("Flying " + (player.flying ? "enabled" : "disabled")); }
                case "give" -> {
                    String name = String.join("_", java.util.Arrays.copyOfRange(a, 1, a.length));
                    Block b = blockByName(name);
                    if (b == null || b == Block.AIR) { chat("Unknown block: " + name); return; }
                    hotbar[selected] = b.id;
                    chat("Gave " + b.name);
                }
                case "rd", "renderdistance" -> { options.renderDistance = Math.max(2, Math.min(32, Integer.parseInt(a[1]))); chat("Render distance: " + options.renderDistance); }
                case "help" -> {
                    chat("/time set <day|night|n>, /gamemode <c|s>, /tp x y z, /give <block>");
                    chat("/setblock x y z <block>, /fill x1 y1 z1 x2 y2 z2 <block>, /fly, /seed, /rd <n>");
                }
                default -> chat("Unknown command. Type /help for help.");
            }
        } catch (Exception e) {
            chat("Invalid command arguments");
        }
    }

    private static Block blockByName(String name) {
        String n = name.replace("minecraft:", "").replace('_', ' ');
        for (Block b : Block.BY_ID) if (b != null && b.name.equalsIgnoreCase(n)) return b;
        if (n.equalsIgnoreCase("grass block")) return Block.GRASS;
        return null;
    }

    private static double coord(String s, double current) {
        if (s.startsWith("~")) return current + (s.length() > 1 ? Double.parseDouble(s.substring(1)) : 0);
        return Double.parseDouble(s);
    }

    // ------------------------------------------------------------------ GUI

    private void renderLoading() {
        glClearColor(0.12f, 0.09f, 0.07f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gui.begin(window.width, window.height);
        gui.fill(0, 0, gui.width, gui.height, 0xFF3a2a1d);
        gui.centered("Generating terrain...", gui.width / 2, gui.height / 2 - 20, 0xFFFFFFFF);
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

    private void renderGui() {
        gui.begin(window.width, window.height);
        double mx = input.mouseX / gui.scale, my = input.mouseY / gui.scale;
        if (!hideGui) {
            if (screen == Screen.NONE) gui.crosshair();
            renderHotbar();
            if (showDebug) renderDebug();
            renderChat();
        }
        switch (screen) {
            case PAUSE -> renderPause(mx, my);
            case INVENTORY -> renderInventory(mx, my);
            default -> { }
        }
    }

    private void renderHotbar() {
        float w = 182, h = 22;
        float x = gui.width / 2 - w / 2, y = gui.height - h - 1;
        gui.fill(x, y, w, h, 0xB0101010);
        gui.frame(x, y, w, h, 1, 0xFF5A5A5A);
        for (int i = 0; i < 9; i++) {
            float sx = x + 1 + i * 20;
            gui.frame(sx + 1, y + 1, 20, 20, 1, 0x80404040);
        }
        float sx = x - 1 + selected * 20;
        gui.frame(sx, y - 1, 24, 24, 2, 0xFFF0F0F0);
        gui.frame(sx + 2, y + 1, 20, 20, 1, 0xFF404040);
        for (int i = 0; i < 9; i++) {
            Block b = Block.get(hotbar[i]);
            if (b != Block.AIR) gui.icon(b, x + 3 + i * 20, y + 3, 16);
        }
        if (player.creative) gui.text("Creative", 2, gui.height - 10, 0xFFFFFF80);
        // Selected item name shows briefly after switching
        if (hotbar[selected] != nameShownFor) { nameShownFor = hotbar[selected]; nameShownAt = ticks; }
        long age = ticks - nameShownAt;
        if (age < 50) {
            int a = (int) (Math.min(1, (50 - age) / 10f) * 255);
            gui.centered(Block.get(hotbar[selected]).name, gui.width / 2, y - 12, a << 24 | 0xFFFFFF);
        }
    }

    private void renderDebug() {
        List<String> left = new ArrayList<>();
        left.add("Minecraft-like 1.0 (LWJGL " + org.lwjgl.Version.getVersion() + ")");
        left.add(fps + " fps  C: " + renderer.renderedChunks + "/" + world.loadedChunkCount() + "  jobs: " + world.pendingJobs());
        left.add("");
        left.add(String.format("XYZ: %.3f / %.5f / %.3f", player.x, player.y, player.z));
        int bx = (int) Math.floor(player.x), by = (int) Math.floor(player.y), bz = (int) Math.floor(player.z);
        left.add("Block: " + bx + " " + by + " " + bz);
        left.add("Chunk: " + (bx & 15) + " " + (by & 15) + " " + (bz & 15) + " in " + (bx >> 4) + " " + (by >> 4) + " " + (bz >> 4));
        String[] dirs = {"south (Towards positive Z)", "west (Towards negative X)", "north (Towards negative Z)", "east (Towards positive X)"};
        int facing = Math.floorMod(Math.round(player.yaw / 90f), 4);
        left.add("Facing: " + dirs[facing] + String.format(" (%.1f / %.1f)", wrap(player.yaw), player.pitch));
        left.add("Biome: " + world.biomeAt(bx, bz).displayName);
        long day = world.time / 24000, tod = world.time % 24000;
        left.add("Day " + day + ", time " + tod + String.format(" (daylight %.2f)", renderer.daylight));
        left.add("Mode: " + (player.creative ? "Creative" : "Survival") + (player.flying ? " (flying)" : "") + (player.sprinting ? " sprinting" : ""));
        if (hit != null) {
            left.add("");
            left.add("Targeted Block: " + hit.x + ", " + hit.y + ", " + hit.z);
            left.add(Block.get(world.getBlock(hit.x, hit.y, hit.z)).name);
        }
        float y = 2;
        for (String s : left) {
            if (!s.isEmpty()) {
                gui.fill(1, y - 1, gui.textWidth(s) + 2, 10, 0x90505050);
                gui.text(s, 2, y, 0xFFE0E0E0);
            }
            y += 10;
        }
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
        String[] right = {
                "Java: " + System.getProperty("java.version"),
                "Mem: " + (used * 100 / max) + "% " + used + "/" + max + "MB",
                "",
                "Display: " + window.width + "x" + window.height,
                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),
        };
        y = 2;
        for (String s : right) {
            if (s != null && !s.isEmpty()) {
                float w = gui.textWidth(s);
                gui.fill(gui.width - w - 3, y - 1, w + 2, 10, 0x90505050);
                gui.text(s, gui.width - w - 2, y, 0xFFE0E0E0);
            }
            y += 10;
        }
    }

    private static float wrap(float yaw) {
        float y = yaw % 360;
        if (y >= 180) y -= 360;
        if (y < -180) y += 360;
        return y;
    }

    private void renderChat() {
        boolean open = screen == Screen.CHAT;
        float y = gui.height - 48;
        int shown = 0;
        for (int i = chatLines.size() - 1; i >= 0 && shown < (open ? 20 : 10); i--) {
            long age = ticks - chatTimes.get(i);
            if (!open && age > 200) continue;
            float alpha = open ? 1 : Math.min(1, (200 - age) / 20f);
            int a = (int) (alpha * 255);
            gui.fill(2, y - 1, Math.max(gui.textWidth(chatLines.get(i)[0]) + 4, 1), 10, (int) (alpha * 0x80) << 24);
            gui.text(chatLines.get(i)[0], 4, y, (a << 24) | 0xFFFFFF);
            y -= 10;
            shown++;
        }
        if (open) {
            gui.fill(2, gui.height - 14, gui.width - 4, 12, 0x80000000);
            String caret = (ticks / 6) % 2 == 0 ? "_" : "";
            gui.text(chatInput + caret, 4, gui.height - 12, 0xFFFFFFFF);
        }
    }

    private void renderPause(double mx, double my) {
        gui.gradient(0, 0, gui.width, gui.height, 0xA0101010, 0xC0101010);
        gui.centered("Game Menu", gui.width / 2, 30, 0xFFFFFFFF);
        float bw = 200, bh = 20, x = gui.width / 2 - bw / 2;
        float y = gui.height / 4 + 8;
        boolean click = input.clicked(GLFW_MOUSE_BUTTON_LEFT);
        boolean rclick = input.clicked(GLFW_MOUSE_BUTTON_RIGHT);
        if (gui.button("Back to Game", x, y, bw, bh, mx, my) && click) { sound.click(); closeScreen(); return; }
        y += 24;
        if (gui.button("Render Distance: " + options.renderDistance + " chunks", x, y, bw, bh, mx, my) && (click || rclick)) {
            sound.click();
            int[] steps = {2, 4, 6, 8, 10, 12, 16, 20, 24, 32};
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= options.renderDistance) idx = i;
            options.renderDistance = steps[Math.floorMod(idx + (click ? 1 : -1), steps.length)];
        }
        y += 24;
        if (gui.button("FOV: " + (int) options.fov, x, y, 98, bh, mx, my) && (click || rclick)) {
            sound.click();
            options.fov += click ? 10 : -10;
            if (options.fov > 110) options.fov = 30;
            if (options.fov < 30) options.fov = 110;
        }
        if (gui.button("Brightness: " + (int) (options.gamma * 100) + "%", x + 102, y, 98, bh, mx, my) && (click || rclick)) {
            sound.click();
            options.gamma = Math.round((options.gamma + (click ? 0.25f : -0.25f)) * 4) / 4f;
            if (options.gamma > 1) options.gamma = 0;
            if (options.gamma < 0) options.gamma = 1;
        }
        y += 24;
        if (gui.button("View Bobbing: " + (options.viewBobbing ? "ON" : "OFF"), x, y, 98, bh, mx, my) && click) {
            sound.click();
            options.viewBobbing = !options.viewBobbing;
        }
        if (gui.button("Clouds: " + (options.clouds ? "Fancy" : "OFF"), x + 102, y, 98, bh, mx, my) && click) {
            sound.click();
            options.clouds = !options.clouds;
        }
        y += 24;
        if (gui.button("Sensitivity: " + (int) (options.sensitivity * 200) + "%", x, y, 98, bh, mx, my) && (click || rclick)) {
            sound.click();
            options.sensitivity = Math.round((options.sensitivity + (click ? 0.1f : -0.1f)) * 10) / 10f;
            if (options.sensitivity > 1) options.sensitivity = 0.1f;
            if (options.sensitivity < 0.1f) options.sensitivity = 1f;
        }
        if (gui.button("Mode: " + (player.creative ? "Creative" : "Survival"), x + 102, y, 98, bh, mx, my) && click) {
            sound.click();
            setCreative(!player.creative);
        }
        y += 36;
        if (gui.button("Save and Quit", x, y, bw, bh, mx, my) && click) {
            sound.click();
            running = false;
        }
        gui.centered("Left-click a setting to change it, right-click to go back", gui.width / 2, 44, 0xFF909090);
    }

    private void renderInventory(double mx, double my) {
        gui.fill(0, 0, gui.width, gui.height, 0x80101010);
        List<Block> blocks = new ArrayList<>();
        for (Block b : Block.BY_ID) if (b != null && b.inCreativeInventory) blocks.add(b);
        int cols = 9, rows = (blocks.size() + cols - 1) / cols;
        float slot = 18, pw = cols * slot + 14, ph = rows * slot + 30;
        float px = gui.width / 2 - pw / 2, py = gui.height / 2 - ph / 2 - 16;
        gui.fill(px, py, pw, ph, 0xF0C6C6C6);
        gui.frame(px, py, pw, ph, 1, 0xFF000000);
        gui.fill(px + 1, py + 1, pw - 2, 1, 0xFFFFFFFF);
        gui.text("Blocks", px + 8, py + 6, 0xFF404040);
        Block hover = null;
        for (int i = 0; i < blocks.size(); i++) {
            float sx = px + 7 + (i % cols) * slot, sy = py + 18 + (float) (i / cols) * slot;
            gui.fill(sx, sy, slot - 1, slot - 1, 0xFF8B8B8B);
            gui.fill(sx, sy, slot - 1, 1, 0xFF373737);
            gui.fill(sx, sy, 1, slot - 1, 0xFF373737);
            boolean h = mx >= sx && mx < sx + slot - 1 && my >= sy && my < sy + slot - 1;
            if (h) { hover = blocks.get(i); gui.fill(sx + 1, sy + 1, slot - 2, slot - 2, 0x80FFFFFF); }
            gui.icon(blocks.get(i), sx + 0.5f, sy + 0.5f, 16);
        }
        if (hover != null) {
            if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) { hotbar[selected] = hover.id; sound.click(); }
            for (int k = 0; k < 9; k++) if (input.pressed(GLFW_KEY_1 + k)) hotbar[k] = hover.id;
            float tw = gui.textWidth(hover.name);
            gui.fill((float) mx + 8, (float) my - 12, tw + 6, 12, 0xF0100010);
            gui.frame((float) mx + 8, (float) my - 12, tw + 6, 12, 1, 0xFF3a0080);
            gui.text(hover.name, (float) mx + 11, (float) my - 10, 0xFFFFFFFF);
        }
        if (input.scroll != 0) selected = Math.floorMod(selected - (int) Math.signum(input.scroll), 9);
        gui.centered("Click a block to put it in the selected hotbar slot (or hover + press 1-9)", gui.width / 2, py + ph + 4, 0xFFE0E0E0);
    }

    // ------------------------------------------------------------------ screenshots

    private void screenshot(Path target) {
        int w = window.width, h = window.height;
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 3);
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
            chat("Saved screenshot as " + file.getFileName());
        } catch (Exception e) {
            chat("Couldn't save screenshot: " + e.getMessage());
        }
    }
}
