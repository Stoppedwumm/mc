package mc.client;

import mc.entity.Entity;
import mc.entity.Mob;
import mc.entity.MobType;
import mc.item.Item;
import mc.item.ItemStack;
import mc.world.Block;

/** Chat commands, mirroring Minecraft's syntax where possible. */
final class Commands {
    private final Game g;

    Commands(Game g) {
        this.g = g;
    }

    private void chat(String s) { g.hud.chat(s); }

    static Item itemByName(String name) {
        String n = name.replace("minecraft:", "").replace('_', ' ').trim();
        for (Item i : Item.BY_ID) if (i != null && i.name.equalsIgnoreCase(n)) return i;
        if (n.equalsIgnoreCase("grass block")) return Item.of(Block.GRASS);
        return null;
    }

    static Block blockByName(String name) {
        String n = name.replace("minecraft:", "").replace('_', ' ');
        for (Block b : Block.BY_ID) if (b != null && b.name.equalsIgnoreCase(n)) return b;
        if (n.equalsIgnoreCase("grass block")) return Block.GRASS;
        return null;
    }

    private double coord(String s, double current) {
        if (s.startsWith("~")) return current + (s.length() > 1 ? Double.parseDouble(s.substring(1)) : 0);
        return Double.parseDouble(s);
    }

