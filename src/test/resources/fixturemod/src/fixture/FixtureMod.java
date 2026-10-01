package fixture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A small NeoForge-style mod used by reamc's tests: blocks, a facing block, a block entity, items, a tool, events. */
@Mod("fixture")
public class FixtureMod {
    public static int ticks;
    public static boolean setupRan;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fixture");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fixture");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fixture");

    public static final DeferredBlock<Block> GEM_BLOCK = BLOCKS.registerSimpleBlock("gem_block",
            BlockBehaviour.Properties.of().strength(2f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> LAMP = BLOCKS.register("lamp", () -> new Lamp(BlockBehaviour.Properties.of().strength(1f).lightLevel(s -> 12)));
    public static final DeferredBlock<Block> COUNTER = BLOCKS.register("counter", () -> new Counter(BlockBehaviour.Properties.of().strength(1f)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CounterEntity>> COUNTER_ENTITY = BLOCK_ENTITIES.register("counter",
            () -> BlockEntityType.Builder.of(CounterEntity::new, COUNTER.get()).build(null));

    public static final DeferredItem<Item> GEM = ITEMS.registerSimpleItem("gem");
    public static final DeferredItem<PickaxeItem> GEM_PICKAXE = ITEMS.register("gem_pickaxe", () -> new PickaxeItem(Tiers.IRON, new Item.Properties()));

    static {
        ITEMS.registerSimpleBlockItem(GEM_BLOCK);
        ITEMS.registerSimpleBlockItem(LAMP);
        ITEMS.registerSimpleBlockItem(COUNTER);
    }

    public FixtureMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(this::onTick);
    }

    private void onTick(ServerTickEvent.Post event) { ticks++; }

    /** Faces the player who places it. */
    public static class Lamp extends HorizontalDirectionalBlock {
        public Lamp(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    }

    /** Counts right clicks in its block entity. */
    public static class Counter extends Block implements EntityBlock {
        public Counter(Properties p) { super(p); }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CounterEntity(pos, state); }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.getBlockEntity(pos) instanceof CounterEntity c) c.clicks++;
            return InteractionResult.SUCCESS;
        }
    }

    public static class CounterEntity extends BlockEntity {
        public int clicks;

        public CounterEntity(BlockPos pos, BlockState state) { super(COUNTER_ENTITY.get(), pos, state); }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { tag.putInt("clicks", clicks); }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { clicks = tag.getInt("clicks"); }
    }
}
