package mc.world;

/** The worlds a player can be in; each is stored in its own folder. */
public enum Dimension {
    OVERWORLD(""), NETHER("DIM-1");

    /** Sub-folder of the world directory ("" = the world directory itself). */
    public final String folder;

    Dimension(String folder) {
        this.folder = folder;
    }
}
