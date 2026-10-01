package mc.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** User settings, persisted as options.json next to the saves folder. */
public final class Options {
    public int renderDistance = 10;
    public float fov = 70;
    public float sensitivity = 0.5f;
    public float gamma = 0.5f;
    public boolean viewBobbing = true;
    public boolean vsync = true;
    public boolean clouds = true;
    public boolean shadows = true;
    public float volume = 1f;
    /** Distant terrain (level-of-detail) range in chunks; 0 turns it off. */
    public int lodDistance = 64;
    /** Name shown to other players in multiplayer. */
    public String playerName = "Player" + (100 + new java.util.Random().nextInt(900));
    /** Last address typed into Direct Connection. */
    public String lastServer = "localhost";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Options load(Path file) {
        try {
            if (Files.exists(file)) {
                Options o = GSON.fromJson(Files.readString(file), Options.class);
                if (o != null) return o;
            }
        } catch (Exception e) {
            System.err.println("Could not read options: " + e);
        }
        return new Options();
    }

    public void save(Path file) {
        try {
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("Could not save options: " + e);
        }
    }

    /** Serialised chest/furnace contents. */
    public static final class BlockEntityData {
        public int x, y, z;
        public String type;
        public int[][] slots;
        public int burnTime, burnTotal, cookTime;
        public String mob;

        public static List<BlockEntityData> loadList(Path file) {
            try {
                if (Files.exists(file)) return java.util.Arrays.asList(GSON.fromJson(Files.readString(file), BlockEntityData[].class));
            } catch (Exception e) {
                System.err.println("Could not read block entities: " + e);
            }
            return null;
        }

        /** Snapshot of a world's chests, furnaces, spawners and brewing stands. */
        public static List<BlockEntityData> capture(mc.world.World world) {
            List<BlockEntityData> out = new java.util.ArrayList<>();
            for (mc.world.BlockEntity be : world.blockEntities.values()) {
                BlockEntityData d = new BlockEntityData();
                d.x = be.x; d.y = be.y; d.z = be.z;
                d.type = be instanceof mc.world.BlockEntity.Chest ? "chest" : be instanceof mc.world.BlockEntity.Spawner ? "spawner"
                        : be instanceof mc.world.BlockEntity.BrewingStand ? "brewing" : "furnace";
                if (be instanceof mc.world.BlockEntity.BrewingStand bs) { d.cookTime = bs.brewTime; d.burnTime = bs.fuel; }
                if (be instanceof mc.world.BlockEntity.Spawner sp) d.mob = sp.mob;
                d.slots = Level.slots(be.slots);
                if (be instanceof mc.world.BlockEntity.Furnace f) { d.burnTime = f.burnTime; d.burnTotal = f.burnTotal; d.cookTime = f.cookTime; }
                out.add(d);
            }
            return out;
        }

        /** Recreates saved block entities in a world. */
        public static void apply(mc.world.World world, List<BlockEntityData> list) {
            for (BlockEntityData d : list) {
                mc.world.BlockEntity be = switch (d.type) {
                    case "chest" -> new mc.world.BlockEntity.Chest(d.x, d.y, d.z);
                    case "spawner" -> { mc.world.BlockEntity.Spawner sp = new mc.world.BlockEntity.Spawner(d.x, d.y, d.z); if (d.mob != null) sp.mob = d.mob; yield sp; }
                    case "brewing" -> { mc.world.BlockEntity.BrewingStand bs = new mc.world.BlockEntity.BrewingStand(d.x, d.y, d.z); bs.brewTime = d.cookTime; bs.fuel = d.burnTime; yield bs; }
                    default -> new mc.world.BlockEntity.Furnace(d.x, d.y, d.z);
                };
                for (int i = 0; d.slots != null && i < Math.min(be.slots.length, d.slots.length); i++) {
                    int[] e = d.slots[i];
                    if (e != null) be.slots[i] = mc.item.ItemStack.fromArray(e);
                }
                if (be instanceof mc.world.BlockEntity.Furnace f) { f.burnTime = d.burnTime; f.burnTotal = d.burnTotal; f.cookTime = d.cookTime; }
                world.blockEntities.put(mc.world.World.posKey(d.x, d.y, d.z), be);
            }
        }

        public static void saveList(Path file, List<BlockEntityData> list) {
            try {
                Files.writeString(file, GSON.toJson(list));
            } catch (IOException e) {
                System.err.println("Could not save block entities: " + e);
            }
        }
    }

