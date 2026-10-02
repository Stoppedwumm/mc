package net.neoforged.neoforge.common.extensions;

/** NeoForge's ItemStack additions (reamc-compat: implemented on ItemStack itself). */
public interface IItemStackExtension {
    int getEnchantmentLevel(net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment);
}
