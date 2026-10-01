package mc.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mc.entity.*;
import mc.item.ItemStack;
import mc.net.Connection;
import mc.net.Net;
import mc.net.Protocol;
import mc.world.Block;
import mc.world.BlockEntity;
import mc.world.Chunk;
import mc.world.Dimension;
import mc.world.EntityCodec;
import mc.world.World;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static mc.net.Protocol.*;

/**
 * The client side of a multiplayer game. The local player moves, fights and manages its inventory itself (and
 * reports its position every tick); the world, mobs and other players are copies kept up to date by the server,
 * and every change the player makes to the world is sent to the server.
 */
final class MultiplayerSession implements World.Remote {
    private final Game g;
    final String address;
    private final CompletableFuture<Connection> connecting;
    private Connection connection;
    private String status = "Connecting to the server...";
    private boolean loggedIn;
    private final Int2ObjectOpenHashMap<Entity> entities = new Int2ObjectOpenHashMap<>();
    private long ticks;
    private final long startTime = System.currentTimeMillis();
    // Last equipment and container contents sent, to send only changes
    private String lastEquip = "";
    private BlockEntity lastContainer;
    private String lastContainerSig = "";
    private boolean wasRiding;

    MultiplayerSession(Game g, String address) {
        this.g = g;
        this.address = address;
        String[] hp = Net.hostPort(address);
        connecting = Net.connect(hp[0], Net.port(hp));
    }

    String status() { return status; }

    boolean loggedIn() { return loggedIn; }

    // ------------------------------------------------------------------ lifecycle

