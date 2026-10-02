package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/** Something items can provide, such as an item handler (reamc-compat). */
public final class ItemCapability<T, C> {
    private final ResourceLocation name;
    final Map<Item, ICapabilityProvider<ItemStack, C, T>> providers = new IdentityHashMap<>();

    private ItemCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> ItemCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new ItemCapability<>(name); }

    public static <T> ItemCapability<T, Void> createVoid(ResourceLocation name, Class<T> type) { return create(name, type, Void.class); }

    public ResourceLocation name() { return name; }

    public T reamc$get(ItemStack stack, C context) {
        if (stack.isEmpty()) return null;
        ICapabilityProvider<ItemStack, C, T> p = providers.get(stack.getItem());
        return p == null ? null : p.getCapability(stack, context);
    }
}
