package net.minecraft.network.chat;

/** Chat and GUI text: literal or translated through the loaded language files (reamc-compat). */
public interface Component {
    String getString();

    static MutableComponent literal(String text) { return new MutableComponent(text, null, new Object[0]); }

    static MutableComponent translatable(String key) { return new MutableComponent(null, key, new Object[0]); }

    static MutableComponent translatable(String key, Object... args) { return new MutableComponent(null, key, args); }

    static MutableComponent translatableWithFallback(String key, String fallback) {
        return new MutableComponent(mc.mod.Lang.has(key) ? null : fallback, key, new Object[0]);
    }

    static Component empty() { return literal(""); }

    default MutableComponent copy() { return literal(getString()); }
}
