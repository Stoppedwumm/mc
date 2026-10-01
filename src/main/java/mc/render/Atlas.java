package mc.render;

/** Layout shared by the block and item atlases: 16 x 16 pixel tiles, {@link #ROW} to a row. */
public final class Atlas {
    private Atlas() { }

    public static final int ROW = 64, SIZE = ROW * 16, TILES = ROW * ROW;
    /** Width of one tile in texture coordinates. */
    public static final float STEP = 1f / ROW;

    public static float u(int tile) { return (tile % ROW) * STEP; }

    public static float v(int tile) { return (tile / ROW) * STEP; }

    /** Index of pixel (x, y) of a tile in the atlas pixel array. */
    public static int index(int tile, int x, int y) { return ((tile / ROW) * 16 + y) * SIZE + (tile % ROW) * 16 + x; }
}
