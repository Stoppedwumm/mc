package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

/** A block position (reamc-compat). */
public class BlockPos extends Vec3i {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);
    public static final com.mojang.serialization.Codec<BlockPos> CODEC = com.mojang.serialization.Codec.INT_STREAM.comapFlatMap(
            s -> {
                int[] a = s.toArray();
                return a.length == 3 ? com.mojang.serialization.DataResult.success(new BlockPos(a[0], a[1], a[2])) : com.mojang.serialization.DataResult.error(() -> "Not a block position");
            }, p -> java.util.stream.IntStream.of(p.getX(), p.getY(), p.getZ()));
    public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<>() {
        @Override public BlockPos decode(ByteBuf b) { return of(b.readLong()); }
        @Override public void encode(ByteBuf b, BlockPos p) { b.writeLong(p.asLong()); }
    };

    public BlockPos(int x, int y, int z) { super(x, y, z); }

    public BlockPos(Vec3i v) { this(v.getX(), v.getY(), v.getZ()); }

    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockPos containing(Position p) { return containing(p.x(), p.y(), p.z()); }

    @Override public BlockPos offset(int dx, int dy, int dz) { return dx == 0 && dy == 0 && dz == 0 ? this : new BlockPos(getX() + dx, getY() + dy, getZ() + dz); }
    @Override public BlockPos offset(Vec3i o) { return offset(o.getX(), o.getY(), o.getZ()); }
    public BlockPos subtract(Vec3i o) { return offset(-o.getX(), -o.getY(), -o.getZ()); }
    @Override public BlockPos multiply(int f) { return new BlockPos(getX() * f, getY() * f, getZ() * f); }
    public BlockPos relative(Direction d) { return offset(d.getStepX(), d.getStepY(), d.getStepZ()); }
    @Override public BlockPos relative(Direction d, int n) { return offset(d.getStepX() * n, d.getStepY() * n, d.getStepZ() * n); }
    public BlockPos relative(Direction.Axis a, int n) { return offset(a == Direction.Axis.X ? n : 0, a == Direction.Axis.Y ? n : 0, a == Direction.Axis.Z ? n : 0); }
    @Override public BlockPos above() { return relative(Direction.UP); }
    public BlockPos above(int n) { return relative(Direction.UP, n); }
    @Override public BlockPos below() { return relative(Direction.DOWN); }
    public BlockPos below(int n) { return relative(Direction.DOWN, n); }
    public BlockPos north() { return relative(Direction.NORTH); }
    public BlockPos north(int n) { return relative(Direction.NORTH, n); }
    public BlockPos south() { return relative(Direction.SOUTH); }
    public BlockPos south(int n) { return relative(Direction.SOUTH, n); }
    public BlockPos west() { return relative(Direction.WEST); }
    public BlockPos west(int n) { return relative(Direction.WEST, n); }
    public BlockPos east() { return relative(Direction.EAST); }
    public BlockPos east(int n) { return relative(Direction.EAST, n); }
    public BlockPos immutable() { return this; }
    public MutableBlockPos mutable() { return new MutableBlockPos(getX(), getY(), getZ()); }
    public Vec3 getCenter() { return Vec3.atCenterOf(this); }
    public Vec3 getBottomCenter() { return Vec3.atBottomCenterOf(this); }

    public long asLong() { return asLong(getX(), getY(), getZ()); }

    public static long asLong(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF); }

    public static BlockPos of(long packed) {
        int px = (int) (packed >> 38), pz = (int) (packed << 26 >> 38), py = (int) (packed << 52 >> 52);
        return new BlockPos(px, py, pz);
    }

    public static int getX(long packed) { return (int) (packed >> 38); }
    public static int getY(long packed) { return (int) (packed << 52 >> 52); }
    public static int getZ(long packed) { return (int) (packed << 26 >> 38); }

    /** Every position in the box, x fastest (the same mutable position each time, like Minecraft). */
    public static Iterable<BlockPos> betweenClosed(BlockPos a, BlockPos b) {
        return betweenClosed(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public static Iterable<BlockPos> betweenClosed(int x0, int y0, int z0, int x1, int y1, int z1) {
        return () -> new Iterator<>() {
            final MutableBlockPos cur = new MutableBlockPos();
            int i, n = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
            @Override public boolean hasNext() { return i < n; }
            @Override public BlockPos next() {
                int w = x1 - x0 + 1, d = z1 - z0 + 1;
                int x = i % w, z = i / w % d, y = i / (w * d);
                i++;
                return cur.set(x0 + x, y0 + y, z0 + z);
            }
        };
    }

    public static java.util.stream.Stream<BlockPos> betweenClosedStream(BlockPos a, BlockPos b) {
        return java.util.stream.StreamSupport.stream(betweenClosed(a, b).spliterator(), false).map(BlockPos::immutable);
    }

    @Override public String toString() { return "BlockPos{x=" + getX() + ", y=" + getY() + ", z=" + getZ() + "}"; }

    /** A position that can be moved in place (reamc-compat). */
    public static class MutableBlockPos extends BlockPos {
        public MutableBlockPos() { this(0, 0, 0); }
        public MutableBlockPos(int x, int y, int z) { super(x, y, z); }
        public MutableBlockPos(double x, double y, double z) { this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)); }

        @Override public BlockPos offset(int dx, int dy, int dz) { return new BlockPos(getX() + dx, getY() + dy, getZ() + dz); }
        @Override public BlockPos immutable() { return new BlockPos(getX(), getY(), getZ()); }

        public MutableBlockPos set(int x, int y, int z) { setX(x); setY(y); setZ(z); return this; }
        public MutableBlockPos set(double x, double y, double z) { return set((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)); }
        public MutableBlockPos set(Vec3i v) { return set(v.getX(), v.getY(), v.getZ()); }
        public MutableBlockPos set(long packed) { return set(getX(packed), getY(packed), getZ(packed)); }
        public MutableBlockPos setWithOffset(Vec3i v, Direction d) { return set(v.getX() + d.getStepX(), v.getY() + d.getStepY(), v.getZ() + d.getStepZ()); }
        public MutableBlockPos setWithOffset(Vec3i v, int dx, int dy, int dz) { return set(v.getX() + dx, v.getY() + dy, v.getZ() + dz); }
        public MutableBlockPos setWithOffset(Vec3i v, Vec3i o) { return set(v.getX() + o.getX(), v.getY() + o.getY(), v.getZ() + o.getZ()); }
        public MutableBlockPos move(Direction d) { return move(d, 1); }
        public MutableBlockPos move(Direction d, int n) { return set(getX() + d.getStepX() * n, getY() + d.getStepY() * n, getZ() + d.getStepZ() * n); }
        public MutableBlockPos move(int dx, int dy, int dz) { return set(getX() + dx, getY() + dy, getZ() + dz); }
        public MutableBlockPos move(Vec3i v) { return move(v.getX(), v.getY(), v.getZ()); }
        @Override public MutableBlockPos setX(int x) { super.setX(x); return this; }
        @Override public MutableBlockPos setY(int y) { super.setY(y); return this; }
        @Override public MutableBlockPos setZ(int z) { super.setZ(z); return this; }
    }
}