    /** World metadata (level.json). */
    public static final class Level {
        public long seed;
        public long time = 1000;
        public double x, y, z;
        public float yaw, pitch;
        public boolean flying, creative, spawned;
        public int[] hotbar;
        public int selected;
        /** Inventory as [id, count, damage] triples (null entries for empty slots). */
        public int[][] inventory;
        public int[][] armor;
        public int xpLevel, xpTotal;
        public float xpProgress;
        /** Active effects as [ordinal, amplifier, duration]; absorption hearts left. */
        public int[][] effects;
        public float absorption;
        public float health = 20;
        public int food = 20;
        public float saturation = 5;
        public double spawnX, spawnY = -1, spawnZ;
        public boolean raining;
        public int weatherTimer;
        public List<BlockEntityData> blockEntities;
        /** Dimension the player is in (Dimension enum name). */
        public String dimension = "OVERWORLD";
        /** Display name shown in the world list, and when it was last played (epoch millis). */
        public String name;
        public long lastPlayed;

        /** Copies the saved player state onto a player. */
        public void readPlayer(mc.entity.Player player) {
            player.setPos(x, y, z);
            player.yaw = yaw;
            player.pitch = pitch;
            player.flying = flying;
            player.creative = creative;
            player.health = health;
            player.food = food;
            player.saturation = saturation;
            player.spawnX = spawnX;
            player.spawnY = spawnY;
            player.spawnZ = spawnZ;
            if (inventory != null) {
                for (int i = 0; i < Math.min(36, inventory.length); i++) {
                    int[] e = inventory[i];
                    if (e != null) player.inventory.slots[i] = mc.item.ItemStack.fromArray(e);
                }
            } else if (hotbar != null) {
                for (int i = 0; i < 9 && i < hotbar.length; i++)
                    if (mc.item.Item.get(hotbar[i]) != null) player.inventory.slots[i] = new mc.item.ItemStack(mc.item.Item.get(hotbar[i]), 64);
            }
            if (armor != null) {
                for (int i = 0; i < Math.min(4, armor.length); i++) {
                    int[] e = armor[i];
                    if (e != null) player.inventory.armor[i] = mc.item.ItemStack.fromArray(e);
                }
            }
            player.xpLevel = xpLevel;
            player.xpProgress = xpProgress;
            player.xpTotal = xpTotal;
            mc.entity.Effect[] all = mc.entity.Effect.values();
            if (effects != null)
                for (int[] e : effects)
                    if (e != null && e.length == 3 && e[0] >= 0 && e[0] < all.length)
                        player.effects.put(all[e[0]], new mc.entity.Effect.Instance(all[e[0]], e[1], e[2]));
            player.absorption = absorption;
            player.inventory.selected = Math.max(0, Math.min(8, selected));
        }

        /** Stores a player's state (position, inventory, health, experience, effects). */
        public void writePlayer(mc.entity.Player player) {
            x = player.x; y = player.y; z = player.z;
            yaw = player.yaw; pitch = player.pitch;
            flying = player.flying;
            creative = player.creative;
            inventory = slots(player.inventory.slots);
            armor = slots(player.inventory.armor);
            xpLevel = player.xpLevel;
            xpProgress = player.xpProgress;
            xpTotal = player.xpTotal;
            effects = player.effects.values().stream().map(e -> new int[]{e.effect.ordinal(), e.amplifier, e.duration}).toArray(int[][]::new);
            absorption = player.absorption;
            hotbar = null;
            selected = player.inventory.selected;
            health = player.isDead() ? player.maxHealth : player.health;
            food = player.food;
            saturation = player.saturation;
            spawnX = player.spawnX; spawnY = player.spawnY; spawnZ = player.spawnZ;
            spawned = true;
        }

        public static int[][] slots(mc.item.ItemStack[] slots) {
            int[][] out = new int[slots.length][];
            for (int i = 0; i < slots.length; i++) {
                mc.item.ItemStack s = slots[i];
                if (!mc.item.ItemStack.isEmpty(s)) out[i] = s.toArray();
            }
            return out;
        }

        public static Level load(Path file) {
            try {
                if (Files.exists(file)) return GSON.fromJson(Files.readString(file), Level.class);
            } catch (Exception e) {
                System.err.println("Could not read level: " + e);
            }
            return null;
        }

        public void save(Path file) {
            try {
                Files.writeString(file, GSON.toJson(this));
            } catch (IOException e) {
                System.err.println("Could not save level: " + e);
            }
        }
    }
}