    void run(String line) {
        String[] a = line.replaceFirst("^/", "").trim().split("\\s+");
        var p = g.player;
        var world = g.world;
        try {
            switch (a[0]) {
                case "time" -> {
                    long v = switch (a[2]) {
                        case "day" -> 1000; case "noon" -> 6000; case "sunset" -> 12000; case "night" -> 13000; case "midnight" -> 18000; case "sunrise" -> 23000;
                        default -> Long.parseLong(a[2]);
                    };
                    if (a[1].equals("add")) world.time += v;
                    else world.time = (world.time / 24000) * 24000 + v;
                    chat("Set the time to " + world.time % 24000);
                }
                case "gamemode", "gm" -> {
                    String m = a.length > 1 ? a[1] : "";
                    if (m.startsWith("c") || m.equals("1")) g.setCreative(true);
                    else if (m.startsWith("s") || m.equals("0")) g.setCreative(false);
                    else chat("Usage: /gamemode creative|survival");
                }
                case "tp" -> {
                    double x = coord(a[1], p.x), y = coord(a[2], p.y), z = coord(a[3], p.z);
                    p.setPos(x, y, z);
                    p.motionX = p.motionY = p.motionZ = 0;
                    p.fallDistance = 0;
                    chat(String.format("Teleported to %.1f, %.1f, %.1f", x, y, z));
                }
                case "look" -> { p.yaw = Float.parseFloat(a[1]); p.pitch = Float.parseFloat(a[2]); }
                case "debug" -> g.showDebug = !g.showDebug;
                case "perspective" -> g.perspective = Integer.parseInt(a[1]) % 3;
                case "xp" -> { p.addXp(Integer.parseInt(a[1])); chat("Gave " + a[1] + " experience points"); }
                case "screen" -> {
                    switch (a[1]) {
                        case "pause" -> g.setScreen(Game.Screen.PAUSE);
                        case "inventory" -> g.screens.openInventory();
                        case "crafting" -> g.screens.openCrafting();
                        case "trading" -> {
                            Mob v = new Mob(MobType.VILLAGER);
                            v.profession = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                            v.setPos(p.x + 2, p.y, p.z);
                            world.addEntity(v);
                            g.screens.openTrading(v);
                        }
                        case "creative" -> {
                            p.creative = true;
                            g.screens.openInventory();
                            if (a.length > 2) g.screens.creativeScroll = Integer.parseInt(a[2]);
                        }
                        default -> g.closeScreen();
                    }
                }
                case "give" -> {
                    Item it = itemByName(a[1]);
                    int count = a.length > 2 ? Integer.parseInt(a[2]) : 1;
                    if (it == null) { chat("Unknown item: " + a[1]); return; }
                    ItemStack left = p.inventory.add(new ItemStack(it, count));
                    if (left.count > 0) world.spawnItem(p.x, p.y + 1, p.z, left);
                    chat("Gave " + count + " [" + it.name + "] to Player");
                }
                case "armor" -> {
                    int mat = java.util.Arrays.asList("leather", "chainmail", "iron", "golden", "diamond").indexOf(a[1]);
                    for (int i = 0; i < 4; i++) p.inventory.armor[i] = mat < 0 ? null : new ItemStack(Item.armor(mat, i), 1);
                    chat(mat < 0 ? "Removed armor" : "Equipped " + a[1] + " armor");
                }
                case "clear" -> { p.inventory.clear(); chat("Cleared the inventory"); }
                case "summon" -> {
                    MobType t = MobType.valueOf(a[1].replace("minecraft:", "").toUpperCase());
                    Mob m = new Mob(t);
                    double x = a.length > 4 ? coord(a[2], p.x) : p.x + 3, y = a.length > 4 ? coord(a[3], p.y) : p.y, z = a.length > 4 ? coord(a[4], p.z) : p.z;
                    m.setPos(x, y, z);
                    m.yaw = m.bodyYaw = m.headYaw = (float) Math.random() * 360;
                    for (int k = 5; k < a.length; k++) {
                        int mat = java.util.Arrays.asList("leather", "chainmail", "iron", "golden", "diamond").indexOf(a[k]);
                        if (mat >= 0) for (int i = 0; i < 4; i++) m.armor[i] = new ItemStack(Item.armor(mat, i), 1);
                        switch (a[k]) {
                            case "baby" -> m.setGrowingAge(-24000);
                            case "tamed" -> { m.tamed = true; m.maxHealth = m.health = 20; }
                            case "sitting" -> m.sitting = true;
                            case "sheared" -> m.sheared = true;
                            case "angry" -> m.angerTicks = 600;
                            default -> {
                                if (a[k].matches("\\d+")) {
                                    if (t == MobType.SLIME) m.setSlimeSize(Integer.parseInt(a[k]));
                                    else if (t == MobType.SHEEP) m.sheepColor = Integer.parseInt(a[k]) % 5;
                                    else if (t == MobType.ENDERMAN) m.carriedBlock = Integer.parseInt(a[k]);
                                }
                            }
                        }
                    }
                    world.addEntity(m);
                    chat("Summoned new " + t.displayName);
                }
                case "kill" -> {
                    if (a.length > 1 && a[1].equals("@e")) {
                        int n = 0;
                        for (Entity e : world.entities()) if (e instanceof Mob) { e.remove(); n++; }
                        chat("Killed " + n + " entities");
                    } else {
                        p.damage(mc.entity.DamageSource.VOID, 1000, null);
                    }
                }
                case "weather" -> {
                    g.weather.raining = a[1].equals("rain") || a[1].equals("thunder");
                    g.weather.timer = 12000;
                    chat("Changed the weather to " + a[1]);
                }
                case "heal" -> { p.health = p.maxHealth; p.food = 20; p.saturation = 5; chat("Healed"); }
                case "spawnpoint" -> { p.spawnX = p.x; p.spawnY = p.y; p.spawnZ = p.z; chat("Set spawn point"); }
                case "setblock" -> {
                    int x = (int) Math.floor(coord(a[1], p.x)), y = (int) Math.floor(coord(a[2], p.y)), z = (int) Math.floor(coord(a[3], p.z));
                    Block b = blockByName(a[4]);
                    if (b == null) { chat("Unknown block: " + a[4]); return; }
                    if (!world.isLoaded(x, z)) { chat("That position is not loaded"); return; }
                    int meta = a.length > 5 ? Integer.parseInt(a[5]) : 0;
                    // Two-block structures get their other half too
                    if (b.shape == Block.Shape.DOOR) world.setBlock(x, y + 1, z, b.id, meta | 8, false);
                    if (b == Block.BED) world.setBlock(x + mc.world.Shapes.DX[meta & 3], y, z + mc.world.Shapes.DZ[meta & 3], b.id, (meta & 3) | 4, false);
                    world.setBlock(x, y, z, b.id, meta, true);
                    if (b.isLiquid()) world.scheduleTick(x, y, z, 5);
                    chat("Changed the block at " + x + ", " + y + ", " + z);
                }
                case "fill" -> {
                    int x0 = (int) Math.floor(coord(a[1], p.x)), y0 = (int) Math.floor(coord(a[2], p.y)), z0 = (int) Math.floor(coord(a[3], p.z));
                    int x1 = (int) Math.floor(coord(a[4], p.x)), y1 = (int) Math.floor(coord(a[5], p.y)), z1 = (int) Math.floor(coord(a[6], p.z));
                    Block b = blockByName(a[7]);
                    if (b == null) { chat("Unknown block: " + a[7]); return; }
                    int fillMeta = a.length > 8 ? Integer.parseInt(a[8]) : 0;
                    long vol = (long) (Math.abs(x1 - x0) + 1) * (Math.abs(y1 - y0) + 1) * (Math.abs(z1 - z0) + 1);
                    if (vol > 32768) { chat("Too many blocks in the specified area (" + vol + " > 32768)"); return; }
                    if (!world.isLoaded(x0, z0) || !world.isLoaded(x1, z1)) { chat("That position is not loaded"); return; }
                    for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) world.setBlock(x, y, z, b.id, fillMeta, false);
                    if (mc.world.Redstone.relevant(b.id) || b == Block.AIR)
                        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) if (vol <= 512) world.notifyAround(x, y, z);
                    chat("Successfully filled " + vol + " blocks");
                }
                case "seed" -> chat("Seed: [" + world.seed + "]");
                case "dimension", "dim" -> {
                    mc.world.Dimension d = mc.world.Dimension.valueOf(a[1].toUpperCase().replace("THE_", ""));
                    if (d != world.dimension) g.changeDimension(d, true);
                }
                case "locate" -> {
                    int[] v = world.decorator.structures.nearestVillage((int) p.x, (int) p.z);
                    if (v == null) chat("Could not find a village nearby");
                    else {
                        chat("The nearest village is at [" + v[0] + ", ~, " + v[1] + "] (" + (int) Math.hypot(v[0] - p.x, v[1] - p.z) + " blocks away)");
                        if (a.length > 2 && a[2].equals("tp")) {
                            p.setPos(v[0] + 0.5, world.generator.estimateHeight(v[0], v[1]) + 30, v[1] + 0.5);
                            p.flying = true;
                            p.motionX = p.motionY = p.motionZ = 0;
                        }
                    }
                }
                case "fly" -> { p.flying = !p.flying; chat("Flying " + (p.flying ? "enabled" : "disabled")); }
                case "rd", "renderdistance" -> { g.options.renderDistance = Math.max(2, Math.min(32, Integer.parseInt(a[1]))); chat("Render distance: " + g.options.renderDistance); }
                case "help" -> {
                    chat("/time set <day|night|n>, /gamemode <c|s>, /tp x y z, /give <item> [n], /clear");
                    chat("/summon <mob> [x y z], /kill [@e], /weather <clear|rain>, /heal, /spawnpoint");
                    chat("/setblock x y z <block> [meta], /fill x1 y1 z1 x2 y2 z2 <block> [meta], /fly, /seed, /rd <n>");
                }
                default -> chat("Unknown command. Type /help for help.");
            }
        } catch (Exception e) {
            chat("Invalid command arguments");
        }
    }
}
