package mc;

import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import mc.entity.DamageSource;
import mc.entity.Mob;
import mc.entity.MobType;
import mc.net.Connection;
import mc.net.Net;
import mc.net.Protocol;
import mc.net.StatusPing;
import mc.server.NetPlayer;
import mc.server.Server;
import mc.world.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static mc.net.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;

/** A real server on a local port with a client speaking the protocol over TCP. */
class MultiplayerTest {
    /** A decoded packet: its id and a copy of its payload. */
    private record Packet(int id, ByteBuf data) { }

    private final List<Packet> received = new ArrayList<>();

    private void pump(Server srv, World w, Connection c) {
        w.tick();
        srv.tick();
        ByteBuf b;
        while ((b = c.poll()) != null) {
            int id = b.readUnsignedByte();
            received.add(new Packet(id, b.copy()));
            b.release();
        }
    }

    /** Ticks the server until a packet matching the test arrives (or fails after a few seconds). */
    private Packet await(Server srv, World w, Connection c, int id, Predicate<ByteBuf> test) throws Exception {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            pump(srv, w, c);
            for (Packet p : received) {
                if (p.id() == id && test.test(p.data().duplicate())) {
                    received.remove(p);
                    return p;
                }
            }
            Thread.sleep(5);
        }
        fail("No packet " + id + " arrived");
        return null;
    }

    private static void send(Connection c, ByteBuf b) {
        c.sendNow(b);
    }

    @Test
    void clientLogsInGetsChunksAndEditsTheWorld() throws Exception {
        Path dir = Files.createTempDirectory("mp");
        World w = new World(42, new WorldStorage(dir), Dimension.OVERWORLD);
        w.headless = true;
        w.loadAreaBlocking(0, 0, 3);
        Server srv = new Server(w, 0, dir.resolve("players"), null, s -> { }, "Test Server", false);
        srv.spawnX = 8.5;
        srv.spawnY = 120;
        srv.spawnZ = 8.5;
        Connection c = Net.connect("localhost", srv.port()).get(5, TimeUnit.SECONDS);
        try {
            ByteBuf hello = packet(C_HELLO);
            hello.writeInt(VERSION);
            hello.writeByte(INTENT_LOGIN);
            writeString(hello, "Alice");
            send(c, hello);

            Packet login = await(srv, w, c, S_LOGIN, b -> true);
            ByteBuf lb = login.data();
            int myId = lb.readInt();
            assertEquals(42, lb.readLong(), "seed");
            assertTrue(myId > 0);
            assertEquals(1, srv.players().size());
            NetPlayer alice = srv.players().get(0);
            assertEquals("Alice", alice.name);

            // Tell the server where we stand, then expect the chunk under us
            ByteBuf pos = packet(C_POS);
            pos.writeDouble(8.5); pos.writeDouble(100); pos.writeDouble(8.5);
            pos.writeFloat(0); pos.writeFloat(0);
            pos.writeByte(1);
            pos.writeFloat(20);
            pos.writeByte(20);
            send(c, pos);
            Packet chunk = await(srv, w, c, S_CHUNK, b -> b.readInt() == 0 && b.readInt() == 0);
            ByteBuf cb = chunk.data();
            cb.readInt(); cb.readInt();
            byte[] packed = new byte[cb.readInt()];
            cb.readBytes(packed);
            byte[] raw = inflate(packed, Chunk.VOLUME * 2);
            Chunk server = w.getChunk(0, 0);
            for (int i = 0; i < Chunk.VOLUME; i += 97) assertEquals(server.blocks[i], raw[i], "block " + i);
            for (int i = 0; i < 200 && alice.y != 100; i++) { pump(srv, w, c); Thread.sleep(5); }
            assertEquals(100, alice.y, 1e-9);

            // Client places a block: the server world changes
            ByteBuf set = packet(C_SET_BLOCK);
            set.writeInt(8); set.writeInt(101); set.writeInt(8);
            set.writeByte(Block.GOLD_BLOCK.id); set.writeByte(0);
            send(c, set);
            for (int i = 0; i < 50 && w.getBlock(8, 101, 8) != Block.GOLD_BLOCK.id; i++) { pump(srv, w, c); Thread.sleep(5); }
            assertEquals(Block.GOLD_BLOCK.id, w.getBlock(8, 101, 8));

            // A server-side change reaches the client
            w.setBlock(9, 101, 8, Block.DIAMOND_BLOCK.id, 0, true);
            await(srv, w, c, S_BLOCK, b -> b.readInt() == 9 && b.readInt() == 101 && b.readInt() == 8 && b.readUnsignedByte() == Block.DIAMOND_BLOCK.id);

            // Chat is broadcast with the sender's name
            ByteBuf chat = packet(C_CHAT);
            writeString(chat, "hello there");
            send(c, chat);
            await(srv, w, c, S_CHAT, b -> readString(b).equals("<Alice> hello there"));

            // Mobs near the player are sent with their type
            Mob pig = new Mob(MobType.PIG);
            pig.setPos(10.5, 100, 10.5);
            w.addEntity(pig);
            await(srv, w, c, S_SPAWN, b -> b.readInt() == pig.id
                    && JsonParser.parseString(readString(b)).getAsJsonObject().get("mob").getAsString().equals("PIG"));

            // Damage to the server's copy of the player is forwarded to its client
            assertTrue(alice.damage(DamageSource.ATTACK, 3, pig));
            await(srv, w, c, S_HURT, b -> b.readUnsignedByte() == DamageSource.ATTACK.ordinal() && b.readFloat() == 3f && b.readInt() == pig.id);

            // Mining in survival: the server breaks the block, drops it and hands the item over
            w.setBlock(8, 98, 8, Block.STONE.id, 0, false);
            w.setBlock(8, 99, 8, Block.STONE.id, 0, false);
            ByteBuf stand = packet(C_POS);
            stand.writeDouble(8.5); stand.writeDouble(99); stand.writeDouble(8.5);
            stand.writeFloat(0); stand.writeFloat(0);
            stand.writeByte(1);
            stand.writeFloat(20);
            stand.writeByte(20);
            send(c, stand);
            ByteBuf mine = packet(C_BREAK);
            mine.writeInt(8); mine.writeInt(99); mine.writeInt(8);
            writeStack(mine, new mc.item.ItemStack(mc.item.Item.WOODEN_PICKAXE, 1));
            mine.writeBoolean(true);
            send(c, mine);
            await(srv, w, c, S_GIVE, b -> {
                mc.item.ItemStack s = readStack(b);
                return s != null && s.item == mc.item.Item.of(Block.COBBLESTONE);
            });
            assertEquals(0, w.getBlock(8, 99, 8));

            // The server list ping sees the server and its player count
            CompletableFuture<StatusPing.Result> ping = StatusPing.pingAsync("localhost:" + srv.port());
            long deadline = System.currentTimeMillis() + 8000;
            while (!ping.isDone() && System.currentTimeMillis() < deadline) { pump(srv, w, c); Thread.sleep(5); }
            StatusPing.Result r = ping.get(1, TimeUnit.SECONDS);
            assertNull(r.error(), "ping error");
            assertEquals("Test Server", r.motd());
            assertEquals(1, r.online());
        } finally {
            c.close("done");
            srv.stop();
            w.shutdown();
        }
    }

    @Test
    void playersRideVehiclesOverTheNetwork() throws Exception {
        Path dir = Files.createTempDirectory("mp3");
        World w = new World(9, new WorldStorage(dir), Dimension.OVERWORLD);
        w.headless = true;
        w.loadAreaBlocking(0, 0, 2);
        Server srv = new Server(w, 0, dir.resolve("players"), null, s -> { }, "Test", false);
        Connection c = Net.connect("localhost", srv.port()).get(5, TimeUnit.SECONDS);
        try {
            ByteBuf hello = packet(C_HELLO);
            hello.writeInt(VERSION);
            hello.writeByte(INTENT_LOGIN);
            writeString(hello, "Carol");
            send(c, hello);
            await(srv, w, c, S_LOGIN, b -> true);
            NetPlayer carol = srv.players().get(0);
            mc.entity.MinecartEntity cart = new mc.entity.MinecartEntity();
            cart.setPos(carol.x + 1, carol.y, carol.z);
            w.addEntity(cart);
            ByteBuf use = packet(C_INTERACT);
            use.writeInt(cart.id);
            writeStack(use, null);
            send(c, use);
            await(srv, w, c, S_RIDE, b -> b.readInt() == cart.id);
            assertSame(cart, carol.vehicle);
            ByteBuf off = packet(C_STEER);
            off.writeFloat(0);
            off.writeFloat(0);
            off.writeBoolean(true);
            send(c, off);
            await(srv, w, c, S_RIDE, b -> b.readInt() == -1);
            assertNull(carol.vehicle);
            assertNull(cart.passenger);
        } finally {
            c.close("done");
            srv.stop();
            w.shutdown();
        }
    }

    @Test
    void secondPlayerWithTheSameNameIsRejected() throws Exception {
        Path dir = Files.createTempDirectory("mp2");
        World w = new World(7, new WorldStorage(dir), Dimension.OVERWORLD);
        w.headless = true;
        w.loadAreaBlocking(0, 0, 1);
        Server srv = new Server(w, 0, dir.resolve("players"), null, s -> { }, "Test", false);
        Connection a = Net.connect("localhost", srv.port()).get(5, TimeUnit.SECONDS);
        Connection b = Net.connect("localhost", srv.port()).get(5, TimeUnit.SECONDS);
        try {
            for (Connection c : new Connection[]{a, b}) {
                ByteBuf hello = packet(C_HELLO);
                hello.writeInt(VERSION);
                hello.writeByte(INTENT_LOGIN);
                writeString(hello, "Bob");
                send(c, hello);
                await(srv, w, c, c == a ? S_LOGIN : S_KICK, x -> true);
                received.clear();
            }
            assertEquals(1, srv.players().size());
        } finally {
            a.close("done");
            b.close("done");
            srv.stop();
            w.shutdown();
        }
    }

    @Test
    void remoteWorldForwardsEditsInsteadOfSimulating() throws Exception {
        World w = new World(3, null, Dimension.OVERWORLD);
        List<String> sent = new ArrayList<>();
        w.remote = new World.Remote() {
            public void setBlock(int x, int y, int z, int id, int meta) { sent.add("set " + x + " " + y + " " + z + " " + id); }
            public void breakBlock(int x, int y, int z, mc.item.ItemStack tool, boolean drop) { sent.add("break " + x + " " + y + " " + z + " " + drop); }
            public void addEntity(mc.entity.Entity e) { sent.add("entity " + e.getClass().getSimpleName()); }
        };
        byte[] blocks = new byte[Chunk.VOLUME], meta = new byte[Chunk.VOLUME];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) blocks[Chunk.index(x, 10, z)] = (byte) Block.STONE.id;
        w.putNetChunk(new Chunk(0, 0, blocks, meta));
        assertEquals(Block.STONE.id, w.getBlock(3, 10, 3));

        // Placing water: shown locally, sent to the server, but not flowing here
        w.setBlock(3, 11, 3, Block.WATER.id, 0, true);
        assertEquals(List.of("set 3 11 3 " + Block.WATER.id), sent);
        for (int i = 0; i < 20; i++) w.tick();
        assertEquals(0, w.getBlock(4, 11, 3), "no local liquid flow");

        // Breaking a block and dropping an item go to the server
        sent.clear();
        w.breakBlock(5, 10, 5, null, true);
        assertEquals(0, w.getBlock(5, 10, 5));
        w.spawnItem(1, 11, 1, new mc.item.ItemStack(mc.item.Item.STICK, 1));
        assertEquals(List.of("break 5 10 5 true", "entity ItemEntity"), sent);
        assertTrue(w.entities().isEmpty());

        // Changes coming from the server are applied without being echoed back
        sent.clear();
        w.applyingRemote = true;
        w.setBlock(6, 11, 6, Block.GOLD_BLOCK.id, 0, false);
        w.applyingRemote = false;
        assertEquals(Block.GOLD_BLOCK.id, w.getBlock(6, 11, 6));
        assertTrue(sent.isEmpty());
        w.shutdown();
    }

    @Test
    void addressesParse() {
        assertArrayEquals(new String[]{"localhost", "25565"}, Net.hostPort("localhost"));
        assertArrayEquals(new String[]{"10.0.0.2", "1234"}, Net.hostPort("10.0.0.2:1234"));
        assertArrayEquals(new String[]{"::1", "25566"}, Net.hostPort("[::1]:25566"));
        byte[] data = new byte[5000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 7 == 0 ? i : 0);
        assertArrayEquals(data, Protocol.inflate(Protocol.deflate(data), data.length));
    }
}
