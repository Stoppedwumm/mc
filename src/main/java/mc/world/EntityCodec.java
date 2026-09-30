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
        return !e.removed && (e instanceof Mob m ? !m.isDead() : e instanceof ItemEntity || e instanceof XpOrbEntity);
    }

    private static JsonArray stack(ItemStack s) {
        JsonArray a = new JsonArray();
        a.add(s.item.id);
        a.add(s.count);
        a.add(s.damage);
        return a;
    }

    private static ItemStack stack(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        JsonArray a = e.getAsJsonArray();
        Item it = Item.get(a.get(0).getAsInt());
        return it == null ? null : new ItemStack(it, a.get(1).getAsInt(), a.get(2).getAsInt());
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
                m.sheepColor = o.get("color").getAsInt();
                if (o.has("sheared")) m.sheared = o.get("sheared").getAsBoolean();
                if (o.has("tamed")) m.tamed = o.get("tamed").getAsBoolean();
                if (o.has("growing")) m.setGrowingAge(o.get("growing").getAsInt());
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
