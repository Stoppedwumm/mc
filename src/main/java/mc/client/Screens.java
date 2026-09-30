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
    private int creativeScroll;
    private final List<Item> creativeItems = new ArrayList<>();

    /** Slot kinds */
    private static final int NORMAL = 0, CRAFT_OUT = 1, FURNACE_OUT = 2, CREATIVE = 3;

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
        return s == Game.Screen.INVENTORY || s == Game.Screen.CRAFTING || s == Game.Screen.FURNACE || s == Game.Screen.CHEST || s == Game.Screen.CREATIVE;
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
        if (!ItemStack.isEmpty(cursor)) giveOrDrop(cursor);
        cursor = null;
        furnace = null;
        chest = null;
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
            if (it == Item.WATER_BUCKET || it == Item.LAVA_BUCKET) craft[i] = new ItemStack(Item.BUCKET, 1);
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
            if (screen == Game.Screen.CHEST) {
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
        }
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
            default -> "";
        };
        gui.text(title, px + (screen == Game.Screen.INVENTORY ? 97 : 8), py + 6, 0xFF404040);
        if (screen != Game.Screen.CREATIVE && screen != Game.Screen.INVENTORY) gui.text("Inventory", px + 8, py + 73, 0xFF404040);
        if (screen == Game.Screen.INVENTORY) {
            // Player preview box
            gui.fill(px + 25, py + 7, 51, 72, 0xFF000000);
            gui.fill(px + 26, py + 8, 49, 70, 0xFF8B8B8B);
            gui.hudIcon(mc.render.ItemTextureGen.HEART, px + 40, py + 30, 20, 20);
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
            String name = hs.item.name;
            if (hs.item.maxDamage > 0) name += " (" + (hs.item.maxDamage - hs.damage) + "/" + hs.item.maxDamage + ")";
            gui.tooltip(name, (float) mx, (float) my);
        }
    }

    // ------------------------------------------------------------------ pause & death

    void renderPause(Gui gui, Input input) {
        Options options = g.options;
        double mx = input.mouseX / gui.scale, my = input.mouseY / gui.scale;
        gui.gradient(0, 0, gui.width, gui.height, 0xA0101010, 0xC0101010);
        gui.centered("Game Menu", gui.width / 2, 30, 0xFFFFFFFF);
        gui.centered("Left-click a setting to change it, right-click to go back", gui.width / 2, 44, 0xFF909090);
        float bw = 200, bh = 20, x = gui.width / 2 - bw / 2;
        float y = gui.height / 4 + 8;
        boolean click = input.clicked(GLFW_MOUSE_BUTTON_LEFT);
        boolean rclick = input.clicked(GLFW_MOUSE_BUTTON_RIGHT);
        boolean any = click || rclick;
        if (gui.button("Back to Game", x, y, bw, bh, mx, my) && click) { g.sound.click(); g.closeScreen(); return; }
        y += 24;
        if (gui.button("Render Distance: " + options.renderDistance + " chunks", x, y, bw, bh, mx, my) && any) {
            g.sound.click();
            int[] steps = {2, 4, 6, 8, 10, 12, 16, 20, 24, 32};
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= options.renderDistance) idx = i;
            options.renderDistance = steps[Math.floorMod(idx + (click ? 1 : -1), steps.length)];
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
        if (gui.button("Game Mode: " + (g.player.creative ? "Creative" : "Survival"), x, y, bw, bh, mx, my) && click) {
            g.sound.click();
            g.setCreative(!g.player.creative);
        }
        y += 36;
        if (gui.button("Save and Quit", x, y, bw, bh, mx, my) && click) {
            g.sound.click();
            g.running = false;
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
        if (gui.button("Save and Quit", x, gui.height / 4 + 84, bw, 20, mx, my) && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            g.running = false;
        }
    }
}