    /** Runs every client tick (also while the world is loading). */
    void tick() {
        if (ended) return;
        ticks++;
        if (connection == null) {
            if (!connecting.isDone()) {
                if (System.currentTimeMillis() - startTime > 10_000) fail("Failed to connect to the server", "Connection timed out");
                return;
            }
            try {
                connection = connecting.get(0, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                fail("Failed to connect to the server", c.getMessage() != null ? c.getMessage() : c.toString());
                return;
            }
            ByteBuf hello = packet(C_HELLO);
            hello.writeInt(VERSION);
            hello.writeByte(INTENT_LOGIN);
            writeString(hello, g.options.playerName);
            connection.sendNow(hello);
            status = "Logging in...";
        }
        ByteBuf b;
        int budget = 4000;
        while (budget-- > 0 && connection != null && (b = connection.poll()) != null) {
            try {
                handle(b.readUnsignedByte(), b);
            } catch (RuntimeException e) {
                e.printStackTrace();
                b.release();
                fail("Connection Lost", "Bad packet from the server: " + e);
                return;
            }
            b.release();
            if (ended) return;
        }
        if (connection == null) return;
        if (!connection.isOpen()) {
            fail(loggedIn ? "Connection Lost" : "Failed to connect to the server", connection.closeReason());
            return;
        }
        if (ticks % 20 == 0) connection.send(packet(C_KEEPALIVE));
        connection.flush();
    }

    /** After the world ticked: report the player and any changes to equipment and open containers. */
    void afterTick() {
        if (!loggedIn || connection == null) return;
        Player p = g.player;
        ByteBuf b = packet(C_POS);
        b.writeDouble(p.x);
        b.writeDouble(p.y);
        b.writeDouble(p.z);
        b.writeFloat(p.yaw);
        b.writeFloat(p.pitch);
        b.writeByte((p.onGround ? 1 : 0) | (p.sneaking ? 2 : 0) | (p.sprinting ? 4 : 0) | (p.creative ? 8 : 0) | (p.flying ? 16 : 0));
        b.writeFloat(p.health);
        b.writeByte(Math.max(0, Math.min(255, p.food)));
        connection.send(b);

        // Steering whatever we ride, and telling the server when we got off
        if (p.vehicle != null) {
            ByteBuf s = packet(C_STEER);
            s.writeFloat(p.vehicle.riderForward);
            s.writeFloat(p.vehicle.riderStrafe);
            s.writeBoolean(false);
            connection.send(s);
        } else if (wasRiding) {
            ByteBuf s = packet(C_STEER);
            s.writeFloat(0);
            s.writeFloat(0);
            s.writeBoolean(true);
            connection.send(s);
        }
        wasRiding = p.vehicle != null;

        String equip = p.inventory.selected + ":" + sig(p.inventory.held()) + ":" + sig(p.inventory.armor);
        if (!equip.equals(lastEquip)) {
            lastEquip = equip;
            ByteBuf e = packet(C_EQUIP);
            e.writeByte(p.inventory.selected);
            writeStack(e, p.inventory.held());
            for (int i = 0; i < 4; i++) writeStack(e, p.inventory.armor[i]);
            connection.send(e);
        }

        BlockEntity open = g.screens.openContainer();
        if (open != lastContainer) {
            if (lastContainer != null) connection.send(packet(C_CLOSE));
            if (open != null) {
                ByteBuf o = packet(C_OPEN);
                o.writeInt(open.x);
                o.writeInt(open.y);
                o.writeInt(open.z);
                connection.send(o);
            }
            lastContainer = open;
            lastContainerSig = open == null ? "" : sig(open.slots);
        } else if (open != null) {
            String s = sig(open.slots);
            if (!s.equals(lastContainerSig)) {
                lastContainerSig = s;
                ByteBuf c = packet(C_CONTAINER);
                c.writeInt(open.x);
                c.writeInt(open.y);
                c.writeInt(open.z);
                writeStacks(c, open.slots);
                connection.send(c);
            }
        }
        connection.flush();
    }

    private static String sig(ItemStack... stacks) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : stacks) sb.append(ItemStack.isEmpty(s) ? "-" : Arrays.toString(s.toArray())).append(';');
        return sb.toString();
    }

    private boolean ended;

    private void fail(String title, String reason) {
        if (ended) return;
        ended = true;
        disconnect(null);
        g.multiplayerEnded(title, reason);
    }

    /** Closes the connection and drops the world copy. */
    void disconnect(String reason) {
        if (connection != null) {
            connection.flush();
            connection.close(reason == null ? "Disconnected" : reason);
        } else connecting.thenAccept(c -> c.close("Cancelled"));
        loggedIn = false;
    }

    // ------------------------------------------------------------------ outgoing (World.Remote and actions)

    @Override
    public void setBlock(int x, int y, int z, int id, int meta) {
        if (connection == null) return;
        ByteBuf b = packet(C_SET_BLOCK);
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        b.writeShort(id);
        b.writeByte(meta);
        connection.send(b);
    }

    @Override
    public void breakBlock(int x, int y, int z, ItemStack tool, boolean drop) {
        if (connection == null) return;
        ByteBuf b = packet(C_BREAK);
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        writeStack(b, tool);
        b.writeBoolean(drop);
        connection.send(b);
    }

    @Override
    public void addEntity(Entity e) {
        if (connection == null || e instanceof Player) return;
        ByteBuf b = packet(C_ADD_ENTITY);
        writeString(b, EntityCodec.writeNet(e).toString());
        connection.send(b);
    }

    void attack(Entity target, float damage, float extraKnockback, int fireTicks, int looting) {
        if (connection == null || target.netId == 0) return;
        ByteBuf b = packet(C_ATTACK);
        b.writeInt(target.netId);
        b.writeFloat(damage);
        b.writeFloat(extraKnockback);
        b.writeInt(fireTicks);
        b.writeByte(looting);
        connection.send(b);
    }

    void interact(Entity target, ItemStack held) {
        if (connection == null || target.netId == 0) return;
        ByteBuf b = packet(C_INTERACT);
        b.writeInt(target.netId);
        writeStack(b, held);
        connection.send(b);
    }

    void reel() {
        if (connection != null) connection.send(packet(C_REEL));
    }

    void chat(String msg) {
        if (connection == null) return;
        ByteBuf b = packet(C_CHAT);
        writeString(b, msg);
        connection.send(b);
    }

    void swing() {
        if (connection != null && loggedIn) connection.send(packet(C_SWING));
    }

    void respawned() {
        if (connection != null) connection.send(packet(C_RESPAWN));
    }

    void sendViewDistance() {
        if (connection == null) return;
        ByteBuf b = packet(C_VIEW);
        b.writeByte(Math.min(32, g.options.renderDistance));
        connection.send(b);
    }

    // ------------------------------------------------------------------ incoming

    private World world() { return g.world; }

    private void handle(int id, ByteBuf b) {
        if (!loggedIn) {
            if (id == S_LOGIN) login(b);
            else if (id == S_KICK) {
                String reason = readString(b);
                fail("Failed to connect to the server", reason);
            }
            return;
        }
        World w = world();
        if (w == null) return;
        switch (id) {
            case S_CHUNK -> {
                int cx = b.readInt(), cz = b.readInt(), len = b.readInt();
                byte[] packed = new byte[len];
                b.readBytes(packed);
                byte[] raw = inflate(packed, Chunk.VOLUME * 3);
                Chunk c = Chunk.unpack(cx, cz, raw, 0);
                w.computeBiomeData(c);
                w.putNetChunk(c);
            }
            case S_UNLOAD -> w.removeNetChunk(b.readInt(), b.readInt());
            case S_BLOCK -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt(), block = b.readUnsignedShort(), meta = b.readUnsignedByte();
                if (w.getBlock(x, y, z) == block && w.getMeta(x, y, z) == meta) return;
                w.applyingRemote = true;
                try {
                    w.setBlock(x, y, z, block, meta, false);
                } finally {
                    w.applyingRemote = false;
                }
            }
            case S_TIME -> {
                long t = b.readLong();
                boolean rain = b.readBoolean();
                if (Math.abs(t - w.time) > 5) w.time = t;
                w.raining = rain;
            }
            case S_SPAWN -> {
                int eid = b.readInt();
                JsonObject o = JsonParser.parseString(readString(b)).getAsJsonObject();
                Entity old = entities.remove(eid);
                if (old != null) old.remove();
                Entity e = EntityCodec.readNet(o, null);
                if (e == null) return;
                e.netId = eid;
                float head = o.has("head") ? o.get("head").getAsFloat() : e.yaw;
                e.setNetTarget(e.x, e.y, e.z, e.yaw, e.pitch, head);
                if (e instanceof LivingEntity le) le.bodyYaw = le.prevBodyYaw = e.yaw;
                w.addNetEntity(e);
                entities.put(eid, e);
            }
            case S_MOVE -> {
                int eid = b.readInt();
                double x = b.readDouble(), y = b.readDouble(), z = b.readDouble();
                float yaw = b.readFloat(), pitch = b.readFloat(), head = b.readFloat();
                int flags = b.readUnsignedByte();
                Entity e = entities.get(eid);
                if (e == null) return;
                e.setNetTarget(x, y, z, yaw, pitch, head);
                e.onGround = (flags & 1) != 0;
                if (e instanceof Player p) p.sneaking = (flags & 2) != 0;
            }
            case S_RIDE -> {
                Entity v = entities.get(b.readInt());
                Player p = g.player;
                if (p.vehicle != null && p.vehicle != v) {
                    p.vehicle.passenger = null;
                    p.vehicle = null;
                }
                if (v instanceof Vehicle veh && p.vehicle == null) {
                    if (veh.passenger != null) veh.passenger.vehicle = null;
                    veh.passenger = null;
                    veh.mount(p);
                }
                wasRiding = p.vehicle != null;
            }
            case S_STATE -> {
                int eid = b.readInt();
                String json = readString(b);
                Entity e = entities.get(eid);
                if (e != null) EntityCodec.applyState(e, JsonParser.parseString(json).getAsJsonObject());
            }
            case S_REMOVE -> {
                Entity e = entities.remove(b.readInt());
                if (e != null) e.remove();
            }
            case S_EVENT -> {
                Entity e = entities.get(b.readInt());
                int ev = b.readUnsignedByte();
                if (e instanceof LivingEntity le && ev == EV_HURT) le.hurtTime = 10;
                if (e instanceof Player p && ev == EV_SWING) p.swingTicks = 6;
            }
            case S_SOUND -> {
                String name = readString(b);
                double x = b.readDouble(), y = b.readDouble(), z = b.readDouble();
                float vol = b.readFloat(), pitch = b.readFloat();
                g.sound.play(name, x, y, z, vol, pitch);
            }
            case S_PARTICLE -> {
                String type = readString(b);
                g.particles.spawn(type, b.readDouble(), b.readDouble(), b.readDouble());
            }
            case S_BROKEN -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt(), block = b.readUnsignedShort(), meta = b.readUnsignedByte();
                g.blockBroken(x, y, z, Block.get(block), meta);
            }
            case S_CHAT -> g.hud.chat(readString(b));
            case S_HURT -> {
                DamageSource src = DamageSource.values()[Math.min(DamageSource.values().length - 1, b.readUnsignedByte())];
                float amount = b.readFloat();
                Entity attacker = entities.get(b.readInt());
                g.player.damage(src, amount, attacker);
            }
            case S_KNOCKBACK -> g.player.knockback(b.readDouble(), b.readDouble(), b.readDouble());
            case S_EFFECT -> {
                Effect e = Effect.values()[b.readUnsignedByte()];
                g.player.addEffect(e, b.readInt(), b.readInt());
            }
            case S_HEAL -> g.player.heal(b.readFloat());
            case S_GIVE -> {
                ItemStack s = readStack(b);
                if (s == null) return;
                ItemStack left = g.player.inventory.add(s);
                g.sound.play("pop", g.player.x, g.player.y, g.player.z, 0.2f, 1.6f + g.random.nextFloat() * 0.4f);
                if (left.count > 0) {
                    ItemEntity drop = new ItemEntity(left);
                    drop.setPos(g.player.x, g.player.y + 1, g.player.z);
                    drop.pickupDelay = 40;
                    w.addEntity(drop);
                }
            }
            case S_XP -> {
                int before = g.player.xpLevel;
                g.player.addXp(b.readInt());
                if (g.player.xpLevel > before && g.player.xpLevel % 5 == 0) g.sound.play("levelup", g.player.x, g.player.y, g.player.z, 0.75f, 1);
                else g.sound.play("orb", g.player.x, g.player.y, g.player.z, 0.1f, 0.5f + g.random.nextFloat() * 0.9f);
            }
            case S_SET_HELD -> g.player.inventory.setHeld(readStack(b));
            case S_CONTAINER -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt();
                ItemStack[] slots = readStacks(b);
                int f1 = b.readInt(), f2 = b.readInt(), f3 = b.readInt();
                BlockEntity be = w.getOrCreateBlockEntity(x, y, z);
                if (be == null) return;
                for (int i = 0; i < Math.min(slots.length, be.slots.length); i++) be.slots[i] = slots[i];
                if (be instanceof BlockEntity.Furnace f) { f.burnTime = f1; f.burnTotal = f2; f.cookTime = f3; }
                if (be instanceof BlockEntity.BrewingStand bs) { bs.brewTime = f1; bs.fuel = f2; }
                if (be == lastContainer) lastContainerSig = sig(be.slots);
            }
            case S_TELEPORT -> {
                g.player.setPos(b.readDouble(), b.readDouble(), b.readDouble());
                g.player.motionX = g.player.motionY = g.player.motionZ = 0;
                g.player.fallDistance = 0;
            }
            case S_KICK -> fail("Disconnected", readString(b));
            default -> { }
        }
    }

    private void login(ByteBuf b) {
        int myId = b.readInt();
        long seed = b.readLong(), time = b.readLong();
        int dim = b.readUnsignedByte();
        boolean raining = b.readBoolean(), fresh = b.readBoolean();
        Options.Level level = new Gson().fromJson(readString(b), Options.Level.class);
        level.seed = seed;
        World w = new World(seed, null, Dimension.values()[Math.min(dim, Dimension.values().length - 1)]);
        w.remote = this;
        w.time = time;
        w.raining = raining;
        loggedIn = true;
        status = "Downloading terrain...";
        g.enterRemoteWorld(w, level, fresh);
        sendViewDistance();
        connection.flush();
    }
}
