package net.neoforged.neoforge.capabilities;

import net.minecraft.resources.ResourceLocation;

public final class EntityCapability<T, C> {
    private final ResourceLocation name;

    private EntityCapability(ResourceLocation name) { this.name = name; }

    public static <T, C> EntityCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) { return new EntityCapability<>(name); }

    public ResourceLocation name() { return name; }
}
