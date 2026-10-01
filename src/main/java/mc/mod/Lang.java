package mc.mod;

import java.util.HashMap;
import java.util.Map;

/** Translations from the loaded mods' en_us.json files. */
public final class Lang {
    private Lang() { }

    private static final Map<String, String> STRINGS = new HashMap<>();

    static {
        STRINGS.put("container.inventory", "Inventory");
    }

    public static void putAll(Map<String, String> m) { STRINGS.putAll(m); }

    public static boolean has(String key) { return STRINGS.containsKey(key); }

    public static String get(String key) { return STRINGS.getOrDefault(key, key); }

    /** Minecraft-style formatting: %s and %1$s placeholders. */
    public static String format(String key, Object... args) {
        String s = get(key);
        if (args.length == 0) return s;
        try {
            return String.format(s, args);
        } catch (RuntimeException e) {
            return s;
        }
    }
}
