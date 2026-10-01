package mc.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Converts persistent entities (mobs, dropped items, experience orbs) to and from JSON for chunk storage. */
public final class EntityCodec {
    private EntityCodec() { }

    public static boolean persistent(Entity e) {
        return !e.removed && (e instanceof Mob m ? !m.isDead() : e instanceof ItemEntity || e instanceof XpOrbEntity || e instanceof Vehicle);
    }

    private static JsonArray stack(ItemStack s) {
        JsonArray a = new JsonArray();
        for (int v : s.toArray()) a.add(v);
        return a;
    }

    private static ItemStack stack(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        JsonArray a = e.getAsJsonArray();
        int[] v = new int[a.size()];
        for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsInt();
        return ItemStack.fromArray(v);
    }

    public static JsonObject write(Entity e) {
        JsonObject o = new JsonObject();
        o.addProperty("x", e.x);
        o.addProperty("y", e.y);
        o.addProperty("z", e.z);
        o.addProperty("yaw", e.yaw);
        o.addProperty("mx", e.motionX);
        o.addProperty("my", e.motionY);
        o.addProperty("mz", e.motionZ);
        o.addProperty("fire", e.fireTicks);
        o.addProperty("age", e.age);
        if (e instanceof Mob m) {
            o.addProperty("kind", "mob");
            o.addProperty("mob", m.type.name());
            o.addProperty("health", m.health);
            o.addProperty("color", m.sheepColor);
            o.addProperty("sheared", m.sheared);
            o.addProperty("tamed", m.tamed);
            o.addProperty("growing", m.growingAge);
            o.addProperty("sitting", m.sitting);
            o.addProperty("size", m.slimeSize);
            o.addProperty("carried", m.carriedBlock);
            o.addProperty("profession", m.profession);
            JsonArray armor = new JsonArray();
            for (ItemStack s : m.armor) armor.add(ItemStack.isEmpty(s) ? null : stack(s));
            o.add("armor", armor);
        } else if (e instanceof ItemEntity it) {
            o.addProperty("kind", "item");
            o.add("stack", stack(it.stack));
            o.addProperty("pickup", it.pickupDelay);
        } else if (e instanceof XpOrbEntity orb) {
            o.addProperty("kind", "xp");
            o.addProperty("value", orb.value);
        } else if (e instanceof Vehicle v) {
            o.addProperty("kind", v instanceof BoatEntity ? "boat" : "minecart");
            o.addProperty("pitch", v.pitch);
            o.addProperty("damage", v.damageTaken);
            o.addProperty("hurt", v.hurtTicks);
            o.addProperty("hurtDir", v.hurtDir);
        }
        return o;
    }

