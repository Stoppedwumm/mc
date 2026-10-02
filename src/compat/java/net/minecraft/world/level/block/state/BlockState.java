package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.TriState;

/** A block with its property values (reamc-compat). */
public class BlockState extends BlockBehaviour.BlockStateBase {
    public static final Codec<BlockState> CODEC = mc.mod.DataCodecs.BLOCK_STATE;

    public BlockState(Block owner, StateDefinition<Block, ?> definition, int index) { super(owner, definition, index); }

    @Override protected BlockState asState() { return this; }

    // NeoForge's IBlockStateExtension
    public boolean onDestroyedByPlayer(Level l, BlockPos p, Player pl, boolean willHarvest, FluidState fluid) { return owner.onDestroyedByPlayer(this, l, p, pl, willHarvest, fluid); }
    public ItemStack getCloneItemStack(HitResult hit, LevelReader l, BlockPos p, Player pl) { return owner.getCloneItemStack(this, hit, l, p, pl); }
    public TriState canSustainPlant(BlockGetter l, BlockPos p, Direction d, BlockState plant) { return owner.canSustainPlant(this, l, p, d, plant); }
    public boolean isFertile(BlockGetter l, BlockPos p) { return owner.isFertile(this, l, p); }
    public BlockState rotate(LevelAccessor l, BlockPos p, Rotation r) { return owner.rotate(this, l, p, r); }
    public SoundType getSoundType(LevelReader l, BlockPos p, Entity e) { return owner.getSoundType(this, l, p, e); }
    public PathType getBlockPathType(BlockGetter l, BlockPos p, Mob m) { return owner.getBlockPathType(this, l, p, m); }
    public int getFlammability(BlockGetter l, BlockPos p, Direction d) { return owner.getFlammability(this, l, p, d); }
    public boolean isFlammable(BlockGetter l, BlockPos p, Direction d) { return owner.isFlammable(this, l, p, d); }
    public int getFireSpreadSpeed(BlockGetter l, BlockPos p, Direction d) { return owner.getFireSpreadSpeed(this, l, p, d); }
    public BlockState getToolModifiedState(UseOnContext c, ItemAbility a, boolean simulate) { return owner.getToolModifiedState(this, c, a, simulate); }
    public boolean canBeHydrated(BlockGetter l, BlockPos p, FluidState fluid, BlockPos fluidPos) { return owner.canBeHydrated(this, l, p, fluid, fluidPos); }
    public float getFriction(LevelReader l, BlockPos p, Entity e) { return owner.getFriction(); }
    public int getLightEmission(BlockGetter l, BlockPos p) { return getLightEmission(); }
    public boolean isLadder(LevelReader l, BlockPos p, net.minecraft.world.entity.LivingEntity e) { return is(net.minecraft.tags.BlockTags.CLIMBABLE); }
}
