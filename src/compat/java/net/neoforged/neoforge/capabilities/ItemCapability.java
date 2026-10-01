package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

public final class ItemCapability<T, C> {
    private final ResourceLocation name;

    private ItemCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> ItemCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new ItemCapability<>(name); }

    public ResourceLocation name() { return name; }
}
