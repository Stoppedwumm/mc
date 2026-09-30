package mc.server;

import io.netty.buffer.ByteBuf;
import mc.client.Options;
import mc.entity.Player;
import mc.net.Connection;
import mc.net.LanDiscovery;
import mc.net.Net;
import mc.net.Protocol;
import mc.world.Block;
import mc.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hosts a world for players on the network. Runs on the thread that ticks the world (the game thread for a
 * singleplayer world opened to LAN, the server loop for a dedicated server): call tick() after world.tick().
 */
public final class Server {
    public final World world;
    private final Net.Listener listener;
    final List<Session> sessions = new ArrayList<>();
    private final Path playersDir;
    /** The local player of a LAN game (null on a dedicated server). */
    final Player host;
    private final Consumer<String> log;
    public String motd;
    /** Where chat lines go on this machine (the LAN host's chat); null logs them. */
    public Consumer<String> chatSink;
    public int maxPlayers = 8;
    /** Furthest view distance (chunks) the server sends. */
    public int viewDistanceCap = 12;
    private LanDiscovery.Broadcaster lan;
    private final World.Listener previousListener;
    /** Session whose packet is being handled (its own action effects aren't echoed back to it). */
    Session current;
    /** Where new players appear. */
    public double spawnX, spawnY = -1, spawnZ;
    long ticks;

    public Server(World world, int port, Path playersDir, Player host, Consumer<String> log, String motd, boolean announceLan) throws IOException {
        this.world = world;
        this.playersDir = playersDir;
        this.host = host;
        this.log = log;
        this.motd = motd;
        Files.createDirectories(playersDir);
        try {
            listener = Net.listen(port);
        } catch (Exception e) {
            throw new IOException("Could not listen on port " + port + ": " + e.getMessage(), e);
        }
        if (announceLan) {
            lan = new LanDiscovery.Broadcaster(motd, listener.port);
            lan.start();
        }
        previousListener = world.listener;
        World.Listener prev = previousListener;
        world.listener = new World.Listener() {
            @Override
            public void playSound(String name, double x, double y, double z, float volume, float pitch) {
                if (prev != null) prev.playSound(name, x, y, z, volume, pitch);
                broadcastNear(x, z, 48, s -> s.sendSound(name, x, y, z, volume, pitch));
            }

            @Override
            public void addParticle(String type, double x, double y, double z) {
                if (prev != null) prev.addParticle(type, x, y, z);
                broadcastNear(x, z, 32, s -> s.sendParticle(type, x, y, z));
            }

            @Override
            public void blockBroken(int x, int y, int z, Block block, int meta) {
                if (prev != null) prev.blockBroken(x, y, z, block, meta);
                broadcastNear(x, z, 48, s -> s.sendBroken(x, y, z, block.id, meta));
            }
        };
        world.blockWatcher = (x, y, z, id, meta) -> {
            for (Session s : sessions) if (s.player != null) s.blockChanged(x, y, z, id, meta);
        };
    }

    public int port() {
        return listener.port;
    }

    void log(String msg) {
        if (log != null) log.accept(msg);
    }

    /** Runs a callback for every logged-in session near a point, except the one whose action caused it. */
    private void broadcastNear(double x, double z, double range, Consumer<Session> action) {
        for (Session s : sessions) {
            if (s.player == null || s == current) continue;
            double dx = s.player.x - x, dz = s.player.z - z;
            if (dx * dx + dz * dz <= range * range) action.accept(s);
        }
    }

    /** One server tick: new connections, incoming packets, then everything the clients need to see. */
    public void tick() {
        ticks++;
        Connection c;
        while ((c = listener.poll()) != null) sessions.add(new Session(this, c));
        for (Session s : new ArrayList<>(sessions)) {
            current = s;
            try {
                s.handlePackets();
            } catch (RuntimeException e) {
                log("Bad packet from " + s.name() + ": " + e);
                s.kick("Invalid packet");
            }
            current = null;
        }
        for (Session s : new ArrayList<>(sessions)) {
            if (!s.connection.isOpen()) {
                remove(s, s.connection.closeReason());
                continue;
            }
            s.tick();
            s.connection.flush();
        }
    }

    private void remove(Session s, String reason) {
        sessions.remove(s);
        if (s.player != null) {
            savePlayer(s.player);
            world.removeNetworkPlayer(s.player);
            s.player.removed = true;
            log(s.player.name + " lost connection: " + reason);
            broadcastChat("§e" + s.player.name + " left the game");
        }
    }

    /** Chunk-loading centres for connected players: {x, z, radius}. */
    public List<double[]> playerCenters() {
        List<double[]> out = new ArrayList<>();
        for (Session s : sessions) if (s.player != null) out.add(new double[]{s.player.x, s.player.z, s.viewDistance()});
        return out;
    }

    public int online() {
        int n = host != null ? 1 : 0;
        for (Session s : sessions) if (s.player != null) n++;
        return n;
    }

    public List<String> playerNames() {
        List<String> out = new ArrayList<>();
        if (host != null) out.add(host.name);
        for (Session s : sessions) if (s.player != null) out.add(s.player.name);
        return out;
    }

