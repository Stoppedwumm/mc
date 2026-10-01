package mc.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import mc.client.Options;
import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.net.Connection;
import mc.net.Protocol;
import mc.world.BlockEntity;
import mc.world.Chunk;
import mc.world.EntityCodec;
import mc.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static mc.net.Protocol.*;

/** One connected client: its login, the chunks and entities it has been sent, and its requests. */
final class Session {
    final Server server;
    final Connection connection;
    NetPlayer player;
    private int viewDistance = 8;
    private final LongOpenHashSet sentChunks = new LongOpenHashSet();
    private final Int2ObjectOpenHashMap<Tracked> tracked = new Int2ObjectOpenHashMap<>();
    private BlockEntity openContainer;
    private int ticksAlive;
    /** Vehicle the client was last told it rides (-1 = none). */
    private int lastRiding = -1;

    /** What a client last saw of an entity, to send only changes. */
    private static final class Tracked {
        double x, y, z;
        float yaw, pitch, head;
        int flags;
        String state;
    }

    Session(Server server, Connection connection) {
        this.server = server;
        this.connection = connection;
    }

    String name() {
        return player != null ? player.name : connection.remoteAddress();
    }

    int viewDistance() {
        return Math.max(2, Math.min(viewDistance, server.viewDistanceCap));
    }

    World world() { return server.world; }

    void kick(String reason) {
        ByteBuf b = packet(S_KICK);
        writeString(b, reason);
        connection.sendNow(b);
        connection.close(reason);
    }

    // ------------------------------------------------------------------ incoming

    void handlePackets() {
        ByteBuf b;
        int budget = 400;
        while (budget-- > 0 && (b = connection.poll()) != null) {
            try {
                handle(b.readUnsignedByte(), b);
            } finally {
                b.release();
            }
        }
        ticksAlive++;
        if (player == null && ticksAlive > 200) kick("Took too long to log in");
    }

