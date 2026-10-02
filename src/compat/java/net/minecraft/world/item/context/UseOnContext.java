package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** An item used on a block (reamc-compat). */
public class UseOnContext {
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult hitResult;
    private final Level level;
    private final ItemStack itemStack;

    public UseOnContext(Player player, InteractionHand hand, BlockHitResult hit) { this(player.level(), player, hand, player.getItemInHand(hand), hit); }

    public UseOnContext(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hit;
        this.itemStack = stack;
        this.level = level;
    }

    protected final BlockHitResult getHitResult() { return hitResult; }
    public BlockPos getClickedPos() { return hitResult.getBlockPos(); }
    public Direction getClickedFace() { return hitResult.getDirection(); }
    public Vec3 getClickLocation() { return hitResult.getLocation(); }
    public boolean isInside() { return hitResult.isInside(); }
    public ItemStack getItemInHand() { return itemStack; }
    public Player getPlayer() { return player; }
    public InteractionHand getHand() { return hand; }
    public Level getLevel() { return level; }
    public Direction getHorizontalDirection() { return player == null ? Direction.NORTH : player.getDirection(); }
    public boolean isSecondaryUseActive() { return player != null && player.isSecondaryUseActive(); }
    public float getRotation() { return player == null ? 0 : player.getYRot(); }
}
