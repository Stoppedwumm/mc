package mc.client;

import mc.entity.*;
import mc.item.Item;
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

    enum Screen { LOADING, NONE, PAUSE, INVENTORY, CRAFTING, FURNACE, CHEST, CREATIVE, CHAT, DEATH }

    private final Path gameDir, worldDir;
    final Options options;
    private Options.Level level;
    Window window;
    Input input;
    Sound sound;
    WorldRenderer renderer;
    PostProcess post;
    private Gui gui;
    private ItemRenderer itemRenderer;
    private final EntityRenderer entityRenderer = new EntityRenderer();
    final Weather weather = new Weather();
    final Particles particles = new Particles();
    World world;
    final Player player = new Player();
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
    private final Random random = new Random();
    private final long seedArg;
    private final boolean seedGiven;
    private final int rdArg;
    private final String screenshotAfter;
    private final List<String> startupCommands = new ArrayList<>();
    private boolean deathHandled;

    public Game(Path gameDir, String worldName, Long seed, int renderDistance, String screenshotAfter, List<String> commands) {
        this.gameDir = gameDir;
        this.worldDir = gameDir.resolve("saves").resolve(worldName);
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
        world.listener = this;
        world.setPlayer(player);
        renderer = new WorldRenderer(level.seed);
        post = new PostProcess(window.width, window.height);
        itemRenderer = new ItemRenderer();
        itemRenderer.setBlockPixels(new TextureGen().generate());
        entityRenderer.items = itemRenderer;
        gui = new Gui(renderer, new Font());
        gui.items = itemRenderer;
        interaction = new Interaction(this);
        screens = new Screens(this);
        hud = new Hud(this);
        commands = new Commands(this);

        if (level.spawned) {
            player.setPos(level.x, level.y, level.z);
            player.yaw = level.yaw;
            player.pitch = level.pitch;
            player.flying = level.flying;
            player.creative = level.creative;
            player.health = level.health;
            player.food = level.food;
            player.saturation = level.saturation;
            player.spawnX = level.spawnX;
            player.spawnY = level.spawnY;
            player.spawnZ = level.spawnZ;
            weather.raining = level.raining;
            if (level.weatherTimer > 0) weather.timer = level.weatherTimer;
            if (level.inventory != null) {
                for (int i = 0; i < Math.min(36, level.inventory.length); i++) {
                    int[] e = level.inventory[i];
                    if (e != null && Item.get(e[0]) != null) player.inventory.slots[i] = new ItemStack(Item.get(e[0]), e[1], e[2]);
                }
            } else if (level.hotbar != null) {
                for (int i = 0; i < 9 && i < level.hotbar.length; i++) if (Item.get(level.hotbar[i]) != null) player.inventory.slots[i] = new ItemStack(Item.get(level.hotbar[i]), 64);
            }
            player.inventory.selected = Math.max(0, Math.min(8, level.selected));
            if (level.blockEntities != null) loadBlockEntities(level.blockEntities);
        } else {
            int[] spawn = findSpawn(world.generator);
            player.setPos(spawn[0] + 0.5, 120, spawn[1] + 0.5);
            spawnNeeded = true;
        }
        System.out.println("World seed: " + level.seed);
    }

    private void loadBlockEntities(List<Options.BlockEntityData> list) {
        for (Options.BlockEntityData d : list) {
            BlockEntity be = d.type.equals("chest") ? new BlockEntity.Chest(d.x, d.y, d.z) : new BlockEntity.Furnace(d.x, d.y, d.z);
            for (int i = 0; i < Math.min(be.slots.length, d.slots.length); i++) {
                int[] e = d.slots[i];
                if (e != null && Item.get(e[0]) != null) be.slots[i] = new ItemStack(Item.get(e[0]), e[1], e[2]);
            }
            if (be instanceof BlockEntity.Furnace f) { f.burnTime = d.burnTime; f.burnTotal = d.burnTotal; f.cookTime = d.cookTime; }
            world.blockEntities.put(World.posKey(d.x, d.y, d.z), be);
        }
    }

    private static int[][] saveSlots(ItemStack[] slots) {
        int[][] out = new int[slots.length][];
        for (int i = 0; i < slots.length; i++) {
            ItemStack s = slots[i];
            if (!ItemStack.isEmpty(s)) out[i] = new int[]{s.item.id, s.count, s.damage};
        }
        return out;
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
        if (isContainer(screen)) screens.onClose();
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
        level.inventory = saveSlots(player.inventory.slots);
        level.hotbar = null;
        level.selected = player.inventory.selected;
        level.health = player.isDead() ? player.maxHealth : player.health;
        level.food = player.food;
        level.saturation = player.saturation;
        level.spawnX = player.spawnX; level.spawnY = player.spawnY; level.spawnZ = player.spawnZ;
        level.raining = weather.raining;
        level.weatherTimer = weather.timer;
        level.spawned = true;
        List<Options.BlockEntityData> bes = new ArrayList<>();
        for (BlockEntity be : world.blockEntities.values()) {
            Options.BlockEntityData d = new Options.BlockEntityData();
            d.x = be.x; d.y = be.y; d.z = be.z;
            d.type = be instanceof BlockEntity.Chest ? "chest" : "furnace";
            d.slots = saveSlots(be.slots);
            if (be instanceof BlockEntity.Furnace f) { d.burnTime = f.burnTime; d.burnTotal = f.burnTotal; d.cookTime = f.cookTime; }
            bes.add(d);
        }
        level.blockEntities = bes;
        level.save(worldDir.resolve("level.json"));
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
        if (player.spawnY < 0) {
            player.spawnX = level.x;
            player.spawnY = level.y;
            player.spawnZ = level.z;
        }
        player.respawn();
        deathHandled = false;
        closeScreen();
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

        weather.tick();
        double f = (world.time % 24000) / 24000.0 - 0.25;
        f = f - Math.floor(f);
        f = f + (1 - (Math.cos(f * Math.PI) + 1) / 2 - f) / 3;
        world.dayFactor = (float) Math.max(0, Math.min(1, Math.cos(f * Math.PI * 2) * 2 + 0.5)) * (1 - weather.rain(1) * 0.3f);

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
        particles.tick(world);
        if (ticks % 6000 == 0) saveWorld();
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
        }
        hud.chat("Player " + (player.deathCause == null ? "died" : player.deathCause.message));
        setScreen(Screen.DEATH);
    }

    private void pickupItems() {
        var box = player.box();
        box.minX -= 1; box.maxX += 1; box.minY -= 0.5; box.maxY += 0.5; box.minZ -= 1; box.maxZ += 1;
        for (Entity e : world.entities()) {
            if (e.removed) continue;
            if (e instanceof ItemEntity it && it.pickupDelay == 0 && box.intersects(e.box())) {
                int before = it.stack.count;
                ItemStack left = player.inventory.add(it.stack);
                if (left.count < before) sound.play("pop", e.x, e.y, e.z, 0.2f, (random.nextFloat() - random.nextFloat()) * 1.4f * 0.7f + 2f);
                if (left.count <= 0) it.remove();
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
    private int sleepTicks;

    private void runStartupCommands() {
        if (startupCommands.isEmpty()) return;
        if (sleepTicks > 0) { sleepTicks--; return; }
        String c = startupCommands.get(0);
        if (c.equals("/wait") || c.equals("wait")) {
            if (!areaReady()) return;
        } else if (c.startsWith("/sleep")) {
            sleepTicks = Integer.parseInt(c.substring(7).trim());
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
        return renderer.daylight * 1.4f * sky * sky + 1.8f * b * b + 0.3f * b + 0.03f;
    }

    // ------------------------------------------------------------------ frame

    private void frame(float pt, double dt) {
        if (input.resized) {
            window.updateSize();
            input.resized = false;
        }
        handleInput();

        world.update(player.x, player.z, options.renderDistance, 6_000_000L);
        if (screen != Screen.LOADING && screen != Screen.PAUSE) interaction.pick(pt);

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

        renderer.brightness = options.gamma;
        renderer.shadows = options.shadows;
        renderer.clouds = options.clouds;
        float rain = weather.rain(pt);
        renderer.updateEnvironment(world, pt, underwater, inLava, options.renderDistance, rain);
        renderer.setupCamera(player, pt, fovNow, window.width, window.height, options.renderDistance, options.viewBobbing);
        sound.listener(renderer.camX, renderer.camY, renderer.camZ, player.yaw, player.pitch);
        int ex = (int) Math.floor(renderer.camX), ey = (int) Math.floor(renderer.camY), ez = (int) Math.floor(renderer.camZ);
        float skyAtEye = world.getSkyLight(ex, ey, ez) / 15f, blkAtEye = world.getBlockLight(ex, ey, ez) / 15f;
        sound.setRain(rain * skyAtEye);

        // Eye adaptation: expose for how much light reaches the camera
        float env = Math.max(Math.max(renderer.daylight * skyAtEye * skyAtEye, 0.35f * blkAtEye * blkAtEye), 0.015f);
        float targetExposure = Math.max(0.3f, Math.min(2.4f, 0.3f / (float) Math.pow(env, 0.6)));
        post.exposure += (targetExposure - post.exposure) * (float) Math.min(1, dt * 1.5);

        renderer.renderShadows(world);
        post.resize(window.width, window.height);
        post.beginScene();
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        renderer.renderSky();
        renderer.renderOpaque(world);
        entityRenderer.render(world, renderer, pt, this::lightValue);
        renderer.renderParticles(particles, player, pt);
        weather.render(renderer, world, pt);
        if (!hideGui) renderer.renderSelection(world, interaction.hit, interaction.breakProgress);
        post.copyDepth();
        renderer.renderTranslucent(post.depthCopy.depth);

        if (!hideGui && !player.isDead()) {
            float sw = interaction.prevSwing + (interaction.swing - interaction.prevSwing) * pt;
            float eq = interaction.prevEquip + (interaction.equip - interaction.prevEquip) * pt;
            float br = lightValue(renderer.camX, renderer.camY, renderer.camZ);
            if (player.inLava) br = 3;
            float use = interaction.useType == 2 ? interaction.bowPower() : (interaction.useTicks + pt) / 32f;
            renderer.renderHeldItem(interaction.shownItem, itemRenderer, sw, eq, br, (float) window.width / window.height,
                    interaction.useType, use, ticks + pt);
        }
        float damageFlash = player.hurtTime > 0 ? (player.hurtTime - pt) / 10f : 0;
        post.finish(window.width, window.height, renderer.time, underwater || inLava, Math.max(damageFlash, 0));
        renderGui();

        if (screenshotAfter != null && startupCommands.isEmpty() && ticks - loadedTick > 60 && (world.pendingJobs() == 0 || ticks - loadedTick > 300) && ticks % 20 == 0) {
            screenshot(Path.of(screenshotAfter));
            running = false;
        }
    }

    private void renderGui() {
        gui.begin(window.width, window.height);
        if (!hideGui) hud.render(gui);
        if (isContainer(screen)) screens.renderContainer(gui, input);
        else if (screen == Screen.PAUSE) screens.renderPause(gui, input);
        else if (screen == Screen.DEATH) screens.renderDeath(gui, input);
    }

    private void renderLoading() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
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

    // ------------------------------------------------------------------ input

    private void handleInput() {
        boolean captured = screen == Screen.NONE;
        glfwSetInputMode(window.handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        if (input.pressed(GLFW_KEY_F11)) window.toggleFullscreen();
        if (input.pressed(GLFW_KEY_F2)) screenshot(null);
        switch (screen) {
            case NONE -> handleGameInput();
            case PAUSE -> { if (input.pressed(GLFW_KEY_ESCAPE)) closeScreen(); }
            case INVENTORY, CRAFTING, FURNACE, CHEST, CREATIVE -> {
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
                if (msg.startsWith("/")) commands.run(msg);
                else hud.chat("<Player> " + msg);
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
