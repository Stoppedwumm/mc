package mc.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mc.net.LanDiscovery;
import mc.net.StatusPing;
import mc.render.Gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The out-of-game screens: title screen, world selection and creation, the multiplayer server list, direct
 * connection, options, and the connecting / disconnected screens.
 */
final class Menus {
    /** Landscape shown behind the title screen. */
    static final long PANORAMA_SEED = 20240611L;

    /** A hilly spot on land with a view: {x, z, cameraY}. */
    static int[] panoramaSpot(mc.world.gen.TerrainGenerator gen) {
        int sea = mc.world.gen.TerrainGenerator.SEA_LEVEL;
        int[] best = {0, 0, sea + 20};
        double bestScore = -1e9;
        for (int x = -640; x <= 640; x += 48)
            for (int z = -640; z <= 640; z += 48) {
                int h = gen.estimateHeight(x, z);
                if (h < sea + 3 || h > sea + 30) continue;
                int hi = 0, water = 0;
                for (int a = 0; a < 12; a++) {
                    double ang = a * Math.PI / 6;
                    for (int r = 24; r <= 72; r += 24) {
                        int hh = gen.estimateHeight(x + (int) (Math.cos(ang) * r), z + (int) (Math.sin(ang) * r));
                        hi = Math.max(hi, hh - h);
                        if (hh < sea) water++;
                    }
                }
                // Hills around, a little water in view, not too far from the origin
                double score = Math.min(hi, 30) + Math.min(water, 6) * 1.5 - Math.max(0, water - 12) * 2 - Math.hypot(x, z) / 200;
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{x, z, h + 22};
                }
            }
        return best;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] SPLASHES = {
            "Now with multiplayer!", "Also try the Nether!", "100% procedural!", "LWJGL powered!", "Made with JOML!",
            "Punch the trees!", "Watch out for creepers!", "Open to LAN!", "Brew potions!", "Redstone inside!",
            "Enchant everything!", "Villages!", "Pixels galore!", "Look behind you!", "Blocks all the way down!",
    };

    private final Game g;
    private final String splash = SPLASHES[new Random().nextInt(SPLASHES.length)];
    private double mx, my;
    private boolean click, rclick;

    Menus(Game g) {
        this.g = g;
    }

    boolean isMenuScreen(Game.Screen s) {
        return switch (s) {
            case TITLE, WORLDS, CREATE_WORLD, DELETE_WORLD, MULTIPLAYER, ADD_SERVER, DIRECT_CONNECT, CONNECTING, DISCONNECTED, OPTIONS -> true;
            default -> false;
        };
    }

    void tick() {
        if (g.screen() == Game.Screen.MULTIPLAYER) lan.poll();
    }

    /** Opens a menu by name (scripted --cmd "/menu worlds"). */
    void open(String name) {
        switch (name) {
            case "worlds" -> openWorlds();
            case "create" -> openCreate();
            case "multiplayer" -> openMultiplayer();
            case "direct" -> openDirect();
            case "add" -> openAddServer();
            case "options" -> openOptions(Game.Screen.TITLE);
            default -> g.setScreen(Game.Screen.TITLE);
        }
    }

    // ------------------------------------------------------------------ text fields

    /** A single-line text box. */
    static final class TextField {
        final StringBuilder text = new StringBuilder();
        final String hint;
        final int max;
        boolean focused;

        TextField(String hint, int max) {
            this.hint = hint;
            this.max = max;
        }

        String value() { return text.toString().trim(); }

        void set(String s) {
            text.setLength(0);
            text.append(s == null ? "" : s);
        }
    }

    private final List<TextField> fields = new ArrayList<>();

    private TextField field(String hint, int max, String initial) {
        TextField f = new TextField(hint, max);
        f.set(initial);
        fields.add(f);
        return f;
    }

    private void resetFields() {
        fields.clear();
    }

    /** Keyboard input for the focused field: typing, backspace, paste, tab to the next field. */
    private void typeInto(Input in) {
        TextField f = null;
        for (TextField t : fields) if (t.focused) f = t;
        if (in.pressed(GLFW_KEY_TAB) && !fields.isEmpty()) {
            int i = f == null ? -1 : fields.indexOf(f);
            for (TextField t : fields) t.focused = false;
            f = fields.get((i + 1) % fields.size());
            f.focused = true;
            return;
        }
        if (f == null) return;
        for (int i = 0; i < in.typed.length(); i++) {
            char c = in.typed.charAt(i);
            if (c >= 32 && c < 127 && f.text.length() < f.max) f.text.append(c);
        }
        if (in.pressed(GLFW_KEY_BACKSPACE) && f.text.length() > 0) f.text.setLength(f.text.length() - 1);
        boolean ctrl = in.down(GLFW_KEY_LEFT_CONTROL) || in.down(GLFW_KEY_RIGHT_CONTROL) || in.down(GLFW_KEY_LEFT_SUPER) || in.down(GLFW_KEY_RIGHT_SUPER);
        if (ctrl && in.pressed(GLFW_KEY_V)) {
            String clip = glfwGetClipboardString(g.window.handle);
            if (clip != null) for (char c : clip.toCharArray()) if (c >= 32 && c < 127 && f.text.length() < f.max) f.text.append(c);
        }
    }

    private void drawField(Gui gui, TextField f, float x, float y, float w) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 20;
        if (click && hover) {
            for (TextField t : fields) t.focused = false;
            f.focused = true;
        }
        gui.fill(x - 1, y - 1, w + 2, 22, f.focused ? 0xFFFFFFFF : 0xFFA0A0A0);
        gui.fill(x, y, w, 20, 0xFF000000);
        String shown = f.text.toString();
        // Keep the end of long text visible
        while (gui.textWidth(shown) > w - 10 && shown.length() > 0) shown = shown.substring(1);
        if (shown.isEmpty() && !f.focused) gui.text(f.hint, x + 4, y + 6, 0xFF707070);
        else gui.text(shown + (f.focused && (g.ticks / 6) % 2 == 0 ? "_" : ""), x + 4, y + 6, 0xFFE0E0E0);
    }

    // ------------------------------------------------------------------ drawing helpers

    private boolean button(Gui gui, String label, float x, float y, float w, boolean enabled) {
        if (!enabled) {
            gui.fill(x, y, w, 20, 0xFF3A3A3A);
            gui.frame(x, y, w, 20, 1, 0xFF000000);
            gui.centered(label, x + w / 2, y + 6, 0xFF808080);
            return false;
        }
        boolean hover = gui.button(label, x, y, w, 20, mx, my);
        if (hover && click) {
            g.sound.click();
            return true;
        }
        return false;
    }

    private void background(Gui gui, boolean plain) {
        if (plain || g.world == null) {
            // Dirt-coloured backdrop with a soft vignette
            gui.gradient(0, 0, gui.width, gui.height, 0xFF2a2018, 0xFF15100c);
        } else {
            gui.gradient(0, 0, gui.width, gui.height * 0.5f, 0x50000000, 0x10000000);
            gui.gradient(0, gui.height * 0.5f, gui.width, gui.height * 0.5f, 0x10000000, 0x80000000);
        }
    }

    /** Darkened panel behind lists and forms. */
    private void listBackdrop(Gui gui, float top, float bottom) {
        gui.fill(0, top, gui.width, bottom - top, 0xA0000000);
        gui.fill(0, top, gui.width, 1, 0xFF000000);
        gui.fill(0, bottom - 1, gui.width, 1, 0x60FFFFFF);
    }

    // Blocky logo glyphs, 5 rows each ('#' = block)
    private static final String[][] GLYPHS = {
            {"M", "#...#", "##.##", "#.#.#", "#...#", "#...#"},
            {"I", "###", ".#.", ".#.", ".#.", "###"},
            {"N", "#...#", "##..#", "#.#.#", "#..##", "#...#"},
            {"E", "####", "#...", "###.", "#...", "####"},
            {"C", ".###", "#...", "#...", "#...", ".###"},
            {"R", "###.", "#..#", "###.", "#.#.", "#..#"},
            {"A", ".##.", "#..#", "####", "#..#", "#..#"},
            {"F", "####", "#...", "###.", "#...", "#..."},
            {"T", "#####", "..#..", "..#..", "..#..", "..#.."},
    };

    private static String[] glyph(char c) {
        for (String[] gl : GLYPHS) if (gl[0].charAt(0) == c) return gl;
        return GLYPHS[0];
    }

    /** Minecraft-style stone logo: every letter pixel is a bevelled stone block with an extruded shadow. */
    private void logo(Gui gui, String word, float cx, float top, float px) {
        float total = 0;
        for (char c : word.toCharArray()) total += (glyph(c)[1].length() + 1) * px;
        total -= px;
        float x0 = cx - total / 2;
        // Extrusion: darker copies stepping down-right
        for (int layer = 3; layer >= 0; layer--) {
            float x = x0;
            for (char c : word.toCharArray()) {
                String[] gl = glyph(c);
                for (int row = 0; row < 5; row++)
                    for (int col = 0; col < gl[row + 1].length(); col++) {
                        if (gl[row + 1].charAt(col) != '#') continue;
                        float bx = x + col * px + layer * px * 0.18f, by = top + row * px + layer * px * 0.18f;
                        if (layer > 0) {
                            gui.fill(bx, by, px, px, layer == 3 ? 0x80000000 : 0xFF2a2a2a + (2 - layer) * 0x101010);
                            continue;
                        }
                        int hsh = (int) ((c * 31 + row * 7 + col * 13) * 2654435761L >>> 26) & 31;
                        int shade = 0x80 + hsh;
                        int base = 0xFF000000 | shade << 16 | shade << 8 | shade;
                        gui.fill(bx, by, px, px, base);
                        // Speckles, highlight and shade edges like the stone texture
                        gui.fill(bx + px * 0.2f, by + px * 0.55f, px * 0.25f, px * 0.2f, 0x30000000);
                        gui.fill(bx + px * 0.6f, by + px * 0.2f, px * 0.2f, px * 0.2f, 0x28000000);
                        gui.fill(bx, by, px, px * 0.12f, 0x60FFFFFF);
                        gui.fill(bx, by, px * 0.12f, px, 0x40FFFFFF);
                        gui.fill(bx, by + px * 0.88f, px, px * 0.12f, 0x50000000);
                        gui.fill(bx + px * 0.88f, by, px * 0.12f, px, 0x40000000);
                    }
                x += (gl[1].length() + 1) * px;
            }
        }
    }

    // ------------------------------------------------------------------ entry point

    void render(Gui gui, boolean plain) {
        Input in = g.input;
        mx = in.mouseX / gui.scale;
        my = in.mouseY / gui.scale;
        click = in.clicked(GLFW_MOUSE_BUTTON_LEFT);
        rclick = in.clicked(GLFW_MOUSE_BUTTON_RIGHT);
        background(gui, plain);
        typeInto(in);
        switch (g.screen()) {
            case TITLE -> renderTitle(gui);
            case WORLDS -> renderWorlds(gui);
            case CREATE_WORLD -> renderCreate(gui);
            case DELETE_WORLD -> renderDelete(gui);
            case MULTIPLAYER -> renderMultiplayer(gui);
            case ADD_SERVER -> renderAddServer(gui);
            case DIRECT_CONNECT -> renderDirect(gui);
            case CONNECTING -> renderConnecting(gui);
            case DISCONNECTED -> renderDisconnected(gui);
            case OPTIONS -> renderOptions(gui);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ title

    private void renderTitle(Gui gui) {
        float w = gui.width, h = gui.height;
        float px = Math.max(3, Math.min(8, (float) Math.floor(w / 60)));
        float logoTop = Math.max(12, h / 4 - px * 5 - 14);
        logo(gui, "MINECRAFT", w / 2, logoTop, px);
        // Pulsing splash text
        float pulse = 1.8f - Math.abs((float) Math.sin(System.nanoTime() / 1e9 * Math.PI * 2 / 1.1)) * 0.15f;
        float sw = gui.textWidth(splash) * pulse;
        gui.textScaled(splash, Math.min(w - sw - 4, w / 2 + px * 12 - sw / 2), logoTop + px * 5 + 2, pulse, 0xFFFFFF00, true);

        float bw = 200, x = w / 2 - bw / 2;
        float y = Math.max(logoTop + px * 5 + 24, h / 4 + 48);
        if (button(gui, "Singleplayer", x, y, bw, true)) openWorlds();
        if (button(gui, "Multiplayer", x, y + 24, bw, true)) openMultiplayer();
        if (button(gui, "Options...", x, y + 60, 98, true)) openOptions(Game.Screen.TITLE);
        if (button(gui, "Quit Game", x + 102, y + 60, 98, true)) g.running = false;
        gui.text("Minecraft-like 2.0", 2, h - 10, 0xFFFFFFFF);
        String right = "Built with LWJGL, JOML, fastutil, Gson and Netty";
        gui.text(right, w - gui.textWidth(right) - 2, h - 10, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ singleplayer

    /** A world folder in saves/. */
    record WorldInfo(String folder, String name, long lastPlayed, boolean creative, long seed, boolean fresh) { }

    private final List<WorldInfo> worlds = new ArrayList<>();
    private int selectedWorld = -1;
    private float worldScroll;
    private long lastClickTime;
    private int lastClickIndex = -1;

    private Path savesDir() { return g.gameDir.resolve("saves"); }

    private void openWorlds() {
        worlds.clear();
        try {
            Files.createDirectories(savesDir());
            try (var dirs = Files.list(savesDir())) {
                for (Path d : dirs.toList()) {
                    if (!Files.isDirectory(d)) continue;
                    Options.Level lv = Options.Level.load(d.resolve("level.json"));
                    if (lv == null) continue;
                    long played = lv.lastPlayed != 0 ? lv.lastPlayed : Files.getLastModifiedTime(d.resolve("level.json")).toMillis();
                    String folder = d.getFileName().toString();
                    worlds.add(new WorldInfo(folder, lv.name != null ? lv.name : folder, played, lv.creative, lv.seed, !lv.spawned));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not list worlds: " + e);
        }
        worlds.sort(Comparator.comparingLong(WorldInfo::lastPlayed).reversed());
        selectedWorld = worlds.isEmpty() ? -1 : 0;
        worldScroll = 0;
        g.setScreen(Game.Screen.WORLDS);
    }

    private void renderWorlds(Gui gui) {
        float w = gui.width, h = gui.height;
        gui.centered("Select World", w / 2, 12, 0xFFFFFFFF);
        float top = 30, bottom = h - 58;
        listBackdrop(gui, top, bottom);
        float rowH = 36, listW = Math.min(270, w - 20), lx = w / 2 - listW / 2;
        float maxScroll = Math.max(0, worlds.size() * rowH + 8 - (bottom - top));
        if (g.input.scroll != 0) worldScroll = (float) Math.max(0, Math.min(maxScroll, worldScroll - g.input.scroll * 18));
        gui.clip(0, top, w, bottom - top);
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy HH:mm");
        for (int i = 0; i < worlds.size(); i++) {
            WorldInfo wi = worlds.get(i);
            float y = top + 4 + i * rowH - worldScroll;
            if (y + rowH < top || y > bottom) continue;
            boolean hover = mx >= lx && mx < lx + listW && my >= y && my < y + rowH - 2 && my >= top && my < bottom;
            if (i == selectedWorld) {
                gui.fill(lx - 2, y - 2, listW + 4, rowH, 0xFF808080);
                gui.fill(lx - 1, y - 1, listW + 2, rowH - 2, 0xFF000000);
            } else if (hover) gui.fill(lx - 2, y - 2, listW + 4, rowH, 0x30FFFFFF);
            // Icon: a grass block sprite
            gui.fill(lx + 2, y + 2, 28, 28, 0xFF000000);
            gui.stack(new mc.item.ItemStack(mc.item.Item.of(wi.creative ? mc.world.Block.DIAMOND_BLOCK : mc.world.Block.GRASS), 1), lx + 8, y + 8);
            gui.text(wi.name, lx + 36, y + 2, 0xFFFFFFFF);
            gui.text(wi.folder + " (" + fmt.format(new Date(wi.lastPlayed)) + ")", lx + 36, y + 12, 0xFF808080);
            gui.text((wi.creative ? "Creative Mode" : "Survival Mode") + (wi.fresh ? ", not yet played" : ""), lx + 36, y + 22, 0xFF808080);
            if (hover && click) {
                long now = System.currentTimeMillis();
                if (lastClickIndex == i && now - lastClickTime < 350) { playSelected(); gui.noClip(); return; }
                selectedWorld = i;
                lastClickIndex = i;
                lastClickTime = now;
            }
        }
        gui.noClip();
        if (worlds.isEmpty()) gui.centered("No worlds yet: create one!", w / 2, top + 20, 0xFFA0A0A0);
        boolean sel = selectedWorld >= 0 && selectedWorld < worlds.size();
        float bx = w / 2 - 154;
        if (button(gui, "Play Selected World", bx, h - 52, 150, sel)) playSelected();
        if (button(gui, "Create New World", bx + 158, h - 52, 150, true)) openCreate();
        if (button(gui, "Delete", bx, h - 28, 150, sel)) g.setScreen(Game.Screen.DELETE_WORLD);
        if (button(gui, "Cancel", bx + 158, h - 28, 150, true) || g.input.pressed(GLFW_KEY_ESCAPE)) g.setScreen(Game.Screen.TITLE);
        if (sel && (g.input.pressed(GLFW_KEY_ENTER) || g.input.pressed(GLFW_KEY_KP_ENTER))) playSelected();
    }

    private void playSelected() {
        if (selectedWorld < 0 || selectedWorld >= worlds.size()) return;
        g.loadWorld(worlds.get(selectedWorld).folder, null, null, null);
    }

    private TextField worldName, worldSeed;
    private boolean newCreative;

    private void openCreate() {
        resetFields();
        worldName = field("New World", 32, "New World");
        worldSeed = field("Leave blank for a random seed", 32, "");
        worldName.focused = true;
        newCreative = false;
        g.setScreen(Game.Screen.CREATE_WORLD);
    }

    private void renderCreate(Gui gui) {
        float w = gui.width, h = gui.height, x = w / 2 - 100;
        gui.centered("Create New World", w / 2, 16, 0xFFFFFFFF);
        float y = Math.max(40, h / 4);
        gui.text("World Name", x, y, 0xFFA0A0A0);
        drawField(gui, worldName, x, y + 12, 200);
        gui.text("Will be saved in: " + folderFor(worldName.value()), x, y + 36, 0xFF707070);
        gui.text("Seed for the World Generator", x, y + 52, 0xFFA0A0A0);
        drawField(gui, worldSeed, x, y + 64, 200);
        if (button(gui, "Game Mode: " + (newCreative ? "Creative" : "Survival"), x, y + 92, 200, true)) newCreative = !newCreative;
        gui.centered(newCreative ? "Unlimited resources, free flying and" : "Search for resources, craft, gain", w / 2, y + 116, 0xFF909090);
        gui.centered(newCreative ? "destroy blocks instantly" : "levels, health and hunger", w / 2, y + 126, 0xFF909090);
        boolean ok = !worldName.value().isEmpty();
        if (button(gui, "Create New World", w / 2 - 154, h - 28, 150, ok) || (ok && (g.input.pressed(GLFW_KEY_ENTER) || g.input.pressed(GLFW_KEY_KP_ENTER)))) {
            String name = worldName.value();
            String s = worldSeed.value();
            Long seed = null;
            if (!s.isEmpty()) {
                try { seed = Long.parseLong(s); } catch (NumberFormatException e) { seed = (long) s.hashCode(); }
            }
            resetFields();
            g.loadWorld(folderFor(name), seed, newCreative, name);
            return;
        }
        if (button(gui, "Cancel", w / 2 + 4, h - 28, 150, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            resetFields();
            openWorlds();
        }
    }

    /** A folder name for a new world that doesn't clash with existing ones. */
    private String folderFor(String name) {
        String base = name.replaceAll("[^A-Za-z0-9 _-]", "_").trim();
        if (base.isEmpty()) base = "World";
        String f = base;
        for (int n = 2; Files.exists(savesDir().resolve(f)); n++) f = base + " (" + n + ")";
        return f;
    }

    private void renderDelete(Gui gui) {
        float w = gui.width, h = gui.height;
        if (selectedWorld < 0 || selectedWorld >= worlds.size()) { openWorlds(); return; }
        WorldInfo wi = worlds.get(selectedWorld);
        gui.centered("Are you sure you want to delete this world?", w / 2, h / 3, 0xFFFFFFFF);
        gui.centered("'" + wi.name + "' will be lost forever! (A long time!)", w / 2, h / 3 + 16, 0xFFA0A0A0);
        if (button(gui, "Delete", w / 2 - 154, h / 3 + 50, 150, true)) {
            try (var paths = Files.walk(savesDir().resolve(wi.folder))) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (IOException e) {
                System.err.println("Could not delete world: " + e);
            }
            openWorlds();
            return;
        }
        if (button(gui, "Cancel", w / 2 + 4, h / 3 + 50, 150, true) || g.input.pressed(GLFW_KEY_ESCAPE)) g.setScreen(Game.Screen.WORLDS);
    }

    // ------------------------------------------------------------------ multiplayer

    /** An entry of servers.json. */
    static final class ServerEntry {
        String name, address;
        transient CompletableFuture<StatusPing.Result> status;
    }

    private List<ServerEntry> servers = new ArrayList<>();
    private int selectedServer = -1;
    /** LAN games are listed below the saved servers (indices >= servers.size()). */
    private final LanDiscovery.Listener lan = new LanDiscovery.Listener();

    private Path serversFile() { return g.gameDir.resolve("servers.json"); }

    private void loadServers() {
        servers = new ArrayList<>();
        try {
            if (Files.exists(serversFile())) {
                ServerEntry[] arr = GSON.fromJson(Files.readString(serversFile()), ServerEntry[].class);
                if (arr != null) for (ServerEntry e : arr) if (e != null && e.address != null) servers.add(e);
            }
        } catch (Exception e) {
            System.err.println("Could not read servers.json: " + e);
        }
    }

    private void saveServers() {
        try {
            Files.writeString(serversFile(), GSON.toJson(servers));
        } catch (IOException e) {
            System.err.println("Could not save servers.json: " + e);
        }
    }

    private void refreshStatus() {
        for (ServerEntry e : servers) e.status = StatusPing.pingAsync(e.address);
    }

    void openMultiplayer() {
        loadServers();
        refreshStatus();
        lan.start();
        selectedServer = servers.isEmpty() ? -1 : 0;
        g.setScreen(Game.Screen.MULTIPLAYER);
    }

    private void leaveMultiplayerList() {
        lan.stop();
    }

    private void renderMultiplayer(Gui gui) {
        float w = gui.width, h = gui.height;
        gui.centered("Play Multiplayer", w / 2, 12, 0xFFFFFFFF);
        float top = 30, bottom = h - 58;
        listBackdrop(gui, top, bottom);
        float rowH = 36, listW = Math.min(300, w - 20), lx = w / 2 - listW / 2;
        List<LanDiscovery.Game> lanGames = lan.games();
        int count = servers.size() + lanGames.size();
        gui.clip(0, top, w, bottom - top);
        float y = top + 4;
        for (int i = 0; i < count; i++, y += rowH) {
            if (y > bottom) break;
            boolean hover = mx >= lx && mx < lx + listW && my >= y && my < y + rowH - 2 && my < bottom;
            if (i == selectedServer) {
                gui.fill(lx - 2, y - 2, listW + 4, rowH, 0xFF808080);
                gui.fill(lx - 1, y - 1, listW + 2, rowH - 2, 0xFF000000);
            } else if (hover) gui.fill(lx - 2, y - 2, listW + 4, rowH, 0x30FFFFFF);
            if (i < servers.size()) {
                ServerEntry e = servers.get(i);
                gui.text(e.name, lx + 4, y + 2, 0xFFFFFFFF);
                StatusPing.Result r = e.status != null && e.status.isDone() ? e.status.getNow(null) : null;
                if (e.status == null || !e.status.isDone()) {
                    gui.text("Pinging...", lx + 4, y + 13, 0xFF808080);
                } else if (r == null || r.error() != null) {
                    gui.text("Can't connect to server", lx + 4, y + 13, 0xFFE04040);
                    drawBars(gui, lx + listW - 14, y + 2, -1);
                } else {
                    gui.text(r.motd(), lx + 4, y + 13, 0xFFA0A0A0);
                    String pl = r.online() + "/" + r.max();
                    gui.text(pl, lx + listW - 18 - gui.textWidth(pl), y + 2, 0xFFA0A0A0);
                    drawBars(gui, lx + listW - 14, y + 2, r.pingMs());
                }
                gui.text(e.address, lx + 4, y + 24, 0xFF606060);
            } else {
                LanDiscovery.Game lg = lanGames.get(i - servers.size());
                gui.text("LAN World", lx + 4, y + 2, 0xFFFFFFFF);
                gui.text(lg.motd(), lx + 4, y + 13, 0xFFA0A0A0);
                gui.text(lg.address(), lx + 4, y + 24, 0xFF606060);
            }
            if (hover && click) {
                long now = System.currentTimeMillis();
                if (lastClickIndex == 1000 + i && now - lastClickTime < 350) { joinSelected(); gui.noClip(); return; }
                selectedServer = i;
                lastClickIndex = 1000 + i;
                lastClickTime = now;
            }
        }
        gui.noClip();
        if (count == 0) {
            gui.centered("No servers added yet.", w / 2, top + 16, 0xFFA0A0A0);
            gui.centered("Scanning for games on your local network...", w / 2, top + 28, 0xFF707070);
        }
        boolean sel = selectedServer >= 0 && selectedServer < count;
        boolean saved = sel && selectedServer < servers.size();
        float bx = w / 2 - 154;
        if (button(gui, "Join Server", bx, h - 52, 100, sel)) joinSelected();
        if (button(gui, "Direct Connection", bx + 104, h - 52, 100, true)) openDirect();
        if (button(gui, "Add Server", bx + 208, h - 52, 100, true)) openAddServer();
        if (button(gui, "Delete", bx, h - 28, 74, saved)) {
            servers.remove(selectedServer);
            saveServers();
            selectedServer = Math.min(selectedServer, servers.size() - 1);
        }
        if (button(gui, "Refresh", bx + 78, h - 28, 74, true)) refreshStatus();
        if (button(gui, "Back", bx + 156, h - 28, 152, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            leaveMultiplayerList();
            g.setScreen(Game.Screen.TITLE);
        }
    }

    /** Connection-strength bars: 5 for a fast ping, a red cross when unreachable. */
    private void drawBars(Gui gui, float x, float y, long ping) {
        int bars = ping < 0 ? 0 : ping < 150 ? 5 : ping < 300 ? 4 : ping < 600 ? 3 : ping < 1000 ? 2 : 1;
        for (int i = 0; i < 5; i++) {
            float bh = 2 + i * 1.5f;
            gui.fill(x + i * 2.4f, y + 8 - bh, 1.6f, bh, i < bars ? 0xFF40E040 : 0xFF303030);
        }
        if (ping < 0) gui.text("x", x + 3, y, 0xFFE04040);
    }

    private void joinSelected() {
        List<LanDiscovery.Game> lanGames = lan.games();
        if (selectedServer < 0) return;
        String address;
        if (selectedServer < servers.size()) address = servers.get(selectedServer).address;
        else if (selectedServer - servers.size() < lanGames.size()) address = lanGames.get(selectedServer - servers.size()).address();
        else return;
        leaveMultiplayerList();
        connect(address);
    }

    private TextField serverName, serverAddress;

    private void openAddServer() {
        resetFields();
        serverName = field("Minecraft Server", 32, "Minecraft Server");
        serverAddress = field("Server Address", 64, "");
        serverAddress.focused = true;
        g.setScreen(Game.Screen.ADD_SERVER);
    }

    private void renderAddServer(Gui gui) {
        float w = gui.width, h = gui.height, x = w / 2 - 100;
        gui.centered("Edit Server Info", w / 2, 17, 0xFFFFFFFF);
        float y = Math.max(40, h / 4);
        gui.text("Server Name", x, y, 0xFFA0A0A0);
        drawField(gui, serverName, x, y + 12, 200);
        gui.text("Server Address", x, y + 44, 0xFFA0A0A0);
        drawField(gui, serverAddress, x, y + 56, 200);
        boolean ok = !serverAddress.value().isEmpty();
        if (button(gui, "Done", x, h / 4 + 120, 200, ok) || (ok && g.input.pressed(GLFW_KEY_ENTER))) {
            ServerEntry e = new ServerEntry();
            e.name = serverName.value().isEmpty() ? "Minecraft Server" : serverName.value();
            e.address = serverAddress.value();
            servers.add(e);
            saveServers();
            e.status = StatusPing.pingAsync(e.address);
            resetFields();
            selectedServer = servers.size() - 1;
            g.setScreen(Game.Screen.MULTIPLAYER);
            return;
        }
        if (button(gui, "Cancel", x, h / 4 + 144, 200, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            resetFields();
            g.setScreen(Game.Screen.MULTIPLAYER);
        }
    }

    private TextField directAddress;

    private void openDirect() {
        resetFields();
        directAddress = field("Server Address", 64, g.options.lastServer);
        directAddress.focused = true;
        g.setScreen(Game.Screen.DIRECT_CONNECT);
    }

    private void renderDirect(Gui gui) {
        float w = gui.width, h = gui.height, x = w / 2 - 100;
        gui.centered("Direct Connection", w / 2, 20, 0xFFFFFFFF);
        float y = Math.max(50, h / 4);
        gui.text("Server Address", x, y, 0xFFA0A0A0);
        drawField(gui, directAddress, x, y + 12, 200);
        gui.text("Example: localhost or 192.168.1.20:25565", x, y + 36, 0xFF707070);
        boolean ok = !directAddress.value().isEmpty();
        if (button(gui, "Join Server", x, h / 4 + 96, 200, ok) || (ok && (g.input.pressed(GLFW_KEY_ENTER) || g.input.pressed(GLFW_KEY_KP_ENTER)))) {
            g.options.lastServer = directAddress.value();
            String addr = directAddress.value();
            resetFields();
            leaveMultiplayerList();
            connect(addr);
            return;
        }
        if (button(gui, "Cancel", x, h / 4 + 120, 200, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            resetFields();
            openMultiplayer();
        }
    }

    // ------------------------------------------------------------------ connecting

    private String errorTitle = "", errorMessage = "";

    /** Starts joining a server; the session drives the CONNECTING screen. */
    void connect(String address) {
        g.setScreen(Game.Screen.CONNECTING);
        g.startMultiplayer(address);
    }

    /** Shows a failure (connection lost, kicked, bad world) with a button back to the title. */
    void error(String title, String message) {
        errorTitle = title;
        errorMessage = message == null ? "" : message;
        g.setScreen(Game.Screen.DISCONNECTED);
    }

    private void renderConnecting(Gui gui) {
        float w = gui.width, h = gui.height;
        String status = g.multiplayer != null ? g.multiplayer.status() : "Connecting to the server...";
        gui.centered(status, w / 2, h / 2 - 30, 0xFFFFFFFF);
        int dots = (int) (g.ticks / 5 % 4);
        gui.centered(".".repeat(dots), w / 2, h / 2 - 16, 0xFFA0A0A0);
        if (button(gui, "Cancel", w / 2 - 100, h / 2 + 20, 200, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            if (g.multiplayer != null) g.multiplayer.disconnect("Cancelled");
            g.multiplayer = null;
            openMultiplayer();
        }
    }

    private void renderDisconnected(Gui gui) {
        float w = gui.width, h = gui.height;
        gui.centered(errorTitle, w / 2, h / 2 - 40, 0xFFA0A0A0);
        float y = h / 2 - 20;
        for (String line : wrap(gui, errorMessage, (int) Math.min(300, w - 20))) {
            gui.centered(line, w / 2, y, 0xFFFFFFFF);
            y += 10;
        }
        if (button(gui, "Back to Title Screen", w / 2 - 100, Math.max(y + 16, h / 2 + 20), 200, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            g.setScreen(Game.Screen.TITLE);
        }
    }

    private static List<String> wrap(Gui gui, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && gui.textWidth(line + " " + word) > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    // ------------------------------------------------------------------ options

    private Game.Screen optionsReturn = Game.Screen.TITLE;
    private TextField playerName;

    void openOptions(Game.Screen back) {
        optionsReturn = back;
        resetFields();
        playerName = field("Player name", 16, g.options.playerName);
        g.setScreen(Game.Screen.OPTIONS);
    }

    private void renderOptions(Gui gui) {
        float w = gui.width, h = gui.height, x = w / 2 - 100;
        gui.centered("Options", w / 2, 12, 0xFFFFFFFF);
        float y = Math.max(28, h / 6 - 12);
        gui.text("Name (shown in multiplayer)", x, y, 0xFFA0A0A0);
        drawField(gui, playerName, x, y + 11, 200);
        String n = playerName.value().replaceAll("[^A-Za-z0-9_]", "");
        if (!n.isEmpty()) g.options.playerName = n;
        g.screens.settingsButtons(gui, x, y + 40, mx, my, click, rclick);
        if (button(gui, "Done", x, Math.min(h - 26, y + 40 + 5 * 24 + 8), 200, true) || g.input.pressed(GLFW_KEY_ESCAPE)) {
            resetFields();
            g.options.save(g.gameDir.resolve("options.json"));
            g.setScreen(optionsReturn);
        }
    }
}
