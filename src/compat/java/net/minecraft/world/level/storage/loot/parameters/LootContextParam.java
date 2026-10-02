package net.minecraft.world.level.storage.loot.parameters;

import net.minecraft.resources.ResourceLocation;

public class LootContextParam<T> {
    private final ResourceLocation name;

    public LootContextParam(ResourceLocation name) { this.name = name; }

    public ResourceLocation getName() { return name; }

    @Override public String toString() { return "<parameter " + name + ">"; }
}
