package mc;

import mc.entity.Player;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.mod.Bridge;
import mc.mod.ModAnalyzer;
import mc.mod.ModLoader;
import mc.util.RayCast;
import mc.world.Block;
import mc.world.Drops;
import mc.world.World;
import mc.world.WorldStorage;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.imageio.ImageIO;
import javax.tools.ToolProvider;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NeoForge mods through reamc-compat: a small mod is compiled against the compat classes, packed into a jar with
 * assets and data, and loaded like a real mod. (-Dreamc.testJar=path also loads a real mod jar alongside it.)
 */
class ModsTest {
    static Path modsDir;

    @BeforeAll
    static void buildAndLoad() throws Exception {
        Path work = Files.createTempDirectory("fixturemod");
        Path classes = work.resolve("classes");
        Files.createDirectories(classes);
        List<String> args = new ArrayList<>(List.of("-proc:none", "--release", "17", "-classpath", System.getProperty("java.class.path"), "-d", classes.toString()));
        try (Stream<Path> s = Files.walk(Path.of("src/test/resources/fixturemod/src"))) {
            s.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString()));
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])), "fixture mod compiles against reamc-compat");
        Path res = Path.of("src/test/resources/fixturemod/res");
        try (Stream<Path> s = Files.walk(res)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                Path to = classes.resolve(res.relativize(p).toString());
                Files.createDirectories(to.getParent());
                Files.copy(p, to);
            }
        }
        texture(classes.resolve("assets/fixture/textures/block/gem_block.png"), 0x30c060);
        texture(classes.resolve("assets/fixture/textures/block/lamp_front.png"), 0xf0e040);
        texture(classes.resolve("assets/fixture/textures/item/gem.png"), 0x40e080);
        texture(classes.resolve("assets/fixture/textures/item/gem_pickaxe.png"), 0x808080);
        modsDir = work.resolve("mods");
        Files.createDirectories(modsDir);
        jar(classes, modsDir.resolve("fixture.jar"));
        String extra = System.getProperty("reamc.testJar");
        if (extra != null) Files.copy(Path.of(extra), modsDir.resolve(Path.of(extra).getFileName()));
        ModLoader.loadAll(modsDir, Dist.CLIENT);
    }

    @org.junit.jupiter.api.AfterAll
    static void unhook() {
        // The blocks stay registered for the rest of the run, but other tests' worlds shouldn't call into the mod
        World.modHooks = null;
    }

    private static void texture(Path p, int rgb) throws Exception {
        Files.createDirectories(p.getParent());
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, 0xFF000000 | ((x + y) % 4 == 0 ? rgb / 2 & 0x7f7f7f : rgb));
        ImageIO.write(img, "png", p.toFile());
    }

    private static void jar(Path dir, Path out) throws Exception {
        try (JarOutputStream j = new JarOutputStream(Files.newOutputStream(out)); Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                j.putNextEntry(new JarEntry(dir.relativize(p).toString().replace('\\', '/')));
                j.write(Files.readAllBytes(p));
                j.closeEntry();
            }
        }
    }

    private static Class<?> modClass(String name) throws Exception {
        net.minecraft.world.level.block.Block lamp = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("fixture:lamp"));
        return Class.forName(name, true, lamp.getClass().getClassLoader());
    }

    @Test
    void blocksAndItemsComeFromTheModsCodeAssetsAndData() {
        assertTrue(ModLoader.MODS.stream().anyMatch(m -> m.id().equals("fixture") && m.loaded() && m.version().equals("1.2.3")));
        Block gem = Block.byKey("fixture:gem_block"), lamp = Block.byKey("fixture:lamp");
        assertNotNull(gem);
        assertEquals("Block of Gem", gem.name, "name from the language file");
        assertEquals(2f, gem.hardness);
        assertEquals(Item.Tool.PICKAXE, Item.effectiveTool(gem), "mineable/pickaxe tag");
        assertEquals(2, Item.requiredTier(gem), "needs_iron_tool tag");
        assertEquals(4, lamp.stateFaceTex.length, "one entry per facing");
        assertEquals(12, lamp.lightEmission);
        assertTrue(gem.stateFaceTex[0][0] >= Block.Tex.COUNT, "mod texture in the atlas");

        Item gemItem = Item.byKey("fixture:gem"), pick = Item.byKey("fixture:gem_pickaxe");
        assertEquals("Gem", gemItem.name);
        assertTrue(gemItem.icon >= 146);
        assertEquals(Item.Tool.PICKAXE, pick.tool);
        assertEquals(2, pick.tier, "iron tier");
        assertEquals(250, pick.maxDamage);
        assertTrue(pick.handheld);
        assertSame(Item.get(gem.id), Item.byKey("fixture:gem_block"), "block items share the block's id");

        // Loot table: 4 gems, but only with an iron pickaxe
        List<ItemStack> drops = Drops.of(gem, 0, new ItemStack(pick, 1));
        assertEquals(1, drops.size());
        assertSame(gemItem, drops.get(0).item);
        assertEquals(4, drops.get(0).count);
        assertTrue(Drops.of(gem, 0, new ItemStack(Item.WOODEN_PICKAXE, 1)).isEmpty());
    }

    @Test
    void recipesFromTheModsDataWork() {
        Item gem = Item.byKey("fixture:gem");
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) grid[i] = new ItemStack(gem, 1);
        assertSame(Item.byKey("fixture:gem_block"), Recipes.match(grid, 3).item);
        ItemStack[] one = new ItemStack[9];
        one[4] = new ItemStack(Item.byKey("fixture:gem_block"), 1);
        ItemStack out = Recipes.match(one, 3);
        assertEquals(9, out.count);
        ItemStack[] pick = new ItemStack[9];
        pick[0] = pick[1] = pick[2] = new ItemStack(gem, 1);
        pick[4] = pick[7] = new ItemStack(Item.STICK, 1);
        assertSame(Item.byKey("fixture:gem_pickaxe"), Recipes.match(pick, 3).item, "minecraft:stick resolves to reamc's stick");
    }

    @Test
    void eventsBlockEntitiesAndPlacementRunTheModsCode() throws Exception {
        Class<?> mod = modClass("fixture.FixtureMod");
        assertTrue(mod.getField("setupRan").getBoolean(null), "@EventBusSubscriber got FMLCommonSetupEvent");

        Path dir = Files.createTempDirectory("modworld");
        World w = new World(5, new WorldStorage(dir));
        w.loadAreaBlocking(0, 0, 1);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            w.setBlock(x, 99, z, Block.STONE.id, 0, false);
            w.setBlock(x, 100, z, 0, 0, false);
        }
        Block counter = Block.byKey("fixture:counter");
        w.setBlock(3, 100, 3, counter.id, 0, true);
        Object be = Bridge.blockEntity(w, new BlockPos(3, 100, 3));
        assertNotNull(be, "placing the block created its block entity");
        assertEquals("fixture.FixtureMod$CounterEntity", be.getClass().getName());

        Player p = new Player();
        p.setPos(3.5, 100, 1.5);
        w.setPlayer(p);
        RayCast.Hit hit = new RayCast.Hit();
        hit.x = 3; hit.y = 100; hit.z = 3; hit.nz = -1;
        for (int i = 0; i < 3; i++) assertTrue(World.modHooks.useBlock(w, p, 3, 100, 3, hit), "useWithoutItem returned SUCCESS");
        assertEquals(3, be.getClass().getField("clicks").getInt(be));

        int before = mod.getField("ticks").getInt(null);
        for (int i = 0; i < 5; i++) w.tick();
        assertEquals(before + 5, mod.getField("ticks").getInt(null), "ServerTickEvent.Post every tick");

        // The block entity is saved with the world and comes back
        w.saveAll();
        w.shutdown();
        World again = new World(5, new WorldStorage(dir));
        Object loaded = Bridge.blockEntity(again, new BlockPos(3, 100, 3));
        assertNotNull(loaded);
        assertEquals(3, loaded.getClass().getField("clicks").getInt(loaded));
        again.shutdown();

        // Placement asks the block for its state: the lamp faces the player
        Block lamp = Block.byKey("fixture:lamp");
        p.yaw = 0; // looking south
        int meta = World.modHooks.placementMeta(w, lamp, p, 1, 100, 1, hit);
        assertEquals("NORTH", Bridge.state(lamp.id, meta).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).name());
        p.yaw = 90; // looking west
        meta = World.modHooks.placementMeta(w, lamp, p, 1, 100, 1, hit);
        int front = lamp.textureForFace(5, meta), side = lamp.textureForFace(2, meta);
        assertNotEquals(front, side, "the front texture turned to the east face");
        assertEquals(lamp.textureForFace(2, 0), front, "facing north, the front is on the north face");
    }

    @Test
    void theAnalyzerReportsWhatReamcDoesNotProvide() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "bad/Bad", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/world/level/block/Block", "reamcHasNoSuchMethod", "()V", false);
        mv.visitTypeInsn(Opcodes.NEW, "net/minecraft/world/level/NoSuchClass");
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();
        Path jar = Files.createTempFile("bad", ".jar");
        try (JarOutputStream j = new JarOutputStream(Files.newOutputStream(jar))) {
            j.putNextEntry(new JarEntry("bad/Bad.class"));
            j.write(cw.toByteArray());
            j.closeEntry();
        }
        ModAnalyzer a = ModAnalyzer.analyze(new ZipFile(jar.toFile()), ModsTest.class.getClassLoader());
        assertTrue(a.missing.contains("net.minecraft.world.level.block.Block.reamcHasNoSuchMethod()V"), a.missing.toString());
        assertTrue(a.missing.contains("net.minecraft.world.level.NoSuchClass"));
        ModAnalyzer good = ModAnalyzer.analyze(new ZipFile(modsDir.resolve("fixture.jar").toFile()), ModsTest.class.getClassLoader());
        assertTrue(good.missing.isEmpty(), good.missing.toString());
        assertEquals(List.of("fixture.FixtureMod"), good.modClasses);
    }
}
