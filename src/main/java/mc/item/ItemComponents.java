package mc.item;

/** Extra per-stack data owned by the mod layer (data components), carried, copied and saved with a stack. */
public interface ItemComponents {
    ItemComponents copy();

    /** Saved form (JSON text). */
    String save();

    boolean isEmpty();

    /** Reads the saved form back; set by the mod layer. */
    java.util.function.Function<String, ItemComponents>[] PARSER = new java.util.function.Function[1];

    static ItemComponents parse(String s) { return PARSER[0] == null || s == null || s.isEmpty() ? null : PARSER[0].apply(s); }
}
