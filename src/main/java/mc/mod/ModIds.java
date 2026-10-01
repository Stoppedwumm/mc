package mc.mod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Numeric ids of mod blocks and items. Chunks and inventories store numbers, so a key keeps the id it got the first
 * time, recorded in mods/ids.json, even when mods are added or removed later.
 */
final class ModIds {
    private final Path file;
    private final Map<String, Integer> blocks = new TreeMap<>(), items = new TreeMap<>();

    ModIds(Path dir) {
        file = dir == null ? null : dir.resolve("ids.json");
        if (file == null || !Files.exists(file)) return;
        try {
            JsonObject o = new Gson().fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (o.has("blocks")) o.getAsJsonObject("blocks").entrySet().forEach(e -> blocks.put(e.getKey(), e.getValue().getAsInt()));
            if (o.has("items")) o.getAsJsonObject("items").entrySet().forEach(e -> items.put(e.getKey(), e.getValue().getAsInt()));
        } catch (Exception e) {
            System.err.println("[mods] Could not read " + file + ": " + e);
        }
    }

    /** The block id for a key: the remembered one if it is still free, else the first id nobody has used. */
    int block(String key) {
        Integer known = blocks.get(key);
        if (known != null && known < mc.world.Block.MAX && mc.world.Block.BY_ID[known] == null) return known;
        for (int i = mc.world.Block.FIRST_MOD_ID; i < mc.world.Block.MAX; i++) {
            if (mc.world.Block.BY_ID[i] == null && mc.item.Item.BY_ID[i] == null && !blocks.containsValue(i)) {
                blocks.put(key, i);
                return i;
            }
        }
        return -1;
    }

    int item(String key) {
        Integer known = items.get(key);
        if (known != null && known < mc.world.Block.FIRST_MOD_ID && mc.item.Item.BY_ID[known] == null) return known;
        for (int i = 256; i < mc.world.Block.FIRST_MOD_ID; i++) {
            if (mc.item.Item.BY_ID[i] == null && !items.containsValue(i)) {
                items.put(key, i);
                return i;
            }
        }
        return -1;
    }

    void save() {
        if (file == null) return;
        JsonObject o = new JsonObject(), b = new JsonObject(), it = new JsonObject();
        blocks.forEach(b::addProperty);
        items.forEach(it::addProperty);
        o.add("blocks", b);
        o.add("items", it);
        try {
            Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[mods] Could not write " + file + ": " + e);
        }
    }
}
