package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.List;

/** Extra tooltip lines (reamc-compat). */
public record ItemLore(List<Component> lines, List<Component> styledLines) {
    public static final ItemLore EMPTY = new ItemLore(List.of());
    public static final Codec<ItemLore> CODEC = ComponentSerialization.CODEC.listOf().xmap(ItemLore::new, ItemLore::lines);

    public ItemLore(List<Component> lines) { this(lines, lines); }

    public ItemLore withLineAdded(Component c) {
        List<Component> l = new ArrayList<>(lines);
        l.add(c);
        return new ItemLore(l);
    }
}