    private void handle(int id, ByteBuf b) {
        if (player == null) {
            if (id != C_HELLO) return;
            int version = b.readInt();
            int intent = b.readUnsignedByte();
            String name = readString(b);
            if (intent == INTENT_STATUS) {
                connection.sendNow(server.statusPacket());
                connection.close("Status");
                return;
            }
            if (version != VERSION) { kick("Outdated " + (version < VERSION ? "client" : "server") + " (protocol " + VERSION + ")"); return; }
            login(name);
            return;
        }
        switch (id) {
            case C_POS -> {
                double x = b.readDouble(), y = b.readDouble(), z = b.readDouble();
                float yaw = b.readFloat(), pitch = b.readFloat();
                int flags = b.readUnsignedByte();
                float health = b.readFloat();
                int food = b.readUnsignedByte();
                player.moveFromClient(x, y, z, yaw, pitch);
                player.onGround = (flags & 1) != 0;
                player.sneaking = (flags & 2) != 0;
                player.sprinting = (flags & 4) != 0;
                player.creative = (flags & 8) != 0;
                player.flying = (flags & 16) != 0;
                player.health = health;
                player.food = food;
                if (player.health > 0) player.deathTime = 0;
            }
            case C_EQUIP -> {
                player.inventory.selected = Math.max(0, Math.min(8, b.readUnsignedByte()));
                ItemStack held = readStack(b);
                java.util.Arrays.fill(player.inventory.slots, null);
                player.inventory.slots[player.inventory.selected] = held;
                for (int i = 0; i < 4; i++) player.inventory.armor[i] = readStack(b);
            }
            case C_SET_BLOCK -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt(), block = b.readUnsignedByte(), meta = b.readUnsignedByte();
                if (!inReach(x, y, z)) { blockChanged(x, y, z, world().getBlock(x, y, z), world().getMeta(x, y, z)); return; }
                world().setBlock(x, y, z, block, meta, true);
                world().checkFalling(x, y, z);
            }
            case C_BREAK -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt();
                ItemStack tool = readStack(b);
                boolean drop = b.readBoolean();
                if (!inReach(x, y, z)) { blockChanged(x, y, z, world().getBlock(x, y, z), world().getMeta(x, y, z)); return; }
                world().breakBlock(x, y, z, tool, drop);
            }
            case C_ADD_ENTITY -> {
                JsonObject o = JsonParser.parseString(readString(b)).getAsJsonObject();
                Entity e = EntityCodec.readNet(o, player);
                if (e == null || e instanceof Player || e.distanceSq(player.x, player.y, player.z) > 16 * 16) return;
                world().addEntity(e);
            }
            case C_ATTACK -> {
                int target = b.readInt();
                float damage = b.readFloat();
                float extraKnockback = b.readFloat();
                int fire = b.readInt();
                int looting = b.readUnsignedByte();
                Entity e = find(target);
                if (e instanceof Vehicle v && v.distanceTo(player) < 8) { v.hit(player, damage); return; }
                if (!(e instanceof LivingEntity le) || le.distanceTo(player) > 8) return;
                if (le instanceof Mob m) m.lootingBonus = looting;
                if (le.damage(DamageSource.ATTACK, damage, player)) {
                    if (extraKnockback > 0) {
                        double r = Math.toRadians(player.yaw);
                        le.knockback(extraKnockback, Math.sin(r), -Math.cos(r));
                    }
                    if (fire > 0 && !le.fireImmune()) le.fireTicks = Math.max(le.fireTicks, fire);
                }
            }
            case C_INTERACT -> {
                int target = b.readInt();
                ItemStack held = readStack(b);
                Entity e = find(target);
                if (e instanceof Vehicle v) {
                    if (v.distanceTo(player) < 8 && player.vehicle == null) v.mount(player);
                    return;
                }
                java.util.Arrays.fill(player.inventory.slots, null);
                player.inventory.slots[player.inventory.selected] = held;
                if (e instanceof Mob m && m.distanceTo(player) < 8) m.interact(player);
                // The client's held item becomes whatever is left; anything else the interaction gave goes back too
                ByteBuf r = packet(S_SET_HELD);
                writeStack(r, player.inventory.held());
                connection.send(r);
                for (int i = 0; i < player.inventory.slots.length; i++) {
                    if (i == player.inventory.selected || ItemStack.isEmpty(player.inventory.slots[i])) continue;
                    give(player.inventory.slots[i]);
                    player.inventory.slots[i] = null;
                }
            }
            case C_CHAT -> {
                String msg = readString(b).trim();
                if (msg.isEmpty()) return;
                if (msg.length() > 256) msg = msg.substring(0, 256);
                if (msg.startsWith("/")) {
                    String reply = server.command(msg, player);
                    if (!reply.isEmpty()) sendChat(reply);
                } else server.broadcastChat("<" + player.name + "> " + msg);
            }
            case C_OPEN -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt();
                openContainer = inReach(x, y, z) ? world().getOrCreateBlockEntity(x, y, z) : null;
                if (openContainer != null) sendContainer();
            }
            case C_CONTAINER -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt();
                ItemStack[] slots = readStacks(b);
                BlockEntity be = world().getBlockEntity(x, y, z);
                if (be != null && be == openContainer) {
                    for (int i = 0; i < Math.min(slots.length, be.slots.length); i++) be.slots[i] = slots[i];
                }
            }
            case C_CLOSE -> openContainer = null;
            case C_SWING -> {
                for (Session s : server.sessions) if (s != this && s.player != null) s.sendEvent(player.id, EV_SWING);
                player.swingTicks = 6;
            }
            case C_STEER -> {
                float forward = b.readFloat(), strafe = b.readFloat();
                boolean off = b.readBoolean();
                Vehicle v = player.vehicle;
                if (v == null) return;
                if (off) v.dismount();
                else {
                    v.riderForward = Math.max(-1, Math.min(1, forward));
                    v.riderStrafe = Math.max(-1, Math.min(1, strafe));
                }
            }
            case C_REEL -> {
                for (Entity e : world().entities())
                    if (e instanceof FishingBobberEntity f && f.owner == player && !f.removed) f.reel();
            }
            case C_VIEW -> viewDistance = b.readUnsignedByte();
            case C_RESPAWN -> {
                player.health = player.maxHealth;
                player.deathTime = 0;
            }
            default -> { }
        }
    }

    private boolean inReach(int x, int y, int z) {
        return player.distanceSq(x + 0.5, y + 0.5, z + 0.5) < 12 * 12 && world().isLoaded(x, z);
    }

    private Entity find(int id) {
        for (Entity e : world().entities()) if (e.id == id) return e;
        for (Player p : world().players()) if (p.id == id) return p;
        return null;
    }

    private void login(String rawName) {
        String name = rawName.replaceAll("[^A-Za-z0-9_]", "");
        if (name.isEmpty() || name.length() > 16) { kick("Invalid name"); return; }
        if (server.nameTaken(name)) { kick("A player called " + name + " is already playing"); return; }
        if (server.online() >= server.maxPlayers) { kick("The server is full"); return; }
        NetPlayer p = new NetPlayer(this, name);
        Options.Level data = server.loadPlayer(name);
        boolean fresh = data == null || !data.spawned;
        if (fresh) {
            data = new Options.Level();
            double sx = server.spawnX, sz = server.spawnZ;
            double sy = server.spawnY > 0 ? server.spawnY : world().generator.estimateHeight((int) Math.floor(sx), (int) Math.floor(sz)) + 2;
            data.x = sx; data.y = sy; data.z = sz;
            data.spawnX = sx; data.spawnY = sy; data.spawnZ = sz;
            data.spawned = true;
        }
        // Fill the fields without triggering a teleport packet
        Options.Level copy = data;
        copy.readPlayer(p);
        p.prevX = p.x; p.prevY = p.y; p.prevZ = p.z;
        player = p;
        world().addNetworkPlayer(p);
        ByteBuf b = packet(S_LOGIN);
        b.writeInt(p.id);
        b.writeLong(world().seed);
        b.writeLong(world().time);
        b.writeByte(world().dimension.ordinal());
        b.writeBoolean(world().raining);
        b.writeBoolean(fresh);
        writeString(b, new com.google.gson.Gson().toJson(copy));
        connection.sendNow(b);
        server.log(name + " joined the game from " + connection.remoteAddress());
        server.broadcastChat("§e" + name + " joined the game");
    }

    // ------------------------------------------------------------------ outgoing

    void tick() {
        if (player == null) return;
        sendChunks();
        trackEntities();
        int riding = player.vehicle != null && !player.vehicle.removed ? player.vehicle.id : -1;
        if (riding != lastRiding) {
            lastRiding = riding;
            ByteBuf b = packet(S_RIDE);
            b.writeInt(riding);
            connection.send(b);
        }
        pickups();
        if (server.ticks % 20 == 0) sendTime();
        if (openContainer != null && server.ticks % 4 == 0) {
            if (world().getBlockEntity(openContainer.x, openContainer.y, openContainer.z) != openContainer) openContainer = null;
            else sendContainer();
        }
        if (server.ticks % 20 == 10) connection.send(packet(S_KEEPALIVE));
    }

    private void sendChunks() {
        int pcx = (int) Math.floor(player.x) >> 4, pcz = (int) Math.floor(player.z) >> 4;
        int vd = viewDistance();
        // Forget chunks out of range
        List<Long> drop = new ArrayList<>();
        for (long k : sentChunks) {
            int cx = (int) (k >> 32), cz = (int) k;
            int dx = cx - pcx, dz = cz - pcz;
            if (dx * dx + dz * dz > (vd + 2) * (vd + 2)) drop.add(k);
        }
        for (long k : drop) {
            sentChunks.remove(k);
            ByteBuf b = packet(S_UNLOAD);
            b.writeInt((int) (k >> 32));
            b.writeInt((int) k);
            connection.send(b);
        }
        int sent = 0;
        for (int r = 0; r <= vd && sent < 6; r++)
            for (int dx = -r; dx <= r && sent < 6; dx++)
                for (int dz = -r; dz <= r && sent < 6; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || dx * dx + dz * dz > vd * vd + vd) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    long k = Chunk.key(cx, cz);
                    if (sentChunks.contains(k)) continue;
                    Chunk c = world().getChunk(cx, cz);
                    if (c == null || c.state != Chunk.STATE_DECORATED || !world().isChunkFinal(cx, cz)) continue;
                    sendChunk(c);
                    sentChunks.add(k);
                    sent++;
                }
    }

    private void sendChunk(Chunk c) {
        int volume = c.blocks.length;
        byte[] raw = new byte[volume * 2];
        System.arraycopy(c.blocks, 0, raw, 0, volume);
        System.arraycopy(c.meta, 0, raw, volume, volume);
        byte[] packed = deflate(raw);
        ByteBuf b = packet(S_CHUNK);
        b.writeInt(c.cx);
        b.writeInt(c.cz);
        b.writeInt(packed.length);
        b.writeBytes(packed);
        connection.send(b);
    }

    boolean hasChunk(int cx, int cz) {
        return sentChunks.contains(Chunk.key(cx, cz));
    }

    void blockChanged(int x, int y, int z, int id, int meta) {
        if (!hasChunk(x >> 4, z >> 4)) return;
        ByteBuf b = packet(S_BLOCK);
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        b.writeByte(id);
        b.writeByte(meta);
        connection.send(b);
    }

    /** Spawns, moves, updates and removes the client's copies of nearby entities and players. */
    private void trackEntities() {
        Set<Integer> seen = new HashSet<>();
        List<Entity> candidates = new ArrayList<>(world().entities());
        for (Player p : world().players()) if (p != player) candidates.add(p);
        boolean stateTick = server.ticks % 4 == 0;
        for (Entity e : candidates) {
            if (e.removed) continue;
            double dx = e.x - player.x, dz = e.z - player.z;
            if (dx * dx + dz * dz > 80 * 80 || !hasChunk((int) Math.floor(e.x) >> 4, (int) Math.floor(e.z) >> 4)) continue;
            seen.add(e.id);
            Tracked t = tracked.get(e.id);
            float head = e instanceof LivingEntity le ? le.headYaw : e.yaw;
            int flags = (e.onGround ? 1 : 0) | (e instanceof Player p && p.sneaking ? 2 : 0);
            if (t == null) {
                t = new Tracked();
                tracked.put(e.id, t);
                JsonObject o = EntityCodec.writeNet(e);
                t.state = o.toString();
                ByteBuf b = packet(S_SPAWN);
                b.writeInt(e.id);
                writeString(b, t.state);
                connection.send(b);
            } else {
                boolean fast = e instanceof Mob m && (m.fuse > 0 || m.ghastCharge > 0);
                if (stateTick || fast) {
                    String state = EntityCodec.writeNet(e).toString();
                    if (!state.equals(t.state) && (stateTick || !sameExceptPosition(state, t.state))) {
                        t.state = state;
                        ByteBuf b = packet(S_STATE);
                        b.writeInt(e.id);
                        writeString(b, state);
                        connection.send(b);
                    }
                }
                if (Math.abs(e.x - t.x) < 1e-3 && Math.abs(e.y - t.y) < 1e-3 && Math.abs(e.z - t.z) < 1e-3
                        && Math.abs(e.yaw - t.yaw) < 0.5f && Math.abs(e.pitch - t.pitch) < 0.5f && Math.abs(head - t.head) < 0.5f && flags == t.flags) {
                    if (e instanceof LivingEntity le && le.hurtTime == 10) sendEvent(e.id, EV_HURT);
                    continue;
                }
            }
            t.x = e.x; t.y = e.y; t.z = e.z; t.yaw = e.yaw; t.pitch = e.pitch; t.head = head; t.flags = flags;
            ByteBuf b = packet(S_MOVE);
            b.writeInt(e.id);
            b.writeDouble(e.x);
            b.writeDouble(e.y);
            b.writeDouble(e.z);
            b.writeFloat(e.yaw);
            b.writeFloat(e.pitch);
            b.writeFloat(head);
            b.writeByte(flags);
            connection.send(b);
            if (e instanceof LivingEntity le && le.hurtTime == 10) sendEvent(e.id, EV_HURT);
        }
        for (var it = tracked.int2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            var en = it.next();
            if (seen.contains(en.getIntKey())) continue;
            ByteBuf b = packet(S_REMOVE);
            b.writeInt(en.getIntKey());
            connection.send(b);
            it.remove();
        }
    }

    /** States differ only by position fields (already covered by move packets). */
    private static boolean sameExceptPosition(String a, String b) {
        if (a == null || b == null) return false;
        JsonObject x = JsonParser.parseString(a).getAsJsonObject(), y = JsonParser.parseString(b).getAsJsonObject();
        for (String k : new String[]{"x", "y", "z", "yaw", "mx", "my", "mz", "age", "pitch", "head"}) { x.remove(k); y.remove(k); }
        return x.equals(y);
    }

    /** Items, experience and arrows the player walks over. */
    private void pickups() {
        if (player.isDead()) return;
        var box = player.box();
        box.minX -= 1; box.maxX += 1; box.minY -= 0.5; box.maxY += 0.5; box.minZ -= 1; box.maxZ += 1;
        for (Entity e : world().entities()) {
            if (e.removed) continue;
            if (e instanceof ItemEntity it && it.pickupDelay == 0 && box.intersects(e.box())) {
                give(it.stack.copy());
                it.remove();
                world().playSound("pop", e.x, e.y, e.z, 0.2f, 1.6f + (float) Math.random() * 0.4f);
            } else if (e instanceof XpOrbEntity orb && orb.pickupDelay == 0 && box.intersects(e.box())) {
                orb.remove();
                ByteBuf b = packet(S_XP);
                b.writeInt(orb.value);
                connection.send(b);
            } else if (e instanceof ArrowEntity a && a.inGround && a.pickup && a.shooter == player && a.distanceSq(player.x, player.y, player.z) < 2.5) {
                a.remove();
                give(new ItemStack(Item.ARROW, 1));
            }
        }
    }

    void give(ItemStack s) {
        if (ItemStack.isEmpty(s)) return;
        ByteBuf b = packet(S_GIVE);
        writeStack(b, s);
        connection.send(b);
    }

    void sendTime() {
        ByteBuf b = packet(S_TIME);
        b.writeLong(world().time);
        b.writeBoolean(world().raining);
        connection.send(b);
    }

    private void sendContainer() {
        BlockEntity be = openContainer;
        ByteBuf b = packet(S_CONTAINER);
        b.writeInt(be.x);
        b.writeInt(be.y);
        b.writeInt(be.z);
        writeStacks(b, be.slots);
        int f1 = 0, f2 = 0, f3 = 0;
        if (be instanceof BlockEntity.Furnace f) { f1 = f.burnTime; f2 = f.burnTotal; f3 = f.cookTime; }
        if (be instanceof BlockEntity.BrewingStand bs) { f1 = bs.brewTime; f2 = bs.fuel; }
        b.writeInt(f1);
        b.writeInt(f2);
        b.writeInt(f3);
        connection.send(b);
    }

    void sendEvent(int entityId, int event) {
        ByteBuf b = packet(S_EVENT);
        b.writeInt(entityId);
        b.writeByte(event);
        connection.send(b);
    }

    void sendSound(String name, double x, double y, double z, float volume, float pitch) {
        ByteBuf b = packet(S_SOUND);
        writeString(b, name);
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        b.writeFloat(volume);
        b.writeFloat(pitch);
        connection.send(b);
    }

    void sendParticle(String type, double x, double y, double z) {
        ByteBuf b = packet(S_PARTICLE);
        writeString(b, type);
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        connection.send(b);
    }

    void sendBroken(int x, int y, int z, int id, int meta) {
        if (!hasChunk(x >> 4, z >> 4)) return;
        ByteBuf b = packet(S_BROKEN);
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        b.writeByte(id);
        b.writeByte(meta);
        connection.send(b);
    }

    void sendChat(String msg) {
        ByteBuf b = packet(S_CHAT);
        writeString(b, msg);
        connection.send(b);
    }

    void sendHurt(DamageSource source, float amount, Entity attacker) {
        ByteBuf b = packet(S_HURT);
        b.writeByte(source.ordinal());
        b.writeFloat(amount);
        b.writeInt(attacker != null ? attacker.id : -1);
        connection.send(b);
    }

    void sendKnockback(double strength, double dx, double dz) {
        ByteBuf b = packet(S_KNOCKBACK);
        b.writeDouble(strength);
        b.writeDouble(dx);
        b.writeDouble(dz);
        connection.send(b);
    }

    void sendEffect(Effect e, int amplifier, int duration) {
        ByteBuf b = packet(S_EFFECT);
        b.writeByte(e.ordinal());
        b.writeInt(amplifier);
        b.writeInt(duration);
        connection.send(b);
    }

    void sendHeal(float amount) {
        ByteBuf b = packet(S_HEAL);
        b.writeFloat(amount);
        connection.send(b);
    }

    void sendTeleport(double x, double y, double z) {
        ByteBuf b = packet(S_TELEPORT);
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        connection.send(b);
    }
}
