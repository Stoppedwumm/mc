package mc.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.item.Recipes;
import mc.render.ItemTextureGen;
import mc.render.TextureGen;
import mc.world.gen.TerrainGenerator;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads NeoForge mods from a folder: checks each jar, runs its code against reamc-compat, fires the registration
 * and setup events, and turns its assets and data (textures, models, names, recipes, drops, tags, ores) into
 * reamc's tables. Must run before the renderer builds its texture atlases.
 */
public final class ModLoader {
    private ModLoader() { }

    /** A mod that loaded (or was refused, with the reason). */
    public record LoadedMod(String id, String version, String name, Path file, boolean loaded, String problem) { }

    public static final List<LoadedMod> MODS = new ArrayList<>();
    private static final List<ZipFile> JARS = new ArrayList<>();
    private static final Map<String, ModEventBus> BUSES = new LinkedHashMap<>();
    /** Each loaded mod's class loader, by mod id. */
    public static final Map<String, ClassLoader> LOADERS = new LinkedHashMap<>();
    private static boolean done;
    private static ModIds ids = new ModIds(null);

    /** Display name of a loaded mod by id (the id itself if unknown). */
    public static String displayName(String modId) {
        for (LoadedMod m : MODS) if (m.id.equals(modId)) return m.name;
        return modId;
    }

