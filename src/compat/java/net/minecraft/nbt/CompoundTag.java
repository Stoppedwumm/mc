package net.minecraft.nbt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** A map of named values (reamc-compat: numbers, strings, arrays, lists and nested compounds). */
public class CompoundTag implements Tag {
    final Map<String, Object> values = new LinkedHashMap<>();

    @Override public byte getId() { return TAG_COMPOUND; }

    public Set<String> getAllKeys() { return values.keySet(); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public boolean contains(String key) { return values.containsKey(key); }

    public boolean contains(String key, int type) {
        Object v = values.get(key);
        if (v == null) return false;
        byte id = idOf(v);
        return id == type || (type == TAG_ANY_NUMERIC && id >= TAG_BYTE && id <= TAG_DOUBLE);
    }

    public byte getTagType(String key) { Object v = values.get(key); return v == null ? TAG_END : idOf(v); }

    static byte idOf(Object v) {
        if (v instanceof Byte || v instanceof Boolean) return TAG_BYTE;
        if (v instanceof Short) return TAG_SHORT;
        if (v instanceof Integer) return TAG_INT;
        if (v instanceof Long) return TAG_LONG;
        if (v instanceof Float) return TAG_FLOAT;
        if (v instanceof Double) return TAG_DOUBLE;
        if (v instanceof String) return TAG_STRING;
        if (v instanceof byte[]) return TAG_BYTE_ARRAY;
        if (v instanceof int[]) return TAG_INT_ARRAY;
        if (v instanceof long[]) return TAG_LONG_ARRAY;
        if (v instanceof Tag t) return t.getId();
        return TAG_END;
    }

    public Tag put(String key, Tag tag) { values.put(key, tag); return tag; }
    public void putByte(String key, byte v) { values.put(key, v); }
    public void putShort(String key, short v) { values.put(key, v); }
    public void putInt(String key, int v) { values.put(key, v); }
    public void putLong(String key, long v) { values.put(key, v); }
    public void putFloat(String key, float v) { values.put(key, v); }
    public void putDouble(String key, double v) { values.put(key, v); }
    public void putString(String key, String v) { values.put(key, v); }
    public void putBoolean(String key, boolean v) { values.put(key, (byte) (v ? 1 : 0)); }
    public void putByteArray(String key, byte[] v) { values.put(key, v); }
    public void putIntArray(String key, int[] v) { values.put(key, v); }
    public void putLongArray(String key, long[] v) { values.put(key, v); }
    public void putUUID(String key, java.util.UUID id) { putLongArray(key, new long[]{id.getMostSignificantBits(), id.getLeastSignificantBits()}); }
    public void remove(String key) { values.remove(key); }

    private Number num(String key) { return values.get(key) instanceof Number n ? n : 0; }
    public byte getByte(String key) { return num(key).byteValue(); }
    public short getShort(String key) { return num(key).shortValue(); }
    public int getInt(String key) { return num(key).intValue(); }
    public long getLong(String key) { return num(key).longValue(); }
    public float getFloat(String key) { return num(key).floatValue(); }
    public double getDouble(String key) { return num(key).doubleValue(); }
    public boolean getBoolean(String key) { return getByte(key) != 0; }
    public String getString(String key) { return values.get(key) instanceof String s ? s : ""; }
    public byte[] getByteArray(String key) { return values.get(key) instanceof byte[] a ? a : new byte[0]; }
    public int[] getIntArray(String key) { return values.get(key) instanceof int[] a ? a : new int[0]; }
    public long[] getLongArray(String key) { return values.get(key) instanceof long[] a ? a : new long[0]; }
    public CompoundTag getCompound(String key) { return values.get(key) instanceof CompoundTag c ? c : new CompoundTag(); }
    public ListTag getList(String key, int type) { return values.get(key) instanceof ListTag l && (l.isEmpty() || l.elementType() == type) ? l : new ListTag(); }

    public Tag get(String key) {
        Object v = values.get(key);
        return v == null ? null : v instanceof Tag t ? t : new Value(v);
    }

    public CompoundTag merge(CompoundTag other) { values.putAll(other.copy().values); return this; }

    @Override
    public CompoundTag copy() {
        CompoundTag c = new CompoundTag();
        for (var e : values.entrySet()) c.values.put(e.getKey(), e.getValue() instanceof Tag t ? t.copy() : e.getValue());
        return c;
    }

    @Override public String toString() { return toJson().toString(); }
    @Override public boolean equals(Object o) { return o instanceof CompoundTag c && c.toJson().equals(toJson()); }
    @Override public int hashCode() { return toJson().hashCode(); }

    /** A primitive wrapped as a tag (what get() returns for numbers and strings). */
    public record Value(Object value) implements Tag {
        @Override public byte getId() { return idOf(value); }
        @Override public Tag copy() { return this; }
        @Override public String getAsString() { return String.valueOf(value); }
    }

    // ------------------------------------------------------------------ JSON form for reamc saves

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        for (var e : values.entrySet()) o.add(e.getKey(), encode(e.getValue()));
        return o;
    }

