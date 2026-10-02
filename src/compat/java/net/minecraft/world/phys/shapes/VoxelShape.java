package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A block's shape as a list of boxes in block coordinates (reamc-compat). */
public class VoxelShape {
    final List<AABB> boxes;

    VoxelShape(List<AABB> boxes) { this.boxes = Collections.unmodifiableList(boxes); }

    public boolean isEmpty() { return boxes.isEmpty(); }

    public double min(Direction.Axis a) { double m = Double.POSITIVE_INFINITY; for (AABB b : boxes) m = Math.min(m, b.min(a)); return m; }

    public double max(Direction.Axis a) { double m = Double.NEGATIVE_INFINITY; for (AABB b : boxes) m = Math.max(m, b.max(a)); return m; }

    public AABB bounds() {
        if (isEmpty()) throw new UnsupportedOperationException("No bounds for empty shape.");
        AABB r = boxes.get(0);
        for (AABB b : boxes) r = r.minmax(b);
        return r;
    }

    public VoxelShape move(double x, double y, double z) {
        if (isEmpty()) return this;
        List<AABB> l = new ArrayList<>();
        for (AABB b : boxes) l.add(b.move(x, y, z));
        return new VoxelShape(l);
    }

    public VoxelShape optimize() { return this; }

    public List<AABB> toAabbs() { return boxes; }

    public void forAllBoxes(Shapes.DoubleLineConsumer c) { for (AABB b : boxes) c.consume(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }

    public void forAllEdges(Shapes.DoubleLineConsumer c) {
        for (AABB b : boxes) {
            double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
            for (double y : ys) for (double z : zs) c.consume(b.minX, y, z, b.maxX, y, z);
            for (double x : xs) for (double z : zs) c.consume(x, b.minY, z, x, b.maxY, z);
            for (double x : xs) for (double y : ys) c.consume(x, y, b.minZ, x, y, b.maxZ);
        }
    }

    public VoxelShape singleEncompassing() { return isEmpty() ? this : Shapes.create(bounds()); }

    /** The part of the shape touching a face of the block, flattened onto it. */
    public VoxelShape getFaceShape(Direction d) {
        List<AABB> l = new ArrayList<>();
        for (AABB b : boxes) {
            boolean touches = switch (d) {
                case DOWN -> b.minY <= 1e-7; case UP -> b.maxY >= 1 - 1e-7;
                case NORTH -> b.minZ <= 1e-7; case SOUTH -> b.maxZ >= 1 - 1e-7;
                case WEST -> b.minX <= 1e-7; case EAST -> b.maxX >= 1 - 1e-7;
            };
            if (touches) l.add(b);
        }
        return new VoxelShape(l);
    }

    public BlockHitResult clip(Vec3 from, Vec3 to, BlockPos pos) {
        BlockHitResult best = null;
        double bd = Double.MAX_VALUE;
        for (AABB b : boxes) {
            AABB w = b.move(pos);
            var hit = w.clip(from, to);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(from);
            if (d < bd) {
                bd = d;
                Vec3 h = hit.get();
                Direction face = Direction.getNearest(h.x - w.getCenter().x, h.y - w.getCenter().y, h.z - w.getCenter().z);
                best = new BlockHitResult(h, face, pos, false);
            }
        }
        return best;
    }

    @Override public String toString() { return "VoxelShape" + boxes; }
}
