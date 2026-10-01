package mc.render.lod;

import mc.world.Block;

/**
 * Average colours of each block's top and side textures (sRGB), the way distant-terrain mods flatten blocks
 * into single colours. Grass and leaves are greyscale in the atlas and get their biome tint on top.
 */
public final class LodPalette {
    private final int[] top = new int[Block.MAX], side = new int[Block.MAX];

    public LodPalette(int[] atlas, int atlasSize) {
        int tilesPerRow = atlasSize / 16;
        for (int id = 1; id < Block.MAX; id++) {
            Block b = Block.BY_ID[id];
            if (b == null) continue;
            top[id] = average(atlas, atlasSize, tilesPerRow, b.textureForFace(0, 0));
            side[id] = average(atlas, atlasSize, tilesPerRow, b.textureForFace(2, 0));
        }
        // Leaf textures are mostly dark gaps between leaves; seen from afar a canopy reads much brighter
        for (Block b : new Block[]{Block.OAK_LEAVES, Block.BIRCH_LEAVES, Block.SPRUCE_LEAVES}) {
            int c = top[b.id];
            top[b.id] = side[b.id] = Math.min(255, (int) ((c >> 16 & 255) * 1.45f)) << 16 | Math.min(255, (int) ((c >> 8 & 255) * 1.45f)) << 8 | Math.min(255, (int) ((c & 255) * 1.45f));
        }
        // Liquids have no meaningful texture average
        top[Block.WATER.id] = side[Block.WATER.id] = 0x2f5fbf;
        top[Block.LAVA.id] = side[Block.LAVA.id] = 0xd85a12;
    }

    private static int average(int[] px, int size, int perRow, int tile) {
        int tx = (tile % perRow) * 16, ty = (tile / perRow) * 16;
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = px[(ty + y) * size + tx + x];
                if ((c >>> 24) < 40) continue;
                r += c >> 16 & 255; g += c >> 8 & 255; b += c & 255; n++;
            }
        if (n == 0) return 0x808080;
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    static int multiply(int a, int b) {
        int r = (a >> 16 & 255) * (b >> 16 & 255) / 255, g = (a >> 8 & 255) * (b >> 8 & 255) / 255, bl = (a & 255) * (b & 255) / 255;
        return r << 16 | g << 8 | bl;
    }

    static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Colour of a block seen from above, with grass/foliage tints applied. */
    public int top(int id, int grass, int foliage) {
        Block b = Block.get(id);
        int c = top[id];
        return switch (b.tint) {
            case GRASS -> multiply(c, grass);
            case FOLIAGE -> multiply(c, foliage);
            case BIRCH -> multiply(c, 0x80a755);
            case SPRUCE -> multiply(c, 0x619961);
            default -> c;
        };
    }

    /** Colour of a block's sides (cliffs, walls between LOD cells). */
    public int side(int id, int grass, int foliage) {
        Block b = Block.get(id);
        int c = side[id];
        if (b.tint == Block.Tint.NONE || b.tintTopOnly) return c;
        return top(id, grass, foliage);
    }
}
