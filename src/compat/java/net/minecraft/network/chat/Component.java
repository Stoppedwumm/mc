package net.minecraft.network.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.HolderLookup;

import java.util.List;

/** Chat and GUI text: literal or translated through the loaded language files (reamc-compat). */
public interface Component {
    String getString();

    Style getStyle();

    List<Component> getSiblings();

    default String getString(int maxLength) { String s = getString(); return s.length() > maxLength ? s.substring(0, maxLength) : s; }

    default MutableComponent copy() { return MutableComponent.reamc$copyOf(this); }

    default MutableComponent plainCopy() { return copy(); }

    default List<Component> toFlatList() { return List.of(this); }

    static MutableComponent literal(String text) { return new MutableComponent(text, null, new Object[0]); }

    static MutableComponent translatable(String key) { return new MutableComponent(null, key, new Object[0]); }

    static MutableComponent translatable(String key, Object... args) { return new MutableComponent(null, key, args); }

    static MutableComponent translatableWithFallback(String key, String fallback) {
        return new MutableComponent(mc.mod.Lang.has(key) ? null : fallback, key, new Object[0]);
    }

    static MutableComponent translatableWithFallback(String key, String fallback, Object... args) {
        return mc.mod.Lang.has(key) ? translatable(key, args) : literal(fallback);
    }

    static MutableComponent empty() { return literal(""); }

    static MutableComponent keybind(String key) { return literal(key); }

    static Component nullToEmpty(String s) { return s == null ? CommonComponents.EMPTY : literal(s); }

    /** JSON text form (literal text and translation keys with their arguments). */
    class Serializer {
        private Serializer() { }

        public static String toJson(Component c, HolderLookup.Provider registries) { return toTree(c).toString(); }

        public static MutableComponent fromJson(String json, HolderLookup.Provider registries) {
            if (json == null) return null;
            try {
                return fromTree(JsonParser.parseString(json));
            } catch (RuntimeException e) {
                return literal(json);
            }
        }

        public static MutableComponent fromJsonLenient(String json, HolderLookup.Provider registries) { return fromJson(json, registries); }

        public static MutableComponent fromJson(JsonElement json, HolderLookup.Provider registries) { return fromTree(json); }

        static JsonElement toTree(Component c) {
            JsonObject o = new JsonObject();
            if (c instanceof MutableComponent m) {
                if (m.reamc$key() != null) {
                    o.addProperty("translate", m.reamc$key());
                    if (m.reamc$args().length > 0) {
                        JsonArray a = new JsonArray();
                        for (Object x : m.reamc$args()) a.add(x instanceof Component cc ? toTree(cc) : new com.google.gson.JsonPrimitive(String.valueOf(x)));
                        o.add("with", a);
                    }
                } else o.addProperty("text", m.reamc$literal() == null ? "" : m.reamc$literal());
                if (m.getStyle().getColor() != null) o.addProperty("color", m.getStyle().getColor().serialize());
            } else o.addProperty("text", c.getString());
            if (!c.getSiblings().isEmpty()) {
                JsonArray a = new JsonArray();
                for (Component s : c.getSiblings()) a.add(toTree(s));
                o.add("extra", a);
            }
            return o;
        }

        static MutableComponent fromTree(JsonElement e) {
            if (e.isJsonPrimitive()) return literal(e.getAsString());
            if (e.isJsonArray()) {
                MutableComponent m = empty();
                for (JsonElement x : e.getAsJsonArray()) m.append(fromTree(x));
                return m;
            }
            JsonObject o = e.getAsJsonObject();
            MutableComponent m;
            if (o.has("translate")) {
                Object[] args = new Object[0];
                if (o.has("with")) {
                    JsonArray a = o.getAsJsonArray("with");
                    args = new Object[a.size()];
                    for (int i = 0; i < args.length; i++) args[i] = fromTree(a.get(i));
                }
                m = translatable(o.get("translate").getAsString(), args);
            } else m = literal(o.has("text") ? o.get("text").getAsString() : "");
            if (o.has("color")) {
                String col = o.get("color").getAsString();
                net.minecraft.ChatFormatting f = net.minecraft.ChatFormatting.getByName(col);
                if (f != null) m.withStyle(f);
                else if (col.startsWith("#")) m.withColor(Integer.parseInt(col.substring(1), 16));
            }
            if (o.has("extra")) for (JsonElement x : o.getAsJsonArray("extra")) m.append(fromTree(x));
            return m;
        }
    }
}
