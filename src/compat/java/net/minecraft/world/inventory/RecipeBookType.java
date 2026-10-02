package net.minecraft.world.inventory;

/** Recipe book tabs (reamc-compat: reamc has no recipe book; mods may add their own, which NeoForge allows). */
public enum RecipeBookType {
    CRAFTING, FURNACE, BLAST_FURNACE, SMOKER;

    private static final java.util.Map<String, RecipeBookType> EXTRA = new java.util.HashMap<>();

    /** NeoForge creates extra book types by name; reamc maps unknown names to CRAFTING. */
    public static RecipeBookType reamc$byName(String name) {
        for (RecipeBookType t : values()) if (t.name().equals(name)) return t;
        return EXTRA.computeIfAbsent(name, n -> CRAFTING);
    }
}
