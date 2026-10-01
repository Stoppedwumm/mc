package mc.client;

import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Recipes;
import mc.render.Gui;
import mc.world.Block;
import mc.world.BlockEntity;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/** Container GUIs (inventory, crafting table, furnace, chest, creative), pause menu and death screen. */
final class Screens {
    private final Game g;
    ItemStack cursor;
    private final ItemStack[] craft = new ItemStack[9];
    private final ItemStack[] result = new ItemStack[1];
    private int craftSize = 2;
    private BlockEntity.Furnace furnace;
    private BlockEntity.Chest chest;
    int creativeScroll;
    private final List<Item> creativeItems = new ArrayList<>();

    /** Slot kinds */
    private static final int NORMAL = 0, CRAFT_OUT = 1, FURNACE_OUT = 2, CREATIVE = 3, ARMOR = 4, ANVIL_OUT = 5;

    private record Slot(ItemStack[] arr, int index, float x, float y, int kind, int group) { }

    Screens(Game g) {
        this.g = g;
        for (Item i : Item.BY_ID) {
            if (i == null) continue;
            if (i.isBlock() && !i.block.inCreativeInventory) continue;
            creativeItems.add(i);
        }
    }

    boolean isContainer(Game.Screen s) {
        return s == Game.Screen.INVENTORY || s == Game.Screen.CRAFTING || s == Game.Screen.FURNACE || s == Game.Screen.CHEST
                || s == Game.Screen.CREATIVE || s == Game.Screen.TRADING || s == Game.Screen.ENCHANTING || s == Game.Screen.ANVIL
                || s == Game.Screen.BREWING;
    }

    void openInventory() {
        craftSize = 2;
        g.setScreen(g.player.creative ? Game.Screen.CREATIVE : Game.Screen.INVENTORY);
    }

    void openCrafting() {
        craftSize = 3;
        g.setScreen(Game.Screen.CRAFTING);
    }

    void openFurnace(BlockEntity.Furnace f) {
        furnace = f;
        g.setScreen(Game.Screen.FURNACE);
    }

    private mc.entity.Mob trader;
    private List<mc.item.Trades.Offer> offers = List.of();
    private int offerScroll;

    void openTrading(mc.entity.Mob villager) {
        trader = villager;
        offers = mc.item.Trades.offers(villager.profession);
        offerScroll = 0;
        g.setScreen(Game.Screen.TRADING);
        g.sound.play("villager_say", villager.x, villager.y + 1.5, villager.z, 1, 1);
    }

    /** Offer grid in the top part of the trading screen: 2 columns x 3 rows. */
    private void renderOffers(Gui gui, Input input, float px, float py, double mx, double my) {
        var inv = g.player.inventory;
        int pages = (offers.size() + 1) / 2 - 2;
        if (input.scroll != 0) offerScroll = Math.max(0, Math.min(Math.max(0, pages), offerScroll - (int) Math.signum(input.scroll)));
        for (int k = 0; k < 6; k++) {
            int i = k + offerScroll * 2;
            if (i >= offers.size()) break;
            var o = offers.get(i);
            float ox = px + 8 + (k % 2) * 82, oy = py + 16 + (k / 2) * 18;
            boolean afford = mc.item.Trades.canAfford(inv, o);
            boolean hover = mx >= ox && mx < ox + 80 && my >= oy && my < oy + 17;
            gui.fill(ox, oy, 80, 17, hover ? 0xFFB0B0B0 : 0xFF9A9A9A);
            gui.frame(ox, oy, 80, 17, 1, afford ? 0xFF404040 : 0xFF7A3030);
            gui.stack(o.cost(), ox + 2, oy + 0.5f);
            if (o.cost2() != null) gui.stack(o.cost2(), ox + 20, oy + 0.5f);
            gui.hudIcon(mc.render.ItemTextureGen.PROGRESS, ox + 40, oy + 4, 14, 9);
            gui.stack(o.result(), ox + 60, oy + 0.5f);
            if (!afford) gui.fill(ox + 1, oy + 1, 78, 15, 0x60501010);
            if (hover && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
                input.consumeClick(GLFW_MOUSE_BUTTON_LEFT);
                ItemStack r = mc.item.Trades.trade(inv, o);
                if (r == null) {
                    g.sound.play("villager_no", trader.x, trader.y + 1.5, trader.z, 1, 1);
                } else {
                    giveOrDrop(r);
                    g.sound.play("villager_yes", trader.x, trader.y + 1.5, trader.z, 1, 1);
                    mc.entity.XpOrbEntity.spawn(g.world, trader.x, trader.y + 0.5, trader.z, 1 + g.random.nextInt(3));
                }
            }
        }
    }

    /** Block entity behind the open container screen (chest, furnace or brewing stand), or null. */
    BlockEntity openContainer() {
        return switch (g.screen()) {
            case FURNACE -> furnace;
            case CHEST -> chest;
            case BREWING -> brewing;
            default -> null;
        };
    }

    void openChest(BlockEntity.Chest c) {
        chest = c;
        g.setScreen(Game.Screen.CHEST);
        g.sound.dig(Block.CHEST, c.x + 0.5, c.y + 0.5, c.z + 0.5);
    }

