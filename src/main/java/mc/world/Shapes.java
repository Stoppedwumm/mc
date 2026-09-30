package mc.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry of non-cube blocks as boxes in 1/16 block units. The same boxes drive chunk meshing, collision,
 * picking and the selection outline.
 * <p>
 * Horizontal facing (bits 0-1) uses 0 = south (+z), 1 = west (-x), 2 = north (-z), 3 = east (+x).
 * Meta layouts:
 * <ul>
 * <li>slab: 0 bottom, 1 top, 2 double</li>
 * <li>stairs: facing (side of the low step), bit 2 upside down</li>
 * <li>fence gate: facing, bit 2 open</li>
 * <li>door: facing (side the closed panel sits on), bit 2 open, bit 3 upper half, bit 4 right hinge</li>
 * <li>trapdoor: facing (side the open panel rests against), bit 2 open, bit 3 top half</li>
 * <li>ladder: facing (direction of the supporting wall)</li>
 * <li>bed: facing (foot to head), bit 2 head half</li>
 * <li>snow layer: layers - 1</li>
 * <li>cake: slices eaten (0-6)</li>
 * <li>nether portal: 0 = spans the x axis, 1 = spans the z axis</li>
 * <li>redstone wire: power 0-15</li>
 * <li>lever / button: attached face (0 floor, 1-4 wall, 5 ceiling), bit 3 on</li>
 * <li>pressure plate: bit 0 pressed</li>
 * <li>repeater: facing of the output (bits 0-1), delay - 1 (bits 2-3)</li>
 * <li>piston: 6-way facing (bits 0-2: down, up, north, south, west, east), bit 3 extended; head: facing, bit 3 sticky</li>
 * <li>torch: 0 standing, 1-4 on a wall in direction (meta - 1)</li>
 * </ul>
 */
public final class Shapes {
    public enum Mode { RENDER, COLLISION, OUTLINE }

    /** Read access to blocks, implemented by the world and by the mesher's snapshot. */
    public interface Getter {
        int getBlock(int x, int y, int z);
        int getMeta(int x, int y, int z);
    }

    public static final int[] DX = {0, -1, 0, 1}, DZ = {1, 0, -1, 0};

    private Shapes() { }

    public static int opposite(int facing) { return (facing + 2) & 3; }

    /** Facing that points from (0,0) along (dx, dz). */
    public static int facingOf(int dx, int dz) {
        if (dz > 0) return 0;
        if (dx < 0) return 1;
        if (dz < 0) return 2;
        return 3;
    }

    /** Box of thickness t hugging the side of the cell in direction `facing`. */
    private static int[] side(int facing, int t, int y0, int y1) {
        return switch (facing) {
            case 0 -> new int[]{0, y0, 16 - t, 16, y1, 16};
            case 1 -> new int[]{0, y0, 0, t, y1, 16};
            case 2 -> new int[]{0, y0, 0, 16, y1, t};
            default -> new int[]{16 - t, y0, 0, 16, y1, 16};
        };
    }

    /** Arm from the centre (half-width w) to the cell edge in direction `facing`. */
    private static int[] arm(int facing, int w, int y0, int y1) {
        int a = 8 - w, b = 8 + w;
        return switch (facing) {
            case 0 -> new int[]{a, y0, b, b, y1, 16};
            case 1 -> new int[]{0, y0, a, a, y1, b};
            case 2 -> new int[]{a, y0, 0, b, y1, a};
            default -> new int[]{b, y0, a, 16, y1, b};
        };
    }

    public static boolean connects(Block self, Getter g, int x, int y, int z, int facing) {
        Block n = Block.get(g.getBlock(x + DX[facing], y, z + DZ[facing]));
        if (n.model == Block.Model.CUBE && n.opaque) return true;
        return switch (self.shape) {
            case FENCE -> n.shape == Block.Shape.FENCE || n.shape == Block.Shape.GATE;
            case PANE -> n.shape == Block.Shape.PANE || n.shape == Block.Shape.WALL;
            case WALL -> n.shape == Block.Shape.WALL || n.shape == Block.Shape.GATE || n.shape == Block.Shape.PANE;
            default -> false;
        };
    }

