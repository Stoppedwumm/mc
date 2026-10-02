package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** Minecraft's fluid tag keys; their contents come from tag files (reamc-compat, see mc.mod.Tags). */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class FluidTags {
    private FluidTags() { }

    private static TagKey<net.minecraft.world.level.material.Fluid> tag(String ns, String path) { return create(ResourceLocation.fromNamespaceAndPath(ns, path)); }

    public static TagKey<net.minecraft.world.level.material.Fluid> create(ResourceLocation id) { return TagKey.create((net.minecraft.resources.ResourceKey) Registries.FLUID, id); }

    public static final TagKey<net.minecraft.world.level.material.Fluid> WATER = tag("minecraft", "water");
    public static final TagKey<net.minecraft.world.level.material.Fluid> LAVA = tag("minecraft", "lava");
}
