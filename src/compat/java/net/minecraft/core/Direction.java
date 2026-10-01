package net.minecraft.core;

/** The six directions, in Minecraft's order and with its steps (reamc-compat). */
public enum Direction implements net.minecraft.util.StringRepresentable {
    DOWN(0, -1, 0, "down"), UP(0, 1, 0, "up"), NORTH(0, 0, -1, "north"), SOUTH(0, 0, 1, "south"), WEST(-1, 0, 0, "west"), EAST(1, 0, 0, "east");

    private final int dx, dy, dz;
    private final String name;

    Direction(int dx, int dy, int dz, String name) {
        this.dx = dx; this.dy = dy; this.dz = dz; this.name = name;
    }

    public int getStepX() { return dx; }
    public int getStepY() { return dy; }
    public int getStepZ() { return dz; }
    public String getName() { return name; }
    public String getSerializedName() { return name; }
    public int get3DDataValue() { return ordinal(); }
    public int get2DDataValue() { return switch (this) { case SOUTH -> 0; case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> -1; }; }

    public Direction getOpposite() { return values()[ordinal() ^ 1]; }

    public Direction getClockWise() { return switch (this) { case NORTH -> EAST; case EAST -> SOUTH; case SOUTH -> WEST; case WEST -> NORTH; default -> this; }; }
    public Direction getCounterClockWise() { return switch (this) { case NORTH -> WEST; case WEST -> SOUTH; case SOUTH -> EAST; case EAST -> NORTH; default -> this; }; }

    public static Direction from3DDataValue(int v) { return values()[Math.floorMod(v, 6)]; }
    public static Direction from2DDataValue(int v) { return new Direction[]{SOUTH, WEST, NORTH, EAST}[Math.floorMod(v, 4)]; }

    public static Direction byName(String n) {
        for (Direction d : values()) if (d.name.equals(n)) return d;
        return null;
    }

    public Axis getAxis() { return dx != 0 ? Axis.X : dy != 0 ? Axis.Y : Axis.Z; }

    @Override public String toString() { return name; }

    public enum Axis { X, Y, Z }
}
