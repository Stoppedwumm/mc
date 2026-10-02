package net.minecraft.resources;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

/** A registry reference or an inline value (reamc-compat). */
public final class RegistryFileCodec {
    private RegistryFileCodec() { }

    public static <E> Codec<Holder<E>> create(ResourceKey<? extends Registry<E>> registry, Codec<E> direct) { return mc.mod.DataCodecs.holder(registry, direct); }

    public static <E> Codec<Holder<E>> create(ResourceKey<? extends Registry<E>> registry, Codec<E> direct, boolean allowInline) { return create(registry, direct); }
}
