package mc.client;

import mc.entity.Player;
import mc.item.ItemStack;
import mc.render.Gui;
import mc.render.ItemTextureGen;
import mc.world.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11C.GL_RENDERER;
import static org.lwjgl.opengl.GL11C.GL_VERSION;
import static org.lwjgl.opengl.GL11C.glGetString;

/** In-game overlay: crosshair, hotbar, health, hunger, air, debug screen and chat. */
final class Hud {
    private final Game g;
    private final Random random = new Random();
    final List<String> chatLines = new ArrayList<>();
    final List<Long> chatTimes = new ArrayList<>();
    private ItemStack nameShownFor;
    private long nameShownAt;
    private float lastHealth = 20;
    private long healthFlashUntil;

    Hud(Game g) {
        this.g = g;
    }

    void chat(String s) {
        chatLines.add(s);
        chatTimes.add(g.ticks);
        while (chatLines.size() > 50) { chatLines.remove(0); chatTimes.remove(0); }
    }

    void render(Gui gui) {
        Player p = g.player;
        if (g.screen() == Game.Screen.NONE) gui.crosshair();
        float w = 182, h = 22;
        float x = gui.width / 2 - w / 2, y = gui.height - h - 1;
        gui.fill(x, y, w, h, 0xB0101010);
        gui.frame(x, y, w, h, 1, 0xFF5A5A5A);
        for (int i = 0; i < 9; i++) gui.frame(x + 2 + i * 20, y + 1, 20, 20, 1, 0x80404040);
        int sel = p.inventory.selected;
        gui.frame(x - 1 + sel * 20, y - 1, 24, 24, 2, 0xFFF0F0F0);
        for (int i = 0; i < 9; i++) gui.stack(p.inventory.slots[i], x + 3 + i * 20, y + 3);

        if (!p.creative) {
            float top = y - 10;
            // Hearts
            if (p.health < lastHealth) healthFlashUntil = g.ticks + 10;
            lastHealth = p.health;
            boolean flash = g.ticks < healthFlashUntil && (g.ticks / 3) % 2 == 0;
            for (int i = 0; i < 10; i++) {
                float hx = x + i * 8, hy = top;
                if (p.health <= 4) hy += random.nextInt(2);
                gui.hudIcon(flash ? ItemTextureGen.HEART_HURT : ItemTextureGen.HEART_EMPTY, hx, hy, 9, 9);
                float v = p.health - i * 2;
                if (v >= 2) gui.hudIcon(ItemTextureGen.HEART, hx, hy, 9, 9);
                else if (v >= 1) gui.hudIcon(ItemTextureGen.HEART_HALF, hx, hy, 9, 9);
            }
            // Hunger (right to left)
            for (int i = 0; i < 10; i++) {
                float fx = x + w - 9 - i * 8, fy = top;
                if (p.saturation <= 0 && g.ticks % (p.food * 3 + 1) == 0) fy += random.nextInt(3) - 1;
                int v = p.food - i * 2;
                gui.hudIcon(v >= 2 ? ItemTextureGen.FOOD : v >= 1 ? ItemTextureGen.FOOD_HALF : ItemTextureGen.FOOD_EMPTY, fx, fy, 9, 9);
            }
            // Air
            if (p.air < 300 && p.eyeInBlock(Block.WATER.id)) {
                int full = (int) Math.ceil((p.air - 2) * 10.0 / 300), partial = (int) Math.ceil(p.air * 10.0 / 300) - full;
                for (int i = 0; i < full + partial; i++)
                    gui.hudIcon(i < full ? ItemTextureGen.BUBBLE : ItemTextureGen.BUBBLE_POP, x + w - 9 - i * 8, top - 10, 9, 9);
            }
        }
        // Held item name
        ItemStack held = p.inventory.held();
        if (held != nameShownFor) { nameShownFor = held; nameShownAt = g.ticks; }
        long age = g.ticks - nameShownAt;
        if (!ItemStack.isEmpty(held) && age < 50) {
            int a = (int) (Math.min(1, (50 - age) / 10f) * 255);
            gui.centered(held.item.name, gui.width / 2, y - (p.creative ? 12 : 34), a << 24 | 0xFFFFFF);
        }
        if (g.showDebug) renderDebug(gui);
        renderChat(gui);
    }

