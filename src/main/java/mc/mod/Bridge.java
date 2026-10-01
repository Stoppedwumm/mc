package mc.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.util.RayCast;
import mc.world.ModHooks;
import mc.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Connects mods' view of the game (reamc-compat classes) to the engine: blocks and items both ways, worlds, players,
 * block entities, menus and payloads. Also implements the engine's mod hooks.
 */
public final class Bridge {
    private Bridge() { }

    /** Running as a dedicated server (no client screens). */
    public static boolean dedicated;
    private static ModUi ui;

    public static ModUi ui() { return ui; }

    public static void setUi(ModUi u) { ui = u; }

    // ------------------------------------------------------------------ blocks and items

    private static final Block[] BLOCKS = new Block[256];
    private static final Item[] ITEMS = new Item[mc.item.Item.BY_ID.length];

    static void bind(Block compat, mc.world.Block engine) {
        compat.reamc$block = engine;
        BLOCKS[engine.id] = compat;
    }

    static void bind(Item compat, mc.item.Item engine) {
        compat.reamc$item = engine;
        ITEMS[engine.id] = compat;
    }

    public static mc.world.Block engineBlock(Block b) { return b.reamc$block; }

    public static mc.item.Item engineItem(Item i) { return i.reamc$item; }

    /** The compat block for an engine block id (built-in blocks get a stand-in on first use). */
    public static synchronized Block compatBlock(int id) {
        Block b = BLOCKS[id & 255];
        if (b == null) {
            mc.world.Block engine = mc.world.Block.get(id);
            b = new VanillaBlock(engine);
            bind(b, engine);
            ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).register(ResourceLocation.parse(engine.key() + (BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(engine.key())) ? "_" + id : "")), b);
        }
        return b;
    }

    public static synchronized Item compatItem(mc.item.Item engine) {
        Item i = ITEMS[engine.id];
        if (i == null) {
            i = engine.block != null ? new net.minecraft.world.item.BlockItem(compatBlock(engine.block.id), new Item.Properties()) : new Item(new Item.Properties());
            bind(i, engine);
            ResourceLocation id = ResourceLocation.parse(engine.key());
            if (!BuiltInRegistries.ITEM.containsKey(id)) ((MappedRegistry<Item>) BuiltInRegistries.ITEM).register(id, i);
        }
        return i;
    }

    public static Block vanillaBlock(String key) {
        if (key.equals("minecraft:air")) return compatBlock(0);
        mc.world.Block b = mc.world.Block.byKey(key);
        return b == null ? compatBlock(0) : compatBlock(b.id);
    }

    public static Item vanillaItem(String key) {
        if (key.equals("minecraft:air")) return AIR_ITEM;
        mc.item.Item i = mc.item.Item.byKey(key);
        return i == null ? AIR_ITEM : compatItem(i);
    }

    private static final Item AIR_ITEM = new Item(new Item.Properties());

    public static Item blockItem(Block b) {
        if (b.reamc$block == null) return AIR_ITEM;
        mc.item.Item i = mc.item.Item.get(b.reamc$block.id);
        return i == null ? AIR_ITEM : compatItem(i);
    }

    public static BlockState state(int id, int meta) {
        Block b = compatBlock(id);
        return b instanceof VanillaBlock ? b.defaultBlockState() : b.getStateDefinition().reamc$state(meta);
    }

    /** Stand-in compat block for a built-in reamc block. */
    static final class VanillaBlock extends Block {
        VanillaBlock(mc.world.Block engine) { super(BlockBehaviour.Properties.of().strength(engine.hardness)); }
    }

    // ------------------------------------------------------------------ worlds, players, entities

    private static final Map<World, WorldData> WORLDS = new WeakHashMap<>();

    private static final class LevelImpl extends ServerLevel {
        final World world;
        LevelImpl(World world) { this.world = world; }
        @Override public World reamc$world() { return world; }
    }

    private static final class WorldData {
        final LevelImpl level;
        final Map<Long, BlockEntity> blockEntities = new LinkedHashMap<>();
        boolean loaded;
        WorldData(World w) { level = new LevelImpl(w); }
    }

    private static synchronized WorldData data(World w) {
        WorldData d = WORLDS.computeIfAbsent(w, WorldData::new);
        if (!d.loaded) {
            d.loaded = true;
            loadBlockEntities(w, d);
        }
        return d;
    }

    public static Level level(World w) { return w == null ? null : data(w).level; }

    private static final Map<mc.entity.Player, ServerPlayer> SERVER_PLAYERS = new WeakHashMap<>();
    private static final Map<mc.entity.Player, LocalPlayer> LOCAL_PLAYERS = new WeakHashMap<>();

    public static synchronized ServerPlayer serverPlayer(mc.entity.Player p) { return SERVER_PLAYERS.computeIfAbsent(p, ServerPlayer::new); }

    public static synchronized LocalPlayer localPlayer(mc.entity.Player p) { return LOCAL_PLAYERS.computeIfAbsent(p, LocalPlayer::new); }

    public static Player localServerPlayer() { return ui == null ? null : serverPlayer(ui.localPlayer()); }

    public static Entity entity(World w, int id) {
        for (mc.entity.Player p : w.players()) if (p.id == id) return serverPlayer(p);
        return null;
    }

    public static List<Player> players(World w) {
        List<Player> out = new ArrayList<>();
        for (mc.entity.Player p : w.players()) out.add(serverPlayer(p));
        return out;
    }

    public static boolean addEntity(World w, Entity e) {
        w.addEntity(e.reamc$engine());
        return true;
    }

    private static final MinecraftServer SERVER = new MinecraftServer();

    public static MinecraftServer server() { return SERVER; }

    public static void chat(mc.entity.Player p, String message) {
        if (ui != null && p == ui.localPlayer()) ui.chat(message);
        else System.out.println("[mods] to " + p.name + ": " + message);
    }

    // ------------------------------------------------------------------ block entities

    private static long key(BlockPos p) { return World.posKey(p.getX(), p.getY(), p.getZ()); }

    public static BlockEntity blockEntity(World w, BlockPos pos) { return data(w).blockEntities.get(key(pos)); }

    public static void removeBlockEntity(World w, BlockPos pos) {
        BlockEntity be = data(w).blockEntities.remove(key(pos));
        if (be != null) be.setRemoved();
    }

    public static void putBlockEntity(World w, BlockEntity be) {
        be.setLevel(level(w));
        data(w).blockEntities.put(key(be.getBlockPos()), be);
    }

    private static Path blockEntityFile(World w) {
        return w.storage == null ? null : w.storage.dir().resolve("mod_block_entities.json");
    }

    private static void loadBlockEntities(World w, WorldData d) {
        Path f = blockEntityFile(w);
        if (f == null || !Files.exists(f)) return;
        try {
            for (var el : JsonParser.parseString(Files.readString(f)).getAsJsonArray()) {
                JsonObject o = el.getAsJsonObject();
                BlockPos pos = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(o.get("block").getAsString()));
                if (!(b instanceof EntityBlock eb)) continue;
                BlockState state = b.getStateDefinition().reamc$state(o.has("state") ? o.get("state").getAsInt() : 0);
                BlockEntity be = eb.newBlockEntity(pos, state);
                if (be == null) continue;
                be.setLevel(d.level);
                be.reamc$load(CompoundTag.fromJson(o.getAsJsonObject("data")));
                d.blockEntities.put(key(pos), be);
                be.onLoad();
            }
        } catch (Exception e) {
            System.err.println("[mods] Could not load block entities: " + e);
        }
    }

    private static void saveBlockEntities(World w) {
        Path f = blockEntityFile(w);
        WorldData d = WORLDS.get(w);
        if (f == null || d == null) return;
        JsonArray a = new JsonArray();
        for (BlockEntity be : d.blockEntities.values()) {
            try {
                JsonObject o = new JsonObject();
                BlockPos p = be.getBlockPos();
                o.addProperty("x", p.getX());
                o.addProperty("y", p.getY());
                o.addProperty("z", p.getZ());
                o.addProperty("block", String.valueOf(BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())));
                o.addProperty("state", be.getBlockState().reamc$index());
                o.add("data", be.reamc$save().toJson());
                a.add(o);
            } catch (RuntimeException e) {
                System.err.println("[mods] Could not save block entity at " + be.getBlockPos() + ": " + e);
            }
        }
        try {
            Files.writeString(f, a.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[mods] Could not save block entities: " + e);
        }
    }

    // ------------------------------------------------------------------ menus and screens

    private static final Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> SCREENS = new HashMap<>();
    private static int nextContainerId = 1;

    public static void registerScreen(MenuType<?> type, MenuScreens.ScreenConstructor<?, ?> ctor) { SCREENS.put(type, ctor); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static OptionalInt openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        if (player.containerMenu != null) closeMenu(player);
        int id = nextContainerId = nextContainerId % 100 + 1;
        AbstractContainerMenu menu = provider.createMenu(id, player.getInventory(), player);
        if (menu == null) return OptionalInt.empty();
        player.containerMenu = menu;
        mc.entity.Player engine = player.reamc$player();
        if (ui != null && engine == ui.localPlayer()) {
            // Singleplayer: client and server share this process, so the screen shows the server's menu directly
            LocalPlayer local = localPlayer(engine);
            local.containerMenu = menu;
            Minecraft mc = Minecraft.getInstance();
            mc.player = local;
            MenuScreens.ScreenConstructor ctor = SCREENS.get(menu.getType());
            Screen screen = ctor != null ? ctor.create(menu, local.getInventory(), provider.getDisplayName())
                    : new DefaultContainerScreen(menu, local.getInventory(), provider.getDisplayName());
            screen.init(mc, (int) ui.width(), (int) ui.height());
            mc.screen = screen;
            ui.openScreen(screen);
        }
        return OptionalInt.of(id);
    }

    /** Closes the menu a player has open (called when its screen closes). */
    public static void closeMenu(Player p) {
        mc.entity.Player engine = p.reamc$player();
        ServerPlayer sp = serverPlayer(engine);
        AbstractContainerMenu menu = sp.containerMenu;
        sp.containerMenu = null;
        localPlayer(engine).containerMenu = null;
        if (menu != null) {
            try {
                menu.removed(sp);
            } catch (RuntimeException e) {
                System.err.println("[mods] Closing a menu failed: " + e);
            }
        }
        if (ui != null && engine == ui.localPlayer()) {
            Minecraft.getInstance().screen = null;
            ui.closeScreen();
        }
    }

    public static void closeScreen() {
        if (ui != null) closeMenu(localPlayer(ui.localPlayer()));
    }

    // ------------------------------------------------------------------ payloads

    private static final Map<ResourceLocation, IPayloadHandler<?>> PAYLOADS = new HashMap<>();

    public static void registerPayload(CustomPacketPayload.Type<?> type, IPayloadHandler<?> handler) { PAYLOADS.put(type.id(), handler); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void deliver(Player player, CustomPacketPayload payload, PacketFlow flow) {
        IPayloadHandler handler = PAYLOADS.get(payload.type().id());
        if (handler == null || player == null) return;
        IPayloadContext ctx = new IPayloadContext() {
            @Override public Player player() { return flow == PacketFlow.CLIENTBOUND ? localPlayer(player.reamc$player()) : player; }
            @Override public PacketFlow flow() { return flow; }
            @Override public net.minecraft.network.Connection connection() { return new net.minecraft.network.Connection(); }
            @Override public CompletableFuture<Void> enqueueWork(Runnable work) {
                try {
                    work.run();
                    return CompletableFuture.completedFuture(null);
                } catch (RuntimeException e) {
                    return CompletableFuture.failedFuture(e);
                }
            }
        };
        handler.handle(payload, ctx);
    }

    // ------------------------------------------------------------------ the engine's hooks

    public static final ModHooks HOOKS = new ModHooks() {
        @Override
        public void blockChanged(World w, int x, int y, int z, int oldId, int oldMeta, int newId, int newMeta) {
            WorldData d = data(w);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState was = state(oldId, oldMeta), now = state(newId, newMeta);
            try {
                if (mc.world.Block.get(oldId).modded) was.getBlock().reamc$onRemove(was, d.level, pos, now);
                BlockEntity be = d.blockEntities.get(key(pos));
                if (be != null && (!now.hasBlockEntity() || was.getBlock() != now.getBlock())) removeBlockEntity(w, pos);
                else if (be != null) be.setBlockState(now);
                if (now.hasBlockEntity() && !d.blockEntities.containsKey(key(pos))) {
                    BlockEntity created = ((EntityBlock) now.getBlock()).newBlockEntity(pos, now);
                    if (created != null) {
                        putBlockEntity(w, created);
                        created.onLoad();
                    }
                }
                if (mc.world.Block.get(newId).modded) now.getBlock().reamc$onPlace(now, d.level, pos, was);
            } catch (RuntimeException | LinkageError e) {
                report("block change at " + pos, e);
            }
        }

        @Override
        public boolean useBlock(World w, mc.entity.Player p, int x, int y, int z, RayCast.Hit hit) {
            WorldData d = data(w);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState s = state(w.getBlock(x, y, z), w.getMeta(x, y, z));
            ServerPlayer sp = serverPlayer(p);
            BlockHitResult h = new BlockHitResult(hit.px, hit.py, hit.pz, direction(hit), pos);
            try {
                ItemStack held = ItemStack.reamc$wrap(p.inventory.held());
                if (!held.isEmpty()) {
                    ItemInteractionResult r = s.getBlock().reamc$useItemOn(held, s, d.level, pos, sp, h);
                    if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r.consumesAction();
                }
                InteractionResult r = s.getBlock().reamc$use(s, d.level, pos, sp, h);
                return r.consumesAction();
            } catch (RuntimeException | LinkageError e) {
                report("using " + s, e);
                return false;
            }
        }

        @Override
        public int placementMeta(World w, mc.world.Block b, mc.entity.Player p, int x, int y, int z, RayCast.Hit hit) {
            Block block = compatBlock(b.id);
            try {
                BlockPlaceContext ctx = new BlockPlaceContext(level(w), serverPlayer(p), new BlockPos(x, y, z), direction(hit), ItemStack.reamc$wrap(p.inventory.held()));
                BlockState s = block.getStateForPlacement(ctx);
                return (s == null ? block.defaultBlockState() : s).reamc$index();
            } catch (RuntimeException | LinkageError e) {
                report("placing " + block, e);
                return block.defaultBlockState().reamc$index();
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void worldTicked(World w) {
            WorldData d = data(w);
            SERVER.reamc$tick();
            NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(SERVER));
            for (BlockEntity be : new ArrayList<>(d.blockEntities.values())) {
                if (!(be.getBlockState().getBlock() instanceof EntityBlock eb)) continue;
                try {
                    BlockEntityTicker ticker = eb.getTicker(d.level, be.getBlockState(), be.getType());
                    if (ticker != null) ticker.tick(d.level, be.getBlockPos(), be.getBlockState(), be);
                } catch (RuntimeException | LinkageError e) {
                    report("ticking " + be.getClass().getSimpleName(), e);
                }
            }
            NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(SERVER));
        }

        @Override
        public void worldSaved(World w) { saveBlockEntities(w); }
    };

    static Direction direction(RayCast.Hit hit) {
        if (hit == null) return Direction.UP;
        if (hit.ny > 0) return Direction.UP;
        if (hit.ny < 0) return Direction.DOWN;
        if (hit.nz < 0) return Direction.NORTH;
        if (hit.nz > 0) return Direction.SOUTH;
        if (hit.nx < 0) return Direction.WEST;
        if (hit.nx > 0) return Direction.EAST;
        return Direction.UP;
    }

    private static void report(String what, Throwable e) {
        System.err.println("[mods] Error in " + what + ": " + e);
        e.printStackTrace();
    }
}
