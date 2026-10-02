package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** The six directions, in Minecraft's order and with its steps (reamc-compat). */
public enum Direction implements StringRepresentable {
    DOWN(0, -1, 0, "down"), UP(0, 1, 0, "up"), NORTH(0, 0, -1, "north"), SOUTH(0, 0, 1, "south"), WEST(-1, 0, 0, "west"), EAST(1, 0, 0, "east");

    public static final StringRepresentable.EnumCodec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
    public static final com.mojang.serialization.Codec<Direction> VERTICAL_CODEC = CODEC.validate(d -> d.getAxis().isVertical()
            ? com.mojang.serialization.DataResult.success(d) : com.mojang.serialization.DataResult.error(() -> "Expected a vertical direction"));
    public static final IntFunction<Direction> BY_ID = Direction::from3DDataValue;
    public static final StreamCodec<ByteBuf, Direction> STREAM_CODEC = StreamCodec.of((b, d) -> b.writeByte(d.ordinal()), b -> from3DDataValue(b.readByte()));

    private final int dx, dy, dz;
    private final String name;

    Direction(int dx, int dy, int dz, String name) {
        this.dx = dx; this.dy = dy; this.dz = dz; this.name = name;
    }

    public int getStepX() { return dx; }
    public int getStepY() { return dy; }
    public int getStepZ() { return dz; }
    public org.joml.Vector3f step() { return new org.joml.Vector3f(dx, dy, dz); }
    public Vec3i getNormal() { return new Vec3i(dx, dy, dz); }
    public String getName() { return name; }
    @Override public String getSerializedName() { return name; }
    public int get3DDataValue() { return ordinal(); }
    public int get2DDataValue() { return switch (this) { case SOUTH -> 0; case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> -1; }; }
    public AxisDirection getAxisDirection() { return dx + dy + dz > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE; }

    public Direction getOpposite() { return values()[ordinal() ^ 1]; }