    /** Returns crafting ingredients and the cursor stack to the player when a container closes. */
    void onClose() {
        for (int i = 0; i < 9; i++) {
            if (!ItemStack.isEmpty(craft[i])) giveOrDrop(craft[i]);
            craft[i] = null;
        }
        for (int i = 0; i < 2; i++) {
            if (!ItemStack.isEmpty(enchantSlots[i])) giveOrDrop(enchantSlots[i]);
            if (!ItemStack.isEmpty(anvilSlots[i])) giveOrDrop(anvilSlots[i]);
            enchantSlots[i] = anvilSlots[i] = null;
        }
        if (!ItemStack.isEmpty(cursor)) giveOrDrop(cursor);
        cursor = null;
        furnace = null;
        chest = null;
        brewing = null;
    }

    // ------------------------------------------------------------------ enchanting & anvil

    private final ItemStack[] enchantSlots = new ItemStack[2];
    private final ItemStack[] anvilSlots = new ItemStack[2];
    private final ItemStack[] anvilOut = new ItemStack[1];
    private int bookshelves, anvilCost;
    private final int[] enchantLevels = new int[3];
    private final List<java.util.Map<mc.item.Enchantment, Integer>> previews = new ArrayList<>();

    void openEnchanting(int x, int y, int z) {
        // Bookshelves one block away from the table, with air in between, power the offers (max 15)
        int n = 0;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
                int mx = x + Integer.signum(dx), mz = z + Integer.signum(dz);
                for (int dy = 0; dy <= 1; dy++) {
                    if (g.world.getBlock(mx, y + dy, mz) != 0 && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)) continue;
                    if (g.world.getBlock(x + dx, y + dy, z + dz) == Block.BOOKSHELF.id) n++;
                }
            }
        bookshelves = Math.min(15, n);
        g.setScreen(Game.Screen.ENCHANTING);
    }

    private BlockEntity.BrewingStand brewing;

    public void openBrewing(BlockEntity.BrewingStand b) {
        brewing = b;
        g.setScreen(Game.Screen.BREWING);
    }

    void openAnvil() {
        g.setScreen(Game.Screen.ANVIL);
    }

    /** Recomputes the three enchanting offers for the current item (Minecraft 1.8+ level formula). */
    private void updateEnchantOffers() {
        previews.clear();
        java.util.Arrays.fill(enchantLevels, 0);
        ItemStack it = enchantSlots[0];
        if (ItemStack.isEmpty(it) || it.isEnchanted() || mc.item.Enchantment.enchantability(it.item) <= 0 || it.count != 1) return;
        java.util.Random r = new java.util.Random(g.player.enchantSeed);
        int b = bookshelves;
        int base = r.nextInt(8) + 1 + (b >> 1) + r.nextInt(b + 1);
        enchantLevels[0] = Math.max(base / 3, 1);
        enchantLevels[1] = base * 2 / 3 + 1;
        enchantLevels[2] = Math.max(base, b * 2);
        for (int i = 0; i < 3; i++) previews.add(rollEnchants(it.item, enchantLevels[i], i));
    }

    private java.util.Map<mc.item.Enchantment, Integer> rollEnchants(Item item, int level, int slot) {
        java.util.Map<mc.item.Enchantment, Integer> e = mc.item.Enchantment.roll(item, level, new java.util.Random(g.player.enchantSeed * 31L + slot));
        if (e.isEmpty()) {
            for (mc.item.Enchantment x : mc.item.Enchantment.values()) if (x.canApply(item)) { e.put(x, 1); break; }
        }
        return e;
    }

    private void renderEnchanting(Gui gui, Input input, float px, float py, double mx, double my) {
        updateEnchantOffers();
        var p = g.player;
        int lapis = ItemStack.isEmpty(enchantSlots[1]) ? 0 : enchantSlots[1].count;
        for (int i = 0; i < 3; i++) {
            float ox = px + 60, oy = py + 14 + i * 19;
            boolean has = enchantLevels[i] > 0 && !previews.isEmpty();
            boolean afford = has && (p.creative || (p.xpLevel >= enchantLevels[i] && lapis >= i + 1));
            boolean hover = mx >= ox && mx < ox + 108 && my >= oy && my < oy + 19;
            gui.fill(ox, oy, 108, 19, !has ? 0xFF8A7A6A : afford ? (hover ? 0xFFB09AD0 : 0xFF9A84B8) : 0xFF6A5A6A);
            gui.frame(ox, oy, 108, 19, 1, 0xFF3a2a3a);
            if (!has) continue;
            var first = previews.get(i).entrySet().iterator().next();
            String hint = first.getKey().describe(first.getValue()) + " . . . ?";
            gui.text(hint.length() > 20 ? hint.substring(0, 20) : hint, ox + 3, oy + 2, afford ? 0xFFE8E0FF : 0xFF3a2a3a);
            gui.text((i + 1) + " lapis   " + enchantLevels[i], ox + 3, oy + 10, afford ? 0xFF80FF20 : 0xFF3a3030);
            if (hover && afford && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
                input.consumeClick(GLFW_MOUSE_BUTTON_LEFT);
                ItemStack it = enchantSlots[0];
                java.util.Map<mc.item.Enchantment, Integer> result = rollEnchants(it.item, enchantLevels[i], i);
                ItemStack out = it.item == Item.BOOK ? new ItemStack(Item.ENCHANTED_BOOK, 1) : it.copy();
                for (var e : result.entrySet()) out.enchant(e.getKey(), e.getValue());
                enchantSlots[0] = out;
                if (!p.creative) {
                    p.removeLevels(i + 1);
                    enchantSlots[1].count -= i + 1;
                    if (enchantSlots[1].count <= 0) enchantSlots[1] = null;
                }
                p.enchantSeed = g.random.nextInt();
                g.sound.play("levelup", p.x, p.y + 1, p.z, 0.5f, 1.4f);
                for (int k = 0; k < 20; k++) g.particles.spawn("enchant", p.x + g.random.nextGaussian(), p.y + 1.5 + g.random.nextDouble(), p.z + g.random.nextGaussian());
            }
        }
    }

    private static Item repairMaterial(Item it) {
        if (it.isArmor()) return new Item[]{Item.LEATHER, Item.IRON_INGOT, Item.IRON_INGOT, Item.GOLD_INGOT, Item.DIAMOND}[it.armorMaterial];
        if (!it.isTool()) return null;
        if (it.name.startsWith("Wooden")) return Item.of(Block.PLANKS);
        if (it.name.startsWith("Stone")) return Item.of(Block.COBBLESTONE);
        if (it.name.startsWith("Iron")) return Item.IRON_INGOT;
        if (it.name.startsWith("Golden")) return Item.GOLD_INGOT;
        if (it.name.startsWith("Diamond")) return Item.DIAMOND;
        return null;
    }

    private int anvilUnits;

    /** Anvil result: repair with materials or a second item, and merge enchantments (from books too). */
    private void updateAnvil() {
        anvilOut[0] = null;
        anvilCost = 0;
        anvilUnits = 0;
        ItemStack left = anvilSlots[0], right = anvilSlots[1];
        if (ItemStack.isEmpty(left) || ItemStack.isEmpty(right)) return;
        ItemStack out = left.copy();
        out.count = 1;
        int cost = 0;
        int max = left.item.maxDamage;
        if (repairMaterial(left.item) != null && right.item == repairMaterial(left.item) && max > 0) {
            if (left.damage == 0) return;
            int units = 0;
            while (units < right.count && out.damage > 0) { out.damage = Math.max(0, out.damage - max / 4); units++; }
            cost += units;
            anvilUnits = units;
        } else if (right.item == left.item || right.item == Item.ENCHANTED_BOOK) {
            if (right.item == left.item && max > 0) {
                int remaining = (max - left.damage) + (max - right.damage) + max * 12 / 100;
                int newDamage = Math.max(0, max - remaining);
                if (newDamage < out.damage) { out.damage = newDamage; cost += 2; }
            }
            if (right.enchants != null) {
                for (var e : right.enchants.entrySet()) {
                    mc.item.Enchantment en = e.getKey();
                    if (!en.canApply(left.item) && left.item != Item.ENCHANTED_BOOK) continue;
                    boolean clash = false;
                    if (out.enchants != null) for (mc.item.Enchantment have : out.enchants.keySet()) if (have != en && !en.compatibleWith(have)) clash = true;
                    if (clash) { cost++; continue; }
                    int cur = out.level(en), l = e.getValue();
                    int nl = cur == l ? Math.min(en.maxLevel, l + 1) : Math.max(cur, l);
                    if (nl != cur) {
                        out.enchant(en, nl);
                        cost += nl * (right.item == Item.ENCHANTED_BOOK ? 1 : 2) * Math.max(1, 10 / en.weight);
                    }
                }
            }
        } else return;
        if (cost == 0) return;
        anvilCost = cost;
        anvilOut[0] = out;
    }

    private void renderAnvil(Gui gui, float px, float py) {
        updateAnvil();
        gui.text("+", px + 58, py + 51, 0xFF404040);
        gui.hudIcon(mc.render.ItemTextureGen.PROGRESS, px + 100, py + 48, 22, 15);
        if (anvilCost > 0) {
            boolean tooMuch = anvilCost >= 40 && !g.player.creative;
            boolean afford = g.player.creative || g.player.xpLevel >= anvilCost;
            String t = tooMuch ? "Too Expensive!" : "Enchantment Cost: " + anvilCost;
            gui.text(t, px + 60, py + 70, tooMuch || !afford ? 0xFFFF6060 : 0xFF80FF20);
        }
    }

    private void takeAnvilResult() {
        ItemStack out = anvilOut[0];
        if (out == null || !ItemStack.isEmpty(cursor)) return;
        var p = g.player;
        if (!p.creative && (anvilCost >= 40 || p.xpLevel < anvilCost)) return;
        if (!p.creative) p.removeLevels(anvilCost);
        cursor = out;
        anvilSlots[0] = null;
        if (anvilUnits > 0) {
            anvilSlots[1].count -= anvilUnits;
            if (anvilSlots[1].count <= 0) anvilSlots[1] = null;
        } else anvilSlots[1] = null;
        g.sound.play("anvil", p.x, p.y + 1, p.z, 0.6f, 1);
    }

    private void giveOrDrop(ItemStack s) {
        ItemStack left = g.player.inventory.add(s);
        if (left.count > 0) g.interaction.throwStack(left);
    }

    // ------------------------------------------------------------------ layout

    private float panelX, panelY;

    private List<Slot> layout(Game.Screen screen, float w, float h) {
        List<Slot> slots = new ArrayList<>();
        panelX = (float) Math.floor(w / 2 - 88);
        panelY = (float) Math.floor(h / 2 - 83);
        ItemStack[] inv = g.player.inventory.slots;
        float px = panelX, py = panelY;
        if (screen == Game.Screen.CREATIVE) {
            py = (float) Math.floor(h / 2 - 100);
            panelY = py;
            int cols = 9, rows = 6;
            for (int i = 0; i < cols * rows; i++) {
                int idx = creativeScroll * cols + i;
                if (idx >= creativeItems.size()) break;
                slots.add(new Slot(null, idx, px + 8 + (i % cols) * 18, py + 18 + (float) (i / cols) * 18, CREATIVE, 2));
            }
            for (int i = 0; i < 9; i++) slots.add(new Slot(inv, i, px + 8 + i * 18, py + 142 + 30, NORMAL, 0));
            return slots;
        }
        for (int i = 0; i < 27; i++) slots.add(new Slot(inv, 9 + i, px + 8 + (i % 9) * 18, py + 84 + (float) (i / 9) * 18, NORMAL, 1));
        for (int i = 0; i < 9; i++) slots.add(new Slot(inv, i, px + 8 + i * 18, py + 142, NORMAL, 0));
        switch (screen) {
            case INVENTORY -> {
                for (int i = 0; i < 4; i++) slots.add(new Slot(g.player.inventory.armor, i, px + 8, py + 8 + i * 18, ARMOR, 4));
                for (int i = 0; i < 4; i++) slots.add(new Slot(craft, (i / 2) * 2 + i % 2, px + 98 + (i % 2) * 18, py + 18 + (float) (i / 2) * 18, NORMAL, 3));
                slots.add(new Slot(result, 0, px + 154, py + 28, CRAFT_OUT, 3));
            }
            case CRAFTING -> {
                for (int i = 0; i < 9; i++) slots.add(new Slot(craft, i, px + 30 + (i % 3) * 18, py + 17 + (float) (i / 3) * 18, NORMAL, 3));
                slots.add(new Slot(result, 0, px + 124, py + 35, CRAFT_OUT, 3));
            }
            case FURNACE -> {
                slots.add(new Slot(furnace.slots, 0, px + 56, py + 17, NORMAL, 3));
                slots.add(new Slot(furnace.slots, 1, px + 56, py + 53, NORMAL, 3));
                slots.add(new Slot(furnace.slots, 2, px + 116, py + 35, FURNACE_OUT, 3));
            }
            case CHEST -> {
                for (int i = 0; i < 27; i++) slots.add(new Slot(chest.slots, i, px + 8 + (i % 9) * 18, py + 18 + (float) (i / 9) * 18, NORMAL, 3));
            }
            case BREWING -> {
                slots.add(new Slot(brewing.slots, 0, px + 56, py + 51, NORMAL, 3));
                slots.add(new Slot(brewing.slots, 1, px + 79, py + 58, NORMAL, 3));
                slots.add(new Slot(brewing.slots, 2, px + 102, py + 51, NORMAL, 3));
                slots.add(new Slot(brewing.slots, 3, px + 79, py + 17, NORMAL, 3));
                slots.add(new Slot(brewing.slots, 4, px + 17, py + 17, NORMAL, 3));
            }
            case ENCHANTING -> {
                slots.add(new Slot(enchantSlots, 0, px + 15, py + 47, NORMAL, 3));
                slots.add(new Slot(enchantSlots, 1, px + 35, py + 47, NORMAL, 3));
            }
            case ANVIL -> {
                slots.add(new Slot(anvilSlots, 0, px + 27, py + 47, NORMAL, 3));
                slots.add(new Slot(anvilSlots, 1, px + 76, py + 47, NORMAL, 3));
                slots.add(new Slot(anvilOut, 0, px + 134, py + 47, ANVIL_OUT, 3));
            }
            default -> { }
        }
        return slots;
    }

    private void updateResult() {
        if (craftSize == 2) {
            ItemStack[] grid = {craft[0], craft[1], craft[2], craft[3]};
            result[0] = Recipes.match(grid, 2);
        } else {
            result[0] = Recipes.match(craft, 3);
        }
    }

    private ItemStack get(Slot s) {
        if (s.kind == CREATIVE) return new ItemStack(creativeItems.get(s.index), 1);
        return s.arr[s.index];
    }

    // ------------------------------------------------------------------ interaction

    private void consumeCraft() {
        int n = craftSize == 2 ? 4 : 9;
        for (int i = 0; i < n; i++) {
            if (ItemStack.isEmpty(craft[i])) continue;
            Item it = craft[i].item;
            craft[i].count--;
            if (craft[i].count <= 0) craft[i] = null;
            // Buckets are returned when used as ingredients
            if (it == Item.WATER_BUCKET || it == Item.LAVA_BUCKET || it == Item.MILK_BUCKET) craft[i] = new ItemStack(Item.BUCKET, 1);
        }
    }

    private void click(Slot s, int button, boolean shift) {
        Game.Screen screen = g.screen();
        if (s.kind == CREATIVE) {
            Item it = creativeItems.get(s.index);
            if (!ItemStack.isEmpty(cursor)) { cursor = null; return; }
            if (shift) g.player.inventory.add(new ItemStack(it, it.maxStack));
            else cursor = new ItemStack(it, button == 1 ? 1 : it.maxStack);
            return;
        }
        if (s.kind == CRAFT_OUT) {
            if (ItemStack.isEmpty(result[0])) return;
            if (shift) {
                for (int guard = 0; guard < 64 && !ItemStack.isEmpty(result[0]); guard++) {
                    ItemStack out = result[0].copy();
                    ItemStack left = g.player.inventory.add(out);
                    if (left.count > 0) { giveOrDrop(left); consumeCraft(); break; }
                    consumeCraft();
                    updateResult();
                }
            } else {
                ItemStack out = result[0];
                if (ItemStack.isEmpty(cursor)) cursor = out.copy();
                else if (cursor.canMerge(out) && cursor.count + out.count <= cursor.item.maxStack) cursor.count += out.count;
                else return;
                consumeCraft();
            }
            g.sound.click();
            return;
        }
        if (s.kind == ANVIL_OUT) {
            takeAnvilResult();
            return;
        }
        ItemStack in = s.arr[s.index];
        if (shift) {
            if (ItemStack.isEmpty(in)) return;
            quickMove(s, screen);
            return;
        }
        if (s.kind == FURNACE_OUT) {
            if (ItemStack.isEmpty(in)) return;
            if (ItemStack.isEmpty(cursor)) { cursor = in; s.arr[s.index] = null; }
            else if (cursor.canMerge(in) && cursor.count + in.count <= cursor.item.maxStack) { cursor.count += in.count; s.arr[s.index] = null; }
            else return;
            payFurnaceXp();
            return;
        }
        if (s.kind == ARMOR) {
            // Only the matching armor piece fits
            if (!ItemStack.isEmpty(cursor) && cursor.item.armorSlot != s.index) return;
            s.arr[s.index] = cursor;
            cursor = in;
            if (s.arr[s.index] != null) g.sound.play("armor", g.player.x, g.player.y + 1, g.player.z, 0.6f, 1);
            return;
        }
        if (button == 0) {
            if (ItemStack.isEmpty(cursor)) { cursor = in; s.arr[s.index] = null; }
            else if (ItemStack.isEmpty(in)) { s.arr[s.index] = cursor; cursor = null; }
            else if (in.canMerge(cursor)) {
                int n = Math.min(cursor.count, in.item.maxStack - in.count);
                in.count += n;
                cursor.count -= n;
                if (cursor.count <= 0) cursor = null;
            } else { s.arr[s.index] = cursor; cursor = in; }
        } else {
            if (ItemStack.isEmpty(cursor)) {
                if (ItemStack.isEmpty(in)) return;
                cursor = in.split((in.count + 1) / 2);
                if (in.count <= 0) s.arr[s.index] = null;
            } else if (ItemStack.isEmpty(in)) {
                s.arr[s.index] = cursor.split(1);
                if (cursor.count <= 0) cursor = null;
            } else if (in.canMerge(cursor) && in.count < in.item.maxStack) {
                in.count++;
                if (--cursor.count <= 0) cursor = null;
            }
        }
    }

    private void quickMove(Slot s, Game.Screen screen) {
        ItemStack stack = s.arr[s.index];
        var inv = g.player.inventory;
        if (s.group <= 1) {
            // From the player's inventory into the open container, or between hotbar and main inventory
            if (stack.item.isArmor() && inv.armor[stack.item.armorSlot] == null && (screen == Game.Screen.INVENTORY || screen == Game.Screen.CREATIVE)) {
                inv.armor[stack.item.armorSlot] = stack;
                stack = null;
            } else if (screen == Game.Screen.ENCHANTING) {
                if (stack.item == Item.LAPIS_LAZULI) stack = insert(enchantSlots, 1, 2, stack);
                else if (enchantSlots[0] == null && stack.count == 1) { enchantSlots[0] = stack; stack = null; }
            } else if (screen == Game.Screen.ANVIL) {
                stack = insert(anvilSlots, 0, 2, stack);
            } else if (screen == Game.Screen.BREWING) {
                if (stack.item == Item.POTION || stack.item == Item.SPLASH_POTION) stack = insert(brewing.slots, 0, 3, stack);
                else if (stack.item == Item.BLAZE_POWDER && ItemStack.isEmpty(brewing.slots[4])) stack = insert(brewing.slots, 4, 5, stack);
                else stack = insert(brewing.slots, 3, 4, stack);
            } else if (screen == Game.Screen.CHEST) {
                stack = insert(chest.slots, 0, 27, stack);
            } else if (screen == Game.Screen.FURNACE) {
                if (Recipes.smelting(stack.item) != null) stack = insert(furnace.slots, 0, 1, stack);
                else if (stack.item.fuelTicks > 0) stack = insert(furnace.slots, 1, 2, stack);
            } else {
                stack = s.group == 0 ? insert(inv.slots, 9, 36, stack) : insert(inv.slots, 0, 9, stack);
            }
            s.arr[s.index] = ItemStack.isEmpty(stack) ? null : stack;
        } else {
            ItemStack left = inv.add(stack);
            s.arr[s.index] = left.count > 0 ? left : null;
            if (s.kind == FURNACE_OUT) payFurnaceXp();
        }
    }

    private void payFurnaceXp() {
        if (furnace == null) return;
        int xp = furnace.takeXp();
        if (xp > 0) mc.entity.XpOrbEntity.spawn(g.world, g.player.x, g.player.y + 0.5, g.player.z, xp);
    }

    private static ItemStack insert(ItemStack[] arr, int from, int to, ItemStack stack) {
        for (int i = from; i < to && stack.count > 0; i++)
            if (arr[i] != null && arr[i].canMerge(stack) && arr[i].count < arr[i].item.maxStack) {
                int n = Math.min(stack.count, arr[i].item.maxStack - arr[i].count);
                arr[i].count += n;
                stack.count -= n;
            }
        for (int i = from; i < to && stack.count > 0; i++)
            if (ItemStack.isEmpty(arr[i])) {
                arr[i] = stack.copy();
                stack.count = 0;
            }
        return stack.count > 0 ? stack : null;
    }

    // ------------------------------------------------------------------ rendering

    void renderContainer(Gui gui, Input input) {
        Game.Screen screen = g.screen();
        if (screen == Game.Screen.CRAFTING || screen == Game.Screen.INVENTORY) updateResult();
        double mx = input.mouseX / gui.scale, my = input.mouseY / gui.scale;
        gui.fill(0, 0, gui.width, gui.height, 0x90101010);
        List<Slot> slots = layout(screen, gui.width, gui.height);
        float px = panelX, py = panelY;
        float ph = screen == Game.Screen.CREATIVE ? 196 : 166;
        gui.panel(px, py, 176, ph);
        String title = switch (screen) {
            case INVENTORY -> "Crafting";
            case CRAFTING -> "Crafting";
            case FURNACE -> "Furnace";
            case CHEST -> "Chest";
            case CREATIVE -> "Creative Inventory";
            case TRADING -> trader == null ? "Villager" : mc.entity.Mob.PROFESSIONS[trader.profession];
            case ENCHANTING -> "Enchant";
            case BREWING -> "Brewing Stand";
            case ANVIL -> "Repair & Name";
            default -> "";
        };
        gui.text(title, px + (screen == Game.Screen.INVENTORY ? 97 : 8), py + 6, 0xFF404040);
        if (screen != Game.Screen.CREATIVE && screen != Game.Screen.INVENTORY) gui.text("Inventory", px + 8, py + 73, 0xFF404040);
        if (screen == Game.Screen.INVENTORY) {
            // Player preview box
            gui.fill(px + 25, py + 7, 51, 72, 0xFF000000);
            gui.fill(px + 26, py + 8, 49, 70, 0xFF8B8B8B);
            g.entityRenderer.renderPlayerGui(gui, g.player, px + 51, py + 75, 60, (float) (px + 51 - mx), (float) (py + 25 - my));
            gui.hudIcon(mc.render.ItemTextureGen.PROGRESS, px + 134, py + 29, 16, 16);
        } else if (screen == Game.Screen.CRAFTING) {
            gui.hudIcon(mc.render.ItemTextureGen.PROGRESS, px + 90, py + 35, 22, 16);
        } else if (screen == Game.Screen.FURNACE) {
            float burn = furnace.burnTotal > 0 ? (float) furnace.burnTime / furnace.burnTotal : 0;
            gui.hudIcon(mc.render.ItemTextureGen.FLAME, px + 57, py + 36, 14, 14);
            if (burn <= 0) gui.fill(px + 57, py + 36, 14, 14, 0xC08B8B8B);
            else gui.fill(px + 57, py + 36, 14, 14 * (1 - burn), 0xC0C6C6C6);
            gui.hudIcon(mc.render.ItemTextureGen.PROGRESS, px + 79, py + 34, 24, 17);
            float cook = (float) furnace.cookTime / BlockEntity.Furnace.COOK_TOTAL;
            if (cook > 0) {
                gui.fill(px + 79, py + 40, 24 * cook, 5, 0xFFFFFFFF);
            }
        } else if (screen == Game.Screen.TRADING) {
            renderOffers(gui, input, px, py, mx, my);
        } else if (screen == Game.Screen.ENCHANTING) {
            renderEnchanting(gui, input, px, py, mx, my);
        } else if (screen == Game.Screen.BREWING) {
            // Fuel gauge and brewing progress
            gui.fill(px + 18, py + 38, 16, 4, 0xFF404040);
            gui.fill(px + 18, py + 38, 16 * brewing.fuel / 20f, 4, 0xFFf0a020);
            if (brewing.brewTime > 0) {
                float f = 1 - (float) brewing.brewTime / BlockEntity.BrewingStand.BREW_TOTAL;
                gui.fill(px + 98, py + 17, 5, 27, 0xFF606060);
                gui.fill(px + 98, py + 17, 5, 27 * f, 0xFFFFFFFF);
            }
        } else if (screen == Game.Screen.ANVIL) {
            renderAnvil(gui, px, py);
        } else if (screen == Game.Screen.CREATIVE) {
            int rows = (creativeItems.size() + 8) / 9;
            gui.text("Scroll for more (" + (creativeScroll + 1) + "/" + Math.max(1, rows - 5) + ")", px + 8, py + 128, 0xFF404040);
        }
        Slot hover = null;
        for (Slot s : slots) {
            gui.slot(s.x - 1, s.y - 1);
            if (s.kind == CRAFT_OUT || s.kind == FURNACE_OUT) gui.frame(s.x - 5, s.y - 5, 26, 26, 1, 0xFF8B8B8B);
            gui.stack(get(s), s.x, s.y);
            if (mx >= s.x - 1 && mx < s.x + 17 && my >= s.y - 1 && my < s.y + 17) {
                hover = s;
                gui.fill(s.x, s.y, 16, 16, 0x80FFFFFF);
            }
        }
        // Input
        boolean shift = input.down(GLFW_KEY_LEFT_SHIFT) || input.down(GLFW_KEY_RIGHT_SHIFT);
        if (hover != null) {
            if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) click(hover, 0, shift);
            else if (input.clicked(GLFW_MOUSE_BUTTON_RIGHT)) click(hover, 1, shift);
            for (int k = 0; k < 9; k++) {
                if (!input.pressed(GLFW_KEY_1 + k) || hover.kind == CRAFT_OUT) continue;
                var inv = g.player.inventory.slots;
                if (hover.kind == CREATIVE) { Item it = creativeItems.get(hover.index); inv[k] = new ItemStack(it, it.maxStack); }
                else if (hover.arr != inv || hover.index != k) {
                    ItemStack t = inv[k];
                    inv[k] = hover.arr[hover.index];
                    hover.arr[hover.index] = t;
                }
            }
            if (input.pressed(GLFW_KEY_Q) && hover.kind != CREATIVE && !ItemStack.isEmpty(hover.arr[hover.index])) {
                boolean all = input.down(GLFW_KEY_LEFT_CONTROL);
                ItemStack s = hover.arr[hover.index];
                g.interaction.throwStack(s.split(all ? s.count : 1));
                if (s.count <= 0) hover.arr[hover.index] = null;
            }
        } else if (input.clicked(GLFW_MOUSE_BUTTON_LEFT) || input.clicked(GLFW_MOUSE_BUTTON_RIGHT)) {
            boolean outside = mx < px || mx > px + 176 || my < py || my > py + ph;
            if (outside && !ItemStack.isEmpty(cursor)) {
                boolean one = input.clicked(GLFW_MOUSE_BUTTON_RIGHT);
                g.interaction.throwStack(one ? cursor.split(1) : cursor);
                if (!one || cursor.count <= 0) cursor = null;
            }
        }
        if (screen == Game.Screen.CREATIVE && input.scroll != 0) {
            int rows = (creativeItems.size() + 8) / 9;
            creativeScroll = Math.max(0, Math.min(rows - 6, creativeScroll - (int) Math.signum(input.scroll)));
        }
        g.player.inventory.cleanup();
        if (!ItemStack.isEmpty(cursor)) gui.stack(cursor, (float) mx - 8, (float) my - 8);
        else if (hover != null && !ItemStack.isEmpty(get(hover))) {
            ItemStack hs = get(hover);
            String name = Item.displayName(hs);
            if (hs.item.maxDamage > 0) name += " (" + (hs.item.maxDamage - hs.damage) + "/" + hs.item.maxDamage + ")";
            List<String> lines = new ArrayList<>();
            lines.add(name);
            if (hs.enchants != null) for (var e : hs.enchants.entrySet()) lines.add("§7" + e.getKey().describe(e.getValue()));
            if (hs.item == Item.POTION || hs.item == Item.SPLASH_POTION) lines.add("§7" + mc.item.Potions.describe(hs.damage));
            gui.tooltip(lines, (float) mx, (float) my);
        }
    }

    // ------------------------------------------------------------------ pause & death

    void renderPause(Gui gui, Input input) {
        double mx = input.mouseX / gui.scale, my = input.mouseY / gui.scale;
        gui.gradient(0, 0, gui.width, gui.height, 0xA0101010, 0xC0101010);
        float bw = 200, bh = 20, x = gui.width / 2 - bw / 2;
        // Keep the whole column (200 GUI pixels) on screen at large GUI scales; the title moves up when it's tight
        float y = Math.max(20, Math.min(gui.height / 4 + 8, gui.height - 200 - 4));
        gui.centered("Game Menu", gui.width / 2, Math.min(30, y - 14), 0xFFFFFFFF);
        if (y >= 56) gui.centered("Left-click a setting to change it, right-click to go back", gui.width / 2, 44, 0xFF909090);
        boolean click = input.clicked(GLFW_MOUSE_BUTTON_LEFT);
        boolean rclick = input.clicked(GLFW_MOUSE_BUTTON_RIGHT);
        if (gui.button("Back to Game", x, y, bw, bh, mx, my) && click) { g.sound.click(); g.closeScreen(); return; }
        y += 24;
        settingsButtons(gui, x, y, mx, my, click, rclick);
        y += 120;
        if (!g.isMultiplayer()) {
            if (gui.button("Game Mode: " + (g.player.creative ? "Creative" : "Survival"), x, y, 98, bh, mx, my) && click) {
                g.sound.click();
                g.setCreative(!g.player.creative);
            }
            if (g.lanServer == null) {
                if (gui.button("Open to LAN", x + 102, y, 98, bh, mx, my) && click) {
                    g.sound.click();
                    g.openToLan();
                }
            } else {
                String info = "LAN port " + g.lanServer.port();
                gui.button(info, x + 102, y, 98, bh, -1, -1);
            }
        }
        y += 36;
        if (gui.button(g.isMultiplayer() ? "Disconnect" : "Save and Quit to Title", x, y, bw, bh, mx, my) && click) {
            g.sound.click();
            g.quitToTitle();
        }
    }

    /** Video and control settings shared by the pause menu and the options screen (5 rows, 120 pixels). */
    void settingsButtons(Gui gui, float x, float y, double mx, double my, boolean click, boolean rclick) {
        Options options = g.options;
        float bw = 200, bh = 20;
        boolean any = click || rclick;
        if (gui.button("Render Distance: " + options.renderDistance + " chunks", x, y, bw, bh, mx, my) && any) {
            g.sound.click();
            int[] steps = {2, 4, 6, 8, 10, 12, 16, 20, 24, 32};
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= options.renderDistance) idx = i;
            options.renderDistance = steps[Math.floorMod(idx + (click ? 1 : -1), steps.length)];
            if (g.multiplayer != null) g.multiplayer.sendViewDistance();
        }
        y += 24;
        if (gui.button("FOV: " + (int) options.fov, x, y, 98, bh, mx, my) && any) {
            g.sound.click();
            options.fov += click ? 10 : -10;
            if (options.fov > 110) options.fov = 30;
            if (options.fov < 30) options.fov = 110;
        }
        if (gui.button("Brightness: " + (int) (options.gamma * 100) + "%", x + 102, y, 98, bh, mx, my) && any) {
            g.sound.click();
            options.gamma = Math.round((options.gamma + (click ? 0.25f : -0.25f)) * 4) / 4f;
            if (options.gamma > 1) options.gamma = 0;
            if (options.gamma < 0) options.gamma = 1;
        }
        y += 24;
        if (gui.button("Shadows: " + (options.shadows ? "ON" : "OFF"), x, y, 98, bh, mx, my) && click) {
            g.sound.click();
            options.shadows = !options.shadows;
        }
        if (gui.button("Clouds: " + (options.clouds ? "ON" : "OFF"), x + 102, y, 98, bh, mx, my) && click) {
            g.sound.click();
            options.clouds = !options.clouds;
        }
        y += 24;
        if (gui.button("View Bobbing: " + (options.viewBobbing ? "ON" : "OFF"), x, y, 98, bh, mx, my) && click) {
            g.sound.click();
            options.viewBobbing = !options.viewBobbing;
        }
        if (gui.button("Sensitivity: " + (int) (options.sensitivity * 200) + "%", x + 102, y, 98, bh, mx, my) && any) {
            g.sound.click();
            options.sensitivity = Math.round((options.sensitivity + (click ? 0.1f : -0.1f)) * 10) / 10f;
            if (options.sensitivity > 1) options.sensitivity = 0.1f;
            if (options.sensitivity < 0.1f) options.sensitivity = 1f;
        }
        y += 24;
        String lodLabel = options.lodDistance <= 0 ? "OFF" : options.lodDistance + " chunks";
        if (gui.button("Distant Terrain: " + lodLabel, x, y, bw, bh, mx, my) && any) {
            g.sound.click();
            int[] steps = {0, 32, 48, 64, 96, 128, 192, 256};
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= options.lodDistance) idx = i;
            options.lodDistance = steps[Math.floorMod(idx + (click ? 1 : -1), steps.length)];
        }
    }

    void renderDeath(Gui gui, Input input) {
        double mx = input.mouseX / gui.scale, my = input.mouseY / gui.scale;
        gui.gradient(0, 0, gui.width, gui.height, 0x60500000, 0xA0803030);
        gui.centered("You died!", gui.width / 2, gui.height / 4, 0xFFFFFFFF);
        var cause = g.player.deathCause;
        gui.centered("Player " + (cause == null ? "died" : cause.message), gui.width / 2, gui.height / 4 + 20, 0xFFE0E0E0);
        float bw = 200, x = gui.width / 2 - bw / 2;
        if (gui.button("Respawn", x, gui.height / 4 + 60, bw, 20, mx, my) && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            g.sound.click();
            g.respawn();
        }
        if (gui.button(g.isMultiplayer() ? "Disconnect" : "Title Screen", x, gui.height / 4 + 84, bw, 20, mx, my) && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            g.sound.click();
            g.quitToTitle();
        }
    }
}
