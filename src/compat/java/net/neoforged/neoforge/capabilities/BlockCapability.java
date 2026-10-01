package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

public final class BlockCapability<T, C> {
    private final ResourceLocation name;

    private BlockCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> BlockCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new BlockCapability<>(name); }

    public static <T> BlockCapability<T, net.minecraft.core.Direction> createSided(ResourceLocation name, Class<T> type) { return new BlockCapability<>(name); }

    public ResourceLocation name() { return name; }
}