    public Direction getClockWise() { return switch (this) { case NORTH -> EAST; case EAST -> SOUTH; case SOUTH -> WEST; case WEST -> NORTH; default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this); }; }
    public Direction getCounterClockWise() { return switch (this) { case NORTH -> WEST; case WEST -> SOUTH; case SOUTH -> EAST; case EAST -> NORTH; default -> throw new IllegalStateException("Unable to get CCW facing of " + this); }; }

    public Direction getClockWise(Axis axis) {
        return switch (axis) {
            case Y -> getClockWise();
            case X -> switch (this) { case DOWN -> NORTH; case NORTH -> UP; case UP -> SOUTH; case SOUTH -> DOWN; default -> this; };
            case Z -> switch (this) { case DOWN -> WEST; case WEST -> UP; case UP -> EAST; case EAST -> DOWN; default -> this; };
        };
    }

    public Direction getCounterClockWise(Axis axis) { return getClockWise(axis).getClockWise(axis).getClockWise(axis); }

    public float toYRot() { return (get2DDataValue() & 3) * 90; }

    public boolean isFacingAngle(float yRot) {
        float r = yRot * (float) (Math.PI / 180);
        return getStepX() * -Math.sin(r) + getStepZ() * Math.cos(r) > 0;
    }

    public static Direction from3DDataValue(int v) { return values()[Math.floorMod(v, 6)]; }
    public static Direction from2DDataValue(int v) { return new Direction[]{SOUTH, WEST, NORTH, EAST}[Math.floorMod(v, 4)]; }
    public static Direction fromYRot(double yRot) { return from2DDataValue((int) Math.floor(yRot / 90.0 + 0.5) & 3); }

    public static Direction fromDelta(int x, int y, int z) {
        for (Direction d : values()) if (d.dx == x && d.dy == y && d.dz == z) return d;
        return null;
    }

    public static Direction fromAxisAndDirection(Axis axis, AxisDirection dir) { return get(dir, axis); }

    public static Direction get(AxisDirection dir, Axis axis) {
        return switch (axis) {
            case X -> dir == AxisDirection.POSITIVE ? EAST : WEST;
            case Y -> dir == AxisDirection.POSITIVE ? UP : DOWN;
            case Z -> dir == AxisDirection.POSITIVE ? SOUTH : NORTH;
        };
    }

    public static Direction getNearest(double x, double y, double z) {
        Direction best = NORTH;
        double bd = -Double.MAX_VALUE;
        for (Direction d : values()) {
            double v = x * d.dx + y * d.dy + z * d.dz;
            if (v > bd) { bd = v; best = d; }
        }
        return best;
    }

    public static Direction getNearest(float x, float y, float z) { return getNearest((double) x, y, z); }

    public static Direction getNearest(net.minecraft.world.phys.Vec3 v) { return getNearest(v.x, v.y, v.z); }

    public static Direction getRandom(RandomSource r) { return values()[r.nextInt(6)]; }

    public static Direction byName(String n) {
        for (Direction d : values()) if (d.name.equals(n)) return d;
        return null;
    }

    public static Stream<Direction> stream() { return Stream.of(values()); }

    public static java.util.Collection<Direction> allShuffled(RandomSource r) {
        java.util.List<Direction> l = new java.util.ArrayList<>(java.util.List.of(values()));
        for (int i = l.size() - 1; i > 0; i--) java.util.Collections.swap(l, i, r.nextInt(i + 1));
        return l;
    }

    /** Directions ordered by how directly the entity looks along them. */
    public static Direction[] orderedByNearest(net.minecraft.world.entity.Entity e) {
        net.minecraft.world.phys.Vec3 look = e.getLookAngle();
        Direction[] out = values().clone();
        java.util.Arrays.sort(out, java.util.Comparator.comparingDouble(d -> -(look.x * d.dx + look.y * d.dy + look.z * d.dz)));
        return out;
    }

    public Axis getAxis() { return dx != 0 ? Axis.X : dy != 0 ? Axis.Y : Axis.Z; }

    @Override public String toString() { return name; }

    public enum Axis implements StringRepresentable, Predicate<Direction> {
        X("x"), Y("y"), Z("z");

        public static final Axis[] VALUES = values();
        public static final StringRepresentable.EnumCodec<Axis> CODEC = StringRepresentable.fromEnum(Axis::values);
        private final String name;

        Axis(String name) { this.name = name; }

        public String getName() { return name; }
        @Override public String getSerializedName() { return name; }
        public boolean isVertical() { return this == Y; }
        public boolean isHorizontal() { return this != Y; }
        public Plane getPlane() { return this == Y ? Plane.VERTICAL : Plane.HORIZONTAL; }
        public int choose(int x, int y, int z) { return this == X ? x : this == Y ? y : z; }
        public double choose(double x, double y, double z) { return this == X ? x : this == Y ? y : z; }
        @Override public boolean test(Direction d) { return d != null && d.getAxis() == this; }
        public static Axis byName(String n) { for (Axis a : values()) if (a.name.equals(n)) return a; return null; }
        public static Axis getRandom(RandomSource r) { return values()[r.nextInt(3)]; }
        @Override public String toString() { return name; }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"), NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        AxisDirection(int step, String name) { this.step = step; this.name = name; }

        public int getStep() { return step; }
        public String getName() { return name; }
        public AxisDirection opposite() { return this == POSITIVE ? NEGATIVE : POSITIVE; }
        @Override public String toString() { return name; }
    }

    public enum Plane implements Iterable<Direction>, Predicate<Direction> {
        HORIZONTAL(new Direction[]{NORTH, EAST, SOUTH, WEST}, new Axis[]{Axis.X, Axis.Z}),
        VERTICAL(new Direction[]{UP, DOWN}, new Axis[]{Axis.Y});

        private final Direction[] faces;
        private final Axis[] axes;

        Plane(Direction[] faces, Axis[] axes) { this.faces = faces; this.axes = axes; }

        public Direction getRandomDirection(RandomSource r) { return faces[r.nextInt(faces.length)]; }
        public Axis getRandomAxis(RandomSource r) { return axes[r.nextInt(axes.length)]; }
        @Override public boolean test(Direction d) { return d != null && d.getAxis().getPlane() == this; }
        @Override public Iterator<Direction> iterator() { return java.util.Arrays.asList(faces).iterator(); }
        public Stream<Direction> stream() { return Stream.of(faces); }
        public java.util.List<Direction> shuffledCopy(RandomSource r) {
            java.util.List<Direction> l = new java.util.ArrayList<>(java.util.List.of(faces));
            for (int i = l.size() - 1; i > 0; i--) java.util.Collections.swap(l, i, r.nextInt(i + 1));
            return l;
        }
        public int length() { return faces.length; }
    }
}