    public static Entity read(JsonObject o) {
        String kind = o.has("kind") ? o.get("kind").getAsString() : "";
        Entity e;
        switch (kind) {
            case "mob" -> {
                MobType type;
                try { type = MobType.valueOf(o.get("mob").getAsString()); } catch (IllegalArgumentException ex) { return null; }
                Mob m = new Mob(type);
                m.health = o.get("health").getAsFloat();
                if (type == MobType.SLIME && o.has("size")) { m.setSlimeSize(o.get("size").getAsInt()); m.health = o.get("health").getAsFloat(); }
                m.sheepColor = o.get("color").getAsInt();
                if (o.has("sheared")) m.sheared = o.get("sheared").getAsBoolean();
                if (o.has("tamed")) m.tamed = o.get("tamed").getAsBoolean();
                if (o.has("growing")) m.setGrowingAge(o.get("growing").getAsInt());
                if (o.has("sitting")) m.sitting = o.get("sitting").getAsBoolean();
                if (o.has("carried")) m.carriedBlock = o.get("carried").getAsInt();
                if (o.has("profession")) m.profession = o.get("profession").getAsInt();
                if (m.tamed) m.maxHealth = 20;
                java.util.Arrays.fill(m.armor, null);
                if (o.has("armor")) {
                    JsonArray a = o.getAsJsonArray("armor");
                    for (int i = 0; i < Math.min(4, a.size()); i++) m.armor[i] = stack(a.get(i));
                }
                m.bodyYaw = m.headYaw = o.get("yaw").getAsFloat();
                e = m;
            }
            case "item" -> {
                ItemStack s = stack(o.get("stack"));
                if (s == null) return null;
                ItemEntity it = new ItemEntity(s);
                it.pickupDelay = o.get("pickup").getAsInt();
                e = it;
            }
            case "xp" -> e = new XpOrbEntity(o.get("value").getAsInt());
            case "minecart" -> e = new MinecartEntity();
            case "boat" -> e = new BoatEntity();
            default -> { return null; }
        }
        e.setPos(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
        e.yaw = e.prevYaw = o.get("yaw").getAsFloat();
        e.motionX = o.get("mx").getAsDouble();
        e.motionY = o.get("my").getAsDouble();
        e.motionZ = o.get("mz").getAsDouble();
        e.fireTicks = o.get("fire").getAsInt();
        e.age = o.get("age").getAsInt();
        return e;
    }

    // ------------------------------------------------------------------ network form

    /**
     * Everything a client needs to show an entity: the saved form plus kinds that are never saved (players,
     * projectiles, TNT, falling blocks) and animation state.
     */
    public static JsonObject writeNet(Entity e) {
        JsonObject o;
        if (e instanceof Player p) {
            o = base(e);
            o.addProperty("kind", "player");
            o.addProperty("name", p.name);
            o.add("held", ItemStack.isEmpty(p.inventory.held()) ? null : stack(p.inventory.held()));
            JsonArray armor = new JsonArray();
            for (ItemStack s : p.inventory.armor) armor.add(ItemStack.isEmpty(s) ? null : stack(s));
            o.add("armor", armor);
            o.addProperty("sneaking", p.sneaking);
            o.addProperty("seated", p.vehicle != null);
        } else if (e instanceof ArrowEntity a) {
            o = base(e);
            o.addProperty("kind", "arrow");
            o.addProperty("ground", a.inGround);
            o.addProperty("damage", a.damage);
            o.addProperty("pickup", a.pickup);
            o.addProperty("punch", a.punch);
        } else if (e instanceof ThrownEntity t) {
            o = base(e);
            o.addProperty("kind", "thrown");
            o.addProperty("item", t.item.id);
            o.addProperty("potion", t.potionMeta);
        } else if (e instanceof TntEntity t) {
            o = base(e);
            o.addProperty("kind", "tnt");
            o.addProperty("fuse", t.fuse);
        } else if (e instanceof FallingBlockEntity f) {
            o = base(e);
            o.addProperty("kind", "falling");
            o.addProperty("block", f.blockId);
        } else if (e instanceof FishingBobberEntity f) {
            o = base(e);
            o.addProperty("kind", "bobber");
            o.addProperty("owner", f.ownerName);
        } else if (e instanceof FireballEntity f) {
            o = base(e);
            o.addProperty("kind", "fireball");
            o.addProperty("small", f.small);
        } else {
            o = write(e);
        }
        o.addProperty("pitch", e.pitch);
        if (e instanceof LivingEntity le) {
            o.addProperty("health", le.health);
            o.addProperty("maxHealth", le.maxHealth);
            o.addProperty("head", le.headYaw);
            o.addProperty("effectColor", le.effectColor());
        }
        if (e instanceof Mob m) {
            o.addProperty("fuse", m.fuse);
            o.addProperty("charge", m.ghastCharge);
            o.addProperty("anger", m.angerTicks);
            o.addProperty("eat", m.eatGrassTicks);
            o.addProperty("love", m.loveTicks);
            o.addProperty("aggressive", m.aggressive);
        }
        return o;
    }

    private static JsonObject base(Entity e) {
        JsonObject o = new JsonObject();
        o.addProperty("x", e.x);
        o.addProperty("y", e.y);
        o.addProperty("z", e.z);
        o.addProperty("yaw", e.yaw);
        o.addProperty("mx", e.motionX);
        o.addProperty("my", e.motionY);
        o.addProperty("mz", e.motionZ);
        o.addProperty("fire", e.fireTicks);
        o.addProperty("age", e.age);
        return o;
    }

    /**
     * Creates an entity from its network form. owner becomes the shooter or thrower of projectiles (the server
     * uses the player who sent it).
     */
    public static Entity readNet(JsonObject o, Entity owner) {
        String kind = o.has("kind") ? o.get("kind").getAsString() : "";
        Entity e;
        switch (kind) {
            case "player" -> {
                Player p = new Player();
                p.name = o.get("name").getAsString();
                e = p;
            }
            case "arrow" -> {
                ArrowEntity a = new ArrowEntity(owner);
                a.inGround = o.get("ground").getAsBoolean();
                a.damage = o.get("damage").getAsFloat();
                a.pickup = o.get("pickup").getAsBoolean();
                a.punch = o.get("punch").getAsInt();
                e = a;
            }
            case "thrown" -> {
                Item item = Item.get(o.get("item").getAsInt());
                if (item == null) return null;
                ThrownEntity t = new ThrownEntity(item, owner);
                t.potionMeta = o.get("potion").getAsInt();
                e = t;
            }
            case "tnt" -> e = new TntEntity(o.get("fuse").getAsInt());
            case "bobber" -> {
                FishingBobberEntity f = new FishingBobberEntity(owner instanceof Player p ? p : null);
                if (f.owner == null) f.ownerName = o.get("owner").getAsString();
                e = f;
            }
            case "falling" -> e = new FallingBlockEntity(o.get("block").getAsInt());
            case "fireball" -> {
                FireballEntity f = new FireballEntity(owner);
                f.small = o.get("small").getAsBoolean();
                e = f;
            }
            default -> {
                return read(o);
            }
        }
        e.setPos(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
        e.yaw = e.prevYaw = o.get("yaw").getAsFloat();
        e.motionX = o.get("mx").getAsDouble();
        e.motionY = o.get("my").getAsDouble();
        e.motionZ = o.get("mz").getAsDouble();
        e.fireTicks = o.get("fire").getAsInt();
        e.age = o.get("age").getAsInt();
        applyState(e, o);
        return e;
    }

    /** Updates the changeable parts of a client copy (health, wool, armor, held item, animation state). */
    public static void applyState(Entity e, JsonObject o) {
        if (o.has("pitch")) e.pitch = o.get("pitch").getAsFloat();
        if (o.has("fire")) e.fireTicks = o.get("fire").getAsInt();
        if (e instanceof LivingEntity le) {
            if (o.has("maxHealth")) le.maxHealth = o.get("maxHealth").getAsFloat();
            if (o.has("health")) le.health = o.get("health").getAsFloat();
            if (o.has("head")) le.headYaw = le.prevHeadYaw = o.get("head").getAsFloat();
        }
        if (e instanceof Player p) {
            p.inventory.slots[p.inventory.selected] = o.has("held") ? stack(o.get("held")) : null;
            if (o.has("armor")) {
                JsonArray a = o.getAsJsonArray("armor");
                for (int i = 0; i < Math.min(4, a.size()); i++) p.inventory.armor[i] = stack(a.get(i));
            }
            if (o.has("sneaking")) p.sneaking = o.get("sneaking").getAsBoolean();
            if (o.has("seated")) p.seated = o.get("seated").getAsBoolean();
        }
        if (e instanceof Mob m) {
            if (o.has("color")) m.sheepColor = o.get("color").getAsInt();
            if (o.has("sheared")) m.sheared = o.get("sheared").getAsBoolean();
            if (o.has("tamed")) m.tamed = o.get("tamed").getAsBoolean();
            if (o.has("growing") && (m.growingAge < 0) != (o.get("growing").getAsInt() < 0)) m.setGrowingAge(o.get("growing").getAsInt());
            if (o.has("sitting")) m.sitting = o.get("sitting").getAsBoolean();
            if (o.has("carried")) m.carriedBlock = o.get("carried").getAsInt();
            if (o.has("profession")) m.profession = o.get("profession").getAsInt();
            if (o.has("fuse")) m.fuse = o.get("fuse").getAsInt();
            if (o.has("charge")) m.ghastCharge = o.get("charge").getAsInt();
            if (o.has("anger")) m.angerTicks = o.get("anger").getAsInt();
            if (o.has("eat") && o.get("eat").getAsInt() > m.eatGrassTicks) m.eatGrassTicks = o.get("eat").getAsInt();
            if (o.has("love")) m.loveTicks = o.get("love").getAsInt();
            if (o.has("aggressive")) m.aggressive = o.get("aggressive").getAsBoolean();
            if (o.has("armor")) {
                JsonArray a = o.getAsJsonArray("armor");
                for (int i = 0; i < Math.min(4, a.size()); i++) m.armor[i] = stack(a.get(i));
            }
        }
        if (e instanceof ItemEntity it && o.has("stack")) {
            ItemStack s = stack(o.get("stack"));
            if (s != null) it.stack.count = s.count;
        }
        if (e instanceof ArrowEntity a && o.has("ground")) a.inGround = o.get("ground").getAsBoolean();
        if (e instanceof TntEntity t && o.has("fuse")) t.fuse = o.get("fuse").getAsInt();
        if (e instanceof Vehicle v && o.has("damage")) {
            v.damageTaken = o.get("damage").getAsFloat();
            v.hurtTicks = o.get("hurt").getAsInt();
            v.hurtDir = o.get("hurtDir").getAsInt();
        }
    }

    public static String writeAll(List<Entity> list) {
        JsonArray a = new JsonArray();
        for (Entity e : list) a.add(write(e));
        return a.toString();
    }

    public static List<Entity> readAll(String json) {
        List<Entity> out = new ArrayList<>();
        try {
            for (JsonElement el : JsonParser.parseString(json).getAsJsonArray()) {
                Entity e = read(el.getAsJsonObject());
                if (e != null) out.add(e);
            }
        } catch (RuntimeException ex) {
            System.err.println("Could not read entities: " + ex);
        }
        return out;
    }
}