    public static List<int[]> boxes(Block b, int meta, Getter g, int x, int y, int z, Mode mode) {
        List<int[]> out = new ArrayList<>(6);
        boolean col = mode == Mode.COLLISION;
        int f = meta & 3;
        switch (b.shape) {
            case SLAB -> {
                int m = meta & 3;
                out.add(m == 0 ? new int[]{0, 0, 0, 16, 8, 16} : m == 1 ? new int[]{0, 8, 0, 16, 16, 16} : new int[]{0, 0, 0, 16, 16, 16});
            }
            case STAIRS -> {
                boolean up = (meta & 4) != 0;
                out.add(up ? new int[]{0, 8, 0, 16, 16, 16} : new int[]{0, 0, 0, 16, 8, 16});
                out.add(up ? side(opposite(f), 8, 0, 8) : side(opposite(f), 8, 8, 16));
            }
            case FENCE, WALL, PANE -> {
                boolean fence = b.shape == Block.Shape.FENCE, wall = b.shape == Block.Shape.WALL;
                boolean[] c = new boolean[4];
                int n = 0;
                for (int d = 0; d < 4; d++) if (c[d] = connects(b, g, x, y, z, d)) n++;
                int top = col && !(b.shape == Block.Shape.PANE) ? 24 : 16;
                if (col || mode == Mode.OUTLINE) {
                    int w = fence ? 2 : wall ? 4 : 1;
                    if (wall && col) w = 4;
                    out.add(new int[]{8 - w, 0, 8 - w, 8 + w, top, 8 + w});
                    for (int d = 0; d < 4; d++) if (c[d]) out.add(arm(d, fence ? 2 : wall ? 3 : 1, 0, top));
                    break;
                }
                if (fence) {
                    out.add(new int[]{6, 0, 6, 10, 16, 10});
                    for (int d = 0; d < 4; d++) if (c[d]) {
                        int[] a1 = arm(d, 1, 12, 15), a2 = arm(d, 1, 6, 9);
                        trimToPost(a1, d, 2);
                        trimToPost(a2, d, 2);
                        out.add(a1);
                        out.add(a2);
                    }
                } else if (wall) {
                    boolean straightZ = c[0] && c[2] && !c[1] && !c[3], straightX = c[1] && c[3] && !c[0] && !c[2];
                    boolean above = g.getBlock(x, y + 1, z) != 0;
                    if ((straightZ || straightX) && !above) {
                        out.add(straightZ ? new int[]{5, 0, 0, 11, 14, 16} : new int[]{0, 0, 5, 16, 14, 11});
                    } else {
                        out.add(new int[]{4, 0, 4, 12, 16, 12});
                        for (int d = 0; d < 4; d++) if (c[d]) {
                            int[] a = arm(d, 3, 0, 14);
                            trimToPost(a, d, 4);
                            out.add(a);
                        }
                    }
                } else {
                    out.add(new int[]{7, 0, 7, 9, 16, 9});
                    for (int d = 0; d < 4; d++) if (c[d]) {
                        int[] a = arm(d, 1, 0, 16);
                        trimToPost(a, d, 1);
                        out.add(a);
                    }
                }
            }
            case GATE -> {
                boolean open = (meta & 4) != 0;
                boolean alongX = f == 0 || f == 2; // gate spans the x axis
                if (col) {
                    if (!open) out.add(alongX ? new int[]{0, 0, 6, 16, 24, 10} : new int[]{6, 0, 0, 10, 24, 16});
                    break;
                }
                if (mode == Mode.OUTLINE) {
                    out.add(alongX ? new int[]{0, 0, 6, 16, 16, 10} : new int[]{6, 0, 0, 10, 16, 16});
                    break;
                }
                List<int[]> local = new ArrayList<>();
                local.add(new int[]{0, 5, 7, 2, 16, 9});
                local.add(new int[]{14, 5, 7, 16, 16, 9});
                if (!open) {
                    local.add(new int[]{2, 6, 7, 14, 9, 9});
                    local.add(new int[]{2, 12, 7, 14, 15, 9});
                    local.add(new int[]{6, 9, 7, 8, 12, 9});
                    local.add(new int[]{8, 9, 7, 10, 12, 9});
                } else {
                    // Both halves swing away from the side the gate was opened from
                    boolean pos = f == 2 || f == 1;
                    int z0 = pos ? 9 : 1, z1 = pos ? 15 : 7;
                    local.add(new int[]{0, 6, z0, 2, 9, z1});
                    local.add(new int[]{0, 12, z0, 2, 15, z1});
                    local.add(new int[]{14, 6, z0, 16, 9, z1});
                    local.add(new int[]{14, 12, z0, 16, 15, z1});
                }
                for (int[] bx : local) out.add(alongX ? bx : new int[]{bx[2], bx[1], bx[0], bx[5], bx[4], bx[3]});
            }
            case DOOR -> {
                boolean open = (meta & 4) != 0, right = (meta & 16) != 0;
                int side = open ? (f + (right ? 3 : 1)) & 3 : f;
                out.add(side(side, 3, 0, 16));
            }
            case TRAPDOOR -> {
                boolean open = (meta & 4) != 0, top = (meta & 8) != 0;
                out.add(open ? side(f, 3, 0, 16) : top ? new int[]{0, 13, 0, 16, 16, 16} : new int[]{0, 0, 0, 16, 3, 16});
            }
            case LADDER -> out.add(side(f, mode == Mode.RENDER ? 1 : 3, 0, 16));
            case BED -> {
                out.add(new int[]{0, 3, 0, 16, 9, 16});
                if (mode == Mode.RENDER) {
                    boolean head = (meta & 4) != 0;
                    int d = head ? f : opposite(f);
                    int[] s = side(d, 3, 0, 3);
                    // Two legs in the corners of that side
                    if (d == 0 || d == 2) {
                        out.add(new int[]{0, 0, s[2], 3, 3, s[5]});
                        out.add(new int[]{13, 0, s[2], 16, 3, s[5]});
                    } else {
                        out.add(new int[]{s[0], 0, 0, s[3], 3, 3});
                        out.add(new int[]{s[0], 0, 13, s[3], 3, 16});
                    }
                } else if (col) {
                    out.set(0, new int[]{0, 0, 0, 16, 9, 16});
                }
            }
            case CARPET -> out.add(new int[]{0, 0, 0, 16, 1, 16});
            case PORTAL -> { if (!col) out.add((meta & 1) == 0 ? new int[]{0, 0, 6, 16, 16, 10} : new int[]{6, 0, 0, 10, 16, 16}); }
            case WIRE -> {
                if (col) break;
                if (mode == Mode.OUTLINE) { out.add(new int[]{0, 0, 0, 16, 1, 16}); break; }
                boolean[] c = new boolean[4];
                int n = 0;
                for (int d = 0; d < 4; d++) if (c[d] = Redstone.wireConnects(g, x, y, z, d)) n++;
                if (n == 1) for (int d = 0; d < 4; d++) if (c[d]) c[opposite(d)] = true;
                out.add(new int[]{5, 0, 5, 11, 1, 11});
                for (int d = 0; d < 4; d++) if (c[d]) {
                    int[] a = arm(d, 2, 0, 1);
                    trimToPost(a, d, 3);
                    out.add(a);
                }
            }
            case LEVER -> {
                if (col) break;
                boolean on = (meta & 8) != 0;
                int f6 = attachFacing(meta);
                out.add(rot6(new int[]{5, 0, 4, 11, 3, 12}, f6));
                out.add(rot6(on ? new int[]{7, 3, 9, 9, 10, 11} : new int[]{7, 3, 5, 9, 10, 7}, f6));
            }
            case BUTTON -> {
                if (col) break;
                int h = (meta & 8) != 0 ? 1 : 2;
                out.add(rot6(new int[]{5, 0, 6, 11, h, 10}, attachFacing(meta)));
            }
            case PLATE -> { if (!col) out.add(new int[]{1, 0, 1, 15, (meta & 1) != 0 ? 1 : 2, 15}); }
            case REPEATER -> {
                out.add(new int[]{0, 0, 0, 16, 2, 16});
                if (col) break;
                int delay = (meta >> 2) & 3;
                out.add(rotH(new int[]{7, 2, 10, 9, 7, 12}, meta & 3));
                out.add(rotH(new int[]{7, 2, 2 + 2 * delay, 9, 7, 4 + 2 * delay}, meta & 3));
            }
            case PISTON -> {
                int f6 = meta & 7;
                if ((meta & 8) == 0) out.add(new int[]{0, 0, 0, 16, 16, 16});
                else {
                    out.add(rot6(new int[]{0, 0, 0, 16, 12, 16}, f6));
                    if (!col) out.add(rot6(new int[]{6, 12, 6, 10, 16, 10}, f6));
                }
            }
            case TABLE -> out.add(new int[]{0, 0, 0, 16, 12, 16});
            case BREWING -> {
                out.add(new int[]{7, 0, 7, 9, 14, 9});
                if (!col) {
                    out.add(new int[]{9, 0, 5, 15, 2, 11});
                    out.add(new int[]{2, 0, 1, 8, 2, 7});
                    out.add(new int[]{2, 0, 9, 8, 2, 15});
                }
            }
            case ANVIL -> {
                out.add(rotH(new int[]{2, 0, 2, 14, 4, 14}, f));
                out.add(rotH(new int[]{4, 4, 3, 12, 5, 13}, f));
                out.add(rotH(new int[]{6, 5, 4, 10, 10, 12}, f));
                out.add(rotH(new int[]{3, 10, 0, 13, 16, 16}, f));
            }
            case PISTON_HEAD -> {
                int f6 = meta & 7;
                out.add(rot6(new int[]{0, 12, 0, 16, 16, 16}, f6));
                out.add(rot6(new int[]{6, 0, 6, 10, 12, 10}, f6));
            }
            case CAKE -> out.add(new int[]{1 + 2 * Math.min(6, meta & 7), 0, 1, 15, 8, 15});
            case SNOW_LAYER -> {
                int layers = (meta & 7) + 1;
                if (col) { if (layers > 1) out.add(new int[]{0, 0, 0, 16, 2 * (layers - 1), 16}); }
                else out.add(new int[]{0, 0, 0, 16, 2 * layers, 16});
            }
            default -> out.add(new int[]{0, 0, 0, 16, 16, 16});
        }
        return out;
    }