    boolean nameTaken(String name) {
        if (host != null && host.name.equalsIgnoreCase(name)) return true;
        for (Session s : sessions) if (s.player != null && s.player.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** Sends a chat line to everyone (and to the host's chat or the console). */
    public void broadcastChat(String msg) {
        if (chatSink != null) chatSink.accept(msg);
        else log(msg.replaceAll("\u00a7.", ""));
        for (Session s : sessions) if (s.player != null) s.sendChat(msg);
    }

    /** Sends an arm swing of the host (LAN) to the other players. */
    public void hostSwing() {
        if (host == null) return;
        for (Session s : sessions) if (s.player != null) s.sendEvent(host.id, Protocol.EV_SWING);
    }

    // ------------------------------------------------------------------ player data

    private Path playerFile(String name) {
        return playersDir.resolve(name.toLowerCase() + ".json");
    }

    Options.Level loadPlayer(String name) {
        return Options.Level.load(playerFile(name));
    }

    void savePlayer(NetPlayer p) {
        Options.Level lv = new Options.Level();
        lv.writePlayer(p);
        lv.name = p.name;
        lv.save(playerFile(p.name));
    }

    public void savePlayers() {
        for (Session s : sessions) if (s.player != null) savePlayer(s.player);
    }

    /** Disconnects everyone and closes the port. */
    public void stop() {
        for (Session s : new ArrayList<>(sessions)) {
            s.kick(host != null ? "The host closed the game" : "Server closed");
            s.connection.flush();
            if (s.player != null) {
                savePlayer(s.player);
                world.removeNetworkPlayer(s.player);
            }
        }
        sessions.clear();
        listener.close();
        if (lan != null) lan.close();
        world.listener = previousListener;
        world.blockWatcher = null;
        world.updateExtraCenters(null);
    }

    // ------------------------------------------------------------------ commands

    /** Server-side chat commands (from players or the console). Returns the reply. */
    public String command(String line, NetPlayer sender) {
        String[] a = line.trim().replaceFirst("^/", "").split("\\s+");
        if (a.length == 0 || a[0].isEmpty()) return "";
        switch (a[0].toLowerCase()) {
            case "list" -> {
                return "There are " + online() + "/" + maxPlayers + " players online: " + String.join(", ", playerNames());
            }
            case "say" -> {
                broadcastChat("[" + (sender != null ? sender.name : "Server") + "] " + line.substring(line.indexOf("say") + 3).trim());
                return "";
            }
            case "seed" -> {
                return "Seed: " + world.seed;
            }
            case "time" -> {
                if (a.length >= 3 && a[1].equals("set")) {
                    long t = switch (a[2]) {
                        case "day" -> 1000;
                        case "noon" -> 6000;
                        case "night" -> 13000;
                        case "midnight" -> 18000;
                        default -> parseLong(a[2], world.time % 24000);
                    };
                    world.time = world.time - world.time % 24000 + t;
                    for (Session s : sessions) if (s.player != null) s.sendTime();
                    return "Set the time to " + t;
                }
                return "Usage: /time set <day|night|noon|midnight|ticks>";
            }
            case "weather" -> {
                if (a.length >= 2) {
                    world.raining = a[1].equals("rain") || a[1].equals("thunder");
                    weatherOverride = world.raining ? 1 : 0;
                    for (Session s : sessions) if (s.player != null) s.sendTime();
                    return "Changed the weather to " + (world.raining ? "rain" : "clear");
                }
                return "Usage: /weather <clear|rain>";
            }
            case "kick" -> {
                if (a.length < 2) return "Usage: /kick <player>";
                for (Session s : sessions) if (s.player != null && s.player.name.equalsIgnoreCase(a[1])) { s.kick("Kicked by an operator"); return "Kicked " + s.player.name; }
                return "No player named " + a[1];
            }
            case "tp" -> {
                if (sender == null || a.length < 2) return "Usage: /tp <player>";
                Player target = null;
                if (host != null && host.name.equalsIgnoreCase(a[1])) target = host;
                for (Session s : sessions) if (s.player != null && s.player.name.equalsIgnoreCase(a[1])) target = s.player;
                if (target == null) return "No player named " + a[1];
                sender.setPos(target.x, target.y, target.z);
                return "Teleported to " + target.name;
            }
            case "help" -> {
                return "Server commands: /list /say /seed /time set /weather /tp <player> /kick; other commands run on your own client";
            }
            default -> {
                return "Unknown server command. Type /help";
            }
        }
    }

    /** Set by /weather: 1 rain, 0 clear, -1 natural cycle (dedicated servers). */
    int weatherOverride = -1;

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }

    /** A player's view, for tests and the console. */
    public List<NetPlayer> players() {
        List<NetPlayer> out = new ArrayList<>();
        for (Session s : sessions) if (s.player != null) out.add(s.player);
        return out;
    }

    ByteBuf statusPacket() {
        ByteBuf b = Protocol.packet(Protocol.S_STATUS);
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("motd", motd);
        o.addProperty("online", online());
        o.addProperty("max", maxPlayers);
        o.addProperty("version", Protocol.VERSION);
        Protocol.writeString(b, o.toString());
        return b;
    }
}
