package mc.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tag contents for every registry: the built-in Minecraft tags that cover reamc's own blocks and items
 * ({@link VanillaTags}) plus the tag files of loaded mods. Entries are ids; "#ns:path" entries include other tags.
 */
public final class Tags {
    private Tags() { }

    /** registry id -> tag id -> raw entries (ids or "#tag"). */
    private static final Map<ResourceLocation, Map<ResourceLocation, List<String>>> RAW = new HashMap<>();
    private static final Map<TagKey<?>, Set<ResourceLocation>> RESOLVED = new HashMap<>();
    private static final Map<ResourceLocation, Map<ResourceLocation, Set<ResourceLocation>>> TAGS_OF = new HashMap<>();

    static {
        VanillaTags.addTo(Tags::add);
    }

    /** Adds entries to a tag ("block", "item", ... registry path or full id). */
    public static synchronized void add(String registry, String tag, List<String> entries, boolean replace) {
        ResourceLocation reg = ResourceLocation.parse(registry), id = ResourceLocation.parse(tag);
        List<String> l = RAW.computeIfAbsent(reg, k -> new HashMap<>()).computeIfAbsent(id, k -> new ArrayList<>());
        if (replace) l.clear();
        l.addAll(entries);
        RESOLVED.clear();
        TAGS_OF.clear();
    }

    /** Reads one tag file from a data pack ({"replace": false, "values": [...]}). */
    public static void addFile(String registry, String tag, JsonObject o) {
        List<String> values = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("values")) {
            if (e.isJsonPrimitive()) values.add(e.getAsString());
            else values.add(e.getAsJsonObject().get("id").getAsString());
        }
        add(registry, tag, values, o.has("replace") && o.get("replace").getAsBoolean());
    }

    public static synchronized Set<ResourceLocation> entries(TagKey<?> tag) {
        Set<ResourceLocation> r = RESOLVED.get(tag);
        if (r == null) {
            r = new LinkedHashSet<>();
            resolve(tag.registry().location(), tag.location(), r, new HashSet<>());
            r = Collections.unmodifiableSet(r);
            RESOLVED.put(tag, r);
        }
        return r;
    }

    private static void resolve(ResourceLocation registry, ResourceLocation tag, Set<ResourceLocation> out, Set<ResourceLocation> seen) {
        if (!seen.add(tag)) return;
        List<String> raw = RAW.getOrDefault(registry, Map.of()).get(tag);
        if (raw == null) return;
        for (String v : raw) {
            if (v.startsWith("#")) resolve(registry, ResourceLocation.parse(v.substring(1)), out, seen);
            else out.add(ResourceLocation.parse(v));
        }
    }

    public static boolean has(TagKey<?> tag, ResourceLocation entry) { return entries(tag).contains(entry); }

    public static synchronized Set<ResourceLocation> tagNames(ResourceKey<? extends Registry<?>> registry) {
        return Set.copyOf(RAW.getOrDefault(registry.location(), Map.of()).keySet());
    }

    /** The tags an entry is in. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized <T> Stream<TagKey<T>> tagsOf(ResourceKey<T> key) {
        ResourceLocation reg = key.registry();
        Map<ResourceLocation, Set<ResourceLocation>> byEntry = TAGS_OF.get(reg);
        if (byEntry == null) {
            byEntry = new HashMap<>();
            ResourceKey regKey = ResourceKey.createRegistryKey(reg);
            for (ResourceLocation tag : RAW.getOrDefault(reg, Map.of()).keySet())
                for (ResourceLocation e : entries(TagKey.create(regKey, tag))) byEntry.computeIfAbsent(e, k -> new LinkedHashSet<>()).add(tag);
            TAGS_OF.put(reg, byEntry);
        }
        ResourceKey regKey = ResourceKey.createRegistryKey(reg);
        return byEntry.getOrDefault(key.location(), Set.of()).stream().map(t -> (TagKey<T>) TagKey.create(regKey, t));
    }

    /** Registry ids of the engine block/item lookups, for convenience. */
    public static boolean blockHas(String tag, String blockKey) {
        return has(net.minecraft.tags.BlockTags.create(ResourceLocation.parse(tag)), ResourceLocation.parse(blockKey));
    }

    public static boolean itemHas(String tag, String itemKey) {
        return has(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(tag)), ResourceLocation.parse(itemKey));
    }
}
