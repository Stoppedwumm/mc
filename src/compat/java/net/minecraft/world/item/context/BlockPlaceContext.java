package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Where and how a block is being placed (reamc-compat). */
public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) { this(player.level(), player, hand, stack, hit); }

    public BlockPlaceContext(UseOnContext c) { this(c.getLevel(), c.getPlayer(), c.getHand(), c.getItemInHand(), c.getHitResult()); }

    public BlockPlaceContext(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        super(level, player, hand, stack, hit);
        relativePos = hit.getBlockPos().relative(hit.getDirection());
        replaceClicked = level.getBlockState(hit.getBlockPos()).canBeReplaced(this);
    }

    /** reamc's placement: the block goes exactly at pos (the face was clicked on the neighbour). */
    public BlockPlaceContext(Level level, Player player, BlockPos pos, Direction face, ItemStack item) {
        super(level, player, InteractionHand.MAIN_HAND, item, new BlockHitResult(Vec3.atCenterOf(pos), face, pos.relative(face.getOpposite()), false));
        relativePos = pos;
        replaceClicked = false;
    }

    public static BlockPlaceContext at(BlockPlaceContext c, BlockPos pos, Direction face) {
        return new BlockPlaceContext(c.getLevel(), c.getPlayer(), c.getHand(), c.getItemInHand(),
                new BlockHitResult(new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.5, pos.getY() + 0.5 + face.getStepY() * 0.5, pos.getZ() + 0.5 + face.getStepZ() * 0.5), face, pos, false));
    }

    @Override public BlockPos getClickedPos() { return replaceClicked ? super.getClickedPos() : relativePos; }

    public boolean canPlace() { return replaceClicked || getLevel().getBlockState(getClickedPos()).canBeReplaced(this); }

    public boolean replacingClickedOnBlock() { return replaceClicked; }

    /** Direction the player looks in most strongly (up and down included). */
    public Direction getNearestLookingDirection() { return getNearestLookingDirections()[0]; }

    public Direction getNearestLookingVerticalDirection() {
        Player p = getPlayer();
        return p != null && p.getXRot() < 0 ? Direction.UP : Direction.DOWN;
    }

    public Direction[] getNearestLookingDirections() {
        Player p = getPlayer();
        float yaw = p == null ? 0 : p.getYRot(), pitch = p == null ? 0 : p.getXRot();
        double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        Direction[] all = Direction.values().clone();
        java.util.Arrays.sort(all, (a, b) -> Double.compare(
                -(a.getStepX() * dx + a.getStepY() * dy + a.getStepZ() * dz), -(b.getStepX() * dx + b.getStepY() * dy + b.getStepZ() * dz)));
        if (!replaceClicked) {
            Direction face = getClickedFace();
            int i = java.util.Arrays.asList(all).indexOf(face.getOpposite());
            if (i > 0) { System.arraycopy(all, 0, all, 1, i); all[0] = face.getOpposite(); }
        }
        return all;
    }

    @Override
    public Direction getHorizontalDirection() {
        Player p = getPlayer();
        return Direction.fromYRot(p == null ? 0 : p.getYRot());
    }
}