    static JsonElement encode(Object v) {
        JsonObject o = new JsonObject();
        byte id = idOf(v);
        o.addProperty("t", id);
        switch (id) {
            case TAG_COMPOUND -> o.add("v", ((CompoundTag) v).toJson());
            case TAG_LIST -> {
                JsonArray a = new JsonArray();
                for (Object x : ((ListTag) v).items) a.add(encode(x));
                o.add("v", a);
            }
            case TAG_STRING -> o.addProperty("v", (String) v);
            case TAG_BYTE_ARRAY, TAG_INT_ARRAY, TAG_LONG_ARRAY -> {
                JsonArray a = new JsonArray();
                if (v instanceof byte[] b) for (byte x : b) a.add(x);
                if (v instanceof int[] b) for (int x : b) a.add(x);
                if (v instanceof long[] b) for (long x : b) a.add(x);
                o.add("v", a);
            }
            default -> o.addProperty("v", v instanceof Boolean b ? (Number) (byte) (b ? 1 : 0) : (Number) (v instanceof Value w ? w.value() : v));
        }
        return o;
    }

    static Object decode(JsonElement el) {
        JsonObject o = el.getAsJsonObject();
        byte id = o.get("t").getAsByte();
        JsonElement v = o.get("v");
        return switch (id) {
            case TAG_BYTE -> v.getAsByte();
            case TAG_SHORT -> v.getAsShort();
            case TAG_INT -> v.getAsInt();
            case TAG_LONG -> v.getAsLong();
            case TAG_FLOAT -> v.getAsFloat();
            case TAG_DOUBLE -> v.getAsDouble();
            case TAG_STRING -> v.getAsString();
            case TAG_COMPOUND -> fromJson(v.getAsJsonObject());
            case TAG_LIST -> {
                ListTag l = new ListTag();
                for (JsonElement x : v.getAsJsonArray()) l.items.add(decode(x));
                yield l;
            }
            case TAG_BYTE_ARRAY -> { JsonArray a = v.getAsJsonArray(); byte[] b = new byte[a.size()]; for (int i = 0; i < b.length; i++) b[i] = a.get(i).getAsByte(); yield b; }
            case TAG_INT_ARRAY -> { JsonArray a = v.getAsJsonArray(); int[] b = new int[a.size()]; for (int i = 0; i < b.length; i++) b[i] = a.get(i).getAsInt(); yield b; }
            case TAG_LONG_ARRAY -> { JsonArray a = v.getAsJsonArray(); long[] b = new long[a.size()]; for (int i = 0; i < b.length; i++) b[i] = a.get(i).getAsLong(); yield b; }
            default -> null;
        };
    }

    public static CompoundTag fromJson(JsonObject o) {
        CompoundTag c = new CompoundTag();
        for (var e : o.entrySet()) {
            Object v = decode(e.getValue());
            if (v != null) c.values.put(e.getKey(), v);
        }
        return c;
    }

    /** Any tag as JSON text (reamc's save format), and back. */
    public static String reamc$toJson(Tag tag) { return encode(tag).toString(); }

    public static Tag reamc$fromJson(String json) {
        Object v = decode(com.google.gson.JsonParser.parseString(json));
        return v instanceof Tag t ? t : null;
    }
}
