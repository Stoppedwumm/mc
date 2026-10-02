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

    // ------------------------------------------------------------------ registries

    /** Data-driven registries (damage types, biomes, features...): created empty when first asked for, filled from data. */
    private static final Map<ResourceLocation, MappedRegistry<?>> DYNAMIC = new java.util.concurrent.ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> MappedRegistry<T> dynamicRegistry(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> key) {
        net.minecraft.core.Registry<?> builtIn = BuiltInRegistries.REGISTRY.get(key.location());
        if (builtIn != null) return (MappedRegistry<T>) builtIn;
        return (MappedRegistry<T>) DYNAMIC.computeIfAbsent(key.location(), k -> new MappedRegistry<>((net.minecraft.resources.ResourceKey) key));
    }

    private static final net.minecraft.core.RegistryAccess.Frozen ACCESS = new net.minecraft.core.RegistryAccess.Frozen() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <E> java.util.Optional<net.minecraft.core.Registry<E>> registry(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends E>> key) {
            return java.util.Optional.of((net.minecraft.core.Registry<E>) dynamicRegistry((net.minecraft.resources.ResourceKey) key));
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public java.util.stream.Stream<RegistryEntry<?>> registries() {
            java.util.List<RegistryEntry<?>> l = new java.util.ArrayList<>();
            for (net.minecraft.core.Registry<?> r : BuiltInRegistries.REGISTRY) l.add(new RegistryEntry(r.key(), r));
            for (MappedRegistry<?> r : DYNAMIC.values()) l.add(new RegistryEntry(r.key(), r));
            return l.stream();
        }
    };

    private static boolean dataRegistriesReady;

    /** Fills the data-driven registries mods look things up in: damage types and reamc's enchantments. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void initDataRegistries() {
        if (dataRegistriesReady) return;
        dataRegistriesReady = true;
        MappedRegistry damage = dynamicRegistry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        for (java.lang.reflect.Field f : net.minecraft.world.damagesource.DamageTypes.class.getFields()) {
            try {
                net.minecraft.resources.ResourceKey<?> k = (net.minecraft.resources.ResourceKey<?>) f.get(null);
                String path = k.location().getPath();
                String msg = switch (path) { case "player_attack" -> "player"; case "mob_attack", "mob_attack_no_aggro" -> "mob"; case "out_of_world" -> "outOfWorld"; default -> path; };
                if (!damage.containsKey(k.location())) damage.register(k.location(), new net.minecraft.world.damagesource.DamageType(msg, 0.1f));
            } catch (ReflectiveOperationException ignored) { }
        }
        for (mc.world.gen.Biome b : mc.world.gen.Biome.values()) biome(b);
        MappedRegistry ench = dynamicRegistry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.ENCHANTMENT);
        for (mc.item.Enchantment e : mc.item.Enchantment.values()) {
            ResourceLocation id = ResourceLocation.withDefaultNamespace(e.name().toLowerCase(java.util.Locale.ROOT));
            if (!ench.containsKey(id)) ench.register(id, new net.minecraft.world.item.enchantment.Enchantment(net.minecraft.network.chat.Component.literal(e.displayName), e));
        }
    }

    private static final Map<mc.world.gen.Biome, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> BIOMES = new java.util.EnumMap<>(mc.world.gen.Biome.class);

    /** The biome holder for a reamc biome, registered under Minecraft's id. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome(mc.world.gen.Biome b) {
        return BIOMES.computeIfAbsent(b, k -> {
            String id; float t, d; boolean rain = true;
            switch (k) {
                case OCEAN -> { id = "ocean"; t = 0.5f; d = 0.5f; }
                case FROZEN_OCEAN -> { id = "frozen_ocean"; t = 0; d = 0.5f; }
                case BEACH -> { id = "beach"; t = 0.8f; d = 0.4f; }
                case PLAINS -> { id = "plains"; t = 0.8f; d = 0.4f; }
                case FOREST -> { id = "forest"; t = 0.7f; d = 0.8f; }
                case BIRCH_FOREST -> { id = "birch_forest"; t = 0.6f; d = 0.6f; }
                case TAIGA -> { id = "taiga"; t = 0.25f; d = 0.8f; }
                case SNOWY_TAIGA -> { id = "snowy_taiga"; t = -0.5f; d = 0.4f; }
                case DESERT -> { id = "desert"; t = 2; d = 0; rain = false; }
                case MOUNTAINS -> { id = "windswept_hills"; t = 0.2f; d = 0.3f; }
                case SNOWY_PEAKS -> { id = "snowy_slopes"; t = -0.3f; d = 0.9f; }
                case RIVER -> { id = "river"; t = 0.5f; d = 0.5f; }
                case BADLANDS -> { id = "badlands"; t = 2; d = 0; rain = false; }
                default -> { id = "nether_wastes"; t = 2; d = 0; rain = false; }
            }
            MappedRegistry reg = dynamicRegistry((net.minecraft.resources.ResourceKey) net.minecraft.core.registries.Registries.BIOME);
            ResourceLocation rl = ResourceLocation.withDefaultNamespace(id);
            if (reg.containsKey(rl)) return (net.minecraft.core.Holder) reg.getHolder(rl).orElseThrow();
            return reg.register(rl, new net.minecraft.world.level.biome.Biome(k, t, d, rain));
        });
    }

    /** The engine enchantment behind an enchantment holder, or null. */
    public static mc.item.Enchantment engineEnchantment(net.minecraft.core.Holder<?> h) {
        return h != null && h.value() instanceof net.minecraft.world.item.enchantment.Enchantment e ? e.reamc$engine() : null;
    }

    /** Every registry, built-in and data-driven. */
    public static net.minecraft.core.RegistryAccess.Frozen registryAccess() { return ACCESS; }

    // ------------------------------------------------------------------ blocks and items

    private static final Block[] BLOCKS = new Block[mc.world.Block.MAX];
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
        Block b = BLOCKS[id];
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

    /** Minecraft blocks reamc doesn't have: registered placeholders, so lookups by them never match anything else. */
    private static final Map<String, Block> MISSING_BLOCKS = new java.util.HashMap<>();
    private static final Map<String, Item> MISSING_ITEMS = new java.util.HashMap<>();

    public static synchronized Block vanillaBlock(String key) {
        if (key.equals("minecraft:air")) return compatBlock(0);
        mc.world.Block b = mc.world.Block.byKey(key);
        if (b != null) return compatBlock(b.id);
        return MISSING_BLOCKS.computeIfAbsent(key, k -> {
            Block m = new MissingBlock();
            ResourceLocation id = ResourceLocation.parse(k);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).register(id, m);
            return m;
        });
    }

    public static synchronized Item vanillaItem(String key) {
        if (key.equals("minecraft:air")) return airItem();
        mc.item.Item i = mc.item.Item.byKey(key);
        if (i != null) return compatItem(i);
        return MISSING_ITEMS.computeIfAbsent(key, k -> {
            Item m = new Item(new Item.Properties());
            ResourceLocation id = ResourceLocation.parse(k);
            if (!BuiltInRegistries.ITEM.containsKey(id)) ((MappedRegistry<Item>) BuiltInRegistries.ITEM).register(id, m);
            return m;
        });
    }

    /** A Minecraft block reamc lacks: it exists for comparisons but is never in a world. */
    static final class MissingBlock extends Block {
        MissingBlock() { super(BlockBehaviour.Properties.of()); }
    }

    private static Item AIR_ITEM_VALUE;
    private static Item airItem() {
        if (AIR_ITEM_VALUE == null) {
            AIR_ITEM_VALUE = new Item(new Item.Properties());
            ResourceLocation id = ResourceLocation.withDefaultNamespace("air");
            if (!BuiltInRegistries.ITEM.containsKey(id)) ((MappedRegistry<Item>) BuiltInRegistries.ITEM).register(id, AIR_ITEM_VALUE);
        }
        return AIR_ITEM_VALUE;
    }

    /** Every recipe loaded from mods' data. */
    public static final net.minecraft.world.item.crafting.RecipeManager RECIPES = new net.minecraft.world.item.crafting.RecipeManager();

    public static net.minecraft.world.item.crafting.RecipeManager recipeManager() { return RECIPES; }

    /** Tells the block at {@code pos} that its neighbour at {@code from} changed. */
    public static void neighborChanged(Level level, BlockPos pos, Block block, BlockPos from) {
        BlockState s = level.getBlockState(pos);
        if (!s.getBlock().reamc$isModded()) return;
        try {
            s.handleNeighborChanged(level, pos, block, from, false);
        } catch (RuntimeException | LinkageError e) {
            report("neighbour update of " + s, e);
        }
    }

    /** A block entity's data changed: it is saved with the world and, if a screen shows it, redrawn. */
    public static void blockEntityChanged(World w, BlockPos pos) { }

    /** Fire odds set by FireBlock.setFlammable, else 5/20 for blocks reamc burns. */
    public static final Map<Block, int[]> FLAMMABLE = new java.util.concurrent.ConcurrentHashMap<>();

    public static int flammability(Block b) {
        int[] f = FLAMMABLE.get(b);
        return f != null ? f[1] : b.reamc$block != null && b.reamc$block.flammable ? 20 : 0;
    }

    public static int fireSpreadSpeed(Block b) {
        int[] f = FLAMMABLE.get(b);
        return f != null ? f[0] : b.reamc$block != null && b.reamc$block.flammable ? 5 : 0;
    }

    public static Item blockItem(Block b) {
        if (b.reamc$block == null) return airItem();
        mc.item.Item i = mc.item.Item.get(b.reamc$block.id);
        return i == null ? airItem() : compatItem(i);
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
        for (mc.entity.Entity e : w.entities()) if (e.id == id) return wrap(e);
        for (mc.entity.Player p : w.players()) if (p.id == id) return serverPlayer(p);
        return null;
    }

    /** Compat views of engine entities (mods' own entities register themselves). */
    private static final Map<mc.entity.Entity, Entity> WRAPPERS = new WeakHashMap<>();

    public static synchronized void registerWrapper(mc.entity.Entity engine, Entity compat) { WRAPPERS.put(engine, compat); }

    public static synchronized Entity wrap(mc.entity.Entity e) {
        if (e == null) return null;
        if (e instanceof mc.entity.Player p) return serverPlayer(p);
        if (e instanceof ModEntityProxy m) return m.mod;
        Entity w = WRAPPERS.get(e);
        if (w != null) return w;
        if (e instanceof mc.entity.ItemEntity i) return new net.minecraft.world.entity.item.ItemEntity(i);
        if (e instanceof mc.entity.XpOrbEntity o) w = new net.minecraft.world.entity.ExperienceOrb(o);
        else if (e instanceof mc.entity.Mob m) w = switch (m.type) {
            case WOLF -> new net.minecraft.world.entity.animal.Wolf(m);
            case VILLAGER -> new net.minecraft.world.entity.npc.Villager(m);
            default -> new GenericMob(m);
        };
        else if (e instanceof mc.entity.LivingEntity l) w = new GenericLiving(l);
        else w = new GenericEntity(e);
        WRAPPERS.put(e, w);
        return w;
    }

    private static final class GenericEntity extends Entity { GenericEntity(mc.entity.Entity e) { super(e); } }
    private static final class GenericLiving extends net.minecraft.world.entity.LivingEntity { GenericLiving(mc.entity.LivingEntity e) { super(e); } }
    private static final class GenericMob extends net.minecraft.world.entity.Mob { GenericMob(mc.entity.Mob e) { super(e); } }

    // ------------------------------------------------------------------ effects and cooldowns

    /** Effects reamc doesn't have natively, per entity; ticked with the world. */
    private static final Map<mc.entity.LivingEntity, Map<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffectInstance>> MOD_EFFECTS = new WeakHashMap<>();

    public static synchronized Map<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffectInstance> modEffects(mc.entity.LivingEntity e) {
        return MOD_EFFECTS.computeIfAbsent(e, k -> new LinkedHashMap<>());
    }

    /** Runs the mod effects of every living entity in a world for one tick. */
    static void tickModEffects(World w) {
        List<Map.Entry<mc.entity.LivingEntity, Map<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffectInstance>>> all;
        synchronized (Bridge.class) { all = new ArrayList<>(MOD_EFFECTS.entrySet()); }
        for (var e : all) {
            if (e.getKey().world != w || e.getValue().isEmpty()) continue;
            if (e.getKey().isDead() || e.getKey().removed) { e.getValue().clear(); continue; }
            net.minecraft.world.entity.LivingEntity living = (net.minecraft.world.entity.LivingEntity) wrap(e.getKey());
            e.getValue().values().removeIf(i -> {
                try {
                    return !i.tick(living, () -> { });
                } catch (RuntimeException ex) {
                    System.err.println("[mods] effect " + i + " failed: " + ex);
                    return true;
                }
            });
        }
    }

    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectHolder(mc.entity.Effect e) {
        for (var h : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
            if (h.value() instanceof net.minecraft.world.effect.MobEffect m && m.reamc$engine == e) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> r = (net.minecraft.core.Holder) h;
                return r;
            }
        }
        return null;
    }

    private static final Map<mc.entity.Player, net.minecraft.world.item.ItemCooldowns> COOLDOWNS = new WeakHashMap<>();

    public static synchronized net.minecraft.world.item.ItemCooldowns cooldowns(Player p) {
        return COOLDOWNS.computeIfAbsent(p.reamc$player(), k -> new net.minecraft.world.item.ItemCooldowns());
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
            tickModEffects(w);
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
