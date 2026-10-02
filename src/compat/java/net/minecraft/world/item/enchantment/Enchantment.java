package net.minecraft.world.item.enchantment;

import net.minecraft.network.chat.Component;

/** An enchantment (reamc-compat: reamc's own enchantments, by Minecraft id). */
public record Enchantment(Component description, mc.item.Enchantment reamc$engine) {
    public int getMaxLevel() { return reamc$engine == null ? 1 : reamc$engine.maxLevel; }
    public int getMinLevel() { return 1; }

    public static Component getFullname(net.minecraft.core.Holder<Enchantment> e, int level) {
        return Component.literal(e.value().description().getString() + (level > 1 || e.value().getMaxLevel() > 1 ? " " + level : ""));
    }
}
