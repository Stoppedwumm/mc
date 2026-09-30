package mc.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LAN world discovery with Minecraft's scheme: a game opened to LAN multicasts "[MOTD]name[/MOTD][AD]port[/AD]"
 * to 224.0.2.60:4445 every 1.5 seconds, and the multiplayer screen listens for it.
 */
public final class LanDiscovery {
    private LanDiscovery() { }

    static final String GROUP = "224.0.2.60";
    static final int PORT = 4445;

    public record Game(String motd, String address, long seen) { }

    /** Announces a LAN game until closed. */
    public static final class Broadcaster extends Thread {
        private final String motd;
        private final int port;
        private volatile boolean running = true;

        public Broadcaster(String motd, int port) {
            super("LAN Broadcaster");
            setDaemon(true);
            this.motd = motd;
            this.port = port;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] msg = ("[MOTD]" + motd + "[/MOTD][AD]" + port + "[/AD]").getBytes(StandardCharsets.UTF_8);
                InetAddress group = InetAddress.getByName(GROUP);
                while (running) {
                    try {
                        socket.send(new DatagramPacket(msg, msg.length, group, PORT));
                    } catch (IOException ignored) {
                        // No multicast route (offline): keep trying quietly
                    }
                    Thread.sleep(1500);
                }
            } catch (IOException | InterruptedException ignored) {
            }
        }

        public void close() {
            running = false;
            interrupt();
        }
    }

    /** Collects LAN games announced on the local network. */
    public static final class Listener {
        private final Map<String, Game> games = new LinkedHashMap<>();
        private Thread thread;
        private volatile boolean running;

        public synchronized void start() {
            if (running) return;
            running = true;
            thread = new Thread(this::listen, "LAN Listener");
            thread.setDaemon(true);
            thread.start();
        }

        public synchronized void stop() {
            running = false;
            if (thread != null) thread.interrupt();
            thread = null;
            games.clear();
        }

        private void listen() {
            try (MulticastSocket socket = new MulticastSocket(PORT)) {
                socket.setSoTimeout(1000);
                InetAddress group = InetAddress.getByName(GROUP);
                try {
                    socket.joinGroup(new java.net.InetSocketAddress(group, PORT), null);
                } catch (IOException e) {
                    return;
                }
                byte[] buf = new byte[1024];
                while (running) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(p);
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    String s = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                    String motd = between(s, "[MOTD]", "[/MOTD]"), port = between(s, "[AD]", "[/AD]");
                    if (motd == null || port == null) continue;
                    String address = p.getAddress().getHostAddress() + ":" + port.trim();
                    synchronized (this) {
                        games.put(address, new Game(motd, address, System.currentTimeMillis()));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        private static String between(String s, String a, String b) {
            int i = s.indexOf(a), j = s.indexOf(b);
            return i < 0 || j < i ? null : s.substring(i + a.length(), j);
        }

        /** Forgets games that stopped announcing. */
        public synchronized void poll() {
            long now = System.currentTimeMillis();
            games.values().removeIf(g -> now - g.seen() > 5000);
        }

        public synchronized List<Game> games() {
            return new ArrayList<>(games.values());
        }
    }
}