    /** Bytes of a resource ("assets/<ns>/...") from any loaded mod jar, or null. */
    public static byte[] resource(String path) {
        for (ZipFile z : JARS) {
            ZipEntry e = z.getEntry(path);
            if (e == null) continue;
            try (InputStream in = z.getInputStream(e)) {
                return in.readAllBytes();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static synchronized List<LoadedMod> loadAll(Path dir, Dist dist) {
        if (done) return MODS;
        done = true;
        Bridge.dedicated = dist == Dist.DEDICATED_SERVER;
        if (dir == null || !Files.isDirectory(dir)) return MODS;
        List<Path> jars;
        try (Stream<Path> s = Files.list(dir)) {
            jars = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        } catch (Exception e) {
            return MODS;
        }
        if (jars.isEmpty()) return MODS;
        ids = new ModIds(dir);
        List<ModContainer> containers = new ArrayList<>();
        for (Path jar : jars) {
            try {
                ModContainer c = construct(jar, dist);
                if (c != null) containers.add(c);
            } catch (Throwable t) {
                System.err.println("[mods] " + jar.getFileName() + " failed to load: " + t);
                t.printStackTrace();
                MODS.add(new LoadedMod("?", "?", jar.getFileName().toString(), jar, false, t.toString()));
            }
        }
        if (containers.isEmpty()) return MODS;
        // Registration, registry by registry, then the engine gets its blocks and items
        for (Registry<?> r : BuiltInRegistries.ORDER) {
            post(new RegisterEvent(r));
            if (r == BuiltInRegistries.BLOCK) bindBlocks();
            if (r == BuiltInRegistries.ITEM) bindItems();
        }
        ids.save();
        loadData();
        post(new FMLCommonSetupEvent());
        post(new RegisterPayloadHandlersEvent());
        post(new RegisterCapabilitiesEvent());
        if (dist == Dist.CLIENT) {
            post(new FMLClientSetupEvent());
            post(new RegisterMenuScreensEvent());
        } else post(new FMLDedicatedServerSetupEvent());
        mc.world.World.modHooks = Bridge.HOOKS;
        for (LoadedMod m : MODS) if (m.loaded) System.out.println("[mods] Loaded " + m.name + " " + m.version + " (" + m.id + ")");
        return MODS;
    }

    /** Posts a load-time event to every mod's bus (and the game bus, for events that aren't mod-bus events). */
    private static void post(Event e) {
        for (ModEventBus b : BUSES.values()) b.post(e);
        if (!(e instanceof IModBusEvent)) NeoForge.EVENT_BUS.post(e);
    }

    // ------------------------------------------------------------------ construction

    private static ModContainer construct(Path jarPath, Dist dist) throws Exception {
        ZipFile jar = new ZipFile(jarPath.toFile());
        Map<String, String> toml = readModsToml(jar);
        String id = toml.getOrDefault("modId", jarPath.getFileName().toString());
        String version = toml.getOrDefault("version", "?"), name = toml.getOrDefault("displayName", id);
        ModAnalyzer report = ModAnalyzer.analyze(jar, ModLoader.class.getClassLoader());
        boolean force = Boolean.getBoolean("reamc.forceMods");
        if (report.modClasses.isEmpty()) {
            MODS.add(new LoadedMod(id, version, name, jarPath, false, "no @Mod class (not a NeoForge mod?)"));
            System.err.println("[mods] " + jarPath.getFileName() + ": no @Mod class");
            return null;
        }
        if (!report.missing.isEmpty() && !force) {
            String why = report.missing.size() + " unsupported references";
            System.err.println("[mods] Not loading " + name + ": " + why + " (start with -Dreamc.forceMods=true to try anyway)");
            for (String m : report.missing) System.err.println("[mods]   missing " + m + "   (used by " + report.missingFrom.get(m) + ")");
            MODS.add(new LoadedMod(id, version, name, jarPath, false, why));
            return null;
        }
        if (report.usesMixins) System.err.println("[mods] " + name + ": mixins are not applied (the features they patch in are missing)");
        JARS.add(jar);
        loadLang(jar);
        URLClassLoader loader = new URLClassLoader(id, new URL[]{jarPath.toUri().toURL()}, ModLoader.class.getClassLoader());
        LOADERS.put(id, loader);
        ModEventBus bus = new ModEventBus(id);
        BUSES.put(id, bus);
        ModContainer container = new ModContainer(id, version, name, bus);
        for (ModAnalyzer.Subscriber s : report.subscribers) {
            if (dist == Dist.CLIENT ? !s.client() : !s.server()) continue;
            Class<?> c = Class.forName(s.className(), true, loader);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(c, MethodHandles.lookup());
            for (ModAnalyzer.Listener l : s.listeners()) {
                Class<?> event;
                try {
                    event = Class.forName(l.eventClass(), false, loader);
                } catch (ClassNotFoundException | LinkageError e) {
                    continue;
                }
                MethodHandle h = lookup.findStatic(c, l.name(), MethodType.methodType(void.class, event));
                ModEventBus target = IModBusEvent.class.isAssignableFrom(event) ? bus : (ModEventBus) NeoForge.EVENT_BUS;
                target.registerHandle(event, EventPriority.valueOf(l.priority()), l.receiveCanceled(), h, c);
            }
        }
        for (String main : report.modClasses) {
            Class<?> c = Class.forName(main, true, loader);
            Constructor<?> best = null;
            for (Constructor<?> k : c.getDeclaredConstructors()) if (best == null || k.getParameterCount() > best.getParameterCount()) best = k;
            Object[] args = new Object[best.getParameterCount()];
            Class<?>[] types = best.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                if (types[i] == IEventBus.class) args[i] = bus;
                else if (types[i] == ModContainer.class) args[i] = container;
                else if (types[i] == Dist.class) args[i] = dist;
                else throw new IllegalStateException("Unsupported @Mod constructor parameter " + types[i].getName());
            }
            best.setAccessible(true);
            best.newInstance(args);
        }
        MODS.add(new LoadedMod(id, version, name, jarPath, true, null));
        return container;
    }

    /** The first [[mods]] entry of META-INF/neoforge.mods.toml (simple key = "value" lines). */
    private static Map<String, String> readModsToml(ZipFile jar) throws Exception {
        Map<String, String> out = new HashMap<>();
        ZipEntry e = jar.getEntry("META-INF/neoforge.mods.toml");
        if (e == null) e = jar.getEntry("META-INF/mods.toml");
        if (e == null) return out;
        String text;
        try (InputStream in = jar.getInputStream(e)) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean inMods = false;
        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.startsWith("[[")) {
                if (inMods && !out.isEmpty()) break;
                inMods = l.equals("[[mods]]");
                continue;
            }
            if (!inMods || l.startsWith("#") || !l.contains("=")) continue;
            String k = l.substring(0, l.indexOf('=')).trim(), v = l.substring(l.indexOf('=') + 1).trim();
            if (v.startsWith("\"") && v.lastIndexOf('"') > 0) v = v.substring(1, v.lastIndexOf('"'));
            out.putIfAbsent(k, v);
        }
        return out;
    }

