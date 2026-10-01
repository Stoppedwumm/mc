package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A mod's block; reamc creates an engine block (and its states as metadata) when it is registered (reamc-compat). */
public class Block extends BlockBehaviour implements ItemLike {
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    /** The engine block this stands for (set when registered). */
    public mc.world.Block reamc$block;
    private String descriptionId;

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        createBlockStateDefinition(builder);
        stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        registerDefaultState(stateDefinition.any());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { }

    protected final void registerDefaultState(BlockState state) { defaultBlockState = state; }

    public final BlockState defaultBlockState() { return defaultBlockState; }

    public StateDefinition<Block, BlockState> getStateDefinition() { return stateDefinition; }

    public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState(); }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) { }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) { return state; }

    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) { }

    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) { }

    @Override
    public Item asItem() { return mc.mod.Bridge.blockItem(this); }

    public String getDescriptionId() {
        if (descriptionId == null) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this);
            descriptionId = id == null ? "block.unregistered" : id.toLanguageKey("block");
        }
        return descriptionId;
    }

    public MutableComponent getName() { return Component.translatable(getDescriptionId()); }

    public float getExplosionResistance() { return properties.reamc$destroyTime(); }

    public static Block byItem(Item item) { return item instanceof net.minecraft.world.item.BlockItem b ? b.getBlock() : Blocks.AIR; }

    public static BlockState stateById(int id) { return mc.mod.Bridge.state(id & 255, id >> 8); }

    @Override public String toString() { return "Block{" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this) + "}"; }
}
