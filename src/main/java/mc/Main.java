package mc;

import mc.client.Game;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Main {
    public static void main(String[] args) throws Exception {
        String world = null;
        Long seed = null;
        int rd = 0;
        String screenshot = null, connect = null;
        boolean server = false;
        int port = mc.net.Net.DEFAULT_PORT, maxPlayers = 8, viewDistance = 10;
        String motd = "A Minecraft-like Server";
        List<String> commands = new ArrayList<>();
        Path dir = Path.of(System.getProperty("user.dir"), "run");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--world" -> world = args[++i];
                case "--seed" -> {
                    String s = args[++i];
                    try { seed = Long.parseLong(s); } catch (NumberFormatException e) { seed = (long) s.hashCode(); }
                }
                case "--renderDistance" -> rd = Integer.parseInt(args[++i]);
                case "--gameDir" -> dir = Path.of(args[++i]);
                case "--screenshot" -> screenshot = args[++i];
                case "--cmd" -> commands.add(args[++i]);
                case "--connect" -> connect = args[++i];
                case "--server", "--nogui" -> server = true;
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--motd" -> motd = args[++i];
                case "--maxPlayers" -> maxPlayers = Integer.parseInt(args[++i]);
                case "--viewDistance" -> viewDistance = Integer.parseInt(args[++i]);
                default -> System.err.println("Unknown argument " + args[i]);
            }
        }
        java.nio.file.Files.createDirectories(dir);
        if (server) {
            mc.mod.ModLoader.loadAll(dir.resolve("mods"), net.neoforged.api.distmarker.Dist.DEDICATED_SERVER);
            new mc.server.DedicatedServer(dir, world != null ? world : "world", port, seed, motd, maxPlayers, viewDistance).run();
            System.exit(0);
        }
        if (relaunchOnMac(args)) return;
        // A seed without a world name keeps the old behaviour of opening "world" directly
        if (world == null && seed != null && connect == null) world = "world";
        // NeoForge mods from <gameDir>/mods, before anything builds the block and item tables
        mc.mod.ModLoader.loadAll(dir.resolve("mods"), net.neoforged.api.distmarker.Dist.CLIENT);
        new Game(dir, world, seed, rd, screenshot, commands, connect).run();
        System.exit(0);
    }

    /** GLFW on macOS needs the main thread; restart the JVM with -XstartOnFirstThread if necessary. */
    private static boolean relaunchOnMac(String[] args) throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return false;
        String pid = String.valueOf(ProcessHandle.current().pid());
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid))) return false;
        if (System.getProperty("mc.relaunched") != null) return false;
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-XstartOnFirstThread");
        cmd.add("-Dmc.relaunched=true");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(Main.class.getName());
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        System.exit(p.waitFor());
        return true;
    }
}
