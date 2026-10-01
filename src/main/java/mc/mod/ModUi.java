package mc.mod;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** What mods' client code needs from reamc's GUI (implemented by the game client). */
public interface ModUi {
    mc.entity.Player localPlayer();

    float width();

    float height();

    /** Shows a mod screen (a container screen with its menu). */
    void openScreen(Screen screen);

    void closeScreen();

    void texture(ResourceLocation texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1);

    void fill(float x, float y, float w, float h, int argb);

    void text(String s, float x, float y, int argb, boolean shadow);

    int textWidth(String s);

    void item(mc.item.ItemStack stack, float x, float y);

    void tooltip(List<String> lines, float x, float y);

    void stackTooltip(mc.item.ItemStack stack, float x, float y);

    void chat(String message);
}
