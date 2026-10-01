package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Where and how a block is being placed (reamc-compat). */
public class BlockPlaceContext {
    private final Level level;
    private final Player player;
    private final BlockPos pos;
    private final Direction face;
    private final ItemStack item;

    public BlockPlaceContext(Level level, Player player, BlockPos pos, Direction face, ItemStack item) {
        this.level = level;
        this.player = player;
        this.pos = pos;
        this.face = face;
        this.item = item;
    }

    public Level getLevel() { return level; }
    public Player getPlayer() { return player; }
    public BlockPos getClickedPos() { return pos; }
    public Direction getClickedFace() { return face; }
    public ItemStack getItemInHand() { return item; }
    public InteractionHand getHand() { return InteractionHand.MAIN_HAND; }
    public boolean canPlace() { return true; }
    public boolean replacingClickedOnBlock() { return false; }
    public boolean isSecondaryUseActive() { return player != null && player.isShiftKeyDown(); }

    /** Direction the player looks in most strongly (up and down included). */
    public Direction getNearestLookingDirection() { return getNearestLookingDirections()[0]; }

    public Direction[] getNearestLookingDirections() {
        float yaw = player == null ? 0 : player.getYRot(), pitch = player == null ? 0 : player.getXRot();
        double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        Direction[] all = Direction.values().clone();
        java.util.Arrays.sort(all, (a, b) -> Double.compare(
                -(a.getStepX() * dx + a.getStepY() * dy + a.getStepZ() * dz), -(b.getStepX() * dx + b.getStepY() * dy + b.getStepZ() * dz)));
        return all;
    }

    public Direction getHorizontalDirection() {
        float yaw = player == null ? 0 : player.getYRot();
        return Direction.from2DDataValue(Math.floorMod(Math.round(yaw / 90f), 4));
    }

    public float getRotation() { return player == null ? 0 : player.getYRot(); }
}
