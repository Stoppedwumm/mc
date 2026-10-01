package net.minecraft.resources;

/** A namespaced id such as "minecraft:stone" (reamc-compat). */
public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final String DEFAULT_NAMESPACE = "minecraft";
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