    private static JsonObject json(String path) {
        byte[] b = resource(path);
        if (b == null) return null;
        try {
            return JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            System.err.println("[mods] Bad JSON in " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static void loadLang(ZipFile jar) {
        var en = jar.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (!e.getName().matches("assets/[^/]+/lang/en_us\\.json")) continue;
            try (InputStream in = jar.getInputStream(e)) {
                JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                Map<String, String> m = new HashMap<>();
                for (var kv : o.entrySet()) m.put(kv.getKey(), kv.getValue().getAsString());
                Lang.putAll(m);
            } catch (Exception ex) {
                System.err.println("[mods] Bad language file " + e.getName());
            }
        }
    }

    // ------------------------------------------------------------------ textures

    /** A 16 x 16 ARGB tile from a texture id like "jans_mod:block/rubyore" (first frame of animations, scaled). */
    static int[] tile(String textureId) {
        ResourceLocation rl = ResourceLocation.parse(textureId);
        BufferedImage img = image("assets/" + rl.getNamespace() + "/textures/" + rl.getPath() + ".png");
        int[] out = new int[256];
        if (img == null) {
            // Missing texture: Minecraft's magenta and black checkerboard
            for (int i = 0; i < 256; i++) out[i] = ((i & 15) < 8) == ((i >> 4) < 8) ? 0xFFF800F8 : 0xFF000000;
            return out;
        }
        int size = Math.min(img.getWidth(), img.getHeight());
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) out[y * 16 + x] = img.getRGB(x * size / 16, y * size / 16);
        return out;
    }

    static BufferedImage image(String path) {
        byte[] b = resource(path);
        if (b == null) return null;
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(b));
        } catch (Exception e) {
            return null;
        }
    }

    /** Average colour of a texture's opaque pixels (armor colour). */
    static int averageColor(String path) {
        BufferedImage img = image(path);
        if (img == null) return 0x8a8a8a;
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                if ((c >>> 24) < 128) continue;
                r += c >> 16 & 255; g += c >> 8 & 255; b += c & 255; n++;
            }
        return n == 0 ? 0x8a8a8a : (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    private static final Map<String, Integer> BLOCK_TILES = new HashMap<>(), ITEM_TILES = new HashMap<>();

    private static int blockTile(String tex) { return BLOCK_TILES.computeIfAbsent(tex, t -> TextureGen.addTile(tile(t))); }

    private static int itemTile(String tex) { return ITEM_TILES.computeIfAbsent(tex, t -> ItemTextureGen.addTile(tile(t))); }

    // ------------------------------------------------------------------ models

    /** A block model with its textures resolved through its parents, and the built-in parent it ends at. */
    private record Model(String base, Map<String, String> textures) {
        String tex(String key) {
            String v = textures.get(key);
            for (int guard = 0; guard < 8 && v != null && v.startsWith("#"); guard++) v = textures.get(v.substring(1));
            return v == null || v.startsWith("#") ? null : v.contains(":") ? v : "minecraft:" + v;
        }
    }

    private static Model model(String id) {
        Map<String, String> textures = new HashMap<>();
        String base = "block/cube_all";
        for (int guard = 0; guard < 8 && id != null; guard++) {
            ResourceLocation rl = ResourceLocation.parse(id);
            JsonObject o = json("assets/" + rl.getNamespace() + "/models/" + rl.getPath() + ".json");
            if (o == null) { base = rl.getPath(); break; }
            if (o.has("textures")) for (var e : o.getAsJsonObject("textures").entrySet()) textures.putIfAbsent(e.getKey(), e.getValue().getAsString());
            if (!o.has("parent")) break;
            String parent = o.get("parent").getAsString();
            ResourceLocation p = ResourceLocation.parse(parent);
            if (p.getNamespace().equals("minecraft")) { base = p.getPath(); break; }
            id = parent;
        }
        return new Model(base, textures);
    }

    /** Textures of the six faces (up, down, north, south, west, east) of a block model. */
    private static String[] faces(Model m) {
        String all = first(m, "all", "side", "texture", "particle", "cross", "layer0");
        String side = first(m, "side", "all", "particle");
        String top = first(m, "top", "end", "up", "all", "particle");
        String bottom = first(m, "bottom", "end", "down", "top", "all", "particle");
        return switch (m.base) {
            case "block/cube" -> new String[]{first(m, "up", "particle"), first(m, "down", "particle"), first(m, "north", "particle"),
                    first(m, "south", "particle"), first(m, "west", "particle"), first(m, "east", "particle")};
            case "block/cube_column", "block/cube_column_horizontal", "block/cube_bottom_top" -> new String[]{top, bottom, side, side, side, side};
            case "block/orientable", "block/orientable_with_bottom" ->
                    new String[]{top, first(m, "bottom", "top", "particle"), first(m, "front", "side"), side, side, side};
            default -> new String[]{all, all, all, all, all, all};
        };
    }

    private static String first(Model m, String... keys) {
        for (String k : keys) {
            String t = m.tex(k);
            if (t != null) return t;
        }
        for (String k : m.textures.keySet()) {
            String t = m.tex(k);
            if (t != null) return t;
        }
        return "minecraft:missing";
    }

    // Face index remaps for blockstate rotations: model face -> world face
    private static final int[] ROT_X = {2, 3, 1, 0, 4, 5}, ROT_Y = {0, 1, 5, 4, 2, 3};

    // ------------------------------------------------------------------ binding to the engine

    private static String title(String path) {
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("_")) if (!w.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return sb.toString();
    }

    private static final Map<Block, String> CROSS = new HashMap<>();

    private static void bindBlocks() {
        for (var e : ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).entries().entrySet()) {
            ResourceLocation id = e.getKey();
            Block b = e.getValue();
            if (b.reamc$block != null || b instanceof Bridge.VanillaBlock) continue;
            int engineId = ids.block(id.toString());
            if (engineId < 0) {
                System.err.println("[mods] No free block id for " + id);
                continue;
            }
            String langKey = "block." + id.getNamespace() + "." + id.getPath();
            String name = Lang.has(langKey) ? Lang.get(langKey) : title(id.getPath());
            List<BlockState> states = b.getStateDefinition().getPossibleStates();
            int[][] faceTex = new int[states.size()][];
            boolean cross = false;
            JsonObject bs = json("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json");
            for (int s = 0; s < states.size(); s++) {
                JsonObject variant = variant(bs, states.get(s));
                String modelId = variant != null && variant.has("model") ? variant.get("model").getAsString() : id.getNamespace() + ":block/" + id.getPath();
                Model m = model(modelId);
                if (m.base.contains("cross")) cross = true;
                String[] tex = faces(m);
                int rx = variant != null && variant.has("x") ? variant.get("x").getAsInt() / 90 & 3 : 0;
                int ry = variant != null && variant.has("y") ? variant.get("y").getAsInt() / 90 & 3 : 0;
                int[] world = new int[6];
                for (int f = 0; f < 6; f++) {
                    int to = f;
                    for (int k = 0; k < rx; k++) to = ROT_X[to];
                    for (int k = 0; k < ry; k++) to = ROT_Y[to];
                    world[to] = blockTile(tex[f]);
                }
                faceTex[s] = world;
            }
            mc.world.Block eb = mc.world.Block.register(engineId, name, id.toString(), faceTex[0][2]);
            eb.stateFaceTex = faceTex;
            eb.texTop = faceTex[0][0];
            eb.texBottom = faceTex[0][1];
            eb.texSide = faceTex[0][2];
            var p = b.reamc$properties();
            eb.hardness = p.reamc$destroyTime();
            eb.sound = p.reamc$sound().reamc$sound();
            eb.lightEmission = Math.max(0, Math.min(15, p.reamc$light(b.defaultBlockState())));
            eb.replaceable = p.reamc$replaceable();
            if (p.reamc$noOcclusion()) { eb.opaque = false; eb.layer = mc.world.Block.Layer.CUTOUT; }
            if (p.reamc$noCollision()) eb.solid = false;
            if (cross) {
                eb.model = mc.world.Block.Model.CROSS;
                eb.opaque = false;
                eb.solid = false;
                eb.layer = mc.world.Block.Layer.CUTOUT;
                CROSS.put(b, id.toString());
            }
            Bridge.bind(b, eb);
        }
    }

    /** The blockstate variant matching a state ("facing=east,lit=true"), or the only one. */
    private static JsonObject variant(JsonObject blockstate, BlockState state) {
        if (blockstate == null) return null;
        if (blockstate.has("variants")) {
            for (var e : blockstate.getAsJsonObject("variants").entrySet()) {
                if (matches(e.getKey(), state)) {
                    JsonElement v = e.getValue();
                    return v.isJsonArray() ? v.getAsJsonArray().get(0).getAsJsonObject() : v.getAsJsonObject();
                }
            }
        }
        if (blockstate.has("multipart")) {
            JsonArray parts = blockstate.getAsJsonArray("multipart");
            if (!parts.isEmpty()) {
                JsonElement apply = parts.get(0).getAsJsonObject().get("apply");
                return apply.isJsonArray() ? apply.getAsJsonArray().get(0).getAsJsonObject() : apply.getAsJsonObject();
            }
        }
        return null;
    }

    private static boolean matches(String key, BlockState state) {
        if (key.isEmpty()) return true;
        for (String pair : key.split(",")) {
            String[] kv = pair.split("=");
            if (kv.length != 2) continue;
            Property<?> p = state.getBlock().getStateDefinition().getProperty(kv[0]);
            if (p == null || !valueName(p, state).equals(kv[1])) return false;
        }
        return true;
    }

    private static <T extends Comparable<T>> String valueName(Property<T> p, BlockState s) { return p.getName(s.getValue(p)); }

    private static int armorMaterials = 5;
    private static final Map<ArmorMaterial, Integer> ARMOR_INDEX = new HashMap<>();

    private static void bindItems() {
        for (var e : ((MappedRegistry<Item>) BuiltInRegistries.ITEM).entries().entrySet()) {
            ResourceLocation id = e.getKey();
            Item it = e.getValue();
            if (it.reamc$item != null) continue;
            var props = it.reamc$properties();
            mc.item.Item engine;
            if (it instanceof BlockItem bi && bi.getBlock().reamc$block != null) {
                mc.world.Block block = bi.getBlock().reamc$block;
                engine = mc.item.Item.get(block.id);
                if (engine == null) engine = mc.item.Item.register(block.id, block.name, id.toString(), block, -1);
                else engine.key = id.toString();
                if (CROSS.containsKey(bi.getBlock())) block.itemTex = block.texSide;
            } else {
                int engineId = ids.item(id.toString());
                if (engineId < 0) {
                    System.err.println("[mods] No free item id for " + id);
                    continue;
                }
                String langKey = "item." + id.getNamespace() + "." + id.getPath();
                String name = Lang.has(langKey) ? Lang.get(langKey) : title(id.getPath());
                JsonObject model = json("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json");
                String layer = model != null && model.has("textures") && model.getAsJsonObject("textures").has("layer0")
                        ? model.getAsJsonObject("textures").get("layer0").getAsString() : id.getNamespace() + ":item/" + id.getPath();
                engine = mc.item.Item.register(engineId, name, id.toString(), null, itemTile(layer));
                String parent = model != null && model.has("parent") ? model.get("parent").getAsString() : "";
                engine.handheld = parent.endsWith("handheld");
            }
            engine.maxStack = props.reamc$maxStack();
            engine.maxDamage = props.reamc$maxDamage();
            engine.rarity = props.reamc$rarity().ordinal();
            if (props.reamc$food() != null) {
                engine.food = props.reamc$food().nutrition();
                engine.saturation = props.reamc$food().saturation();
            }
            engine.enchantValue = it.getEnchantmentValue() > 0 ? it.getEnchantmentValue() : -1;
            if (it instanceof TieredItem ti) bindTool(engine, ti, props.reamc$attributes());
            if (it instanceof ArmorItem ai) bindArmor(engine, ai);
            Bridge.bind(it, engine);
        }
    }

    private static int tierLevel(TagKey<Block> incorrect) {
        if (incorrect == null) return 0;
        String p = incorrect.location().getPath();
        if (p.contains("netherite")) return 4;
        if (p.contains("diamond")) return 3;
        if (p.contains("iron")) return 2;
        if (p.contains("stone")) return 1;
        return 0;
    }

    private static void bindTool(mc.item.Item e, TieredItem ti, net.minecraft.world.item.component.ItemAttributeModifiers attrs) {
        Tier t = ti.getTier();
        e.handheld = true;
        e.maxStack = 1;
        e.tier = tierLevel(t.getIncorrectBlocksForDrops());
        e.miningSpeed = t.getSpeed();
        e.tool = ti instanceof PickaxeItem ? mc.item.Item.Tool.PICKAXE : ti instanceof AxeItem ? mc.item.Item.Tool.AXE
                : ti instanceof ShovelItem ? mc.item.Item.Tool.SHOVEL : ti instanceof HoeItem ? mc.item.Item.Tool.HOE
                : ti instanceof SwordItem ? mc.item.Item.Tool.SWORD : ti instanceof DiggerItem d ? toolFor(d.reamc$mineable()) : mc.item.Item.Tool.NONE;
        float base = switch (e.tool) { case SWORD -> 3; case AXE -> 5; case PICKAXE -> 1; case SHOVEL -> 1.5f; default -> 0; };
        e.attackDamage = 1 + (attrs != null ? attrs.reamc$attackDamage() : base + t.getAttackDamageBonus());
        if (e.maxDamage <= 0) e.maxDamage = t.getUses();
        e.enchantValue = t.getEnchantmentValue();
        var repair = t.getRepairIngredient();
        if (repair != null && !repair.isEmpty()) e.repairItem = Bridge.engineItem(repair.reamc$items().get(0).asItem());
    }

    private static mc.item.Item.Tool toolFor(TagKey<Block> mineable) {
        if (mineable == null) return mc.item.Item.Tool.NONE;
        String p = mineable.location().getPath();
        return p.endsWith("pickaxe") ? mc.item.Item.Tool.PICKAXE : p.endsWith("axe") ? mc.item.Item.Tool.AXE
                : p.endsWith("shovel") ? mc.item.Item.Tool.SHOVEL : p.endsWith("hoe") ? mc.item.Item.Tool.HOE : mc.item.Item.Tool.NONE;
    }

    private static void bindArmor(mc.item.Item e, ArmorItem ai) {
        e.maxStack = 1;
        e.armorSlot = ai.getType().reamc$slot();
        e.armorPoints = ai.getDefense();
        e.toughness = ai.getToughness();
        if (ai.getMaterial() == null) return;
        ArmorMaterial m = ai.getMaterial().value();
        e.armorMaterial = ARMOR_INDEX.computeIfAbsent(m, k -> armorMaterials++);
        if (!m.layers().isEmpty()) {
            ResourceLocation a = m.layers().get(0).reamc$asset();
            e.armorColor = averageColor("assets/" + a.getNamespace() + "/textures/models/armor/" + a.getPath() + "_layer_1.png");
        }
        var repair = m.repairIngredient() == null ? null : m.repairIngredient().get();
        if (repair != null && !repair.isEmpty()) e.repairItem = Bridge.engineItem(repair.reamc$items().get(0).asItem());
        if (e.enchantValue < 0) e.enchantValue = m.enchantmentValue();
    }

    // ------------------------------------------------------------------ data: tags, drops, recipes, ores

    /** Every JSON file under data/<any>/<folder>/ in the loaded jars, by "namespace:path". */
    private static Map<String, JsonObject> dataFiles(String folder) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        for (ZipFile z : JARS) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (!n.startsWith("data/") || !n.endsWith(".json")) continue;
                String rest = n.substring(5);
                int slash = rest.indexOf('/');
                if (slash < 0 || !rest.startsWith(folder + "/", slash + 1)) continue;
                String ns = rest.substring(0, slash), path = rest.substring(slash + 2 + folder.length(), rest.length() - 5);
                JsonObject o = json(n);
                if (o != null) out.put(ns + ":" + path, o);
            }
        }
        return out;
    }

    private static final Map<String, List<mc.item.Item>> ITEM_TAGS = new HashMap<>();

    private static List<String> tagValues(JsonObject o) {
        List<String> out = new ArrayList<>();
        if (o == null || !o.has("values")) return out;
        for (JsonElement v : o.getAsJsonArray("values")) out.add(v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString());
        return out;
    }

    private static void loadData() {
        // Block tags: which tool mines it and which tier it needs
        Map<String, JsonObject> blockTags = dataFiles("tags/block");
        Map<String, mc.item.Item.Tool> tools = Map.of("minecraft:mineable/pickaxe", mc.item.Item.Tool.PICKAXE, "minecraft:mineable/axe", mc.item.Item.Tool.AXE,
                "minecraft:mineable/shovel", mc.item.Item.Tool.SHOVEL, "minecraft:mineable/hoe", mc.item.Item.Tool.HOE);
        Map<String, Integer> tiers = Map.of("minecraft:needs_stone_tool", 1, "minecraft:needs_iron_tool", 2, "minecraft:needs_diamond_tool", 3);
        Map<mc.world.Block, Integer> needs = new HashMap<>();
        for (var e : blockTags.entrySet()) {
            for (String v : tagValues(e.getValue())) {
                mc.world.Block b = mc.world.Block.byKey(v);
                if (b == null || !b.modded) continue;
                if (tools.containsKey(e.getKey())) b.toolOverride = tools.get(e.getKey());
                if (tiers.containsKey(e.getKey())) needs.merge(b, tiers.get(e.getKey()), Math::max);
            }
        }
        for (Block b : BuiltInRegistries.BLOCK) {
            mc.world.Block eb = b.reamc$block;
            if (eb == null || !eb.modded) continue;
            if (eb.toolOverride == null) eb.toolOverride = mc.item.Item.Tool.NONE;
            boolean requires = b.reamc$properties().reamc$requiresTool();
            eb.requiredTierOverride = requires && eb.toolOverride == mc.item.Item.Tool.PICKAXE ? needs.getOrDefault(eb, 0) : -1;
            // Without a loot table a Minecraft block drops nothing
            eb.dropCount = 0;
        }
        // Item tags (for recipes)
        for (var e : dataFiles("tags/item").entrySet())
            for (String v : tagValues(e.getValue())) {
                mc.item.Item i = mc.item.Item.byKey(v);
                if (i != null) ITEM_TAGS.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(i);
            }
        // Block loot tables
        Map<String, JsonObject> loot = dataFiles("loot_table/blocks");
        loot.putAll(dataFiles("loot_tables/blocks"));
        for (var e : loot.entrySet()) {
            mc.world.Block b = mc.world.Block.byKey(e.getKey());
            if (b == null || !b.modded) continue;
            applyLoot(b, e.getValue());
        }
        // Recipes
        Map<String, JsonObject> recipes = dataFiles("recipe");
        recipes.putAll(dataFiles("recipes"));
        int added = 0;
        for (var e : recipes.entrySet()) {
            try {
                if (addRecipe(e.getValue())) added++;
            } catch (RuntimeException ex) {
                System.err.println("[mods] Skipping recipe " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (added > 0) System.out.println("[mods] Added " + added + " recipes");
        // Ores added to every biome
        for (var e : dataFiles("neoforge/biome_modifier").entrySet()) {
            JsonObject o = e.getValue();
            if (!o.has("type") || !o.get("type").getAsString().equals("neoforge:add_features")) continue;
            JsonElement f = o.get("features");
            List<String> features = new ArrayList<>();
            if (f.isJsonArray()) for (JsonElement x : f.getAsJsonArray()) features.add(x.getAsString());
            else features.add(f.getAsString());
            for (String id : features) addOre(id);
        }
    }

    private static void applyLoot(mc.world.Block b, JsonObject table) {
        if (!table.has("pools")) return;
        for (JsonElement pe : table.getAsJsonArray("pools")) {
            JsonObject pool = pe.getAsJsonObject();
            if (!pool.has("entries")) continue;
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            // Silk-touch alternatives: the last child is the normal drop
            while (entry.has("children")) {
                JsonArray ch = entry.getAsJsonArray("children");
                entry = ch.get(ch.size() - 1).getAsJsonObject();
            }
            if (!entry.has("name")) continue;
            mc.item.Item it = mc.item.Item.byKey(entry.get("name").getAsString());
            if (it == null) continue;
            int count = 1;
            if (entry.has("functions")) for (JsonElement fn : entry.getAsJsonArray("functions")) {
                JsonObject fo = fn.getAsJsonObject();
                if (fo.get("function").getAsString().endsWith("set_count")) count = Math.max(1, Math.round(number(fo.get("count"))));
            }
            b.dropItem = it.block == b ? null : it;
            b.dropCount = count;
            return;
        }
    }

    /** A number provider: a constant or the middle of a uniform range. */
    private static float number(JsonElement e) {
        if (e == null) return 1;
        if (e.isJsonPrimitive()) return e.getAsFloat();
        JsonObject o = e.getAsJsonObject();
        if (o.has("value")) return o.get("value").getAsFloat();
        if (o.has("min") && o.has("max")) return (o.get("min").getAsFloat() + o.get("max").getAsFloat()) / 2;
        return 1;
    }

    private static mc.item.Item[] ingredient(JsonElement e) {
        if (e.isJsonArray()) {
            List<mc.item.Item> all = new ArrayList<>();
            for (JsonElement x : e.getAsJsonArray()) all.addAll(List.of(ingredient(x)));
            return all.toArray(new mc.item.Item[0]);
        }
        JsonObject o = e.getAsJsonObject();
        if (o.has("item")) {
            mc.item.Item i = mc.item.Item.byKey(o.get("item").getAsString());
            if (i == null) throw new IllegalArgumentException("unknown item " + o.get("item").getAsString());
            return new mc.item.Item[]{i};
        }
        if (o.has("tag")) {
            String tag = o.get("tag").getAsString();
            List<mc.item.Item> l = ITEM_TAGS.get(tag);
            if (l == null) l = builtinTag(tag);
            if (l.isEmpty()) throw new IllegalArgumentException("unknown tag " + tag);
            return l.toArray(new mc.item.Item[0]);
        }
        throw new IllegalArgumentException("unsupported ingredient " + o);
    }

    private static List<mc.item.Item> builtinTag(String tag) {
        List<mc.item.Item> l = new ArrayList<>();
        switch (tag) {
            case "minecraft:planks" -> { l.add(mc.item.Item.of(mc.world.Block.PLANKS)); l.add(mc.item.Item.of(mc.world.Block.SPRUCE_PLANKS)); l.add(mc.item.Item.of(mc.world.Block.BIRCH_PLANKS)); }
            case "minecraft:logs", "minecraft:logs_that_burn" -> { l.add(mc.item.Item.of(mc.world.Block.OAK_LOG)); l.add(mc.item.Item.of(mc.world.Block.SPRUCE_LOG)); l.add(mc.item.Item.of(mc.world.Block.BIRCH_LOG)); }
            case "minecraft:wool" -> l.add(mc.item.Item.of(mc.world.Block.WHITE_WOOL));
            case "minecraft:coals" -> { l.add(mc.item.Item.COAL); l.add(mc.item.Item.CHARCOAL); }
            case "c:ingots/iron", "forge:ingots/iron" -> l.add(mc.item.Item.IRON_INGOT);
            case "c:ingots/gold", "forge:ingots/gold" -> l.add(mc.item.Item.GOLD_INGOT);
            case "c:gems/diamond", "forge:gems/diamond" -> l.add(mc.item.Item.DIAMOND);
            case "c:rods/wooden", "forge:rods/wooden" -> l.add(mc.item.Item.STICK);
            case "c:cobblestones", "minecraft:stone_crafting_materials" -> l.add(mc.item.Item.of(mc.world.Block.COBBLESTONE));
            default -> { }
        }
        return l;
    }

    private static mc.item.Item result(JsonElement r) {
        String id = r.isJsonPrimitive() ? r.getAsString() : r.getAsJsonObject().has("id") ? r.getAsJsonObject().get("id").getAsString() : r.getAsJsonObject().get("item").getAsString();
        mc.item.Item i = mc.item.Item.byKey(id);
        if (i == null) throw new IllegalArgumentException("unknown result " + id);
        return i;
    }

    private static int resultCount(JsonElement r) { return r.isJsonObject() && r.getAsJsonObject().has("count") ? r.getAsJsonObject().get("count").getAsInt() : 1; }

    private static boolean addRecipe(JsonObject o) {
        String type = o.get("type").getAsString();
        switch (type) {
            case "minecraft:crafting_shaped" -> {
                JsonArray pat = o.getAsJsonArray("pattern");
                String[] rows = new String[pat.size()];
                for (int i = 0; i < rows.length; i++) rows[i] = pat.get(i).getAsString();
                Map<Character, mc.item.Item[]> keys = new HashMap<>();
                for (var e : o.getAsJsonObject("key").entrySet()) keys.put(e.getKey().charAt(0), ingredient(e.getValue()));
                Recipes.addShaped(result(o.get("result")), resultCount(o.get("result")), rows, keys);
                return true;
            }
            case "minecraft:crafting_shapeless" -> {
                List<mc.item.Item[]> ing = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("ingredients")) ing.add(ingredient(e));
                Recipes.addShapeless(result(o.get("result")), resultCount(o.get("result")), ing);
                return true;
            }
            case "minecraft:smelting" -> {
                for (mc.item.Item in : ingredient(o.get("ingredient"))) Recipes.addSmelting(in, result(o.get("result")), resultCount(o.get("result")));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** A placed ore feature: {block, veins per chunk, vein size, min y, max y} added to world generation. */
    private static void addOre(String placedId) {
        ResourceLocation rl = ResourceLocation.parse(placedId);
        JsonObject placed = json("data/" + rl.getNamespace() + "/worldgen/placed_feature/" + rl.getPath() + ".json");
        if (placed == null || !placed.get("feature").isJsonPrimitive()) return;
        ResourceLocation cf = ResourceLocation.parse(placed.get("feature").getAsString());
        JsonObject configured = json("data/" + cf.getNamespace() + "/worldgen/configured_feature/" + cf.getPath() + ".json");
        if (configured == null || !configured.get("type").getAsString().equals("minecraft:ore")) return;
        JsonObject cfg = configured.getAsJsonObject("config");
        int size = cfg.get("size").getAsInt();
        mc.world.Block ore = null;
        for (JsonElement t : cfg.getAsJsonArray("targets")) {
            String name = t.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString();
            ore = mc.world.Block.byKey(name);
            if (ore != null) break;
        }
        if (ore == null) return;
        int count = 1, minY = 1, maxY = 64;
        for (JsonElement pe : placed.getAsJsonArray("placement")) {
            JsonObject p = pe.getAsJsonObject();
            String type = p.get("type").getAsString();
            if (type.endsWith(":count")) count = Math.max(1, Math.round(number(p.get("count"))));
            if (type.endsWith(":height_range")) {
                JsonObject h = p.getAsJsonObject("height");
                if (h.has("min_inclusive")) minY = anchor(h.get("min_inclusive"), minY);
                if (h.has("max_inclusive")) maxY = anchor(h.get("max_inclusive"), maxY);
            }
        }
        minY = Math.max(1, minY);
        maxY = Math.min(mc.world.Chunk.HEIGHT - 2, Math.max(minY + 1, maxY));
        TerrainGenerator.EXTRA_ORES.add(new int[]{ore.id, count, size, minY, maxY});
        System.out.println("[mods] Ore " + ore.name + ": " + count + " veins of " + size + " per chunk at y " + minY + "-" + maxY);
    }

    private static int anchor(JsonElement e, int fallback) {
        JsonObject o = e.getAsJsonObject();
        if (o.has("absolute")) return o.get("absolute").getAsInt();
        if (o.has("above_bottom")) return o.get("above_bottom").getAsInt();
        if (o.has("below_top")) return mc.world.Chunk.HEIGHT - o.get("below_top").getAsInt();
        return fallback;
    }

    static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