    /** 6-way facing (0 down, 1 up, 2 north, 3 south, 4 west, 5 east) of a horizontal facing (0 south, 1 west, 2 north, 3 east). */
    public static final int[] H_TO_6 = {3, 4, 2, 5};
    public static final int[][] DIR6 = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    /**
     * Levers and buttons: meta bits 0-2 give the attached face (0 floor, 1-4 wall towards horizontal facing m-1,
     * 5 ceiling). Returns the 6-way direction the component points (away from its support).
     */
    public static int attachFacing(int meta) {
        int m = meta & 7;
        if (m == 0) return 1;
        if (m == 5) return 0;
        return H_TO_6[opposite(m - 1)];
    }

    /** Rotates a box modelled pointing up (+y) so it points along 6-way facing f. */
    public static int[] rot6(int[] b, int f) {
        return switch (f) {
            case 0 -> new int[]{b[0], 16 - b[4], b[2], b[3], 16 - b[1], b[5]};
            case 2 -> new int[]{b[0], b[2], 16 - b[4], b[3], b[5], 16 - b[1]};
            case 3 -> new int[]{b[0], b[2], b[1], b[3], b[5], b[4]};
            case 4 -> new int[]{16 - b[4], b[0], b[2], 16 - b[1], b[3], b[5]};
            case 5 -> new int[]{b[1], b[0], b[2], b[4], b[3], b[5]};
            default -> b;
        };
    }

