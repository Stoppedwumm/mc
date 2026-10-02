package net.minecraft.resources;

/** A namespaced id such as "minecraft:stone" (reamc-compat). */
public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final String DEFAULT_NAMESPACE = "minecraft", REALMS_NAMESPACE = "realms";
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final com.mojang.serialization.Codec<ResourceLocation> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(ResourceLocation::read, ResourceLocation::toString);
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, ResourceLocation> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString);
    private final String namespace, path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) { return new ResourceLocation(namespace, path); }

    public static ResourceLocation withDefaultNamespace(String path) { return new ResourceLocation(DEFAULT_NAMESPACE, path); }

    public static ResourceLocation parse(String id) {
        int i = id.indexOf(':');
        return i < 0 ? new ResourceLocation(DEFAULT_NAMESPACE, id) : new ResourceLocation(i == 0 ? DEFAULT_NAMESPACE : id.substring(0, i), id.substring(i + 1));
    }

    public static ResourceLocation tryParse(String id) {
        try { return parse(id); } catch (RuntimeException e) { return null; }
    }

    public static ResourceLocation tryBuild(String namespace, String path) {
        return isValidNamespace(namespace) && isValidPath(path) ? new ResourceLocation(namespace, path) : null;
    }

    public static ResourceLocation bySeparator(String id, char sep) {
        int i = id.indexOf(sep);
        return i < 0 ? withDefaultNamespace(id) : fromNamespaceAndPath(i == 0 ? DEFAULT_NAMESPACE : id.substring(0, i), id.substring(i + 1));
    }

    public static ResourceLocation tryBySeparator(String id, char sep) {
        try { return bySeparator(id, sep); } catch (RuntimeException e) { return null; }
    }

    public static com.mojang.serialization.DataResult<ResourceLocation> read(String id) {
        ResourceLocation r = tryParse(id);
        return r != null ? com.mojang.serialization.DataResult.success(r) : com.mojang.serialization.DataResult.error(() -> "Not a valid resource location: " + id);
    }

    public static boolean validPathChar(char c) { return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '/' || c == '.'; }

    public static boolean isAllowedInResourceLocation(char c) { return validPathChar(c) || c == ':'; }

    public static boolean isValidPath(String p) { for (int i = 0; i < p.length(); i++) if (!validPathChar(p.charAt(i))) return false; return true; }

    public static boolean isValidNamespace(String n) { for (int i = 0; i < n.length(); i++) { char c = n.charAt(i); if (!(validPathChar(c) && c != '/')) return false; } return true; }

    public String toShortLanguageKey() { return namespace.equals(DEFAULT_NAMESPACE) ? path : toLanguageKey(""); }

    public String toDebugFileName() { return toString().replace('/', '_').replace(':', '_'); }

    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    public ResourceLocation withPath(String p) { return new ResourceLocation(namespace, p); }
    public ResourceLocation withPath(java.util.function.UnaryOperator<String> f) { return new ResourceLocation(namespace, f.apply(path)); }
    public ResourceLocation withPrefix(String p) { return new ResourceLocation(namespace, p + path); }
    public ResourceLocation withSuffix(String s) { return new ResourceLocation(namespace, path + s); }
    public String toLanguageKey(String type) { return type + "." + namespace + "." + path.replace('/', '.'); }

    @Override public String toString() { return namespace + ":" + path; }
    @Override public boolean equals(Object o) { return o instanceof ResourceLocation r && r.namespace.equals(namespace) && r.path.equals(path); }
    @Override public int hashCode() { return 31 * namespace.hashCode() + path.hashCode(); }
    @Override public int compareTo(ResourceLocation o) { return toString().compareTo(o.toString()); }
}