    private void renderDebug(Gui gui) {
        Player p = g.player;
        List<String> left = new ArrayList<>();
        left.add("Minecraft-like 2.0 (LWJGL " + org.lwjgl.Version.getVersion() + ")");
        left.add(g.fps + " fps  C: " + g.renderer.renderedChunks + "/" + g.world.loadedChunkCount() + "  jobs: " + g.world.pendingJobs());
        left.add("E: " + g.world.entities().size() + "  P: " + g.particles.size() + String.format("  exposure %.2f", g.post.exposure));
        left.add("");
        left.add(String.format("XYZ: %.3f / %.5f / %.3f", p.x, p.y, p.z));
        int bx = (int) Math.floor(p.x), by = (int) Math.floor(p.y), bz = (int) Math.floor(p.z);
        left.add("Block: " + bx + " " + by + " " + bz);
        left.add("Chunk: " + (bx & 15) + " " + (by & 15) + " " + (bz & 15) + " in " + (bx >> 4) + " " + (by >> 4) + " " + (bz >> 4));
        String[] dirs = {"south (Towards positive Z)", "west (Towards negative X)", "north (Towards negative Z)", "east (Towards positive X)"};
        int facing = Math.floorMod(Math.round(p.yaw / 90f), 4);
        left.add("Facing: " + dirs[facing] + String.format(" (%.1f / %.1f)", wrap(p.yaw), p.pitch));
        left.add("Light: " + g.world.getSkyLight(bx, (int) Math.floor(p.eyeY()), bz) + " sky, " + g.world.getBlockLight(bx, (int) Math.floor(p.eyeY()), bz) + " block");
        left.add("Biome: " + g.world.biomeAt(bx, bz).displayName);
        long day = g.world.time / 24000, tod = g.world.time % 24000;
        left.add("Day " + day + ", time " + tod + (g.weather.raining ? " (raining)" : ""));
        left.add(String.format("Health %.1f  Food %d  Sat %.1f  Air %d", p.health, p.food, p.saturation, p.air));
        var hit = g.interaction.hit;
        if (hit != null) {
            left.add("");
            left.add("Targeted Block: " + hit.x + ", " + hit.y + ", " + hit.z);
            left.add(Block.get(g.world.getBlock(hit.x, hit.y, hit.z)).name + " [meta " + g.world.getMeta(hit.x, hit.y, hit.z) + "]");
        }
        if (g.interaction.targetEntity != null) {
            left.add("");
            left.add("Targeted Entity: " + g.interaction.targetEntity.getClass().getSimpleName()
                    + String.format(" (%.1f hp)", g.interaction.targetEntity.health));
        }
        float yy = 2;
        for (String s : left) {
            if (!s.isEmpty()) {
                gui.fill(1, yy - 1, gui.textWidth(s) + 2, 10, 0x90505050);
                gui.text(s, 2, yy, 0xFFE0E0E0);
            }
            yy += 10;
        }
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
        String[] right = {
                "Java: " + System.getProperty("java.version"),
                "Mem: " + (used * 100 / max) + "% " + used + "/" + max + "MB",
                "",
                "Display: " + g.window.width + "x" + g.window.height,
                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),
        };
        yy = 2;
        for (String s : right) {
            if (s != null && !s.isEmpty()) {
                float tw = gui.textWidth(s);
                gui.fill(gui.width - tw - 3, yy - 1, tw + 2, 10, 0x90505050);
                gui.text(s, gui.width - tw - 2, yy, 0xFFE0E0E0);
            }
            yy += 10;
        }
    }

    private static float wrap(float yaw) {
        float y = yaw % 360;
        if (y >= 180) y -= 360;
        if (y < -180) y += 360;
        return y;
    }

    private void renderChat(Gui gui) {
        boolean open = g.screen() == Game.Screen.CHAT;
        float y = gui.height - 58;
        int shown = 0;
        for (int i = chatLines.size() - 1; i >= 0 && shown < (open ? 20 : 10); i--) {
            long age = g.ticks - chatTimes.get(i);
            if (!open && age > 200) continue;
            float alpha = open ? 1 : Math.min(1, (200 - age) / 20f);
            int a = (int) (alpha * 255);
            gui.fill(2, y - 1, Math.max(gui.textWidth(chatLines.get(i)) + 4, 1), 10, (int) (alpha * 0x80) << 24);
            gui.text(chatLines.get(i), 4, y, (a << 24) | 0xFFFFFF);
            y -= 10;
            shown++;
        }
        if (open) {
            gui.fill(2, gui.height - 14, gui.width - 4, 12, 0x80000000);
            String caret = (g.ticks / 6) % 2 == 0 ? "_" : "";
            gui.text(g.chatInput + caret, 4, gui.height - 12, 0xFFFFFFFF);
        }
    }
}