    /** Rotates a box modelled facing south (+z) to horizontal facing f. */
    public static int[] rotH(int[] b, int f) {
        return switch (f & 3) {
            case 1 -> new int[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
            case 2 -> new int[]{16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]};
            case 3 -> new int[]{b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]};
            default -> b;
        };
    }

    /** Starts an arm at the post's surface instead of the centre, avoiding overlapping faces. */
    private static void trimToPost(int[] a, int facing, int postHalf) {
        switch (facing) {
            case 0 -> a[2] = 8 + postHalf;
            case 1 -> a[3] = 8 - postHalf;
            case 2 -> a[5] = 8 - postHalf;
            default -> a[0] = 8 + postHalf;
        }
    }

    /** Boxes for the item model (GUI icon, held item, dropped item). */
    public static List<int[]> itemBoxes(Block b) {
        List<int[]> out = new ArrayList<>();
        switch (b.shape) {
            case FENCE -> {
                out.add(new int[]{0, 0, 6, 4, 16, 10});
                out.add(new int[]{12, 0, 6, 16, 16, 10});
                out.add(new int[]{4, 12, 7, 12, 15, 9});
                out.add(new int[]{4, 6, 7, 12, 9, 9});
            }
            case WALL -> {
                out.add(new int[]{4, 0, 4, 12, 16, 12});
                out.add(new int[]{0, 0, 5, 4, 13, 11});
                out.add(new int[]{12, 0, 5, 16, 13, 11});
            }
            default -> out.addAll(boxes(b, b.shape == Block.Shape.STAIRS ? 1 : 0, EMPTY, 0, 0, 0, Mode.RENDER));
        }
        return out;
    }

    public static final Getter EMPTY = new Getter() {
        public int getBlock(int x, int y, int z) { return 0; }
        public int getMeta(int x, int y, int z) { return 0; }
    };

    /** Torch geometry: base box plus the horizontal shear applied to its top for wall torches. */
    public static float[] torchOffset(int meta) {
        if (meta < 1 || meta > 4) return new float[]{0, 0, 0, 0};
        int wall = meta - 1;
        // Base sits against the wall, top leans away from it
        float bx = DX[wall] * 5.5f, bz = DZ[wall] * 5.5f;
        return new float[]{bx, bz, -DX[wall] * 3.5f, -DZ[wall] * 3.5f};
    }
}
