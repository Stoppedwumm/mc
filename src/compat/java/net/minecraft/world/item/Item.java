package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** A mod's item; reamc creates an engine item for it when it is registered (reamc-compat). */
public class Item implements ItemLike {
    final Properties properties;
    /** The engine item this stands for (set when registered). */
    public mc.item.Item reamc$item;
    String descriptionId;

    public Item(Properties properties) { this.properties = properties; }

    @Override public Item asItem() { return this; }

    public Properties reamc$properties() { return properties; }

    public String getDescriptionId() {
        if (descriptionId == null) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this);
            descriptionId = id == null ? "item.unregistered" : id.toLanguageKey("item");
        }
        return descriptionId;
    }

    public Component getDescription() { return Component.translatable(getDescriptionId()); }
    public Component getName(ItemStack stack) { return getDescription(); }
    public ItemStack getDefaultInstance() { return new ItemStack(this); }
    public int getMaxStackSize() { return properties.maxStack; }
    public int getMaxDamage() { return properties.maxDamage; }
    public boolean canBeDepleted() { return properties.maxDamage > 0; }
    public int getEnchantmentValue() { return 0; }

    @Override public String toString() { return String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this)); }

    /** Item settings (reamc-compat keeps those the engine can use). */
    public static class Properties {
        int maxStack = 64, maxDamage;
        Rarity rarity = Rarity.COMMON;
        net.minecraft.world.item.component.ItemAttributeModifiers attributes;
        FoodProperties food;
        boolean fireResistant;
        Item craftRemainder;

        public Properties stacksTo(int n) { maxStack = n; return this; }
        public Properties durability(int d) { maxDamage = d; maxStack = 1; return this; }
        public Properties rarity(Rarity r) { rarity = r; return this; }
        public Properties attributes(net.minecraft.world.item.component.ItemAttributeModifiers a) { attributes = a; return this; }
        public Properties food(FoodProperties f) { food = f; return this; }
        public Properties fireResistant() { fireResistant = true; return this; }
        public Properties craftRemainder(Item i) { craftRemainder = i; return this; }
        public Properties setNoRepair() { return this; }

        public int reamc$maxStack() { return maxStack; }
        public int reamc$maxDamage() { return maxDamage; }
        public Rarity reamc$rarity() { return rarity; }
        public net.minecraft.world.item.component.ItemAttributeModifiers reamc$attributes() { return attributes; }
        public FoodProperties reamc$food() { return food; }
    }
}
